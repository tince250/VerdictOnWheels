import sqlite3, json
from typing import Any, Dict, List, Optional, Tuple

DB_PATH = "judgments.db"

def _ensure_schema(con: sqlite3.Connection) -> None:
    """Create judgments table if it doesn't exist."""
    con.execute("""
        CREATE TABLE IF NOT EXISTS judgments (
          judgment_id TEXT PRIMARY KEY,
          doc     JSON NOT NULL
        );
    """)
    con.execute("""
        CREATE INDEX IF NOT EXISTS ix_judgments_decision_date
        ON judgments (json_extract(doc, '$.decision_date'));
    """)
    con.commit()

def get_connection() -> sqlite3.Connection:
    """Return a connection, ensuring schema exists."""
    con = sqlite3.connect(DB_PATH)
    con.row_factory = sqlite3.Row
    con.execute("PRAGMA journal_mode=WAL;")
    con.execute("PRAGMA synchronous=NORMAL;")
    con.execute("PRAGMA foreign_keys=ON;")
    _ensure_schema(con)
    return con

def insert_judgment(doc: Dict[str, Any]) -> None:
    judgment_id = doc.get("judgment_id")
    if not judgment_id:
        raise ValueError("doc must contain 'judgment_id'")
    with get_connection() as con:
        con.execute("""
            INSERT INTO judgments(judgment_id, doc)
            VALUES (?, json(?))
            ON CONFLICT(judgment_id) DO UPDATE SET doc=excluded.doc;
        """, (judgment_id, json.dumps(doc, ensure_ascii=False)))
        con.commit()

def get_judgment(judgment_id: str) -> Optional[Dict[str, Any]]:
    with get_connection() as con:
        cur = con.execute("SELECT doc FROM judgments WHERE judgment_id=?", (judgment_id.strip(),))
        row = cur.fetchone()
        return json.loads(row["doc"]) if row else None

def delete_judgment(judgment_id: str) -> bool:
    with get_connection() as con:
        cur = con.execute("DELETE FROM judgments WHERE judgment_id=?", (judgment_id,))
        con.commit()
        return cur.rowcount > 0

def list_judgments(limit: int = 50, offset: int = 0) -> List[Dict[str, Any]]:
    with get_connection() as con:
        cur = con.execute("""
            SELECT doc FROM judgments
            ORDER BY json_extract(doc, '$.decision_date') NULLS LAST, judgment_id
            LIMIT ? OFFSET ?;
        """, (limit, offset))
        return [json.loads(r["doc"]) for r in cur.fetchall()]
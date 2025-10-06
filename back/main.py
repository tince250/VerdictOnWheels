from fastapi import APIRouter, HTTPException
from utils import xml_parser, reference_utils
from services import law_service
from services import judgment_service
from typing import Any, Dict

router = APIRouter(prefix="", tags=["Laws and Judgments"])

@router.get("/laws")
def list_laws():
    return law_service.list_laws()

@router.get("/laws/{law_id}")
def get_law(law_id: str):
    return law_service.get_law(law_id)

@router.get("/laws/{law_id}/articles/{article_id}")
def get_article(law_id: str, article_id: str):
    return law_service.get_article(law_id, article_id)

@router.get("/judgments")
def list_judgments():
    return judgment_service.list_judgments()

@router.get("/judgments/{judgment_filename}")
def get_judgment(judgment_filename: str):
    return judgment_service.get_judgment(judgment_filename)

@router.get("/judgments/{judgment_id}/references")
def get_references(judgment_id: str):
    return judgment_service.get_references(judgment_id)

@router.post("/judgments/upsert")
def upsert_judgment(judgment: Dict[str, Any]):
    try:
        judgment_service.insert_judgment(judgment)
        return {"status": "ok", "judgment_id": judgment["judgment_id"]}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
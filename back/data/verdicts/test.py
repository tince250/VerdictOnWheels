import os
import re
import pdfplumber
from lxml import etree
from datetime import datetime

# Paths
data_folder = "data"
xml_folder = "xml"
os.makedirs(xml_folder, exist_ok=True)

# ========== Dates ==========
def parse_date(date_text):
    month_map = {
        "januar":1,"februar":2,"mart":3,"april":4,"maj":5,"jun":6,"jul":7,"avgust":8,
        "septembar":9,"oktobar":10,"novembar":11,"decembar":12,
        "januara":1,"februara":2,"juna":6,"jula":7
    }
    m = re.match(r"(\d{1,2})\.\s*([A-Za-zČĆŽŠĐčćžšđ]+)\s*(\d{4})", date_text)
    if not m:
        return "2024-01-01"
    day, month_txt, year = m.groups()
    month_num = month_map.get(month_txt.lower(), 1)
    return f"{year}-{month_num:02d}-{int(day):02d}"

# ========== References parsing ==========
def split_number_list(s):
    if not s:
        return None
    parts = re.split(r'\s*(?:,|i)\s*', s)
    cleaned = [re.sub(r'\.$', '', x.strip()) for x in parts if x.strip()]
    return cleaned

REF_TOKEN_RE = re.compile(
    r"""
    (?P<full>
        (?:
            (?P<cl_kw>čl(?:an)?\.?)\s*(?P<cl_nums>\d+(?:\s*(?:,|i)\s*\d+)*\.?)
            (?:\s*(?P<st_kw>st(?:av)?\.?)\s*(?P<st_nums>\d+(?:\s*(?:,|i)\s*\d+)*\.?))?
            (?:\s*(?P<t_kw>t(?:ačka)?\.?)\s*(?P<t_nums>\d+(?:\s*(?:,|i)\s*\d+)*\.?))?
        )
        |
        (?:
            (?P<only_st_kw>st(?:av)?\.?)\s*(?P<only_st_nums>\d+(?:\s*(?:,|i)\s*\d+)*\.?)
            (?:\s*(?P<only_t_kw>t(?:ačka)?\.?)\s*(?P<only_t_nums>\d+(?:\s*(?:,|i)\s*\d+)*\.?))?
        )
    )
    """,
    re.IGNORECASE | re.VERBOSE
)

def detect_code_context_around(text, start, end):
    window = 180
    seg = text[max(0, start - window): min(len(text), end + window)].lower()
    if any(x in seg for x in ["kz", "kz-a", "krivični zakonik", "krivičnog zakonika", "krivičnog zakonikacg"]):
        return "criminal_code"
    if any(x in seg for x in ["zkp", "zkp-a", "zakonik krivičnog postupka", "zakonika krivičnog postupka"]):
        return "criminal_procedure_code"
    if any(x in seg for x in ["zobs", "zobs-a", "zakon o bezbjednosti saobraćaja na putevima", "zakona o bezbjednosti saobraćaja na putevima"]):
        return "law_on_road_traffic_safety"
    return "criminal_code"

def enumerate_refs(articles, stavs, tacke, code_tag):
    articles = [int(x) for x in articles] if articles else [None]
    stavs = [int(x) for x in stavs] if stavs else [None]
    tacke = [int(x) for x in tacke] if tacke else [None]

    out = []
    for a in articles:
        for s in stavs:
            for t in tacke:
                parts = []
                if a is not None:
                    parts.append(f"clan{a}")
                if s is not None:
                    parts.append(f"stav{s}")
                if t is not None:
                    parts.append(f"t{t}")
                frag = "_".join(parts)
                href = f"{code_tag}#{frag}"
                showAs = frag.replace("clan", "član ").replace("_stav", " stav ").replace("t_", " tačka ")
                out.append({"href": href, "showAs": showAs})
    return out

def tokenize_text_with_refs(text):
    tokens = []
    idx = 0
    last_article_nums = None
    last_stav_nums = None

    text = re.sub(r"[\x00-\x08\x0B\x0C\x0E-\x1F]", " ", text)

    while True:
        m = REF_TOKEN_RE.search(text, idx)
        if not m:
            if idx < len(text):
                tokens.append({"type": "text", "value": text[idx:]})
            break

        start, end = m.start(), m.end()
        if start > idx:
            tokens.append({"type": "text", "value": text[idx:start]})

        code_tag = detect_code_context_around(text, start, end)

        if m.group("cl_kw"):
            article_nums = split_number_list(m.group("cl_nums"))
            st_nums = split_number_list(m.group("st_nums")) if m.group("st_kw") and m.group("st_nums") else None
            t_nums = split_number_list(m.group("t_nums")) if m.group("t_kw") and m.group("t_nums") else None

            last_article_nums = article_nums or last_article_nums
            last_stav_nums = st_nums if st_nums is not None else last_stav_nums

            items = enumerate_refs(article_nums, st_nums, t_nums, code_tag)
            tokens.append({"type": "refs", "items": items})

        elif m.group("only_st_kw"):
            st_nums = split_number_list(m.group("only_st_nums"))
            t_nums = split_number_list(m.group("only_t_nums")) if m.group("only_t_kw") and m.group("only_t_nums") else None

            article_nums = last_article_nums if last_article_nums else None
            last_stav_nums = st_nums

            items = enumerate_refs(article_nums, st_nums, t_nums, code_tag)
            tokens.append({"type": "refs", "items": items})

        idx = end

    return tokens

# ========== XML builder ==========
def add_section(parent, tag, content):
    sec = etree.SubElement(parent, tag)
    merged_text = " ".join(line.strip() for line in content.splitlines() if line.strip())
    tokens = tokenize_text_with_refs(merged_text)

    p = etree.SubElement(sec, "p")
    last_node = None
    for tok in tokens:
        if tok["type"] == "text":
            if last_node is None:
                p.text = (p.text or "") + tok["value"]
            else:
                last_node.tail = (last_node.tail or "") + tok["value"]
        elif tok["type"] == "refs":
            for refitem in tok["items"]:
                ref_el = etree.Element("ref")
                ref_el.set("href", refitem["href"])
                ref_el.set("showAs", refitem["showAs"])
                p.append(ref_el)
                last_node = ref_el

# ========== Section splitting ==========
def split_sections_judgment(text):
    sections = {}
    
    # Introduction
    intro_match = re.search(r"U\s+IME\s+CRNE\s+GORE", text)
    start_intro = intro_match.start() if intro_match else 0
    
    # PRESUDU section
    presudu_match = re.search(r"P\s*R\s*E\s*S\s*U\s*D\s*U", text)
    start_presudu = presudu_match.start() if presudu_match else len(text)
    
    # Arguments / Obrazloženje
    args_match = re.search(r"O\s*b\s*r\s*a\s*z\s*l\s*o\s*ž\s*e\s*n\s*j\s*e|Obrazloženje", text, re.IGNORECASE)
    start_args = args_match.start() if args_match else len(text)
    
    # Introduction text
    sections["introduction"] = text[start_intro:start_presudu].strip()
    
    presudu_text = text[start_presudu:start_args].strip()
    
    # Split PRESUDU into subsections
    def_pattern = r"(okrivljeni|okriveljeni|o\s*k\s*r\s*i\s*v\s*l\s*j\s*e\s*n[ia]?|optuženi|optužena|o\s*p\s*t\s*u\s*ž\s*e\s*n[ia]?)"
    def_match = re.search(def_pattern, presudu_text, re.IGNORECASE)
    start_def = def_match.start() if def_match else 0
    
    verdict_pattern = r"(K\s*R\s*I\s*V\s*(J\s*E|A\s*J\s*E|A\s*JE|JE)|KRIVA\s*JE|NIJE\s*KRIVA|KIJE\s*KRIV|N\s*I\s*J\s*E\s*K\s*R\s*I\s*V|N\s*I\s*J\s*E\s*K\s*R\s*I\s*V\s*A)"
    verdict_match = re.search(verdict_pattern, presudu_text, re.IGNORECASE)
    start_verdict = verdict_match.start() if verdict_match else len(presudu_text)
    
    reason_pattern = r"(Zato\s*što\s*(se\s*)?je\s*:|što\s*(se\s*)?je\s*:)"
    reason_match = re.search(reason_pattern, presudu_text, re.IGNORECASE)
    start_reason = reason_match.start() if reason_match else len(presudu_text)
    
    punishment_pattern = r"(O\s*S\s*U\s*D\s*U|U\s*S\s*L\s*O\s*V\s*N\s*U\s+O\s*S\s*U\s*D\s*U)"
    punishment_match = re.search(punishment_pattern, presudu_text, re.IGNORECASE)
    start_punishment = punishment_match.start() if punishment_match else len(presudu_text)
    
    sections["defendant"] = presudu_text[start_def:start_verdict].strip()
    sections["verdict"] = presudu_text[start_verdict:start_reason].strip()
    sections["reasoning"] = presudu_text[start_reason:start_punishment].strip()
    sections["punishment"] = presudu_text[start_punishment:].strip()
    
    # Arguments
    sections["arguments"] = text[start_args:].strip()
    
    return sections

# ========== Main ==========
pdf_files = [f for f in os.listdir(data_folder) if f.lower().endswith(".pdf")]

for pdf_file in pdf_files:
    pdf_path = os.path.join(data_folder, pdf_file)

    with pdfplumber.open(pdf_path) as pdf:
        text = "\n".join([page.extract_text() or "" for page in pdf.pages])

    sections_text = split_sections_judgment(text)

    # Extract court, case number, and date from beginning
    court_match = re.search(r"Osnovni\s+Sud\s+u\s+[A-ZČĆŽŠĐa-zčćžšđ]+", text)
    court_name = court_match.group(0).strip() if court_match else "Osnovni Sud"

    case_match = re.search(r"K\s*\d{1,4}/\d{4}", text)
    case_number = case_match.group(0).replace(" ", "_").replace("/", "_") if case_match else "unknown"

    date_match = re.search(r"\d{1,2}\.\s*[A-ZČĆŽŠĐa-zčćžšđ]+\s*\d{4}", text)
    date_str = date_match.group(0).strip() if date_match else "2024-01-01"
    date_iso = parse_date(date_str)

    nsmap = {None: "http://docs.oasis-open.org/legaldocml/ns/akn/3.0"}
    akn = etree.Element("akomaNtoso", nsmap=nsmap)
    judgment = etree.SubElement(akn, "judgment")

    meta = etree.SubElement(judgment, "meta")
    identification = etree.SubElement(meta, "identification", {"source": "#court"})
    FRBRWork = etree.SubElement(identification, "FRBRWork")

    frbr_uri = f"/akn/me/judgment/{court_name.lower().replace(' ', '_')}/{date_iso}/{case_number}"
    etree.SubElement(FRBRWork, "FRBRthis", {"value": frbr_uri})
    etree.SubElement(FRBRWork, "FRBRuri", {"value": frbr_uri})
    etree.SubElement(FRBRWork, "FRBRdate", {"date": date_iso, "name": "Decision"})
    etree.SubElement(FRBRWork, "FRBRauthor", {"href": f"#{court_name.lower().replace(' ', '_')}"})

    body = etree.SubElement(judgment, "judgmentBody")
    add_section(body, "introduction", sections_text.get("introduction", ""))
    add_section(body, "defendant", sections_text.get("defendant", ""))
    add_section(body, "verdict", sections_text.get("verdict", ""))
    add_section(body, "reasoning", sections_text.get("reasoning", ""))
    add_section(body, "punishment", sections_text.get("punishment", ""))
    add_section(body, "arguments", sections_text.get("arguments", ""))

    xml_filename = f"{case_number}.xml"
    xml_path = os.path.join(xml_folder, xml_filename)
    xml_tree = etree.ElementTree(akn)
    xml_tree.write(xml_path, pretty_print=True, xml_declaration=True, encoding="UTF-8")

    print(f"✅ Saved XML for {pdf_file} (court={court_name}, case={case_number}, date={date_iso})")

from utils.xml_parser import parse_judgment_xml
import os

DATA_DIR = "data/verdicts/xml"

def list_judgments():
    #return [parse_judgment_xml(os.path.join(DATA_DIR, f))["meta"] for f in os.listdir(DATA_DIR) if f.endswith(".xml")]
    return None

def get_judgment(judgment_id: str):
    path = os.path.join(DATA_DIR, f"{judgment_id}.xml")
    return parse_judgment_xml(path)

def get_references(judgment_id: str):
    judgment = get_judgment(judgment_id)
    return judgment["references"]

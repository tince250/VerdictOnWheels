from utils.xml_parser import parse_law_xml, parse_article_xml
import os

DATA_DIR = "data/codes/xml"

def list_laws():
    return [parse_law_xml(os.path.join(DATA_DIR, f))["meta"] for f in os.listdir(DATA_DIR) if f.endswith(".xml")]

def get_law(law_id: str):
    path = os.path.join(DATA_DIR, f"{law_id}.xml")
    return parse_law_xml(path)

def get_article(law_id: str, article_id: str):
    path = os.path.join(DATA_DIR, f"{law_id}.xml")
    return parse_article_xml(path, article_id)

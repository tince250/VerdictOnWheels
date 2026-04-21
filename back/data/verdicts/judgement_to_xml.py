import os
import pdfplumber
import lxml.etree as ET
from openai import OpenAI
from dotenv import load_dotenv
from llm.llm_service import prompt_llm_with_preset
from utils.utils import extract_text_from_pdf

VERDICTS_DIR = "../verdicts/data"
OUTPUT_DIR = "xml"
os.makedirs(OUTPUT_DIR, exist_ok=True)


def save_xml(raw_xml, out_path):
    try:
        tree = ET.fromstring(raw_xml.encode("utf-8"))
        xml_bytes = ET.tostring(tree, pretty_print=True, xml_declaration=True, encoding="utf-8")
        with open(out_path, "wb") as f:
            f.write(xml_bytes)
        print(f"[+] Saved {out_path}")
    except ET.XMLSyntaxError as e:
        print(f"[!] Syntax error ({out_path}): {e}")
        with open(out_path.replace(".xml", "_raw.xml"), "w", encoding="utf-8") as f:
            f.write(raw_xml)

def create_xml_from_text(text: str) -> str:
    raw_xml = prompt_llm_with_preset("judgment_to_akn").replace("```xml", "").replace("```", "").strip()
    return raw_xml


def main():
    for file in os.listdir(VERDICTS_DIR):
        if not file.lower().endswith(".pdf"):
            continue

        path = os.path.join(VERDICTS_DIR, file)
        print(f"[*] Working on {file}...")
        text = extract_text_from_pdf(path)
        raw_xml = prompt_llm_with_preset("judgment_to_akn").replace("```xml", "").replace("```", "").strip()

        law_name = os.path.splitext(file)[0]
        out_path = os.path.join(OUTPUT_DIR, f"{law_name}.xml")
        save_xml(raw_xml, out_path)


if __name__ == "__main__":
    main()
import os
import pdfplumber
import lxml.etree as ET
from datetime import date
import json
from groq import Groq

VERDICTS_DIR = "../verdicts/data"
OUTPUT_DIR = "akn_verdicts"
os.makedirs(OUTPUT_DIR, exist_ok=True)

client = Groq(api_key="")

AKN_NS = "http://www.akomantoso.org/2.0"
NSMAP = {None: AKN_NS}


def extract_text_from_pdf(path):
    pages = []
    with pdfplumber.open(path) as pdf:
        for p in pdf.pages:
            txt = p.extract_text() or ""
            pages.append(txt)
    return "\n".join(pages)

def ask_llm_for_annotations(text):
    prompt = f"""
    Izdvoj podatke iz sledeće sudske odluke i vrati ih u ispravnom JSON formatu
    sa ključevima:

    - case_number (string, broj predmeta)
    - parties (lista stranaka, npr. tužilac, tuženi)
    - judges (lista sudija ili veća)
    - organizations (lista organizacija koje se pominju)
    - dates (lista datuma u tekstu)
    - references (lista referenci na zakone, članove, stavove i tačke)
    - decision (string, kratak opis presude suda)

    Posalji samo JSON, bez dodatnog teksta.
    Тekst odluke:
    {text}
    """
    #available_models = client.models.list()
    #rint(available_models)

    resp = client.chat.completions.create(
        model="groq/compound-mini", 
        messages=[{"role": "user", "content": prompt}],
        temperature=0,
        max_tokens=800,
    )
    return resp.choices[0].message.content


def build_akn_judgment(info, law_name="verdict"):
    akn = ET.Element("{%s}akomaNtoso" % AKN_NS, nsmap=NSMAP)
    judgment = ET.SubElement(akn, "judgment", name=law_name)

    # meta
    meta = ET.SubElement(judgment, "meta")
    ident = ET.SubElement(meta, "identification", source="#system")
    work = ET.SubElement(ident, "FRBRWork")
    ET.SubElement(work, "FRBRthis", value=f"/akn/me/judgment/{law_name}/2025/main")
    ET.SubElement(work, "FRBRuri", value=f"/akn/me/judgment/{law_name}/2025")
    ET.SubElement(work, "FRBRdate", date=str(date.today()), name="generation")
    ET.SubElement(work, "FRBRcountry", value="me")

    # body
    body = ET.SubElement(judgment, "body")

    if info.get("case_number"):
        ET.SubElement(body, "caseNumber").text = info["case_number"]

    for p in info.get("parties", []):
        ET.SubElement(body, "party").text = ensure_string(p)

    for j in info.get("judges", []):
        ET.SubElement(body, "judge").text = ensure_string(j)

    for org in info.get("organizations", []):
        ET.SubElement(body, "organization").text = ensure_string(org)

    for d in info.get("dates", []):
        ET.SubElement(body, "date").text = ensure_string(d)

    for ref in info.get("references", []):
        ref_text = ref.get("reference", "") if isinstance(ref, dict) else str(ref)
        ET.SubElement(body, "ref").text = ref_text

    if info.get("decision"):
        ET.SubElement(body, "decision").text = ensure_string(info["decision"])

    return ET.ElementTree(akn)

def ensure_string(value):
    """Ensures the value is a string."""
    if isinstance(value, dict):
        # If it's a dictionary, extract 'name' and 'role' if available
        name = value.get("name", "")
        role = value.get("role", "")
        if name:
            return f"{name} ({role})"  # You can customize this format
        return str(value)  # Fallback if no 'name' is found
    elif isinstance(value, list):
        return ", ".join(str(item) for item in value)  # Join list items into a string
    return str(value)  # If it's already a string, return it as is

def main():
    for file in os.listdir(VERDICTS_DIR):
        if not file.lower().endswith(".pdf"):
            continue
        path = os.path.join(VERDICTS_DIR, file)
        text = extract_text_from_pdf(path)
        print(f"[*] Обрађујем {file}...")
        raw_output = ask_llm_for_annotations(text)
        raw_output = raw_output.replace("```json", " ")
        raw_output = raw_output.replace("```", " ")
        raw_output = raw_output.strip()
        
        try:
            info = json.loads(raw_output)
        except Exception:
            print(f"[!] Није могуће парсирати JSON за {file}, чувам сиров излаз као decision.")
            info = {"decision": raw_output}

        law_name = os.path.splitext(file)[0]
        tree = build_akn_judgment(info, law_name=law_name)
        out_path = os.path.join(OUTPUT_DIR, f"{law_name}.xml")
        tree.write(out_path, encoding="utf-8", xml_declaration=True, pretty_print=True)
        print(f"[+] Сачувано {out_path}")


if __name__ == "__main__":
    main()
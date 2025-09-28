import os
import pdfplumber
import lxml.etree as ET
from openai import OpenAI
from dotenv import load_dotenv

VERDICTS_DIR = "../verdicts/data"
OUTPUT_DIR = "xml"
os.makedirs(OUTPUT_DIR, exist_ok=True)

load_dotenv()
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

def extract_text_from_pdf(path):
    pages = []
    with pdfplumber.open(path) as pdf:
        for p in pdf.pages:
            txt = p.extract_text() or ""
            pages.append(txt)
    return "\n".join(pages)


def ask_llm_for_annotations(text):
    prompt = """
Convert the following Montenegrin/Serbian **judgment (presuda)** into valid **Akoma Ntoso 3.0 XML**.
Follow these rules:

1. **Structure**

   * Root: `<judgment xml:lang="sr-Latn-ME">`.
   * Sections: `<meta>`, `<judgmentBody>` (with `<introduction>`, `<decision>`, `<motivation>`, `<conclusions>`), optional `<attachments>`.

2. **Meta**

   * **FRBR identifiers**: Work, Expression, Manifestation with URIs `/akn/me/judgment/YYYY-MM-DD/COURT_SLUG/CASE_NUMBER`. Add `@sr-Latn` for Expression.
   * **References**:

     * `<TLCOrganization>` for court, `<TLCPerson>` for judge, clerk, prosecutor, defendant, counsel.
     * `<ref>` for every cited law article/paragraph/item, URIs like:

       * KZ CG → `/akn/me/act/data/criminal_code/2025`
       * ZKP → `/akn/me/act/data/criminal_procedure_code/2025`
       * ZOBS → `/akn/me/act/data/law_on_road_traffic_safety/2025`
       * the references for go in format clan1_stav1_t1 
   
   * **Classification**: at least keywords `saobraćaj`, `ugrožavanje javnog saobraćaja`; subjects `/akn/me/subjects/criminal-law`, `/akn/me/subjects/traffic`.
   * **Lifecycle**: `<eventRef type="judgment" date="YYYY-MM-DD"/>`.
   * **Parties**: `<party>` linking roles (`#defendant`, `#prosecutor`, `#defence`) to persons.

3. **Judgment body**

   * **Introduction**: court, case number, parties, date.
   * **Decision**: operative part (acquittal/conviction, costs), with legal refs.
   * **Motivation**: facts, reasoning, all cited laws in `<ref>`.
   * **Conclusions**: publication/appeal info.
   * keep most of the text as is.
    * All of the references should be used in this sections in the middle of the text when they are mentioned.
    * If the law uses "clan u vezi sa clanom" references, use seperate references `clanX_stavY` for each part.
    * If the law uses "clan u vezi sa stavom" references, use seperate references `clan_stavX`and `clan_stavY` for each part.

4. **Facts/attachments (optional)**: add `<doc name="facts-summary">` with structured summary if clear.

5. **Extraction & normalization**

   * Dates → ISO `YYYY-MM-DD`.
   * Keep initials for anonymized names.
   * Normalize law acronyms: `kz`, `zkp`, `zobs`.
   * Do **not invent** content; omit missing sections.

RETURN ONLY THE Akoma Ntoso 3.0 XML, NO EXPLANATIONS.
    """

    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": prompt + "\n\n" + text}],
        temperature=0,
        max_tokens=6000,
    )

    return response.choices[0].message.content.strip()


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


def main():
    for file in os.listdir(VERDICTS_DIR):
        if not file.lower().endswith(".pdf"):
            continue

        path = os.path.join(VERDICTS_DIR, file)
        print(f"[*] Working on {file}...")
        text = extract_text_from_pdf(path)
        raw_xml = ask_llm_for_annotations(text).replace("```xml", "").replace("```", "").strip()

        law_name = os.path.splitext(file)[0]
        out_path = os.path.join(OUTPUT_DIR, f"{law_name}.xml")
        save_xml(raw_xml, out_path)


if __name__ == "__main__":
    main()
import re
import os
import pdfplumber
import xml.etree.ElementTree as ET
from datetime import date

laws_data = {
    "data/criminal_code.pdf": [
        "2", "3", "4", "5", "13", "16", "32", "33", "42", "52", "53", "54", "124", "125", "348", "339", "220"
    ],
    "data/law_on_road_traffic_safety.pdf": [
        "26", "27", "36", "40", "76", "176"
    ],
    "data/criminal_procedure_code.pdf": [
        "226", "227", "229", "230", "239", "301", "370", "372", "373", "374", "458"
    ]
}

clan_pattern = re.compile(r"Član\s+(\d+)")
stav_pattern = re.compile(r"\((\d+)\)")
tacka_pattern = re.compile(r"(\d+)\)")

clan_stav_ref_pattern = re.compile(
    r"\(član\s+(\d+)(?:\s+stav\s+(\d+))?\)", re.IGNORECASE
)
stav_ref_pattern = re.compile(
    r"st\.\s*(\d+)(?:\s*i\s*(\d+))?", re.IGNORECASE
)


def clean_text(text):
    text = re.sub(r"\d+\)", "", str(text))
    return " ".join(text.split())

def insert_references(text, current_clan):
    """Insert <ref> tags for legal references in the text."""

    def replace_clan_stav(match):
        clan_num = match.group(1)
        stav_num = match.group(2)
        if stav_num:
            href = f"#clan{clan_num}_stav{stav_num}"
            display = f"(član {clan_num} stav {stav_num})"
        else:
            href = f"#clan{clanNum}"
            display = f"(član {clan_num})"
        return f'<ref href="{href}">{display}</ref>'

    text = clan_stav_ref_pattern.sub(replace_clan_stav, text)

    def replace_stav(match):
        s1 = match.group(1)
        s2 = match.group(2)
        if s2:
            return (f'<ref href="#clan{current_clan}_stav{s1}">st. {s1}</ref> i '
                    f'<ref href="#clan{current_clan}_stav{s2}">{s2}</ref>')
        else:
            return f'<ref href="#clan{current_clan}_stav{s1}">st. {s1}</ref>'

    text = stav_ref_pattern.sub(replace_stav, text)

    return text

def set_content_with_refs(parent_element, text_with_refs, current_clan, exclude_points=False):
    """
    Insert text and <ref> tags into a content element.
    If exclude_points is True, remove any text that matches point patterns.
    """
    if exclude_points:
        text_with_refs = re.sub(r"\d+\)\s*", "", text_with_refs)
        text_with_refs = re.sub(r"(\d+\)\s*|\(\s*\d+\s*\))", "", text_with_refs)
        text_with_refs = text_with_refs.strip()

    text_processed = insert_references(text_with_refs, current_clan)
    wrapped = f"<root>{text_processed}</root>"
    root_fragment = ET.fromstring(wrapped)
    for node in root_fragment:
        parent_element.append(node)
    if root_fragment.text:
        if parent_element.text:
            parent_element.text += root_fragment.text
        else:
            parent_element.text = root_fragment.text

def parse_law(pdf_file, relevant_articles):
    with pdfplumber.open(pdf_file) as pdf:
        text = "\n".join([page.extract_text() or "" for page in pdf.pages])

    clanovi = re.split(r"(Član\s+\d+)", text)
    results = {}

    for i in range(1, len(clanovi), 2):
        clan_header = clanovi[i]
        clan_content = clanovi[i + 1] if i + 1 < len(clanovi) else ""
        clan_number = re.search(r"\d+", clan_header).group()

        if clan_number not in relevant_articles:
            continue

        stavi = re.split(stav_pattern, clan_content)
        stavovi = {}
        if len(stavi) > 1:
            for j in range(1, len(stavi), 2):
                stav_num = stavi[j]
                stav_text = stavi[j + 1] if j + 1 < len(stavi) else ""

                # Split the text into lines and process each line
                lines = stav_text.split("\n")
                main_text = []
                tacke = {}
                current_tacka_num = None

                for line in lines:
                    line = line.strip()
                    if tacka_pattern.match(line):  # If the line starts with a point (e.g., "1)")
                        match = tacka_pattern.match(line)
                        current_tacka_num = match.group(1)
                        tacka_text = line[len(match.group(0)):].strip()
                        tacke[current_tacka_num] = clean_text(tacka_text)
                    elif current_tacka_num:  # If it's a continuation of the current point
                        tacke[current_tacka_num] += " " + clean_text(line)
                    else:  # Otherwise, it's part of the main paragraph text
                        main_text.append(line)

                stavovi[stav_num] = {
                    "text": clean_text(" ".join(main_text)),
                    "tacke": tacke
                }
        else:
            stavovi["1"] = {"text": clean_text(clan_content), "tacke": {}}

        results[clan_number] = stavovi

    return results

def build_akn_xml(law_name, clanovi):
    akn = ET.Element("akomaNtoso", xmlns="http://www.akomantoso.org/2.0")
    act = ET.SubElement(akn, "act", name=law_name)
    meta = ET.SubElement(act, "meta")

    identification = ET.SubElement(meta, "identification", source="#system")
    frbr_work = ET.SubElement(identification, "FRBRWork")
    ET.SubElement(frbr_work, "FRBRthis", value=f"/akn/me/act/{law_name}/2025/main")
    ET.SubElement(frbr_work, "FRBRuri", value=f"/akn/me/act/{law_name}/2025")
    ET.SubElement(frbr_work, "FRBRdate", date=str(date.today()), name="generation")
    ET.SubElement(frbr_work, "FRBRauthor", href="#parliament", as_="#legislature")
    ET.SubElement(frbr_work, "FRBRcountry", value="me")

    references = ET.SubElement(meta, "references", source="#system")
    ET.SubElement(references, "TLCOrganization", eId="parliament", 
                  showAs="Skupština Crne Gore", 
                  href="/akn/me/organization/parliament")

    body = ET.SubElement(act, "body")

    for clan_num, stavi in clanovi.items():
        article = ET.SubElement(body, "article", eId=f"clan{clan_num}")
        ET.SubElement(article, "num").text = f"Član {clan_num}"
        ET.SubElement(article, "heading").text = "Naziv člana"

        for stav_num, stav_content in stavi.items():
            paragraph = ET.SubElement(article, "paragraph", eId=f"clan{clan_num}_stav{stav_num}")
            ET.SubElement(paragraph, "num").text = f"({stav_num})"
            content_el = ET.SubElement(paragraph, "content")
            set_content_with_refs(content_el, stav_content["text"], clan_num, exclude_points=True)

            for tacka_num, tacka_text in stav_content["tacke"].items():
                point = ET.SubElement(paragraph, "point", eId=f"clan{clan_num}_stav{stav_num}_t{tacka_num}")
                ET.SubElement(point, "num").text = f"{tacka_num})"
                point_content = ET.SubElement(point, "content")
                set_content_with_refs(point_content, tacka_text, clan_num)

    return akn

def main():
    for pdf_file, clan_numbers in laws_data.items():
        law_name = os.path.splitext(pdf_file)[0]
        law_name = law_name.replace("data", "xml").lower()
        print(f"Parsing {pdf_file}...")
        clanovi = parse_law(pdf_file, clan_numbers)
        xml_tree = build_akn_xml(law_name, clanovi)

        tree = ET.ElementTree(xml_tree)
        out_file = f"{law_name}.xml"
        tree.write(out_file, encoding="utf-8", xml_declaration=True)
        print(f"Saved: {out_file}")

if __name__ == "__main__":
    main()
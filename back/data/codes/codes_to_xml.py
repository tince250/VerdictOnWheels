import re
import os
import pdfplumber
import xml.etree.ElementTree as ET
from datetime import date

# --------- CONFIG ----------
PDF_PATH = "data/criminal_procedure_code.pdf"
OUT_XML = "xml/criminal_procedure_code.xml"
# ---------------------------

# Patterns
clan_split_pattern = re.compile(r"(Član\s+\d+)", re.IGNORECASE)
clan_number_pattern = re.compile(r"\d+")
stav_split_pattern = re.compile(r"\((\d+)\)")
point_line_pattern = re.compile(r"^\s*(\d+)\)\s*(.*)$")
point_inline_pattern = re.compile(r"(\d+)\)\s*")

# Patterns for references
ref_clan_full = re.compile(
    r"člana?\s+(\d+)\s+stav\s+(\d+)\s+tač(?:\.|ka)?\s+(\d+)", re.IGNORECASE
)
ref_clan_stav = re.compile(r"člana?\s+(\d+)\s+stav\s+(\d+)", re.IGNORECASE)
ref_clan = re.compile(r"člana?\s+(\d+)", re.IGNORECASE)
ref_stav_tacka_same = re.compile(r"stava\s+(\d+)\s+tač(?:\.|ka)?\s+(\d+)", re.IGNORECASE)
ref_stava_ovog = re.compile(r"stava\s+(\d+)\s+ovog\s+člana", re.IGNORECASE)  # NEW
ref_stav = re.compile(r"stav\s+(\d+)", re.IGNORECASE)
ref_tacka = re.compile(r"tač(?:\.|ka)?\s*(\d+)", re.IGNORECASE)
ref_st_dot = re.compile(r"st\.\s*(\d+)(?:\s*i\s*(\d+))?", re.IGNORECASE)

# -------------------------------------------------
def clean_text(text: str) -> str:
    if text is None:
        return ""
    text = str(text)
    text = text.replace("\r", "\n")
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n\s*\n+", "\n", text)
    return text.strip()

# -------------------------------------------------
def tokenize_with_refs(text: str, current_clan: str):
    tokens = []
    pos = 0
    s = text

    patterns = [
        ("clan_full", ref_clan_full),
        ("clan_stav", ref_clan_stav),
        ("clan", ref_clan),
        ("stav_tacka_same", ref_stav_tacka_same),
        ("stava_ovog", ref_stava_ovog),  # NEW handler
        ("st_dot", ref_st_dot),
        ("stav", ref_stav),
        ("tacka", ref_tacka),
    ]

    while pos < len(s):
        earliest = None
        earliest_kind = None
        earliest_match = None
        for kind, pat in patterns:
            m = pat.search(s, pos)
            if m:
                if earliest is None or m.start() < earliest:
                    earliest = m.start()
                    earliest_match = m
                    earliest_kind = kind
        if earliest is None:
            tokens.append(s[pos:])
            break

        if earliest > pos:
            tokens.append(s[pos:earliest])

        m = earliest_match
        kind = earliest_kind

        if kind == "clan_full":
            clan_num, stav_num, t_num = m.group(1), m.group(2), m.group(3)
            href = f"#clan{clan_num}_stav{stav_num}_t{t_num}"
            display = f"člana {clan_num} stav {stav_num} tač. {t_num}"

        elif kind == "clan_stav":
            clan_num, stav_num = m.group(1), m.group(2)
            href = f"#clan{clan_num}_stav{stav_num}"
            display = f"člana {clan_num} stav {stav_num}"

        elif kind == "clan":
            clan_num = m.group(1)
            href = f"#clan{clan_num}"
            display = f"člana {clan_num}"

        elif kind == "stav_tacka_same":
            stav_num, t_num = m.group(1), m.group(2)
            href = f"#clan{current_clan}_stav{stav_num}_t{t_num}"
            display = f"stava {stav_num} tač. {t_num}"

        elif kind == "stava_ovog":  # NEW
            stav_num = m.group(1)
            href = f"#clan{current_clan}_stav{stav_num}"
            display = f"stava {stav_num}"

        elif kind == "st_dot":
            s1 = m.group(1)
            s2 = m.group(2)
            if s2:
                href1 = f"#clan{current_clan}_stav{s1}"
                href2 = f"#clan{current_clan}_stav{s2}"
                tokens.append(('ref', href1, f"st. {s1}"))
                tokens.append(" i ")
                tokens.append(('ref', href2, f"{s2}"))
                pos = m.end()
                continue
            else:
                href = f"#clan{current_clan}_stav{s1}"
                display = f"st. {s1}"

        elif kind == "stav":
            stav_num = m.group(1)
            href = f"#clan{current_clan}_stav{stav_num}"
            display = f"stav {stav_num}"

        elif kind == "tacka":
            t_num = m.group(1)
            href = f"#clan{current_clan}_t{t_num}"
            display = f"tač. {t_num}"

        else:
            href = ""
            display = m.group(0)

        tokens.append(('ref', href, display))
        pos = m.end()

    return tokens

# -------------------------------------------------
def set_content_with_refs(parent_element: ET.Element, raw_text: str, current_clan: str, exclude_points=False):
    text = clean_text(raw_text)
    if exclude_points:
        text = re.sub(r"\b\d+\)\s*", "", text)
        text = re.sub(r"\(\s*\d+\s*\)", "", text)

    tokens = tokenize_with_refs(text, current_clan)
    parent_element.text = None
    for tok in tokens:
        if isinstance(tok, str):
            if parent_element.text is None:
                parent_element.text = tok
            else:
                last = parent_element[-1] if len(parent_element) else None
                if last is not None:
                    last.tail = (last.tail or "") + tok
                else:
                    parent_element.text = (parent_element.text or "") + tok
        elif isinstance(tok, tuple) and tok[0] == 'ref':
            href, display = tok[1], tok[2]
            ref_el = ET.SubElement(parent_element, "ref", href=href)
            ref_el.text = display
        else:
            piece = str(tok)
            if parent_element.text is None:
                parent_element.text = piece
            else:
                last = parent_element[-1] if len(parent_element) else None
                if last is not None:
                    last.tail = (last.tail or "") + piece
                else:
                    parent_element.text = (parent_element.text or "") + piece

# -------------------------------------------------
def parse_full_law(pdf_file):
    with pdfplumber.open(pdf_file) as pdf:
        pages_text = [page.extract_text() or "" for page in pdf.pages]
    full_text = "\n".join(pages_text)
    full_text = clean_text(full_text)

    parts = clan_split_pattern.split(full_text)
    results = {}

    for i in range(1, len(parts), 2):
        clan_header = parts[i]
        clan_content = parts[i+1] if i+1 < len(parts) else ""
        mnum = clan_number_pattern.search(clan_header)
        if not mnum:
            continue
        clan_num = mnum.group()

        lines = clan_content.strip().split("\n")
        heading_candidates = []
        content_start_index = 0
        for idx, ln in enumerate(lines):
            ln_stripped = ln.strip()
            if ln_stripped == "":
                continue
            if re.match(r"^\(\s*\d+\s*\)", ln_stripped) or re.match(r"^\d+\)", ln_stripped):
                content_start_index = idx
                break
            heading_candidates.append(ln_stripped)
            content_start_index = idx + 1
            if len(" ".join(heading_candidates)) > 3:
                break

        heading = " ".join(heading_candidates).strip()
        remaining_lines = lines[content_start_index:]
        remaining_text = "\n".join(remaining_lines).strip()

        stavi = {}
        if stav_split_pattern.search(remaining_text):
            pieces = stav_split_pattern.split(remaining_text)
            for j in range(1, len(pieces), 2):
                stav_num = pieces[j]
                stav_text = pieces[j+1] if j+1 < len(pieces) else ""
                lines_stav = stav_text.split("\n")
                main_lines = []
                tacke = {}
                current_point = None
                for line in lines_stav:
                    line = line.strip()
                    if not line:
                        continue
                    m_point = point_line_pattern.match(line)
                    if m_point:
                        pnum = m_point.group(1)
                        ptext = m_point.group(2).strip()
                        tacke[pnum] = clean_text(ptext)
                        current_point = pnum
                    else:
                        if current_point is not None:
                            tacke[current_point] += " " + clean_text(line)
                        else:
                            main_lines.append(line)
                stavi[stav_num] = {"text": clean_text(" ".join(main_lines)), "tacke": tacke}
        else:
            lines_all = remaining_text.split("\n")
            main_lines = []
            tacke = {}
            current_point = None
            for line in lines_all:
                line = line.strip()
                if not line:
                    continue
                m_point = point_line_pattern.match(line)
                if m_point:
                    pnum = m_point.group(1)
                    ptext = m_point.group(2).strip()
                    tacke[pnum] = clean_text(ptext)
                    current_point = pnum
                else:
                    if current_point is not None:
                        tacke[current_point] += " " + clean_text(line)
                    else:
                        main_lines.append(line)
            stavi["1"] = {"text": clean_text(" ".join(main_lines)), "tacke": tacke}

        results[clan_num] = {"heading": heading if heading else "Naziv člana", "stavi": stavi}

    return results

# -------------------------------------------------
def build_akn_xml_from_parsed(parsed, law_name):
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

    for clan_num in sorted(parsed.keys(), key=lambda x: int(x)):
        info = parsed[clan_num]
        article = ET.SubElement(body, "article", eId=f"clan{clan_num}")
        ET.SubElement(article, "num").text = f"Član {clan_num}"
        ET.SubElement(article, "heading").text = info.get("heading", "Naziv člana")

        for stav_num in sorted(info["stavi"].keys(), key=lambda x: int(x)):
            stav_content = info["stavi"][stav_num]
            paragraph = ET.SubElement(article, "paragraph", eId=f"clan{clan_num}_stav{stav_num}")
            ET.SubElement(paragraph, "num").text = f"({stav_num})"
            content_el = ET.SubElement(paragraph, "content")
            set_content_with_refs(content_el, stav_content.get("text", ""), clan_num, exclude_points=True)

            for tacka_num in sorted(stav_content.get("tacke", {}).keys(), key=lambda x: int(x)):
                tacka_text = stav_content["tacke"][tacka_num]
                point = ET.SubElement(paragraph, "point", eId=f"clan{clan_num}_stav{stav_num}_t{tacka_num}")
                ET.SubElement(point, "num").text = f"{tacka_num})"
                point_content = ET.SubElement(point, "content")
                set_content_with_refs(point_content, tacka_text, clan_num, exclude_points=False)

    return akn

# -------------------------------------------------
def main(pdf_path=PDF_PATH, out_xml=OUT_XML):
    if not os.path.exists(pdf_path):
        raise FileNotFoundError(f"PDF not found: {pdf_path}")

    print(f"Parsing PDF: {pdf_path}")
    parsed = parse_full_law(pdf_path)
    print(f"Found {len(parsed)} articles (Član).")

    law_name = os.path.splitext(out_xml)[0].replace("\\", "/")
    print("Building Akoma Ntoso XML...")
    akn_tree = build_akn_xml_from_parsed(parsed, law_name)

    def indent(elem, level=0):
        i = "\n" + level*"    "
        if len(elem):
            if not elem.text or not elem.text.strip():
                elem.text = i + "    "
            for child in elem:
                indent(child, level+1)
            if not child.tail or not child.tail.strip():
                child.tail = i
        else:
            if level and (not elem.tail or not elem.tail.strip()):
                elem.tail = i
    indent(akn_tree)

    os.makedirs(os.path.dirname(out_xml), exist_ok=True)
    ET.ElementTree(akn_tree).write(out_xml, encoding="utf-8", xml_declaration=True)
    print(f"Written XML to: {out_xml}")

# -------------------------------------------------
if __name__ == "__main__":
    main()

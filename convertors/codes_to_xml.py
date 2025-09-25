#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import re
import os
from datetime import date
import pdfplumber
import lxml.etree as ET
import spacy

# --- Улаз: PDF фајлови и списак чланова који те интересују ---
laws_data = {
    "../codes/data/criminal_code.pdf": [
        "2", "3", "4", "5", "13", "16", "32", "33", "42", "52", "53", "54", "124", "125", "348", "339", "220"
    ],
    "../codes/data/law_on_road_traffic_safety.pdf": [
        "26", "27", "36", "40", "76", "176"
    ],
    "../codes/data/criminal_procedure_code.pdf": [
        "226", "227", "229", "230", "239", "301", "370", "372", "373", "374", "458"
    ]
}

OUTPUT_DIR = "akn_output"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# --- NLP модел за организације, датуме итд. ---
try:
    nlp = spacy.load("xx_ent_wiki_sm")
except Exception:
    raise SystemExit("Install spaCy model: python -m spacy download xx_ent_wiki_sm")

# --- Регекс шаблони ---
clan_header_re = re.compile(r"(?:Član|Члан|Clan)\s+(\d+)\b", flags=re.IGNORECASE)
stav_re = re.compile(r"\((\d+)\)")
tacka_re = re.compile(r"\b(\d+)\)")

other_law_re = re.compile(r"(?:Закон|Zakon|Law)\s+(?:o|on|about)?\s*[A-ZА-Яa-zčćžšđČĆŽŠĐ\s\-]+")
internal_ref_re = re.compile(r"(?:Član|Члан|чл\.|čl\.)\s*(\d+)(?:\s*(?:stav|став|ст\.|st\.)\s*\(?(\d+)\)?)?",
                             flags=re.IGNORECASE)


# --- Помоћне функције ---
def extract_text_from_pdf(path):
    pages = []
    with pdfplumber.open(path) as pdf:
        for p in pdf.pages:
            txt = p.extract_text() or ""
            pages.append(txt)
    return "\n".join(pages)


def clean_whitespace(s):
    return re.sub(r"\s+", " ", s).strip()


def find_all_clan_sections(full_text):
    pieces = re.split(r"((?:Član|Члан|Clan)\s+\d+\b)", full_text, flags=re.IGNORECASE)
    sections = []
    for i in range(1, len(pieces), 2):
        header = pieces[i]
        content = pieces[i + 1] if i + 1 < len(pieces) else ""
        m = clan_header_re.search(header)
        if m:
            num = m.group(1)
            sections.append((num, header.strip(), content.strip()))
    return sections


def parse_stavovi_i_tacke(clan_content):
    parts = re.split(r"(\(\d+\))", clan_content)
    stavovi = []
    if len(parts) <= 1:
        text = clean_whitespace(clan_content)
        if text:
            stavovi.append({"num": "1", "text": text, "tacke": []})
        return stavovi

    idx = 1
    while idx < len(parts):
        stav_marker = parts[idx]
        num = re.search(r"\((\d+)\)", stav_marker).group(1)
        text = clean_whitespace(parts[idx + 1]) if idx + 1 < len(parts) else ""
        # tacke
        tparts = re.split(r"(\b\d+\))", text)
        tacke = []
        if len(tparts) > 1:
            lead = tparts[0].strip()
            j = 1
            while j < len(tparts):
                tnum = re.match(r"(\d+)\)", tparts[j]).group(1)
                ttext = clean_whitespace(tparts[j + 1]) if j + 1 < len(tparts) else ""
                tacke.append({"num": tnum, "text": ttext})
                j += 2
            text = lead
        stavovi.append({"num": num, "text": text, "tacke": tacke})
        idx += 2
    return stavovi


def analyze_entities(text):
    doc = nlp(text)
    orgs, dates = set(), set()
    for ent in doc.ents:
        if ent.label_ in ("ORG", "GPE"):
            orgs.add(ent.text)
        if ent.label_ == "DATE":
            dates.add(ent.text)
    other_laws = {clean_whitespace(m.group(0)) for m in other_law_re.finditer(text)}
    internal_refs = []
    for m in internal_ref_re.finditer(text):
        clan, stav = m.group(1), m.group(2)
        internal_refs.append((clan, stav, m.group(0)))
    return {"orgs": orgs, "dates": dates, "other_laws": other_laws, "internal_refs": internal_refs}


# --- XML Builder ---
AKN_NS = "http://www.akomantoso.org/2.0"
NSMAP = {None: AKN_NS}


def build_akn_tree(law_name, clan_sections, entities, wanted_clanovi):
    akn = ET.Element("{%s}akomaNtoso" % AKN_NS, nsmap=NSMAP)
    act = ET.SubElement(akn, "act", name=law_name)
    meta = ET.SubElement(act, "meta")

    # identification
    ident = ET.SubElement(meta, "identification", source="#system")
    work = ET.SubElement(ident, "FRBRWork")
    ET.SubElement(work, "FRBRthis", value=f"/akn/me/act/{law_name}/2025/main")
    ET.SubElement(work, "FRBRuri", value=f"/akn/me/act/{law_name}/2025")
    ET.SubElement(work, "FRBRdate", date=str(date.today()), name="generation")
    ET.SubElement(work, "FRBRauthor", href="#parliament", **{"as": "#legislature"})
    ET.SubElement(work, "FRBRcountry", value="me")

    # references
    refs = ET.SubElement(meta, "references", source="#system")
    backslash_char = "\\"
    for i, org in enumerate(sorted(entities["orgs"])):
        ET.SubElement(refs, "TLCOrganization", eId=f"org{i+1}",
                      showAs=org, href=f"/akn/me/org/{re.sub(r'{backslash_char}s+', '_', org)}")
    for j, law in enumerate(sorted(entities["other_laws"])):
        ET.SubElement(refs, "TLCAct", eId=f"law{j+1}",
                      showAs=law, href=f"/akn/me/act/{re.sub(r'{backslash_char}s+','_',law)}")

    # classification
    cls = ET.SubElement(meta, "classification")
    for num, _, _ in clan_sections:
        if num in wanted_clanovi:
            ET.SubElement(cls, "classificationUnit", eId=f"cl_{num}",
                          showAs=f"Član {num}", classifier="articleNumber")

    # annotations
    ann = ET.SubElement(meta, "annotations")
    for k, d in enumerate(sorted(entities["dates"])):
        ET.SubElement(ann, "annotation", eId=f"date{k+1}", type="date").text = d

    # internal refs
    irefs = ET.SubElement(meta, "internalReferences")
    for c, s, raw in entities["internal_refs"]:
        ref = ET.SubElement(irefs, "ref")
        ET.SubElement(ref, "targetClan").text = c
        if s:
            ET.SubElement(ref, "targetStav").text = s
        ET.SubElement(ref, "raw").text = raw

    # body
    body = ET.SubElement(act, "body")
    for num, header, content in clan_sections:
        if num not in wanted_clanovi:
            continue
        art = ET.SubElement(body, "article", eId=f"clan{num}")
        ET.SubElement(art, "num").text = f"Član {num}"
        ET.SubElement(art, "heading").text = header
        stavovi = parse_stavovi_i_tacke(content)
        for stav in stavovi:
            par = ET.SubElement(art, "paragraph", eId=f"clan{num}_stav{stav['num']}")
            ET.SubElement(par, "num").text = f"({stav['num']})"
            ET.SubElement(par, "content").text = stav["text"]
            for tacka in stav["tacke"]:
                pt = ET.SubElement(par, "point", eId=f"clan{num}_stav{stav['num']}_t{tacka['num']}")
                ET.SubElement(pt, "num").text = f"{tacka['num']})"
                ET.SubElement(pt, "content").text = tacka["text"]

    return ET.ElementTree(akn)


# --- Главна функција ---
def main():
    for pdf_file, wanted_clanovi in laws_data.items():
        if not os.path.exists(pdf_file):
            print(f"[!] File missing: {pdf_file}")
            continue
        law_name = os.path.splitext(os.path.basename(pdf_file))[0]
        text = extract_text_from_pdf(pdf_file)
        clan_sections = find_all_clan_sections(text)
        entities = analyze_entities(text)
        tree = build_akn_tree(law_name, clan_sections, entities, wanted_clanovi)
        out = os.path.join(OUTPUT_DIR, f"{law_name}.xml")
        tree.write(out, encoding="utf-8", xml_declaration=True, pretty_print=True)
        print(f"[+] Saved {out}")


if __name__ == "__main__":
    main()

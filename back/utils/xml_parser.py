import os
from typing import Any, Dict
from lxml import etree

NS = {"akn": "http://www.akomantoso.org/2.0"}

def parse_refs(element):
    """
    Extract references (<ref>) from any element (paragraph, content, point).
    """
    refs = []
    for ref in element.findall(".//akn:ref", namespaces=NS):
        href = ref.attrib.get("href", "")
        showAs = ref.text or ref.attrib.get("showAs", "")
        ref_type = "internal" if href.startswith("#") else "external"
        refs.append({
            "href": href,
            "showAs": showAs.strip(),
            "type": ref_type
        })
    return refs

def parse_points(paragraph_element):
    """
    Parse <point> elements inside a paragraph (if any).
    """
    points = []
    for pt in paragraph_element.findall("./akn:point", namespaces=NS):
        text = "".join(pt.itertext()).strip()
        refs = parse_refs(pt)
        points.append({
            "id": pt.attrib.get("eId", ""),
            "text": text,
            "references": refs
        })
    return points

def parse_paragraphs(article_element):
    """
    Parse <paragraph> elements inside an article, including their content and points.
    """
    paragraphs = []
    for p in article_element.findall(".//akn:paragraph", namespaces=NS):
        content_el = p.find("./akn:content", namespaces=NS)
        text = "".join(content_el.itertext()).strip() if content_el is not None else "".join(p.itertext()).strip()
        refs = parse_refs(p)
        points = parse_points(p)
        paragraphs.append({
            "id": p.attrib.get("eId", ""),
            "text": text,
            "references": refs,
            "points": points
        })
    return paragraphs

def parse_law_xml(path):
    """
    Parse an Akoma Ntoso law XML and return JSON-like dict with metadata and articles.
    """
    tree = etree.parse(path)
    root = tree.getroot()

    meta = {}
    meta_el = root.find(".//akn:meta", namespaces=NS)
    if meta_el is not None:
        title_el = meta_el.find(".//akn:FRBRWork/akn:FRBRthis", namespaces=NS)
        meta["title"] = title_el.attrib.get("value") if title_el is not None else "Unknown"

        date_el = meta_el.find(".//akn:FRBRWork/akn:FRBRdate", namespaces=NS)
        meta["date"] = date_el.attrib.get("date") if date_el is not None else ""

    articles = []
    for art in root.findall(".//akn:article", namespaces=NS):
        articles.append({
            "id": art.attrib.get("eId", ""),
            "num": art.findtext("akn:num", default="", namespaces=NS),
            "heading": art.findtext("akn:heading", default="", namespaces=NS),
            "paragraphs": parse_paragraphs(art)
        })

    return {
        "meta": meta,
        "articles": articles
    }
from typing import Dict, Any, List, Optional, Tuple
from lxml import etree

def parse_judgment_xml(xml_data: str) -> Dict[str, Any]:
    """
    Parse Akoma Ntoso judgment XML into a JSON-serializable dict.
    Extracts FRBR metadata, sections, and inline <ref> elements with href/showAs and text offsets.
    """
    parser = etree.XMLParser(remove_blank_text=False)
    root = etree.fromstring(xml_data.encode("utf-8"), parser=parser)

    nsmap = {"a": "http://docs.oasis-open.org/legaldocml/ns/akn/3.0"}

    def _get_attr(node: Optional[etree._Element], attr: str) -> Optional[str]:
        if node is None:
            return None
        return node.get(attr)

    meta = root.find("a:judgment/a:meta", namespaces=nsmap)
    identification = meta.find("a:identification", namespaces=nsmap) if meta is not None else None
    frbr_work = identification.find("a:FRBRWork", namespaces=nsmap) if identification is not None else None

    frbr_this = frbr_work.find("a:FRBRthis", namespaces=nsmap) if frbr_work is not None else None
    frbr_uri = frbr_work.find("a:FRBRuri", namespaces=nsmap) if frbr_work is not None else None
    frbr_date = frbr_work.find("a:FRBRdate", namespaces=nsmap) if frbr_work is not None else None
    frbr_author = frbr_work.find("a:FRBRauthor", namespaces=nsmap) if frbr_work is not None else None

    metadata = {
        "FRBRthis": _get_attr(frbr_this, "value"),
        "FRBRuri": _get_attr(frbr_uri, "value"),
        "FRBRdate": _get_attr(frbr_date, "date"),
        "FRBRdate_name": _get_attr(frbr_date, "name"),
        "FRBRauthor": _get_attr(frbr_author, "href"),
    }

    body = root.find("a:judgment/a:judgmentBody", namespaces=nsmap)

    def extract_section(name: str) -> Dict[str, Any]:
        """
        Extracts plain text and ref list with offsets from a section element.
        Returns dict with:
        - text: str
        - refs: List[ {href, showAs, start, end} ]
        """
        sec_el = None
        if body is not None:
            sec_el = body.find(f"a:{name}", namespaces=nsmap)

        if sec_el is None:
            return {"text": "", "refs": []}

        paragraphs = sec_el.findall(".//a:p", namespaces=nsmap) or []

        section_text_parts: List[str] = []
        section_refs: List[Dict[str, Any]] = []

        current_offset = 0

        def append_text(s: Optional[str]) -> None:
            nonlocal current_offset
            if not s:
                return
            section_text_parts.append(s)
            current_offset += len(s)

        def walk_p(p_el: etree._Element):
            """
            Build text for a single <p>, capturing inline <ref> elements positions.
            """
            nonlocal current_offset

            if section_text_parts and (len("".join(section_text_parts).rstrip()) > 0):
                append_text("\n")

            if p_el.text:
                append_text(p_el.text)

            for child in p_el:
                if child.tag.endswith("ref"):
                    href = child.get("href")
                    showAs = child.get("showAs")
                    display = showAs or ""
                    start = current_offset
                    append_text(display)
                    end = current_offset
                    section_refs.append({
                        "href": href,
                        "showAs": showAs,
                        "start": start,
                        "end": end
                    })

                    if child.tail:
                        append_text(child.tail)
                else:
                    if child.text:
                        append_text(child.text)
                    if child.tail:
                        append_text(child.tail)

        if not paragraphs:
            walk_p(sec_el)
        else:
            for p in paragraphs:
                walk_p(p)

        section_text = "".join(section_text_parts)
        return {"text": section_text, "refs": section_refs}

    sections = {
        "introduction": extract_section("introduction"),
        "defendant": extract_section("defendant"),
        "verdict": extract_section("verdict"),
        "reasoning": extract_section("reasoning"),
        "punishment": extract_section("punishment"),
        "arguments": extract_section("arguments"),
    }

    return {
        "metadata": metadata,
        "sections": sections,
    }


def parse_article_xml(path, article_id): 
    return None
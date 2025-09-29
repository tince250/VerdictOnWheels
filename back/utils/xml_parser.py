import os
from lxml import etree

NS = {"akn": "http://www.akomantoso.org/2.0"}

def parse_refs(element):
    """
    Extract references (<ref>) from any element (paragraph, content, point).
    Distinguishes between internal (#anchor) and external references.
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
def parse_judgment_xml(path): 
    return None


def parse_article_xml(path, article_id): 
    return None
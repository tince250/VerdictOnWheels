def map_href_to_frontend(href: str) -> str:
    parts = href.strip("/").split("/")
    if len(parts) >= 7:
        law_id = parts[5]  
        art_part = parts[7] 
        article_num = art_part.split(".")[0].replace("art_", "")
        paragraph = art_part.split(".")[1].replace("par_", "") if ".par_" in art_part else None
        url = f"/laws/{law_id}/articles/{article_num}"
        if paragraph:
            url += f"?focus=par_{paragraph}"
        return url
    return "#"

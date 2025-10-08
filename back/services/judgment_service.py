import os
from typing import Any, Dict

from fastapi import HTTPException
from utils.xml_parser import parse_judgment_xml
from data.db import insert_judgment

DATA_DIR = "data/verdicts/xml"

def list_judgments():
    #return [parse_judgment_xml(os.path.join(DATA_DIR, f))["meta"] for f in os.listdir(DATA_DIR) if f.endswith(".xml")]
    return None

def get_judgment(judgment_filename: str) -> Dict[str, Any]:
    """
    Fetches the judgment XML file from the `data/xml` folder based on the file name.
    """
    try:
        xml_file_path = os.path.join(DATA_DIR, f"{judgment_filename}.xml")
        
        if not os.path.exists(xml_file_path):
            raise HTTPException(status_code=404, detail=f"Judgment file '{judgment_filename}.xml' not found.")
        
        with open(xml_file_path, "r", encoding="utf-8") as file:
            xml_data = file.read()
        
        return parse_judgment_xml(xml_data)
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error reading or parsing the XML file: {str(e)}")
    
def get_references(judgment_id: str):
    judgment = get_judgment(judgment_id)
    return judgment["references"]

def update_judgment(judgment: Dict[str, Any]):
    insert_judgment(judgment)
    return judgment

from typing import Dict, Any

def get_judgment_metadata(case_number: str) -> Dict[str, Any]:
    """
    Mock function that returns metadata for a given judgment case number.
    """
    # Normally, you'd query a database or parse XML here.
    # For now, we're returning static example data.
    return {
        "id": "J12345",
        "filePath": f"/cases/{case_number}/judgment.xml",
        "court": "Općinski sud u Zagrebu",
        "caseNumber": case_number,
        "judge": "Marko Horvat",
        "prosecutor": "Ivana Perić",
        "defendant": "Petar Petrović",

        # Legal core
        "offense": "Ugrožavanje javnog prometa",
        "verdictType": "Kriv",
        "appliedProvisions": ["Zakon o sigurnosti prometa - čl. 45", "Kazneni zakon - čl. 227"],

        # CBR attributes
        "violationTypes": ["failedToYield", "speeding"],
        "speedKmh": 110.0,
        "speedLimitKmh": 80.0,
        "alcoholLevelPromil": 1.25,
        "roadCondition": "wet, night",
        "injurySeverity": "serious",
        "damageEur": 7500.0,
        "mentalState": "nehat",
        "priorRecord": True,
        "punishmentType": "Uvjetna kazna zatvora",
        "sentenceMonths": 8,
        "accidentOccured": True,
        "roadType": "regional road"
    }

import os
import json
import os
import json
import csv
from datetime import date
from typing import Dict, Any, Optional
from fastapi import HTTPException
from utils.xml_parser import parse_judgment_xml
from data.db import insert_judgment
from dto import GenerateJudgmentDTO
from llm.llm_service import prompt_llm_with_preset
from data.verdicts.judgement_to_xml import create_xml_from_text
from data.verdicts.test import text_to_xml


CSV_PATH = "../server/data/presude.csv"

DATA_DIR = "data/verdicts/xml"

def list_judgments():
    """
    Lists all judgment XML file names in the `data/verdicts/xml` folder,
    excluding the '.xml' extension.
    """
    return [
        os.path.splitext(filename)[0]
        for filename in os.listdir(DATA_DIR)
        if filename.endswith(".xml")
    ]

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

def normalize_case_number(case_number: str) -> str:
    """
    Convert case number from format 'K_23_2000' to 'K 23/2000'
    """
    parts = case_number.split("_")
    if len(parts) == 3:
        return f"{parts[0]} {parts[1]}/{parts[2]}"
    return case_number

def parse_bool(value: str) -> bool:
    return value.strip().lower() in ['true', '1', 'yes', 'da']

def parse_list(value: str) -> list:
    return [v.strip() for v in value.split(',')] if value else []

def parse_float(value: str) -> Optional[float]:
    try:
        return float(value)
    except (ValueError, TypeError):
        return None

def parse_int(value: str) -> Optional[int]:
    try:
        return int(value)
    except (ValueError, TypeError):
        return None

def get_judgment_metadata(case_number: str) -> Optional[Dict[str, Any]]:
    """
    Reads metadata for a given judgment case number from the presude.csv file.
    """
    print("Current working directory:", os.getcwd())
    target_case = normalize_case_number(case_number)

    with open(CSV_PATH, mode='r', encoding='utf-8') as file:
        reader = csv.DictReader(file)
        for row in reader:
            print(row["caseNumber"].strip(), target_case)
            if row["caseNumber"].strip() == target_case:
                return {
                    "id": row["id"],
                    "filePath": f"/cases/{case_number}/judgment.xml",
                    "court": row["court"],
                    "caseNumber": row["caseNumber"],
                    "judge": row["judge"],
                    "prosecutor": row["prosecutor"],
                    "defendant": row["defendant"],
                    "offense": row["offense"],
                    "verdictType": row["verdictType"],
                    "appliedProvisions": parse_list(row["appliedProvisions"]),
                    "violationTypes": parse_list(row["violationTypes"]),
                    "speedKmh": parse_float(row["speedKmh"]),
                    "speedLimitKmh": parse_float(row["speedLimitKmh"]),
                    "alcoholLevelPromil": parse_float(row["alcoholLevelPromil"]),
                    "roadCondition": row["roadCondition"],
                    "injurySeverity": row["injurySeverity"],
                    "damageEur": parse_float(row["damageEur"]),
                    "mentalState": row["mentalState"],
                    "priorRecord": parse_bool(row["priorRecord"]),
                    "punishmentType": row["punishmentType"],
                    "sentenceMonths": parse_int(row["sentenceMonths"]),
                    "accidentOccured": parse_bool(row["isGuilty"]), 
                    "roadType": row.get("roadType", "-")
                }

    return None 

def update_judgment(judgment: Dict[str, Any]):
    insert_judgment(judgment)
    return judgment

def generate_new_judgment(dto: GenerateJudgmentDTO):
    dto.date = date.today().strftime("%d.%m.%Y")
    text = prompt_llm_with_preset("generate_new_judgment", dto.model_dump_json())
    text_to_xml(text)

import os
import json
from llm.llm_service import prompt_llm_with_preset
from utils.utils import extract_text_from_pdf
from data.db import insert_judgment, get_judgment

BASE_DIR = os.path.dirname(__file__)
VERDICTS_DIR = os.path.join(BASE_DIR, "data")        

def main():
    for file in os.listdir(VERDICTS_DIR):
        if not file.lower().endswith(".pdf"):
            continue

        path = os.path.join(VERDICTS_DIR, file)
        print(f"[*] Working on {file}...")
        text = extract_text_from_pdf(path)
        response = prompt_llm_with_preset("judgment_to_db", text).replace("```json", "").replace("```", "").strip()
        
        try:
            response_json = json.loads(response)
        except Exception as ex:
            print("Error trying to parse verdict LLM response to JSON. Error message: " + str(ex))

        insert_judgment(response_json)
        print(get_judgment("K 173/2024")["judgment_id"])
        return

if __name__ == "__main__":
    main()
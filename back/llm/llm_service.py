import os
import yaml
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

BASE_DIR = os.path.dirname(__file__)             
prompt_file = os.path.join(BASE_DIR, "prompts.yaml")

def load_prompts(file_path: str = prompt_file) -> dict:
    """Load prompts from a YAML file and return them as a dictionary."""
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"{file_path} not found")

    with open(file_path, "r", encoding="utf-8") as f:
        prompts = yaml.safe_load(f)
    
    if not isinstance(prompts, dict):
        raise ValueError("YAML file must contain a dictionary of prompts")
    
    return prompts

def get_prompt(name: str, file_path: str = prompt_file) -> str:
    """Retrieve a single prompt by name from the YAML file."""
    prompts = load_prompts(file_path)
    if name not in prompts:
        raise KeyError(f"Prompt '{name}' not found in {file_path}")
    return prompts[name]

def prompt_llm(prompt: str) -> str:
    """Send a prompt to the LLM and return the response."""
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": prompt}],
        temperature=0,
        max_tokens=6000,
    )
    return response.choices[0].message.content.strip()

def prompt_llm_with_preset(prompt_key: str, concat_data: str = "") -> str:
    """Send a prompt to the LLM from the prompts.yaml and return the response."""
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": get_prompt(prompt_key) + concat_data}],
        temperature=0,
        max_tokens=6000,
    )
    return response.choices[0].message.content.strip()

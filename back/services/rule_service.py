import subprocess
import xml.etree.ElementTree as ET
from xml.dom import minidom
import os

BASE_PATH = os.path.abspath("../rule_based/resoner/dr-device/")

def run_script(script_name):
    """
    Run a .bat script and wait for it to finish.
    """
    script_path = os.path.join(BASE_PATH, script_name)
    try:
        print(f"▶ Running {script_name} ...")
        subprocess.run(script_path, check=True, shell=True, cwd=BASE_PATH)
        print(f"✅ {script_name} executed successfully.")
    except subprocess.CalledProcessError as e:
        print(f"❌ Error while running {script_name}: {e}")

def run_clean():
    """Run clean.bat"""
    run_script("clean.bat")

def run_start():
    """Run start.bat"""
    run_script("start.bat")

def create_rdf_file(case_id, attributes, filename="case.rdf"):
    """
    Create RDF/XML file for a legal case.
    """

    # Namespaces
    ns = {
        "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
        "xsd": "http://www.w3.org/2001/XMLSchema#",
        "lc": "http://informatika.ftn.uns.ac.rs/legal-case.rdf#"
    }

    for prefix, uri in ns.items():
        ET.register_namespace(prefix, uri)

    # Root element <rdf:RDF>
    rdf = ET.Element(
        f"{{{ns['rdf']}}}RDF",
        {
            f"xmlns:rdfs": ns["rdfs"],
            f"xmlns:xsd": ns["xsd"]
        }
    )

    # Case element
    case = ET.SubElement(
        rdf, f"{{{ns['lc']}}}case",
        {f"{{{ns['rdf']}}}about": f"{ns['lc']}{case_id}"}
    )

    # Loop through provided attributes
    for key, value in attributes.items():
        elem = ET.SubElement(case, f"{{{ns['lc']}}}{key}")

        if isinstance(value, int):
            elem.set(f"{{{ns['rdf']}}}datatype", ns["xsd"] + "integer")
            elem.text = str(value)
        else:
            elem.text = str(value)

    # Convert to string and pretty print with minidom
    rough_string = ET.tostring(rdf, encoding="utf-8", xml_declaration=True)
    reparsed = minidom.parseString(rough_string)
    pretty_xml = reparsed.toprettyxml(indent="    ")

    # Write to file
    with open(os.path.join(BASE_PATH, "facts.rdf"), "w", encoding="utf-8") as f:
        f.write(pretty_xml)

    print("✅ RDF file" + BASE_PATH+" facts.rdf created (pretty formatted).")


# Example usage
if __name__ == '__main__':
    create_rdf_file(
        case_id="case01",
        attributes={
            "name": "case 01",
            "defendant": "John",
            "speed": 140,
            "allowed_speed": 80,
            "speed_over": 140-80,
            "driving_on": "rural_road",
            "caused_accident": "no"
        },
        filename="case01.rdf"
    )

    run_clean()
    run_start()

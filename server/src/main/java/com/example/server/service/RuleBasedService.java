package com.example.server.service;

import com.example.server.model.CaseDetails;
import com.example.server.model.Judgment;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuleBasedService {

    private static final String BASE_PATH = "C:\\Users\\dusan\\OneDrive\\Desktop\\pravnaInformatika\\VerdictOnWheels\\dr-device\\";


    public Map<String, Object> processJudgment(Judgment judgment) {
        // Convert Judgment to CaseDetails
        CaseDetails caseDetails = convertToCaseDetails(judgment);

        // Generate a unique ID for this case
        String caseId = judgment.getId();

        // Convert to attribute map
        Map<String, Object> attributes = toAttributeMap(caseDetails);

        // Create RDF file
        String filename = "facts-" + caseId + ".rdf";
        createRdfFile(caseId, attributes, filename);

        // Run the processing scripts
        runScript("clean.bat");
        runScript("start.bat");

        // Parse the results
        File exportFile = new File(BASE_PATH, "export.rdf");
        return parseExportFile(exportFile);
    }

    // Helper method to convert Judgment to CaseDetails
    private CaseDetails convertToCaseDetails(Judgment judgment) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseNumber(judgment.getCaseNumber());
        caseDetails.setDefendant(judgment.getDefendant());
        caseDetails.setSpeedKmh(judgment.getSpeedKmh().intValue());
        caseDetails.setSpeedLimitKmh(judgment.getSpeedLimitKmh().intValue());
        caseDetails.setAccidentOccured(judgment.getAccidentOccured() != null &&
                judgment.getAccidentOccured() ? "da" : "ne");

        if (judgment.getDamageEur() != null) {
            caseDetails.setDamageEur(judgment.getDamageEur().toString());
        }

        caseDetails.setInjurySeverity(judgment.getInjurySeverity());
        caseDetails.setRoadType(judgment.getRoadType());

        if (judgment.getAlcoholLevelPromil() != null) {
            caseDetails.setAlcoholLevelPromil(judgment.getAlcoholLevelPromil());
        }

        if (judgment.getSpeedKmh() != null && judgment.getSpeedLimitKmh() != null) {
            caseDetails.setSpeedOver(judgment.getSpeedKmh().intValue(), judgment.getSpeedLimitKmh().intValue());
        }

        return caseDetails;
    }

    public static Map<String, Object> toAttributeMap(CaseDetails caseDetails) {
        Map<String, Object> attributes = new LinkedHashMap<>();

        try {
            for (Field field : caseDetails.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(caseDetails);

                if (value == null) continue;
                if (value instanceof String && ((String) value).trim().isEmpty()) continue;
                if (value instanceof List<?>) continue;

                String key = convertFieldName(field.getName());
                attributes.put(key, value);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error while extracting attributes from CaseDetails", e);
        }

        return attributes;
    }

    private static String convertFieldName(String fieldName) {
        return switch (fieldName) {
            case "caseNumber" -> "name";
            case "defendant" -> "defendant";
            case "speedKmh" -> "speed";
            case "speedLimitKmh" -> "allowed_speed";
            case "accidentOccured" -> "caused_accident";
            case "damageEur" -> "damage_eur";
            case "injurySeverity" -> "injury";
            case "roadType" -> "driving_on";
            case "alcoholLevelPromil" -> "alcohol_level";
            case "speedOver" -> "speed_over";
            default -> fieldName;
        };
    }

    private static void createRdfFile(String caseId, Map<String, Object> attributes, String filename) {
        try {
            String rdfNS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
            String rdfsNS = "http://www.w3.org/2000/01/rdf-schema#";
            String xsdNS = "http://www.w3.org/2001/XMLSchema#";
            String lcNS = "http://informatika.ftn.uns.ac.rs/legal-case.rdf#";

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rdfRoot = doc.createElementNS(rdfNS, "rdf:RDF");
            rdfRoot.setAttribute("xmlns:lc", lcNS);
            rdfRoot.setAttribute("xmlns:rdf", rdfNS);
            rdfRoot.setAttribute("xmlns:rdfs", rdfsNS);
            rdfRoot.setAttribute("xmlns:xsd", xsdNS);
            doc.appendChild(rdfRoot);

            Element caseElem = doc.createElementNS(lcNS, "lc:case");
            caseElem.setAttributeNS(rdfNS, "rdf:about", lcNS + caseId);
            rdfRoot.appendChild(caseElem);

            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                Element elem = doc.createElementNS(lcNS, "lc:" + key);

                if (value instanceof Integer) {
                    elem.setAttributeNS(rdfNS, "rdf:datatype", xsdNS + "integer");
                } else if (value instanceof Double) {
                    elem.setAttributeNS(rdfNS, "rdf:datatype", xsdNS + "double");
                }
                elem.setTextContent(value.toString());
                caseElem.appendChild(elem);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(BASE_PATH, filename));
            transformer.transform(source, result);

            System.out.println("✅ RDF file created successfully: " + new File(BASE_PATH, filename).getAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ Error while creating RDF file: " + e.getMessage());
        }
    }

    private static void runScript(String scriptName) {
        File scriptFile = new File(BASE_PATH, scriptName);
        if (!scriptFile.exists()) {
            System.err.println("⚠ Script not found: " + scriptFile.getAbsolutePath());
            return;
        }

        try {
            System.out.println("▶ Running " + scriptName + " ...");

            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", scriptFile.getAbsolutePath());
            builder.directory(new File(BASE_PATH));
            builder.inheritIO();

            Process process = builder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✅ " + scriptName + " executed successfully.");
            } else {
                System.err.println("❌ " + scriptName + " exited with code " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Error while running script " + scriptName + ": " + e.getMessage());
        }
    }

    public static void ensureRdfFileClosed(File rdfFile) {
        try {
            String content = Files.readString(rdfFile.toPath());
            if (!content.trim().endsWith("</rdf:RDF>")) {
                System.out.println("⚠ RDF file incomplete, adding closing tag...");
                Files.writeString(rdfFile.toPath(), "\n</rdf:RDF>", StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            System.err.println("❌ Error while fixing RDF file: " + e.getMessage());
        }
    }

    public static Map<String, Object> parseExportFile(File rdfFile) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> penalties = new LinkedHashMap<>();
        ensureRdfFileClosed(rdfFile);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(rdfFile);
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName("*");

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;

                String tag = node.getNodeName();
                if (tag.contains(":")) tag = tag.split(":")[1];

                NodeList children = node.getChildNodes();

                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().endsWith("defendant")) {
                        result.put("triggered_rule", tag);
                        result.put("defendant", child.getTextContent().trim());
                    }
                }

                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().endsWith("value")) {
                        penalties.put(tag, child.getTextContent().trim());
                    }
                }
            }

            if (!penalties.isEmpty()) {
                result.putAll(penalties);
            }

        } catch (Exception e) {
            System.err.println("⚠️ RDF parsing failed: " + e.getMessage());
        }

        return result;
    }

    public static void main(String[] args) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseNumber("case01");
        caseDetails.setDefendant("John Doe");
        caseDetails.setSpeedKmh(50);
        caseDetails.setSpeedLimitKmh(60);
        caseDetails.setAccidentOccured("ne");
//        caseDetails.setDamageEur("1500");
        caseDetails.setInjurySeverity("uknown");
        caseDetails.setRoadType("town");
        caseDetails.setAlcoholLevelPromil(0.6);
        caseDetails.setSpeedOver(50, 50);

        Map<String, Object> attributes = toAttributeMap(caseDetails);
        createRdfFile("case01", attributes, "facts.rdf");

        runScript("clean.bat");
        runScript("start.bat");

        attributes.forEach((k, v) -> System.out.println(k + " = " + v));

        File f = new File("C:/Users/dusan/OneDrive/Desktop/pravnaInformatika/VerdictOnWheels/dr-device/export.rdf");
        Map<String, Object> parsed = parseExportFile(f);

        System.out.println("✅ Extracted values:");
        parsed.forEach((k, v) -> System.out.println(k + " = " + v));
    }
}

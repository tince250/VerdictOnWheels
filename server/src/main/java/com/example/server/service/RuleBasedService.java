package com.example.server.service;

import com.example.server.model.CaseDetails;
import com.example.server.model.Judgment;
import com.example.server.model.JudgmentResponse;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuleBasedService {

    private static final String BASE_PATH = "C:\\pravna\\VerdictOnWheels\\dr-device";
    private static final String LC_NS = "http://informatika.ftn.uns.ac.rs/legal-case.rdf#";


    public JudgmentResponse processJudgment(Judgment judgment) {
        judgment.setId(judgment.getCaseNumber().replaceAll("/", "").replaceAll(" ", ""));
        // Convert Judgment to CaseDetails
        CaseDetails caseDetails = convertToCaseDetails(judgment);

        // Generate a unique ID for this case
        String caseId = judgment.getId();

        // Convert to attribute map
        Map<String, Object> attributes = toAttributeMap(caseDetails);

        // Run the processing scripts
        runScript("clean.bat");

        // Create RDF and N-Triples files after cleanup.
        String filename = "facts.rdf";
        createRdfFile(caseId, attributes, filename);
        runScript("start.bat");

        // Parse the results
        File exportFile = new File(BASE_PATH, "export.rdf");

        return createJudgmentResponse(parseExportFile(exportFile));
    }

    public JudgmentResponse createJudgmentResponse(Map<String, Object> ruleResults) {
        String triggeredRule = (String) ruleResults.getOrDefault("triggered_rule", "");
        String normalizedTriggeredRule = normalizeTriggeredRule(triggeredRule);
        List<String> appliedProvisions = new ArrayList<>();
        String description = "";

        switch (normalizedTriggeredRule) {
            case "alcohol_level_violation_lv1_cl_182":
                description = "nedozvoljena kolicina alkohola u krvi";
                appliedProvisions.add("cl. 182 ZOBS");
                break;
            case "alcohol_level_violation_lv1_accident_cl_182":
                description = "alkohol u krvi + nesreca";
                appliedProvisions.add("cl. 319 ZOBS");
                break;

            case "town_road_speed_violation_lv1_cl_36_stav_1":
                description = "prekoracenje brzine u naseljenom mestu (manje)";
                appliedProvisions.add("cl. 36 ZOBS");
                break;
            case "town_road_speed_violation_lv2_cl_36_stav_1":
                description = "prekoracenje brzine u naseljenom mestu (vece)";
                appliedProvisions.add("cl. 36 ZOBS");
                break;
            case "town_road_speed_violation_lv1_accident_cl_36_stav_1":
                description = "prekoracenje brzine u naseljenom mestu sa nesrecom";
                appliedProvisions.add("cl. 36 ZOBS");
                break;
            case "town_road_speed_violation_lv2_accident_cl_36_stav_1":
                description = "teze prekoracenje u naseljenom mestu sa nesrecom";
                appliedProvisions.add("cl. 36 ZOBS");
                break;

            case "town_road_speed_violation_lv1_accident_injury_laka_cl_36_stav_1":
                description = "nesreca sa lakom povredom";
                appliedProvisions.add("cl. 339 KZ");
                break;
            case "town_road_speed_violation_lv1_accident_injury_teska_cl_36_stav_1":
                description = "nesreca sa teskom povredom";
                appliedProvisions.add("cl. 340 KZ");
                break;
            case "town_road_speed_violation_lv1_accident_injury_fatalna_cl_36_stav_1":
                description = "nesreca sa smrtnim ishodom";
                appliedProvisions.add("cl. 340 KZ");
                break;
            case "town_road_speed_violation_lv1_accident_damage_gt20000_cl_36_stav_1":
                description = "nesreca sa stetom preko 20000 EUR";
                appliedProvisions.add("cl. 339 KZ");
                break;

            case "rural_road_speed_violation_lv1_cl_37":
                description = "prekoracenje brzine van naselja (manje)";
                appliedProvisions.add("cl. 37 ZOBS");
                break;
            case "rural_road_speed_violation_lv2_cl_37":
                description = "prekoracenje brzine van naselja (vece)";
                appliedProvisions.add("cl. 37 ZOBS");
                break;
            case "rural_road_speed_violation_lv1_accident_cl_37":
                description = "prekoracenje van naselja sa nesrecom";
                appliedProvisions.add("cl. 37 ZOBS");
                break;
            case "rural_road_speed_violation_lv2_accident_cl_37":
                description = "teze prekoracenje van naselja sa nesrecom";
                appliedProvisions.add("cl. 37 ZOBS");
                break;
            default:
                // Default case - empty description and provisions list
                break;
        }

        return new JudgmentResponse(description, appliedProvisions, ruleResults);
    }

    private static String normalizeTriggeredRule(String triggeredRule) {
        if (triggeredRule == null || triggeredRule.isBlank()) {
            return "";
        }
        System.out.println("Original triggered rule: " + triggeredRule);
        String rule = triggeredRule.toLowerCase();
        String[] knownRules = {
                "alcohol_level_violation_lv1_cl_182",
                "alcohol_level_violation_lv1_accident_cl_182",
                "town_road_speed_violation_lv1_cl_36_stav_1",
                "town_road_speed_violation_lv2_cl_36_stav_1",
                "town_road_speed_violation_lv1_accident_cl_36_stav_1",
                "town_road_speed_violation_lv2_accident_cl_36_stav_1",
                "town_road_speed_violation_lv1_accident_injury_laka_cl_36_stav_1",
                "town_road_speed_violation_lv1_accident_injury_teska_cl_36_stav_1",
                "town_road_speed_violation_lv1_accident_injury_fatalna_cl_36_stav_1",
                "town_road_speed_violation_lv1_accident_damage_gt20000_cl_36_stav_1",
                "rural_road_speed_violation_lv1_cl_37",
                "rural_road_speed_violation_lv2_cl_37",
                "rural_road_speed_violation_lv1_accident_cl_37",
                "rural_road_speed_violation_lv2_accident_cl_37"
        };

        for (String knownRule : knownRules) {
            if (rule.contains(knownRule)) {
                return knownRule;
            }
        }

        return triggeredRule;
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

                // DR-DEVICE numeric comparisons (e.g., rule19 damage_eur > 20000)
                // require typed numeric values, not string literals.
                if ("damage_eur".equals(key) && value instanceof String s) {
                    try {
                        value = Double.parseDouble(s.trim().replace(',', '.'));
                    } catch (NumberFormatException ex) {
                        // Skip invalid damage value instead of exporting a non-numeric literal.
                        continue;
                    }
                }

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
            String lcNS = LC_NS;

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

                if (value instanceof Integer || value instanceof Long) {
                    elem.setAttributeNS(rdfNS, "rdf:datatype", xsdNS + "integer");
                } else if (value instanceof Double || value instanceof Float) {
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

            String nTriples = buildNTriplesContent(caseId, attributes);
            Files.writeString(new File(BASE_PATH, "facts.n3").toPath(), nTriples);
            Files.writeString(new File(BASE_PATH, "facts.nt").toPath(), nTriples);

            System.out.println("✅ RDF file created successfully: " + new File(BASE_PATH, filename).getAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ Error while creating RDF file: " + e.getMessage());
        }
    }

    private static String buildNTriplesContent(String caseId, Map<String, Object> attributes) {
        String lcNS = LC_NS;
        String rdfNS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        String rdfsNS = "http://www.w3.org/2000/01/rdf-schema#";
        String xsdNS = "http://www.w3.org/2001/XMLSchema#";
        String subject = "<" + lcNS + caseId + ">";

        StringBuilder triples = new StringBuilder();
        triples.append("<").append(lcNS).append("case> <").append(rdfNS).append("type> <")
            .append(rdfsNS).append("Class> .")
            .append(System.lineSeparator());
        triples.append("<").append(lcNS).append("case> <").append(rdfsNS).append("subClassOf> <")
            .append(rdfsNS).append("Resource> .")
            .append(System.lineSeparator());
        triples.append(subject).append(" <").append(rdfNS).append("type> <")
            .append(lcNS).append("case> .")
            .append(System.lineSeparator());

        for (String key : attributes.keySet()) {
            triples.append("<").append(lcNS).append(key).append("> <").append(rdfNS)
                .append("type> <").append(rdfNS).append("Property> .")
                .append(System.lineSeparator());
            triples.append("<").append(lcNS).append(key).append("> <").append(rdfsNS)
                .append("domain> <").append(lcNS).append("case> .")
                .append(System.lineSeparator());
        }

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String predicate = "<" + lcNS + entry.getKey() + ">";
            Object value = entry.getValue();

            String object;
            if (value instanceof Integer || value instanceof Long) {
                object = "\"" + value + "\"^^<" + xsdNS + "integer>";
            } else if (value instanceof Float || value instanceof Double) {
                object = "\"" + value + "\"^^<" + xsdNS + "double>";
            } else {
                object = "\"" + escapeNTriplesLiteral(String.valueOf(value)) + "\"";
            }

            triples.append(subject)
                    .append(" ")
                    .append(predicate)
                    .append(" ")
                    .append(object)
                    .append(" .")
                    .append(System.lineSeparator());
        }

        return triples.toString();
    }

    private static String escapeNTriplesLiteral(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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
        String firstNonPenaltyRule = null;
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
                        if (!isPenaltyTag(tag) && firstNonPenaltyRule == null) {
                            firstNonPenaltyRule = tag;
                        }
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

            if (firstNonPenaltyRule != null) {
                result.put("triggered_rule", firstNonPenaltyRule);
            }

            if (!penalties.isEmpty()) {
                result.putAll(penalties);
            }

        } catch (Exception e) {
            System.err.println("⚠️ RDF parsing failed: " + e.getMessage());
        }

        return result;
    }

    private static boolean isPenaltyTag(String tag) {
        return switch (tag) {
            case "to_pay_min", "to_pay_max", "driving_ban", "max_imprisonment" -> true;
            default -> false;
        };
    }
}

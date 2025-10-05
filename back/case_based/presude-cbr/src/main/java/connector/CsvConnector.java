package connector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import es.ucm.fdi.gaia.jcolibri.cbrcore.CBRCase;
import es.ucm.fdi.gaia.jcolibri.cbrcore.CaseBaseFilter;
import es.ucm.fdi.gaia.jcolibri.cbrcore.Connector;
import es.ucm.fdi.gaia.jcolibri.exception.InitializingException;
import model.CaseDescription;

public class CsvConnector implements Connector {

    // Resource path of the CSV (place the file under src/main/resources)
    private static final String RESOURCE = "/presude.csv";

    @Override
    public Collection<CBRCase> retrieveAllCases() {
        LinkedList<CBRCase> cases = new LinkedList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(getClass().getResourceAsStream(RESOURCE),
                                "CSV resource not found: " + RESOURCE),
                        StandardCharsets.UTF_8))) {

            String line;
            boolean headerSkipped = false;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // skip header (starts with 'id,'), only once
                if (!headerSkipped && line.toLowerCase(Locale.ROOT).startsWith("id,")) {
                    headerSkipped = true;
                    continue;
                }

                // Split by commas that are NOT inside quotes
                // This regex keeps quoted fields intact
                String[] raw = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                // Trim and unquote every cell
                String[] values = Arrays.stream(raw)
                        .map(String::trim)
                        .map(CsvConnector::unquote)
                        .toArray(String[]::new);

                // id,court,caseNumber,judge,prosecutor,defendant,offense,injuries,verdictType,appliedProvisions,
                // violationTypes,speedKmh,speedLimitKmh,alcoholLevelPromil,roadCondition,injurySeverity,
                // damageEur,mentalState,priorRecord,punishmentType,sentenceMonths
                if (values.length < 20) {
                    System.err.println("Skipping malformed line (cols=" + values.length + "): " + line);
                    continue;
                }

                CaseDescription d = new CaseDescription();
                d.setId(parseInt(values[0], 0));
                d.setCourt(emptyToNull(values[1]));
                d.setCaseNumber(emptyToNull(values[2]));
                d.setJudge(emptyToNull(values[3]));
                d.setProsecutor(emptyToNull(values[4]));
                d.setDefendant(emptyToNull(values[5]));
                d.setOffense(emptyToNull(values[6]));

                d.setVerdictType(emptyToNull(values[7]));
                d.setAppliedProvisions(splitList(values[8]));
                d.setViolationTypes(splitList(values[9]));

                d.setSpeedKmh(parseDouble(values[10]));
                d.setSpeedLimitKmh(parseDouble(values[11]));
                d.setAlcoholLevelPromil(parseDouble(values[12]));
                d.setRoadCondition(emptyToNull(values[13]));
                d.setInjurySeverity(emptyToNull(values[14]));
                d.setDamageEur(parseDouble(values[15]));
                d.setMentalState(emptyToNull(values[16]));
                d.setPriorRecord(parseBoolean(values[17]));
                d.setPunishmentType(emptyToNull(values[18]));
                d.setSentenceMonths(parseIntObj(values[19]));

                CBRCase c = new CBRCase();
                c.setDescription(d);
                cases.add(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cases;
    }

    @Override
    public Collection<CBRCase> retrieveSomeCases(CaseBaseFilter filter) {
        // Not used in this project; return all and let jColibri filter if needed
        return retrieveAllCases();
    }

    @Override
    public void storeCases(Collection<CBRCase> cases) {
        // No-op: this connector is read-only from CSV for this project
    }

    @Override
    public void deleteCases(Collection<CBRCase> cases) {
        // No-op
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public void initFromXMLfile(URL file) throws InitializingException {
        // Not needed (we load from a fixed resource)
    }

    // ---------------- helpers ----------------

    private static String unquote(String s) {
        if (s == null) return null;
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length() - 1);
        return s;
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() || t.equalsIgnoreCase("null") ? null : t;
    }

    private static Double parseDouble(String s) {
        try {
            String t = emptyToNull(s);
            return (t == null) ? null : Double.valueOf(t);
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseIntObj(String s) {
        try {
            String t = emptyToNull(s);
            return (t == null) ? null : Integer.valueOf(t);
        } catch (Exception e) {
            return null;
        }
    }

    private static int parseInt(String s, int def) {
        try {
            String t = emptyToNull(s);
            return (t == null) ? def : Integer.parseInt(t);
        } catch (Exception e) {
            return def;
        }
    }

    private static Boolean parseBoolean(String s) {
        String t = emptyToNull(s);
        if (t == null) return null;
        if (t.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (t.equalsIgnoreCase("false")) return Boolean.FALSE;
        return null;
    }

    private static List<String> splitList(String s) {
        String t = emptyToNull(s);
        if (t == null) return new ArrayList<>();
        // semicolon-separated lists; trim each; drop empties
        return Arrays.stream(t.split(";"))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }

}
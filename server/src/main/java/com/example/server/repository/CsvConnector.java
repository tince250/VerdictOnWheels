package com.example.server.repository;

import com.example.server.model.Judgment;
import es.ucm.fdi.gaia.jcolibri.cbrcore.CBRCase;
import es.ucm.fdi.gaia.jcolibri.cbrcore.CaseBaseFilter;
import es.ucm.fdi.gaia.jcolibri.cbrcore.Connector;
import es.ucm.fdi.gaia.jcolibri.exception.InitializingException;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CSV-backed connector & mini repository for Judgments.
 * - Read-only mode from classpath resource (for jColibri demos).
 * - Read-write mode when constructed with a writable filesystem path.
 * <p>
 * CSV schema (21 columns):
 * id,court,caseNumber,judge,prosecutor,defendant,offense,injuries,verdictType,
 * appliedProvisions,violationTypes,speedKmh,speedLimitKmh,alcoholLevelPromil,
 * roadCondition,injurySeverity,damageEur,mentalState,priorRecord,punishmentType,sentenceMonths
 * <p>
 * List fields are semicolon-separated ("a; b; c"). Strings may be quoted.
 */
public class CsvConnector implements Connector {

    /**
     * Classpath resource for read-only mode (put under src/main/resources)
     */
    private static final String DEFAULT_RESOURCE = "/presude.csv";

    /**
     * If present, all read/write operations use this path instead of the classpath resource.
     */
    private final Path csvPath;      // nullable → read-only from resource
    private final String resource;   // used when csvPath == null

    /**
     * CSV header in canonical order
     */
    private static final String HEADER = String.join(",",
            "id", "court", "caseNumber", "judge", "prosecutor", "defendant", "offense", "verdictType",
            "appliedProvisions", "violationTypes", "speedKmh", "speedLimitKmh", "alcoholLevelPromil",
            "roadCondition", "injurySeverity", "damageEur", "mentalState", "priorRecord", "punishmentType", "sentenceMonths"
    );

    // ---------- ctors ----------

    /**
     * Read-only from the default classpath resource (/presude.csv).
     */
    public CsvConnector() {
        this(null, DEFAULT_RESOURCE);
    }

    /**
     * Read-only from a custom classpath resource (must start with '/').
     * Example: new CsvConnector("/data/my_presude.csv");
     */
    public CsvConnector(String classpathResource) {
        this(null, classpathResource);
    }

    /**
     * Read-write from a filesystem path. If the file does not exist, it will be created with header.
     * Example: new CsvConnector(Paths.get("data/presude.csv"));
     */
    public CsvConnector(Path csvPath) {
        this(Objects.requireNonNull(csvPath), null);
    }

    private CsvConnector(Path csvPath, String resource) {
        this.csvPath = csvPath;
        this.resource = resource;

        if (this.csvPath != null) {
            try {
                Files.createDirectories(this.csvPath.getParent() != null ? this.csvPath.getParent() : Paths.get("."));
                if (!Files.exists(this.csvPath)) {
                    Files.writeString(this.csvPath, HEADER + System.lineSeparator(), StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to prepare CSV at " + this.csvPath, e);
            }
        } else {
            // sanity check for resource mode
            if (this.resource == null || !this.resource.startsWith("/"))
                throw new IllegalArgumentException("Classpath resource must start with '/'. Given: " + this.resource);
            if (getClass().getResourceAsStream(this.resource) == null)
                System.err.println("WARNING: classpath resource not found at " + this.resource);
        }
    }

    // ---------- jColibri Connector ----------

    @Override
    public Collection<CBRCase> retrieveAllCases() {
        List<Judgment> judgments = getAllJudgments();
        LinkedList<CBRCase> cases = new LinkedList<>();
        for (Judgment d : judgments) {
            CBRCase c = new CBRCase();
            c.setDescription(d);
            cases.add(c);
        }
        return cases;
    }

    @Override
    public Collection<CBRCase> retrieveSomeCases(CaseBaseFilter filter) {
        // Minimal: return all and let the calling code filter if needed
        return retrieveAllCases();
    }

    @Override
    public void storeCases(Collection<CBRCase> cases) {
    }

    @Override
    public void deleteCases(Collection<CBRCase> cases) {
    }

    @Override
    public void close() {
        // no resources to close
    }

    @Override
    public void initFromXMLfile(URL file) throws InitializingException {
        // not used
    }

    // ---------- Repository-style API ----------

    /**
     * Load all judgments from CSV (resource or file).
     */
    public synchronized List<Judgment> getAllJudgments() {
        List<Judgment> out = new ArrayList<>();
        try (BufferedReader br = openReader()) {
            String line;
            boolean headerSkipped = false;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (!headerSkipped && isHeader(line)) {
                    headerSkipped = true;
                    continue;
                }
                String[] values = parseCsvLine(line);
                if (values.length < 20) {
                    System.err.println("Skipping malformed line (cols=" + values.length + "): " + line);
                    continue;
                }
                out.add(fromRow(values));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read CSV", e);
        }
        return out;
    }

    /**
     * Get judgment by integer id.
     */
    public synchronized Optional<Judgment> getById(String id) {
        return getAllJudgments().stream().filter(j -> j.getId().equals(id)).findFirst();
    }

    /**
     * List all ids in CSV (fast enough for small datasets).
     */
    public synchronized List<String> listIds() {
        return getAllJudgments().stream().map(Judgment::getId).collect(Collectors.toList());
    }

    private Path getCsvPath() {
        return csvPath;
    }


    /**
     * Insert or update a single judgment (by id). Requires writable csvPath.
     */
    public synchronized void upsert(Judgment j) {
        ensureWritable();
        System.out.println("[CsvConnector] upsert -> " + getCsvPath().toAbsolutePath());  // <—
        List<Judgment> all = getAllJudgments();
        boolean updated = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId() == j.getId()) {
                all.set(i, j);
                updated = true;
                break;
            }
        }
        if (!updated) all.add(j);
        writeAll(all);
    }

    // ---------- IO helpers ----------

    private BufferedReader openReader() throws IOException {
        if (csvPath != null) {
            return Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
        }
        InputStream in = Objects.requireNonNull(
                getClass().getResourceAsStream(resource), "CSV resource not found: " + resource);
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private void writeAll(Collection<Judgment> judgments) {
        ensureWritable();
        try (BufferedWriter bw = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            bw.write(HEADER);
            bw.newLine();
            for (Judgment j : judgments) {
                bw.write(toRow(j));
                bw.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write CSV at " + csvPath, e);
        }
    }

    private void ensureWritable() {
        if (csvPath == null)
            throw new IllegalStateException(
                    "This CsvConnector was created in read-only (classpath) mode. " +
                            "Use new CsvConnector(Path) with a writable file path for upsert/delete.");
    }

    private static boolean isHeader(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.startsWith("id,");
    }

    /**
     * Split CSV by commas outside quotes.
     */
    private static String[] parseCsvLine(String line) {
        String[] raw = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        return Arrays.stream(raw).map(String::trim).map(CsvConnector::unquote).toArray(String[]::new);
    }

    // ---------- mapping Judgment <-> CSV row ----------

    private static Judgment fromRow(String[] v) {
        Judgment d = new Judgment();
        d.setId(v[0]);
        d.setCourt(emptyToNull(v[1]));
        d.setCaseNumber(emptyToNull(v[2]));
        d.setJudge(emptyToNull(v[3]));
        d.setProsecutor(emptyToNull(v[4]));
        d.setDefendant(emptyToNull(v[5]));
        d.setOffense(emptyToNull(v[6]));
        d.setVerdictType(emptyToNull(v[7]));
        d.setAppliedProvisions(splitList(v[8]));
        d.setViolationTypes(splitList(v[9]));
        d.setSpeedKmh(parseDouble(v[10]));
        d.setSpeedLimitKmh(parseDouble(v[11]));
        d.setAlcoholLevelPromil(parseDouble(v[12]));
        d.setRoadCondition(emptyToNull(v[13]));
        d.setInjurySeverity(emptyToNull(v[14]));
        d.setDamageEur(parseDouble(v[15]));
        d.setMentalState(emptyToNull(v[16]));
        d.setPriorRecord(parseBoolean(v[17]));
        d.setPunishmentType(emptyToNull(v[18]));
        d.setSentenceMonths(parseIntObj(v[19]));
        d.setGuilty(parseBoolean(v[20]));
        d.setFine(parseIntObj(v[21]));
        d.setDrivingBan(parseBoolean(v[22]));
        return d;
    }

    private static String toRow(Judgment d) {
        // keep column order in sync with HEADER
        List<String> cols = new ArrayList<>(23);
        cols.add(String.valueOf(d.getId()));
        cols.add(q(d.getCourt()));
        cols.add(q(d.getCaseNumber()));
        cols.add(q(d.getJudge()));
        cols.add(q(d.getProsecutor()));
        cols.add(q(d.getDefendant()));
        cols.add(q(d.getOffense()));
        cols.add(q(d.getVerdictType()));
        cols.add(q(joinList(d.getAppliedProvisions())));
        cols.add(q(joinList(d.getViolationTypes())));
        cols.add(n(d.getSpeedKmh()));
        cols.add(n(d.getSpeedLimitKmh()));
        cols.add(n(d.getAlcoholLevelPromil()));
        cols.add(q(d.getRoadCondition()));
        cols.add(q(d.getInjurySeverity()));
        cols.add(n(d.getDamageEur()));
        cols.add(q(d.getMentalState()));
        cols.add(b(d.getPriorRecord()));
        cols.add(q(d.getPunishmentType()));
        cols.add(n(d.getSentenceMonths()));
        cols.add(b(d.getGuilty()));
        cols.add(n(d.getFine()));
        cols.add(b(d.getDrivingBan()));
        return String.join(",", cols);
    }

    // ---------- small utils ----------

    private static String joinList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining("; "));
    }

    /**
     * quote a string if needed for CSV
     */
    private static String q(String s) {
        String t = emptyToNull(s);
        if (t == null) return "";
        boolean needQuote = t.contains(",") || t.contains("\"") || t.contains("\n") || t.contains("\r");
        if (needQuote) {
            t = t.replace("\"", "\"\"");
            return "\"" + t + "\"";
        }
        return t;
    }

    private static String n(Number n) {
        return n == null ? "" : String.valueOf(n);
    }

    private static String b(Boolean b) {
        return b == null ? "" : String.valueOf(b);
    }

    private static String unquote(String s) {
        if (s == null) return null;
        String t = s;
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\""))
            t = t.substring(1, t.length() - 1).replace("\"\"", "\"");
        return t;
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
        return Arrays.stream(t.split(";"))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }
}


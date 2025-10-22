package com.example.server.service;

import com.example.server.dto.SimilarCaseDTO;
import com.example.server.model.Judgment;
import com.example.server.repository.CsvConnector;
import com.example.server.utils.TabularSimilarity;
import es.ucm.fdi.gaia.jcolibri.casebase.LinealCaseBase;
import es.ucm.fdi.gaia.jcolibri.cbraplications.StandardCBRApplication;
import es.ucm.fdi.gaia.jcolibri.cbrcore.Attribute;
import es.ucm.fdi.gaia.jcolibri.cbrcore.CBRCaseBase;
import es.ucm.fdi.gaia.jcolibri.cbrcore.CBRQuery;
import es.ucm.fdi.gaia.jcolibri.exception.ExecutionException;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.NNConfig;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.NNScoringMethod;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.similarity.global.Average;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.similarity.local.Equal;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.RetrievalResult;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.selection.SelectCases;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class CaseBasedService implements StandardCBRApplication {

    private CsvConnector _connector;
    /**
     * Connector object
     */
    CBRCaseBase _caseBase;
    /**
     * CaseBase object
     */

    NNConfig simConfig;
    /**
     * KNN configuration
     */

    private volatile boolean initialized = false;

    public CaseBasedService(CsvConnector csvConnector) {
        this._connector = csvConnector;
    }

    private void initIfNeeded() {
        if (initialized) return;
        synchronized (this) {
            if (initialized) return;
            try {
                configure();
                preCycle();
                initialized = true;
                System.out.println("[CBR] Initialized. Cases: " + _caseBase.getCases().size());
            } catch (ExecutionException e) {
                throw new RuntimeException("CBR init failed", e);
            }
        }
    }

    public void configure() throws ExecutionException {
        _caseBase = new LinealCaseBase();

        simConfig = new NNConfig();
        simConfig.setDescriptionSimFunction(new Average());

        simConfig.addMapping(new Attribute("offense", Judgment.class), new Equal());
        simConfig.setWeight(new Attribute("offense", Judgment.class), 1.0);

        TabularSimilarity injurySimilarity = new TabularSimilarity(Arrays.asList(new String[]{"nema", "lake", "teske", "fatalne"}));
        injurySimilarity.setSimilarity("lake", "teske", .5);
        injurySimilarity.setSimilarity("teske", "fatalne", .6);
        injurySimilarity.setSimilarity("lake", "fatalne", .2);
        injurySimilarity.setSimilarity("nema", "lake", .2);
        simConfig.addMapping(new Attribute("injurySeverity", Judgment.class), injurySimilarity);
        simConfig.setWeight(new Attribute("injurySeverity", Judgment.class), 1.2);

        TabularSimilarity mensSimilarity = new TabularSimilarity(Arrays.asList(new String[]{"nehat", "umislaj"}));
        mensSimilarity.setSimilarity("nehat", "umislaj", 0.3);

        simConfig.addMapping(new Attribute("mentalState", Judgment.class), mensSimilarity);
        simConfig.setWeight(new Attribute("mentalState", Judgment.class), 0.6);

        simConfig.addMapping(new Attribute("priorRecord", Judgment.class), new Equal());
        simConfig.setWeight(new Attribute("priorRecord", Judgment.class), 0.3);

        simConfig.addMapping(new Attribute("accidentOccured", Judgment.class), new Equal());
        simConfig.setWeight(new Attribute("accidentOccured", Judgment.class), 0.5);

        simConfig.addMapping(new Attribute("alcoholLevelPromil", Judgment.class),
                new es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.similarity.local.Interval(2.0));
        simConfig.setWeight(new Attribute("alcoholLevelPromil", Judgment.class), 1.1);

        simConfig.addMapping(new Attribute("speedKmh", Judgment.class),
                new es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.similarity.local.Interval(60.0));
        simConfig.setWeight(new Attribute("speedKmh", Judgment.class), 0.8);

        simConfig.addMapping(new Attribute("speedLimitKmh", Judgment.class),
                new es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.similarity.local.Interval(50.0));
        simConfig.setWeight(new Attribute("speedLimitKmh", Judgment.class), 0.5);

        simConfig.addMapping(new Attribute("damageEur", Judgment.class),
                new es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.similarity.local.Interval(25000.0));
        simConfig.setWeight(new Attribute("damageEur", Judgment.class), 0.8);

        // Equal - returns 1 if both individuals are equal, otherwise returns 0
        // Interval - returns the similarity of two number inside an interval: sim(x,y) = 1-(|x-y|/interval)
        // Threshold - returns 1 if the difference between two numbers is less than a threshold, 0 in the other case
        // EqualsStringIgnoreCase - returns 1 if both String are the same despite case letters, 0 in the other case
        // MaxString - returns a similarity value depending of the biggest substring that belong to both strings
        // EnumDistance - returns the similarity of two enum values as the their distance: sim(x,y) = |ord(x) - ord(y)|
        // EnumCyclicDistance - computes the similarity between two enum values as their cyclic distance
        // Table - uses a table to obtain the similarity between two values. Allowed values are Strings or Enums. The table is read from a text file.
        // TabularSimilarity - calculates similarity between two strings or two lists of strings on the basis of tabular similarities
    }

    public void cycle(CBRQuery query) throws ExecutionException {
        Collection<RetrievalResult> eval = NNScoringMethod.evaluateSimilarity(_caseBase.getCases(), query, simConfig);
        eval = SelectCases.selectTopKRR(eval, 2);
        System.out.println("Retrieved cases:");
        for (RetrievalResult nse : eval)
            System.out.println(nse.get_case().getDescription() + " -> " + nse.getEval());
    }

    public void postCycle() throws ExecutionException {

    }

    public CBRCaseBase preCycle() throws ExecutionException {
        _caseBase.init(_connector);
        return _caseBase;
    }

    public List<SimilarCaseDTO> retrieveTopK(Judgment query, int k) {
        initIfNeeded();
        normalizeJudgment(query);

        System.out.println(query);

        CBRQuery q = new CBRQuery();
        q.setDescription(query);

        var scored = NNScoringMethod.evaluateSimilarity(_caseBase.getCases(), q, simConfig);
        var top = SelectCases.selectTopKRR(scored, k);

        System.out.println("Retrieved cases:");
        for (RetrievalResult nse : top)
            System.out.println(nse.get_case().getDescription() + " -> " + nse.getEval());

        return top.stream()
                .sorted((a, b) -> Double.compare(b.getEval(), a.getEval()))
                .map(rr -> new SimilarCaseDTO((Judgment) rr.get_case().getDescription(),
                        rr.getEval()))
                .toList();
    }

    public Judgment suggestJudgment(Judgment judgment, List<SimilarCaseDTO> similarCases) {

        Boolean isGuilty = determineIfGuilty(similarCases);

        judgment.setIsGuilty(isGuilty);

        if (isGuilty) {
            judgment.setSentenceMonths(determineSentenceMonths(similarCases));
            judgment.setFine(determineFine(similarCases));
            judgment.setDrivingBan(determineDrivingBan(similarCases));
        }

        return judgment;
    }

    private Boolean determineDrivingBan(List<SimilarCaseDTO> similarCases) {
        double banYes = 0, banNo = 0;
        for (var dto : similarCases) {
            Judgment j = dto.getCaseDesc();
            if (j.getDrivingBan() == null) continue;
            if (j.getDrivingBan()) banYes += dto.getSimilarity();
            else banNo += dto.getSimilarity();
        }
        Boolean drivingBan = null;
        if (banYes > banNo) drivingBan = true;
        else if (banNo > banYes) drivingBan = false;

        return drivingBan;
    }

    private Integer determineFine(List<SimilarCaseDTO> similarCases) {
        double fineSum = 0, fineWeight = 0;
        for (var dto : similarCases) {
            Judgment j = dto.getCaseDesc();
            if (j.getFine() != null) {
                fineSum += j.getFine() * dto.getSimilarity();
                fineWeight += dto.getSimilarity();
            }
        }
        Integer fineEur = (fineWeight > 0)
                ? (int) Math.round(fineSum / fineWeight)
                : null;

        return fineEur;
    }

    private Integer determineSentenceMonths(List<SimilarCaseDTO> similarCases) {
        double prisonSum = 0, prisonWeight = 0;

        for (var dto : similarCases) {
            Judgment j = dto.getCaseDesc();
            if (j.getSentenceMonths() != null) {
                prisonSum += j.getSentenceMonths() * dto.getSimilarity();
                prisonWeight += dto.getSimilarity();
            }
        }

        Integer prisonMonths = (prisonWeight > 0)
                ? (int) Math.round(prisonSum / prisonWeight)
                : null;

        return prisonMonths;
    }

    private boolean determineIfGuilty(List<SimilarCaseDTO> similarCases) {
        double guiltyWeight = 0.0;
        double notGuiltyWeight = 0.0;
        double weightSum = 0.0;

        for (SimilarCaseDTO dto : similarCases) {
            double sim = dto.getSimilarity();
            Judgment j = dto.getCaseDesc();
            if (j.getIsGuilty() == null) continue;

            weightSum += sim;
            if (j.getIsGuilty()) guiltyWeight += sim;
            else notGuiltyWeight += sim;
        }

        Boolean suggestedVerdict = null;
        double confidence = 0.0;

        if (weightSum > 0) {
            if (guiltyWeight > notGuiltyWeight) {
                suggestedVerdict = true;
                confidence = guiltyWeight / weightSum;
            } else if (notGuiltyWeight > guiltyWeight) {
                suggestedVerdict = false;
                confidence = notGuiltyWeight / weightSum;
            }
        }

        return suggestedVerdict;
    }

    private void normalizeJudgment(Judgment j) {
        if (j.getInjurySeverity() != null)
            j.setInjurySeverity(sanitize(j.getInjurySeverity()));
        if (j.getMentalState() != null)
            j.setMentalState(sanitize(j.getMentalState()));
        if (j.getOffense() != null)
            j.setOffense(j.getOffense().trim());
        if (j.getRoadCondition() != null)
            j.setRoadCondition(sanitize(j.getRoadCondition()));
        if (j.getRoadType() != null)
            j.setRoadType(sanitize(j.getRoadType()));
        if (j.getAppliedProvisions() != null)
            j.setAppliedProvisions(j.getAppliedProvisions().stream().map(String::trim).toList());
        if (j.getViolationTypes() != null)
            j.setViolationTypes(j.getViolationTypes().stream().map(this::sanitize).toList());
    }

    private String sanitize(String s) {
        if (s == null) return null;
        String t = s.trim().toLowerCase();
        t = t.replace("š", "s").replace("č", "c").replace("ć", "c").replace("ž", "z").replace("đ", "dj");
        return t;
    }

}

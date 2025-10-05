package com.example.server.service;

import com.example.server.model.Judgment;
import com.example.server.repository.CsvConnector;
import com.example.server.utils.TabularSimilarity;
import es.ucm.fdi.gaia.jcolibri.casebase.LinealCaseBase;
import es.ucm.fdi.gaia.jcolibri.cbraplications.StandardCBRApplication;
import es.ucm.fdi.gaia.jcolibri.cbrcore.*;
import es.ucm.fdi.gaia.jcolibri.exception.ExecutionException;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.NNConfig;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.similarity.LocalSimilarityFunction;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.similarity.global.Average;
import org.springframework.stereotype.Service;

import es.ucm.fdi.gaia.jcolibri.method.retrieve.RetrievalResult;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.NNConfig;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.NNScoringMethod;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.similarity.global.Average;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.NNretrieval.similarity.local.Equal;
import es.ucm.fdi.gaia.jcolibri.method.retrieve.selection.SelectCases;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
class CaseBasedService implements StandardCBRApplication {

    Connector _connector;  /** Connector object */
    CBRCaseBase _caseBase;  /** CaseBase object */

    NNConfig simConfig;  /** KNN configuration */

    public void configure() throws ExecutionException {
        _connector =  new CsvConnector();

        _caseBase = new LinealCaseBase();  // Create a Lineal case base for in-memory organization

        simConfig = new NNConfig(); // KNN configuration
        simConfig.setDescriptionSimFunction(new Average());  // global similarity function = average

        simConfig.addMapping(new Attribute("offense", Judgment.class), new Equal());
        TabularSimilarity injurySimilarity = new TabularSimilarity(Arrays.asList(new String[] {"lake", "teske", "fatalne"}));
        injurySimilarity.setSimilarity("lake", "teske", .5);
        injurySimilarity.setSimilarity("teske", "fatalne", .5);
        injurySimilarity.setSimilarity("lake", "fatalne", .2);
        simConfig.addMapping(new Attribute("injurySeverity", Judgment.class), injurySimilarity);
        TabularSimilarity provisionsSimilarity = new TabularSimilarity(Arrays.asList(new String[] {
                "cl. 42 st. 1 ZOBSNP",
                "cl. 43 st. 1 ZOBSNP",
                "cl. 47 st. 1 ZOBSNP",
                "cl. 47 st. 3 ZOBSNP",
                "cl. 47 st. 4 ZOBSNP"}));
        provisionsSimilarity.setSimilarity("cl. 42 st. 1 ZOBSNP", "cl. 43 st. 1 ZOBSNP", .5);
        provisionsSimilarity.setSimilarity("cl. 47 st. 1 ZOBSNP", "cl. 47 st. 3 ZOBSNP", .5);
        provisionsSimilarity.setSimilarity("cl. 47 st. 3 ZOBSNP", "cl. 47 st. 4 ZOBSNP", .5);
        provisionsSimilarity.setSimilarity("cl. 47 st. 1 ZOBSNP", "cl. 47 st. 4 ZOBSNP", .5);
        simConfig.addMapping(new Attribute("appliedProvisions", Judgment.class), provisionsSimilarity);

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
        java.util.Collection<CBRCase> cases = _caseBase.getCases();
//		for (CBRCase c: cases)
//			System.out.println(c.getDescription());
        return _caseBase;
    }

    public static void main(String[] args) {
        StandardCBRApplication recommender = new CaseBasedService();
        System.out.println("ticko");
        try {
            recommender.configure();

            recommender.preCycle();

            CBRQuery query = new CBRQuery();
            Judgment Judgment = new Judgment();

            Judgment.setOffense("cl. 289 st. 3 KZ");
            List<String> appliedProvisions = new ArrayList();
            appliedProvisions.add("cl. 55 st. 3 tac. 15 ZOBSNP");
            appliedProvisions.add("cl. 43 st. 1 ZOBSNP");
            Judgment.setAppliedProvisions(appliedProvisions);
            Judgment.setSpeedKmh(60.0);
            Judgment.setSpeedLimitKmh(50.0);
            Judgment.setDamageEur(25000.0);
            Judgment.setInjurySeverity("teška");

            query.setDescription( Judgment );

            recommender.cycle(query);

            recommender.postCycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

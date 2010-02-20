package edu.ucla.sspace.evaluation;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.StaticSemanticSpace;


public class DeeseEvaluator {
    public static void main(String[] args) throws Exception {
        DeeseAntonymEvaluation  evaluator = new DeeseAntonymEvaluation();
        for (String file : args) {
            SemanticSpace sspace = new StaticSemanticSpace(file);
            WordAssociationReport report = evaluator.evaluate(sspace);
            System.out.printf("%s: %f\n", file, 100*report.correlation());
        }
    }
}

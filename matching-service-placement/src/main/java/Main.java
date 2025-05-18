import algorithm.*;
import algorithm.model.AlgorithmResults;
import service.MecSystemService;
import utils.TestSystemSetup;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        // get an instance of the MecSystemService
        MecSystemService mecService = MecSystemService.getInstance();

        // setup the test instance configuration
        TestSystemSetup.setupTestInstance(new Random().nextLong());

        // define the algorithms to be compared
        List<MatchingAlg> algorithms = Arrays.asList(new RandomAlg(4762), new GreedyAlg(), new RoundRobinAlg(), new GaleShapleyAlg(true, true, false), new AuctionAlg());

        // run the algorithms and collect results
        Map<Integer, AlgorithmResults> results = new HashMap<>();
        for (MatchingAlg alg : algorithms) {
            results.put(alg.hashCode(), alg.run(false));
            mecService.resetMapping();
        }

        // print the results
        for (MatchingAlg alg : algorithms) {
            System.out.println(results.get(alg.hashCode()));
        }
    }
}
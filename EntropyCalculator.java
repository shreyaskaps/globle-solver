// EntropyService.java

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

/**
 * Computes the Shannon entropy of the feedback distribution for a given guess
 * over a set of remaining candidate countries.
 */
public class EntropyCalculator {

    /**
     * Scores a potential guess by computing the Shannon entropy of the feedback
     * it would produce across all current candidates.
     *
     * @param guessIdx   index of the country being considered as the next guess
     * @param candidates set of indices still possible as the target
     * @param D          precomputed DistanceMatrix (km)
     * @param adj        adjacency lists: adj.get(i) contains neighbors of country i
     * @param lambdaKm   decay constant (in km) for the redness function
     * @return Shannon entropy (in bits) of the feedback distribution
     */
    public static double score(int guessIdx,
                               Set<Integer> candidates,
                               DistanceMatrix D,
                               List<Set<Integer>> adj,
                               double lambdaKm) {
        // Tally feedback frequencies
        Map<Integer, Integer> freq = new HashMap<>();
        for (int targetIdx : candidates) {
            int code = FeedbackEncoding.encode(guessIdx, targetIdx, D, adj, lambdaKm);
            freq.merge(code, 1, Integer::sum);
        }

        // Compute Shannon entropy
        double H = 0.0;
        double total = (double) candidates.size();
        for (int count : freq.values()) {
            double p = count / total;
            H -= p * (Math.log(p) / Math.log(2));  // log base-2
        }
        return H;
    }
}

// FeedbackService.java

import java.util.List;
import java.util.Set;

/**
 * Encodes the feedback you would receive for guessing one country when the target is another.
 * Returns -1 to denote adjacency, or a 0–100 “redness” score otherwise.
 */
public class FeedbackEncoding {

    /**
     * Encodes the feedback for guessing country guessIdx when the true country is targetIdx.
     *
     * @param guessIdx   index of the guessed country
     * @param targetIdx  index of the actual target country
     * @param D          precomputed DistanceMatrix (km)
     * @param adj        adjacency lists: adj.get(i) contains indices of i's neighbors
     * @param lambdaKm   decay constant (in km) for the redness function
     * @return -1 if adjacent; otherwise a 0–100 integer redness
     */
    public static int encode(int guessIdx,
                             int targetIdx,
                             DistanceMatrix D,
                             List<Set<Integer>> adj,
                             double lambdaKm) {
        // If target is in guess's adjacency set, return adjacency code
        Set<Integer> neighbors = adj.get(guessIdx);
        if (neighbors.contains(targetIdx)) {
            return -1;
        }
        // Otherwise compute redness = round(100 * exp(-distance/lambda))
        double distanceKm = D.get(guessIdx, targetIdx);
        double rawScore   = 100.0 * Math.exp(-distanceKm / lambdaKm);
        return (int) Math.round(rawScore);
    }
}

// CandidateManager.java

import java.util.*;

/**
 * Manages the set of possible target countries, applies user feedback to
 * filter candidates, and supports backtracking.
 */
public class CandidateManager {
    private Set<Integer> candidates;
    private Set<Integer> guessed;
    private double bestDistMiles;
    private final double toleranceMiles;
    private final double kmPerMile = 1.60934;

    private final DistanceMatrix D;
    private final List<Set<Integer>> adjList;

    private final Deque<State> history = new ArrayDeque<>();

    /**
     * Snapshot of solver state for backtracking.
     */
    private static class State {
        final Set<Integer> candidates;
        final Set<Integer> guessed;
        final double bestDistMiles;

        State(Set<Integer> c, Set<Integer> g, double b) {
            this.candidates    = new HashSet<>(c);
            this.guessed       = new HashSet<>(g);
            this.bestDistMiles = b;
        }
    }

    /**
     * @param n               number of countries (indices 0..n-1)
     * @param D               precomputed DistanceMatrix (km)
     * @param adjList         adjacency lists by index
     * @param toleranceMiles  ±band for accepting new best-distance readings
     */
    public CandidateManager(int n,
                            DistanceMatrix D,
                            List<Set<Integer>> adjList,
                            double toleranceMiles) {
        this.D               = D;
        this.adjList         = adjList;
        this.toleranceMiles  = toleranceMiles;
        this.candidates      = new HashSet<>();
        for (int i = 0; i < n; i++) candidates.add(i);
        this.guessed         = new HashSet<>();
        this.bestDistMiles   = Double.MAX_VALUE;
    }

    /** Save current state onto the history stack. */
    public void snapshot() {
        history.push(new State(candidates, guessed, bestDistMiles));
    }

    /** Restore the most recent saved state. */
    public void restore() {
        State prev = history.pop();
        this.candidates    = prev.candidates;
        this.guessed       = prev.guessed;
        this.bestDistMiles = prev.bestDistMiles;
    }

    /**
     * Marks a guess as impossible without applying feedback —
     * simply removes it from future consideration.
     */
    public void markImpossible(int idx) {
        guessed.add(idx);
        candidates.remove(idx);
    }

    /**
     * Applies feedback for a guess.
     *
     * @param guessIdx   index of the guessed country
     * @param isAdjacent true if feedback was “adjacent”; false otherwise
     * @param distMiles  reported distance in miles (ignored if adjacent)
     * @return true if at least one candidate remains; false if candidate set is empty
     */
    public boolean applyFeedback(int guessIdx, boolean isAdjacent, double distMiles) {
        // Mark guessed and remove from candidates
        guessed.add(guessIdx);
        candidates.remove(guessIdx);

        if (isAdjacent) {
            // Adjacency: keep only true neighbors
            Set<Integer> nbrs = new HashSet<>(adjList.get(guessIdx));
            nbrs.remove(guessIdx);
            candidates = nbrs;
        } else {
            // Distance feedback
            if (distMiles < bestDistMiles) {
                // New best: tighten to ±tolerance around this reading
                bestDistMiles = distMiles;
                double targetKm = distMiles * kmPerMile;
                double tolKm    = toleranceMiles * kmPerMile;
                candidates.removeIf(
                        c -> Math.abs(D.get(guessIdx, c) - targetKm) > tolKm
                );
            } else {
                // Not closer: exclude any country that would have produced a strictly smaller reading
                double thresholdKm = (bestDistMiles - toleranceMiles) * kmPerMile;
                candidates.removeIf(
                        c -> D.get(guessIdx, c) < thresholdKm
                );
            }
        }
        return !candidates.isEmpty();
    }

    /** @return unmodifiable view of current candidates */
    public Set<Integer> getCandidates() {
        return Collections.unmodifiableSet(candidates);
    }

    /** @return unmodifiable view of all guessed indices */
    public Set<Integer> getGuessed() {
        return Collections.unmodifiableSet(guessed);
    }

    /** @return true if exactly one candidate remains */
    public boolean isSolved() {
        return candidates.size() == 1;
    }

    /**
     * @return the sole remaining candidate index; call only if isSolved() is true
     */
    public int getSolution() {
        if (!isSolved()) {
            throw new IllegalStateException("Not solved yet");
        }
        return candidates.iterator().next();
    }
}

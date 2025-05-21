// GlobleSolver.java

import java.io.IOException;
import java.util.*;

public class GlobleSolver {
    private static final String  DATA_PATH        = "data/country-coord.csv";
    private static final int     ADJ_NEIGHBOR_K    = 6;      // “adjacent” = top‐6 nearest
    private static final double  TOLERANCE_MILES   = 400.0;  // ±200 mi tolerance
    private static final double  LAMBDA_KM         = 5000.0; // decay for redness

    public static void main(String[] args) throws IOException {
        // 1) load country list
        List<Country> countries = CountryLoader.load(DATA_PATH);
        int n = countries.size();

        // 2) build a name→index map
        Map<String,Integer> idxMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idxMap.put(countries.get(i).getName().toLowerCase(), i);
        }

        // 3) precompute distances and adjacency
        DistanceMatrix D = new DistanceMatrix(countries);
        List<Set<Integer>> adj = AdjacencyGraph.build(countries, D, ADJ_NEIGHBOR_K);

        // 4) init candidate manager
        CandidateManager mgr = new CandidateManager(n, D, adj, TOLERANCE_MILES);

        Scanner in = new Scanner(System.in);
        while (!mgr.isSolved()) {
            // 5a) rank unguessed countries by expected info gain (entropy)
            Set<Integer> cands  = mgr.getCandidates();
            Set<Integer> guessed = mgr.getGuessed();
            List<Map.Entry<Integer,Double>> scores = new ArrayList<>();
            for (int g : cands) {
                if (guessed.contains(g)) continue;
                double H = EntropyCalculator.score(g, cands, D, adj, LAMBDA_KM);
                scores.add(Map.entry(g, H));
            }
            scores.sort((a,b) -> Double.compare(b.getValue(), a.getValue()));

            // 5b) show top 5
            System.out.println("\nTop 5 suggestions:");
            for (int i = 0; i < Math.min(5, scores.size()); i++) {
                int idx = scores.get(i).getKey();
                System.out.printf(
                        "%d. %s (%.4f bits)%n",
                        i+1,
                        countries.get(idx).getName(),
                        scores.get(i).getValue()
                );
            }

            // 6) read the user's guess
            System.out.print("\nEnter your guess: ");
            String name = in.nextLine().trim().toLowerCase();
            Integer guessIdx = idxMap.get(name);
            if (guessIdx == null || guessed.contains(guessIdx)) {
                System.out.println("Invalid or already guessed; try again.");
                continue;
            }

            // 7) snapshot state for backtracking
            mgr.snapshot();

            // 8) ask adjacency
            System.out.print("Is it adjacent? (y/n): ");
            boolean isAdj = in.nextLine().trim().equalsIgnoreCase("y");

            // 9) ask distance if not adjacent
            double miles = 0;
            if (!isAdj) {
                System.out.print("Enter approximate distance (miles): ");
                try {
                    miles = Double.parseDouble(in.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Bad number; reverting guess.");
                    mgr.restore();
                    mgr.markImpossible(guessIdx);
                    continue;
                }
            }

            // Save the current candidates before applying feedback
            Set<Integer> prevCands = new HashSet<>(mgr.getCandidates());

            boolean ok = mgr.applyFeedback(guessIdx, isAdj, miles);
            if (!ok) {
                System.out.println("No exact matches—showing the 10 closest by error:");
                PriorityQueue<Map.Entry<Integer,Double>> pq =
                        new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));
                for (int c : prevCands) {
                    double actualMi = D.get(guessIdx, c) / 1.60934;
                    double error    = Math.abs(actualMi - miles);
                    pq.add(Map.entry(c, error));
                }
                for (int i = 0; i < 10 && !pq.isEmpty(); i++) {
                    int idx = pq.poll().getKey();
                    System.out.println(" • " + countries.get(idx).getName());
                }
                mgr.restore();
                mgr.markImpossible(guessIdx);
                continue;
            } else {
                System.out.println("Remaining candidates: " + mgr.getCandidates().size());
            }

        }

        // 11) report solution
        int sol = mgr.getSolution();
        System.out.println("\n🎉 Solved! The country is: " + countries.get(sol).getName());
        in.close();
    }
}

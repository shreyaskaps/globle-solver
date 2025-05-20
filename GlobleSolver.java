import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GlobleSolver {
    private static final int    ADJ_NEIGHBOR_COUNT = 6;    // for true adjacency (0 miles)
    private static final double EARTH_RADIUS_KM    = 6371.0;

    static class Country {
        String name;
        double lat, lon;
        Country(String name, double lat, double lon) {
            this.name = name;
            this.lat  = lat;
            this.lon  = lon;
        }
    }

    // Snapshot for backtracking
    static class State {
        final Set<Integer> candidates, guessed;
        final boolean adjacencyMode;
        final Set<Integer> adjacencyCandidates;
        State(Set<Integer> c, Set<Integer> g,
              boolean aMode, Set<Integer> aCands) {
            this.candidates          = new HashSet<>(c);
            this.guessed             = new HashSet<>(g);
            this.adjacencyMode       = aMode;
            this.adjacencyCandidates = new LinkedHashSet<>(aCands);
        }
    }

    public static void main(String[] args) throws IOException {
        // 1) Load countries & build index
        File csv = new File("data/country-coord.csv");
        List<Country> countries = loadCountries(csv);
        int n = countries.size();
        Map<String,Integer> idxMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idxMap.put(countries.get(i).name.toLowerCase(), i);
        }

        // 2) Compute all-pairs distances (km)
        double[][] distKm = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                double d = haversine(countries.get(i), countries.get(j));
                distKm[i][j] = d;
                distKm[j][i] = d;
            }
        }

        // 3) Precompute true adjacency lists (K nearest neighbors)
        List<Set<Integer>> neighborLists = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            PriorityQueue<Map.Entry<Integer,Double>> pq =
                    new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                pq.add(Map.entry(j, distKm[i][j]));
            }
            Set<Integer> nbrs = new LinkedHashSet<>();
            for (int k = 0; k < ADJ_NEIGHBOR_COUNT && !pq.isEmpty(); k++) {
                nbrs.add(pq.poll().getKey());
            }
            neighborLists.add(nbrs);
        }

        // 4) Initialize solver state
        Set<Integer> candidates = new HashSet<>();
        for (int i = 0; i < n; i++) candidates.add(i);
        Set<Integer> guessed = new HashSet<>();
        boolean adjacencyMode = false;
        Set<Integer> adjacencyCandidates = new LinkedHashSet<>();
        Deque<State> history = new ArrayDeque<>();
        Scanner in = new Scanner(System.in);

        mainLoop:
        while (true) {
            if (candidates.isEmpty()) {
                System.out.println("No candidates remain. Exiting.");
                break;
            }

            // 5) Suggest by entropy
            System.out.println("\nTop 5 suggestions:");
            double tot = candidates.size();
            List<Map.Entry<Integer,Double>> ent = new ArrayList<>();
            for (int g : candidates) {
                if (guessed.contains(g)) continue;
                Map<Integer,Integer> freq = new HashMap<>();
                for (int c : candidates) {
                    int r = (int)Math.round(100 * Math.exp(-distKm[g][c] / 5000.0));
                    freq.merge(r, 1, Integer::sum);
                }
                double H = 0;
                for (int cnt : freq.values()) {
                    double p = cnt / tot;
                    H -= p * (Math.log(p) / Math.log(2));
                }
                ent.add(Map.entry(g, H));
            }
            ent.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            int shown = 0;
            for (Map.Entry<Integer,Double> e : ent) {
                int idx = e.getKey();
                if (guessed.contains(idx)) continue;
                System.out.printf(
                        "%d. %s (%.4f bits)%n",
                        ++shown, countries.get(idx).name, e.getValue()
                );
                if (shown >= 5) break;
            }

            // 6) Read guess
            System.out.print("\nEnter your guess: ");
            String name = in.nextLine().trim().toLowerCase();
            Integer gIdx = idxMap.get(name);
            if (gIdx == null || guessed.contains(gIdx)) {
                System.out.println("Invalid or repeated guess.");
                continue mainLoop;
            }

            // 7) Save state for backtracking
            history.push(new State(candidates, guessed, adjacencyMode, adjacencyCandidates));

            // 8) Read feedback and filter by K nearest
            System.out.print("Enter distance (miles; 0 = adjacent): ");
            double miles;
            try {
                miles = Double.parseDouble(in.nextLine().trim());
            } catch (NumberFormatException ex) {
                System.out.println("Invalid number; reverting guess.");
                State prev = history.pop();
                candidates = prev.candidates;
                guessed    = prev.guessed;
                adjacencyMode       = prev.adjacencyMode;
                adjacencyCandidates = prev.adjacencyCandidates;
                continue mainLoop;
            }

            Set<Integer> filtered;
            if (miles == 0) {
                // switch to adjacency mode
                adjacencyMode = true;
                adjacencyCandidates = neighborLists.get(gIdx);
                filtered = new HashSet<>(adjacencyCandidates);
                filtered.remove(gIdx);
            } else {
                // filter to K closest by actual vs. reported distance
                PriorityQueue<Map.Entry<Integer,Double>> pq =
                        new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));
                for (int c : candidates) {
                    if (c == gIdx) continue;
                    double actualMi = distKm[gIdx][c] / 1.60934;
                    double diffMi   = Math.abs(actualMi - miles);
                    pq.add(Map.entry(c, diffMi));
                }
                int kFilter = Math.min(candidates.size(), Math.max(ADJ_NEIGHBOR_COUNT, candidates.size()/2));
                filtered = new HashSet<>();
                for (int i = 0; i < kFilter && !pq.isEmpty(); i++) {
                    filtered.add(pq.poll().getKey());
                }
            }

            // 9) Commit guess
            guessed.add(gIdx);
            candidates.remove(gIdx);
            candidates.retainAll(filtered);
            System.out.println("Remaining candidates: " + candidates.size());

            // 10) Check for solution (adjacency confirmed)
            if (adjacencyMode && miles == 0 && candidates.size() == 1) {
                int sol = candidates.iterator().next();
                System.out.println("Solved! It’s: " + countries.get(sol).name);
                break;
            }

            // 11) Backtrack on dead end
            if (candidates.isEmpty()) {
                System.out.println("Dead end—backtracking…");
                State prev = history.pop();
                candidates           = prev.candidates;
                guessed              = prev.guessed;
                adjacencyMode        = prev.adjacencyMode;
                adjacencyCandidates  = prev.adjacencyCandidates;
            }
        }

        in.close();
    }

    private static List<Country> loadCountries(File csv) throws IOException {
        List<Country> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            br.readLine();  // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length < 3) continue;
                list.add(new Country(
                        p[0].trim(),
                        Double.parseDouble(p[p.length - 2].trim()),
                        Double.parseDouble(p[p.length - 1].trim())
                ));
            }
        }
        return list;
    }

    private static double haversine(Country a, Country b) {
        double φ1 = Math.toRadians(a.lat),  λ1 = Math.toRadians(a.lon);
        double φ2 = Math.toRadians(b.lat),  λ2 = Math.toRadians(b.lon);
        double dφ = φ2 - φ1, dλ = λ2 - λ1;
        double h  = Math.sin(dφ/2)*Math.sin(dφ/2)
                + Math.cos(φ1)*Math.cos(φ2)
                * Math.sin(dλ/2)*Math.sin(dλ/2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1-h));
    }
}

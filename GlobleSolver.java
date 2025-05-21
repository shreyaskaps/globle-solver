import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GlobleSolver {
    private static final String    DATA_PATH          = "/Users/shreyaskapavarapu/Desktop/globle-solver/out/production/globle-solver/data/country-coord.csv";
    private static final int       ADJ_NEIGHBOR_COUNT = 6;      // for “adjacent” feedback
    private static final double    LAMBDA_KM          = 5000.0; // decay constant for redness
    private static final double    EARTH_RADIUS_KM    = 6371.0;

    static class Country {
        final String name;
        final double lat, lon;
        Country(String name, double lat, double lon) {
            this.name = name;
            this.lat  = lat;
            this.lon  = lon;
        }
    }

    // A snapshot of solver state for backtracking
    static class State {
        final Set<Integer> candidates;
        final Set<Integer> guessed;
        State(Set<Integer> c, Set<Integer> g) {
            this.candidates = new HashSet<>(c);
            this.guessed    = new HashSet<>(g);
        }
    }

    public static void main(String[] args) throws IOException {
        // 1) Load country centroids
        List<Country> countries = loadCountries(DATA_PATH);
        int n = countries.size();

        // 2) Map country name → index
        Map<String,Integer> idxMap = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            idxMap.put(countries.get(i).name.toLowerCase(), i);
        }

        // 3) Precompute all‐pairs distances (km)
        double[][] distKm = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                double d = haversine(countries.get(i), countries.get(j));
                distKm[i][j] = d;
                distKm[j][i] = d;
            }
        }

        // 4) Precompute adjacency = top‐K nearest centroids per country
        List<Set<Integer>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            PriorityQueue<Map.Entry<Integer,Double>> pq =
                    new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));
            for (int j = 0; j < n; j++) if (i != j) {
                pq.add(Map.entry(j, distKm[i][j]));
            }
            Set<Integer> nbrs = new LinkedHashSet<>();
            for (int k = 0; k < ADJ_NEIGHBOR_COUNT && !pq.isEmpty(); k++) {
                nbrs.add(pq.poll().getKey());
            }
            adj.add(nbrs);
        }

        // 5) Initialize solver state
        Set<Integer> candidates = new HashSet<>();
        for (int i = 0; i < n; i++) candidates.add(i);
        Set<Integer> guessed    = new HashSet<>();
        Deque<State> history     = new ArrayDeque<>();
        Scanner in = new Scanner(System.in);

        // 6) Interactive solve loop
        while (true) {
            // 6a) Termination: one candidate left
            if (candidates.size() == 1) {
                int sol = candidates.iterator().next();
                System.out.println("Solved! The country is: " + countries.get(sol).name);
                break;
            }
            // 6b) Suggest top‐5 by max‐entropy
            System.out.println("\nTop 5 suggestions:");
            double tot = candidates.size();
            List<Map.Entry<Integer,Double>> ent = new ArrayList<>();
            for (int g : candidates) {
                if (guessed.contains(g)) continue;
                Map<Integer,Integer> freq = new HashMap<>();
                for (int c : candidates) {
                    int code = adj.get(g).contains(c)
                            ? -1
                            : (int)Math.round(100 * Math.exp(-distKm[g][c] / LAMBDA_KM));
                    freq.merge(code, 1, Integer::sum);
                }
                double H = 0;
                for (int cnt : freq.values()) {
                    double p = cnt / tot;
                    H -= p * (Math.log(p) / Math.log(2));
                }
                ent.add(Map.entry(g, H));
            }
            ent.sort((a,b) -> Double.compare(b.getValue(), a.getValue()));
            int shown = 0;
            for (Map.Entry<Integer,Double> e : ent) {
                int idx = e.getKey();
                System.out.printf(
                        "%d. %s (%.4f bits)%n",
                        ++shown, countries.get(idx).name, e.getValue()
                );
                if (shown >= 5) break;
            }

            // 6c) Read guess
            System.out.print("\nEnter your guess (country name): ");
            String guessName = in.nextLine().trim().toLowerCase();
            Integer gIdx = idxMap.get(guessName);
            if (gIdx == null || guessed.contains(gIdx)) {
                System.out.println("Invalid or already guessed — try again.");
                continue;
            }

            // 6d) Snapshot state for backtracking
            history.push(new State(candidates, guessed));

            // 6e) Read adjacency info
            System.out.print("Is it adjacent? (y/n): ");
            boolean isAdj = in.nextLine().trim().equalsIgnoreCase("y");

            // 6f) Read redness if not adjacent
            int redness = -1;
            if (!isAdj) {
                System.out.print("Enter redness rating (0-100): ");
                try {
                    redness = Integer.parseInt(in.nextLine().trim());
                    if (redness < 0 || redness > 100)
                        throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    System.out.println("Bad rating — reverting guess.");
                    State prev = history.pop();
                    candidates = prev.candidates;
                    guessed    = prev.guessed;
                    continue;
                }
            }

            // 6g) Filter candidates
            Set<Integer> next = new HashSet<>();
            if (isAdj) {
                // only true neighbors remain
                next.addAll(adj.get(gIdx));
            } else {
                // match exact redness code
                for (int c : candidates) {
                    if (adj.get(gIdx).contains(c)) continue;
                    int code = (int)Math.round(100 * Math.exp(-distKm[gIdx][c] / LAMBDA_KM));
                    if (code == redness) next.add(c);
                }
            }

            // 6h) Handle dead‐end
            if (next.isEmpty()) {
                System.out.println("No countries match that feedback — backtracking.");
                State prev = history.pop();
                candidates = prev.candidates;
                guessed    = prev.guessed;
                continue;
            }

            // 6i) Commit guess & advance
            guessed.add(gIdx);
            candidates = next;
            System.out.println("Remaining candidates: " + candidates.size());
        }

        in.close();
    }

    private static List<Country> loadCountries(String path) throws IOException {
        List<Country> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length < 3) continue;
                String name = p[0].trim();
                double lat   = Double.parseDouble(p[p.length-2].trim());
                double lon   = Double.parseDouble(p[p.length-1].trim());
                list.add(new Country(name, lat, lon));
            }
        }
        return list;
    }

    /** Haversine distance (km) between two (lat,lon) points. */
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

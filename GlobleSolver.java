import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GlobleSolver {
    private static final double TOLERANCE_MILES = 200.0;
    private static final int ADJ_NEIGHBOR_COUNT = 10;  // top‐N closest ≈ adjacent

    public static void main(String[] args) throws IOException {
        // 1. Read country centroids
        Map<String,double[]> coords = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("data/country-coord.csv"))) {
            String header = br.readLine();  // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length < 3) continue;
                String name = p[0].trim();
                double lat = Double.parseDouble(p[p.length-2].trim());
                double lon = Double.parseDouble(p[p.length-1].trim());
                coords.put(name, new double[]{lat, lon});
            }
        }

        // 2. Build a complete weighted graph of distances
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph =
                new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        for (String country : coords.keySet()) {
            graph.addVertex(country);
        }
        List<String> list = new ArrayList<>(coords.keySet());
        for (int i = 0; i < list.size(); i++) {
            for (int j = i+1; j < list.size(); j++) {
                String a = list.get(i), b = list.get(j);
                double[] ca = coords.get(a), cb = coords.get(b);
                double km = haversine(ca[0], ca[1], cb[0], cb[1]);
                DefaultWeightedEdge e = graph.addEdge(a, b);
                graph.setEdgeWeight(e, km);
            }
        }

        // 3. Solver state
        Set<String> candidates = new HashSet<>(coords.keySet());
        Set<String> guessed    = new HashSet<>();
        Scanner in = new Scanner(System.in);
        DijkstraShortestPath<String,DefaultWeightedEdge> dsp = new DijkstraShortestPath<>(graph);

        // 4. Interactive loop
        while (true) {
            if (candidates.isEmpty()) {
                System.out.println("No candidates remain. Exiting.");
                break;
            }

            // 4a. Suggest next guesses by entropy
            System.out.println("\nTop suggestions:");
            if (candidates.size() <= 5) {
                int idx = 1;
                for (String c : candidates) {
                    System.out.printf("%d. %s%n", idx++, c);
                }
            } else {
                Map<String,Double> ent = new HashMap<>();
                for (String g : candidates) {
                    if (guessed.contains(g)) continue;
                    Map<Integer,Integer> freq = new HashMap<>();
                    for (String c : candidates) {
                        double d = graph.getEdgeWeight(graph.getEdge(g,c));
                        int r = (int)Math.round(100 * Math.exp(-d/5000.0));
                        freq.merge(r,1,Integer::sum);
                    }
                    double H = 0, tot = candidates.size();
                    for (int cnt : freq.values()) {
                        double p = cnt/tot;
                        H -= p * (Math.log(p)/Math.log(2));
                    }
                    ent.put(g,H);
                }
                ent.entrySet().stream()
                        .sorted(Map.Entry.<String,Double>comparingByValue().reversed())
                        .limit(5)
                        .forEach(e -> System.out.println("• " + e.getKey()));
            }

            // 4b. Read and validate guess
            System.out.print("\nEnter your guess: ");
            String guess = in.nextLine().trim();
            if (!candidates.contains(guess) || guessed.contains(guess)) {
                System.out.println("Invalid or repeated guess.");
                continue;
            }
            guessed.add(guess);
            candidates.remove(guess);

            // 4c. Read feedback
            System.out.print("Enter distance to target (miles; 0 = adjacent): ");
            double miles;
            try {
                miles = Double.parseDouble(in.nextLine().trim());
            } catch (NumberFormatException ex) {
                System.out.println("Bad number; try again.");
                guessed.remove(guess);
                candidates.add(guess);
                continue;
            }

            // 4d. Filter candidates
            if (miles == 0) {
                // adjacency via k‐nearest neighbors
                PriorityQueue<Map.Entry<String,Double>> pq = new PriorityQueue<>(
                        Comparator.comparingDouble(Map.Entry::getValue)
                );
                for (String c : graph.vertexSet()) {
                    if (c.equals(guess)) continue;
                    double d = graph.getEdgeWeight(graph.getEdge(guess, c));
                    pq.add(Map.entry(c, d));
                }
                Set<String> nbrs = new HashSet<>();
                for (int i = 0; i < ADJ_NEIGHBOR_COUNT && !pq.isEmpty(); i++) {
                    nbrs.add(pq.poll().getKey());
                }
                candidates.retainAll(nbrs);
            } else {
                double kmTarget = miles * 1.60934;
                double tolKm    = TOLERANCE_MILES * 1.60934;
                Set<String> keep = new HashSet<>();
                for (String c : candidates) {
                    double dist = dsp.getPathWeight(guess, c);
                    if (!Double.isInfinite(dist)
                            && Math.abs(dist - kmTarget) <= tolKm) {
                        keep.add(c);
                    }
                }
                candidates.retainAll(keep);
            }

            System.out.println("Remaining candidates: " + candidates.size());
        }

        in.close();
    }

    // haversine formula (km)
    private static double haversine(double lat1, double lon1,
                                    double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1),
                dLon = Math.toRadians(lon2 - lon1);
        double h = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1-h));
    }
}

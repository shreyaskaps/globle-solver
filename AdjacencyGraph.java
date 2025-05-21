// AdjacencyService.java

import java.util.*;

/**
 * Builds an “adjacency” list for each country by selecting the k nearest
 * centroids (as a proxy for land borders).
 */
public class AdjacencyGraph {

    /**
     * For each country index i in [0..n), finds the k nearest neighbor indices.
     *
     * @param countries the ordered list of countries
     * @param D         the DistanceMatrix giving inter-country distances in km
     * @param k         how many nearest neighbors to treat as “adjacent”
     * @return a List of size n, where element i is the Set of k nearest neighbor indices of i
     */
    public static List<Set<Integer>> build(List<Country> countries,
                                           DistanceMatrix D,
                                           int k) {
        int n = countries.size();
        List<Set<Integer>> adjacency = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            // Min-heap of (countryIndex, distance) for all j != i
            PriorityQueue<Map.Entry<Integer, Double>> pq =
                    new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));

            for (int j = 0; j < n; j++) {
                if (j == i) continue;
                double dist = D.get(i, j);
                pq.add(new AbstractMap.SimpleEntry<>(j, dist));
            }

            // Take the top k closest
            Set<Integer> nbrs = new LinkedHashSet<>();
            for (int cnt = 0; cnt < k && !pq.isEmpty(); cnt++) {
                nbrs.add(pq.poll().getKey());
            }
            adjacency.add(nbrs);
        }

        return adjacency;
    }
}

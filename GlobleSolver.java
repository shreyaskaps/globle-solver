import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GlobleSolver {
    private static final double TOLERANCE_MILES = 200.0;
    private static final int ADJ_NEIGHBOR_COUNT = 10;
    private static final double EARTH_RADIUS_KM = 6371.0;

    static class Country {
        String name;
        double lat, lon;
        Country(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    public static void main(String[] args) throws IOException {
        // 1) Load countries
        List<Country> countries = loadCountries("data/country-coord.csv");
        Map<String,Integer> idxMap = new HashMap<>();
        for (int i = 0; i < countries.size(); i++) {
            idxMap.put(countries.get(i).name.toLowerCase(), i);
        }

        // 2) Compute all‐pairs distances
        double[][] distKm = new double[countries.size()][countries.size()];
        for (int i = 0; i < countries.size(); i++) {
            for (int j = i; j < countries.size(); j++) {
                double d = haversine(countries.get(i), countries.get(j));
                distKm[i][j] = d;
                distKm[j][i] = d;
            }
        }

        Scanner in = new Scanner(System.in);

        // 3) Read the user's closest guess and its distance
        System.out.print("Enter your closest guess country: ");
        String guessName = in.nextLine().trim().toLowerCase();
        Integer guessIdx = idxMap.get(guessName);
        if (guessIdx == null) {
            System.err.println("Unknown country: " + guessName);
            return;
        }

        System.out.print("Enter distance to target (miles; 0 = adjacent): ");
        double miles;
        try {
            miles = Double.parseDouble(in.nextLine().trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid distance.");
            return;
        }
        double tolKm   = TOLERANCE_MILES * 1.60934;
        double milesKm = miles * 1.60934;

        // 4) Build candidate set from that single reading
        Set<Integer> candidates = new HashSet<>();
        if (miles == 0) {
            // take the ADJ_NEIGHBOR_COUNT nearest centroids
            PriorityQueue<Map.Entry<Integer,Double>> pq =
                    new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));
            for (int i = 0; i < countries.size(); i++) {
                if (i == guessIdx) continue;
                pq.add(Map.entry(i, distKm[guessIdx][i]));
            }
            for (int i = 0; i < ADJ_NEIGHBOR_COUNT && !pq.isEmpty(); i++) {
                candidates.add(pq.poll().getKey());
            }
        } else {
            for (int i = 0; i < countries.size(); i++) {
                double d = distKm[guessIdx][i];
                if (Math.abs(d - milesKm) <= tolKm) {
                    candidates.add(i);
                }
            }
        }

        if (candidates.isEmpty()) {
            System.out.println("No candidates match your distance reading.");
            return;
        }

        // 5) Compute entropy for each possible next guess
        double tot = candidates.size();
        Map<Integer,Double> entropyMap = new HashMap<>();
        for (int g = 0; g < countries.size(); g++) {
            // skip guessing the same country you just tried
            if (g == guessIdx) continue;

            // build distribution of predicted "redness" over candidates
            Map<Integer,Integer> freq = new HashMap<>();
            for (int c : candidates) {
                double d = distKm[g][c];
                int r = (int)Math.round(100 * Math.exp(-d / 5000.0));
                freq.merge(r, 1, Integer::sum);
            }

            // compute Shannon entropy
            double H = 0.0;
            for (int count : freq.values()) {
                double p = count / tot;
                H -= p * (Math.log(p) / Math.log(2));
            }
            entropyMap.put(g, H);
        }

        // 6) Pick the single highest‐entropy country
        int bestIdx = Collections.max(entropyMap.entrySet(),
                Map.Entry.comparingByValue()).getKey();
        System.out.printf("Next best guess: %s (entropy = %.4f bits)%n",
                countries.get(bestIdx).name,
                entropyMap.get(bestIdx));

        // Optionally, list the top 5 by entropy:
        System.out.println("\nTop 5 suggestions:");
        entropyMap.entrySet().stream()
                .sorted(Map.Entry.<Integer,Double>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> {
                    System.out.printf("• %s (%.4f bits)%n",
                            countries.get(e.getKey()).name, e.getValue());
                });
    }

    private static List<Country> loadCountries(String path) throws IOException {
        List<Country> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length < 3) continue;
                String name = p[0].trim();
                double lat = Double.parseDouble(p[p.length - 2].trim());
                double lon = Double.parseDouble(p[p.length - 1].trim());
                list.add(new Country(name, lat, lon));
            }
        }
        return list;
    }

    private static double haversine(Country a, Country b) {
        double lat1 = Math.toRadians(a.lat), lon1 = Math.toRadians(a.lon);
        double lat2 = Math.toRadians(b.lat), lon2 = Math.toRadians(b.lon);
        double dLat = lat2 - lat1, dLon = lon2 - lon1;
        double h = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(lat1)*Math.cos(lat2)
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }
}

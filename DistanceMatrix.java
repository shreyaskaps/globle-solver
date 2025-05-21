// DistanceMatrix.java

import java.util.List;

/**
 * Precomputes and stores the pairwise great-circle distances (in kilometers)
 * between a list of countries.
 */
public class DistanceMatrix {
    private final double[][] distKm;

    /**
     * Constructs the distance matrix for the given list of countries.
     * Runs in O(n²) time where n = countries.size().
     *
     * @param countries list of Country objects
     */
    public DistanceMatrix(List<Country> countries) {
        int n = countries.size();
        distKm = new double[n][n];
        for (int i = 0; i < n; i++) {
            distKm[i][i] = 0.0;
            for (int j = i + 1; j < n; j++) {
                double d = haversine(countries.get(i), countries.get(j));
                distKm[i][j] = d;
                distKm[j][i] = d;
            }
        }
    }

    /**
     * Returns the distance in kilometers between the i-th and j-th countries.
     *
     * @param i index of the first country
     * @param j index of the second country
     * @return great-circle distance in kilometers
     * @throws IndexOutOfBoundsException if i or j is out of range
     */
    public double get(int i, int j) {
        return distKm[i][j];
    }

    /**
     * Returns the number of countries (the dimension of this matrix).
     *
     * @return the size n, where the matrix is n×n
     */
    public int size() {
        return distKm.length;
    }

    // Haversine formula to compute great-circle distance between two centroids.
    private static double haversine(Country a, Country b) {
        final double R = 6371.0; // Earth radius in kilometers
        double lat1 = Math.toRadians(a.getLatitude());
        double lon1 = Math.toRadians(a.getLongitude());
        double lat2 = Math.toRadians(b.getLatitude());
        double lon2 = Math.toRadians(b.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        double sinDLat = Math.sin(dLat / 2);
        double sinDLon = Math.sin(dLon / 2);
        double h = sinDLat * sinDLat
                + Math.cos(lat1) * Math.cos(lat2)
                * sinDLon * sinDLon;
        return R * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }
}

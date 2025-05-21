// CountryLoader.java

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for loading Country objects from a CSV file.
 * Expects the CSV to have at least three columns per row:
 *   [0] = country name,
 *   [...],
 *   [p.length-2] = latitude,
 *   [p.length-1] = longitude.
 */
public class CountryLoader {

    /**
     * Loads a list of Country instances from the given CSV file path.
     *
     * @param csvPath path to the CSV file
     * @return list of Country objects
     * @throws IOException if the file cannot be read
     */
    public static List<Country> load(String csvPath) throws IOException {
        List<Country> countries = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            // Skip header
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length < 3) {
                    // malformed row; skip
                    continue;
                }
                String name = p[0].trim();
                // latitude is second‐to‐last column
                double lat  = Double.parseDouble(p[p.length - 2].trim());
                // longitude is last column
                double lon  = Double.parseDouble(p[p.length - 1].trim());
                countries.add(new Country(name, lat, lon));
            }
        }
        return countries;
    }
}

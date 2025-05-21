// Country.java

public class Country {
    private final String name;
    private final double latitude;
    private final double longitude;

    /**
     * Constructs a Country with the given name and centroid coordinates.
     *
     * @param name      the official country name
     * @param latitude  the centroid latitude, in degrees
     * @param longitude the centroid longitude, in degrees
     */
    public Country(String name, double latitude, double longitude) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Country name must be non-empty");
        }
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** @return the country’s official name */
    public String getName() {
        return name;
    }

    /** @return the centroid latitude in degrees */
    public double getLatitude() {
        return latitude;
    }

    /** @return the centroid longitude in degrees */
    public double getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return name + " (" + latitude + ", " + longitude + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Country)) return false;
        Country c = (Country) o;
        return Double.compare(c.latitude, latitude) == 0
                && Double.compare(c.longitude, longitude) == 0
                && name.equals(c.name);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        long temp = Double.doubleToLongBits(latitude);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        return result;
    }
}

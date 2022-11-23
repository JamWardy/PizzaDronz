package uk.ac.ed.inf;

/**
 * Representation of a No-Fly Zone, in the format returned by the REST-server
 */
public class NoFlyZone {
    private String name;
    private double[][] coordinates;

    public NoFlyZone(String name, double[][] coordinates){
        this.name = name;
        this.coordinates = coordinates;
    };

    public String getName() {
        return name;
    }

    public double[][] getCoordinates() {
        return coordinates;
    }
}

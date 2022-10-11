package uk.ac.ed.inf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.lang.Math;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents a point as a latitude and longitude pair of co-ordinates.
 * @param longitude The longitude of the point.
 * @param latitude The latitude of the point.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LngLat(double longitude, double latitude){
    /**
     * Returns whether the point represented by this LngLat object is within the central area, which is represented by the CentralArea class.
     * @return Boolean value for whether the point defined by this LngLat object is within the central area.
     * @param url The URL that the central area co-ordinates are accessed from
     */
    public boolean inCentralArea(URL url){
        CentralArea centralArea = CentralArea.getInstance(url);
        if (this.longitude <= centralArea.points[0].longitude || this.latitude >= centralArea.points[0].latitude){
            return false;
        }
        if (this.longitude <= centralArea.points[1].longitude || this.latitude <= centralArea.points[1].latitude){
            return false;
        }
        if (this.longitude >= centralArea.points[2].longitude || this.latitude <= centralArea.points[2].latitude){
            return false;
        }
        if (this.longitude >= centralArea.points[3].longitude || this.latitude >= centralArea.points[3].latitude){
            return false;
        }
        else{
            return true;
        }
    }

    /**
     * Default method for when no URL is passed as a parameter, calls inCentralArea(url) with a default url value.
     * @return Boolean value for whether the point defined by this LngLat object is within the central area.
     */
    public boolean inCentralArea() {
        try {
            URL url = new URL("https://ilp-rest.azurewebsites.net/centralArea");
            return inCentralArea(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns the distance in degrees between this point and another point.
     * @param point The point to which the distance is being calculated.
     * @return Euclidean distance between this point and the point inputted as a parameter as a double.
     */
    public double distanceTo(LngLat point){
        return Math.sqrt(Math.pow(point.longitude - this.longitude,2) + Math.pow(point.latitude - this.latitude,2));
    }

    /**
     * Returns whether the distance between this and another point is strictly less than 0.00015 degrees.
     * @param point The point which we are seeing if this point is close to.
     * @return A boolean for if this point is close to the point inputted as a parameter.
     */
    public boolean closeTo(LngLat point){
        return (this.distanceTo(point) < 0.00015);
    }

    /**
     * Returns a LngLat object for the position of the drone after a move of an inputted number of degrees.
     * @param move The angle in degrees at which the drone moves.
     * @return A LngLat object with the new position of the drone.
     */
    public LngLat nextPosition(double move){
        double newLongitude = this.longitude + Math.cos(Math.toRadians(move)) * 0.00015;
        double newLatitude = this.latitude + Math.sin(Math.toRadians(move)) * 0.00015;
        LngLat newPosition = new LngLat(newLongitude, newLatitude);
        return newPosition;
    }
}

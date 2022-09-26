package uk.ac.ed.inf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.lang.Math;

// record for LngLat
@JsonIgnoreProperties(ignoreUnknown = true)
public record LngLat(double longitude, double latitude){
    //constructor for LngLat
    public LngLat(double longitude, double latitude){
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public boolean inCentralArea(){
        CentralArea centralArea = CentralArea.getInstance();
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

    //returns Euclidean distance between this point and the point inputted as a parameter
    public double distanceTo(LngLat point){
        return Math.sqrt(Math.pow(point.longitude - this.longitude,2) + Math.pow(point.latitude - this.latitude,2));
    }

    // returns whether or not the distance between this and another point is strictly less than 0.00015 degrees
    public boolean closeTo(LngLat point){
        if (this.distanceTo(point) < 0.00015) {
            return true;
        }
        else {
            return false;
        }
    }

    /* return a LngLat object for the position of the drone after a move of an inputted number of degrees
    New positon calculated by adding the cosine of the move times 0.00015 degrees to the longitude, and the sine of the same to the latitude
     */
    public LngLat nextPosition(double move){
        double newLongitude = this.longitude + Math.cos(move) * 0.00015;
        double newLatitude = this.latitude + Math.sin(move) * 0.00015;
        LngLat newPosition = new LngLat(newLongitude, newLatitude);
        return newPosition;
    }
}

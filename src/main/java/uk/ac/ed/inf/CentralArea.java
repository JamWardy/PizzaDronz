package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
Singleton central area class which contains an array of four LngLat objects which represent the 4 corners of the rectangular central area.
 */
public class CentralArea {
    private static CentralArea instance = null;
    private LngLat[] points;

    /**
     * Returns the four vertices of the central area, starting top left and moving anti-clockwise.
     * @return Array of LngLat objects.
     */
    public LngLat[] getPoints(){
        return points;
    }

    /**
     * Private constructor class for CentralArea, this performs a REST-request which gets a JSON of the longitude and latitude
     * co-ordinates of the corners of the central area and converts this into an array of LngLat objects.
     */
    private CentralArea(URL url){
        try {
            // perform REST-request and turn result into array of LngLat objects, setting the points attribute to this array
            points = new ObjectMapper().readValue(url, LngLat[].class);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Singleton getInstance method, which ensures that only one REST-request to get the CentralArea is performed per application run.
     * @return The Singleton CentralArea object, which contains the co-ordinates of the central area.
     */
    public static CentralArea getInstance(URL url){
        // if there currently does not exist an instance of the CentralArea class, create one
        if (instance == null){
            instance = new CentralArea(url);
            return instance;
        }
        // otherwise return the existing instance of the CentralArea class
        else{
            return instance;
        }
    }
}

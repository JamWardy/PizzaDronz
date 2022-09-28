package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/*
Singleton central area class which contains an array of LngLat objects for points of the CentralArea
 */
public class CentralArea {
    private static CentralArea instance = null;
    public LngLat[] points;

    /*private constructor class for CentralArea, this performs a REST-request which gets a JSON of the longitude and latitude
    co-ordinates of the corners of the central area and converts this into an array of LngLat objects
     */
    private CentralArea(){
        try {
            // create URL object for the central area url
            URL url = new URL("https://ilp-rest.azurewebsites.net/centralArea");
            // perform REST-request and turn result into array of LngLat objects, setting the points attribute to this array
            points = new ObjectMapper().readValue(url, LngLat[].class);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // singleton get instance method, which ensures that only one REST-request to get the CentralArea is performed per application run
    public static CentralArea getInstance(){
        if (instance == null){
            instance = new CentralArea();
            return instance;
        }
        else{
            return instance;
        }
    }

}

package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class CentralArea {
    private static CentralArea instance = null;
    public LngLat[] points;

    private CentralArea(){
        try {
            URL url = new URL("https://ilp-rest.azurewebsites.net/centralArea");
            LngLat[] response = new ObjectMapper().readValue(url, LngLat[].class);
            points = response;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

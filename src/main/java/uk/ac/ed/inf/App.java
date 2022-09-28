package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        Restaurant a = new Restaurant();
        try {
            Restaurant[] restaurants = a.getRestaurantsFromRestServer(new URL("https://ilp-rest.azurewebsites.net/"));
            System.out.println(restaurants[0].name);
        }
        catch (MalformedURLException e){
            e.printStackTrace();
        }
    }
}
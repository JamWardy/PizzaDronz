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
        String date = args[0];
        String baseUrl = args[1];
        if (!baseUrl.endsWith("/")){
            baseUrl += "/";
        }
        try {
            Restaurant[] restaurants = Restaurant.getRestaurantsFromRestServer(new URL(baseUrl));
            System.out.println(Order.getDeliveryCost(restaurants,"Margarita","Margarita"));
        }
        catch (MalformedURLException e){
            e.printStackTrace();
        }
    }
}
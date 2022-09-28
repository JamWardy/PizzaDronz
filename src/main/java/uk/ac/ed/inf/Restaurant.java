package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/*
Restaurant class used to represent the results from the REST-request. Has the attributes name, longitude, latitude
and a list of menu objects of menu items
 */

public class Restaurant {
    //attributes, each has the same name as in the JSON format received from the REST-request
    public String name;
    public double longitude;
    public double latitude;
    public Menu[] menu;

    //no parameter constructor needed for JSON Object-Mapper
    public Restaurant(){

    };

    public Restaurant(String name, double longitude, double latitude, Menu[] menu){
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.menu = menu;
    };

    /* getRestaurantsFromServer returns a list of restaurant objects returned from the REST-request of the URL
    passed into the function
     */
    Restaurant[] getRestaurantsFromRestServer(URL serverBaseAddress){
        try {
            //adds a / to the base url if there is not one already
            if (!serverBaseAddress.toString().endsWith("/")) {
                serverBaseAddress = new URL(serverBaseAddress.toString() + "/");
            }
            // create a new url with restaurants/ added to the base url
            URL restaurantAddress = new URL(serverBaseAddress.toString() + "restaurants/");
            // get JSON from REST-request and parse this into an array of Restaurant objects
            Restaurant[] restaurants = new ObjectMapper().readValue(restaurantAddress, Restaurant[].class);
            return restaurants;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

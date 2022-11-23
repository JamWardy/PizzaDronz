package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;

/**
 * Restaurant class used to represent the results from the REST-request. Has the attributes name, longitude, latitude
 * and an array of Menu objects on offer by the restaurant.
 */

public class Restaurant {
    //attributes, each has the same name as in the JSON format received from the REST-request
    private String name;
    private double longitude;
    private double latitude;
    private Menu[] menu;

    /**
     * Default constructor for Restaurant.
     */
    public Restaurant(){};

    public Restaurant(String name, double longitude, double latitude, Menu[] menu){
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.menu = menu;
    }

    public String getName(){
        return name;
    }

    public double getLongitude(){
        return longitude;
    }

    public double getLatitude(){
        return latitude;
    }

    public Menu[] getMenu(){
        return menu;
    }

    /**
     * Performs a REST-request, retrieves the list of restaurants as a JSON, deserializes this
     * into an array of Restaurant objects and then returns this array.
     * @param serverBaseAddress The base address, this has 'restaurants/' added to it and then a REST-request is made to this full address.
     * @return An array of Restaurant objects that can be ordered from.
     */
    public static Restaurant[] getRestaurantsFromRestServer(URL serverBaseAddress){
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

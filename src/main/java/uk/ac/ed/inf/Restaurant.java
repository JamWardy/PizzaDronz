package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;

/**
 * Restaurant record used to represent the results from the REST-request. Has the attributes name, longitude, latitude
 * and an array of Menu objects on offer by the restaurant.
 */
public record Restaurant(String name, double longitude, double latitude, Item[] menu) {

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
                serverBaseAddress = new URL(serverBaseAddress + "/");
            }
            // create a new url with restaurants/ added to the base url
            URL restaurantAddress = new URL(serverBaseAddress + "restaurants/");
            // get JSON from REST-request and parse this into an array of Restaurant objects
            return new ObjectMapper().readValue(restaurantAddress, Restaurant[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

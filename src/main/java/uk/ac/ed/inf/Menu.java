package uk.ac.ed.inf;

/**
* Represents a single menu item on offer by a restaurant.
 */
public class Menu {
    private String name;
    private int priceInPence;

    /**
     * Default constructor class.
     */
    public Menu(){};

    public String getName(){
        return name;
    }

    public int getPriceInPence(){
        return priceInPence;
    }
}

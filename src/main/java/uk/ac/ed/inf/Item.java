package uk.ac.ed.inf;

/**
* Represents a single menu item on offer by a restaurant.
 */
public record Item(String name, int priceInPence) {
    public String getName(){
        return name;
    }

    public int getPriceInPence(){
        return priceInPence;
    }
}

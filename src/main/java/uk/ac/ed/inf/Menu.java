package uk.ac.ed.inf;

/*
Class used for individual menu items on offer by each restaurant. Has the attributes name, for name of the menu item
and priceInPence
 */
public class Menu {
    public String name;
    public int priceInPence;

    public Menu(){};

    public Menu(String name, int priceInPence){
        this.name = name;
        this.priceInPence = priceInPence;
    }
}

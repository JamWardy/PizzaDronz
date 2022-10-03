package uk.ac.ed.inf;

/**
 * A custom exception thrown if an order is made up of order items not all from the same restaurant
 */
public class InvalidPizzaCombinationException extends RuntimeException{
    public InvalidPizzaCombinationException(String errorMessage){
        super(errorMessage);
    }
}

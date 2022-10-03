package uk.ac.ed.inf;

/**
 * A custom exception thrown if an order is made up of order items not all from the same restaurant.
 */
public class InvalidPizzaCombinationException extends RuntimeException{
    /**
     * Default constructor for the exception.
     * @param errorMessage The error message that is shown when the error is thrown.
     */
    public InvalidPizzaCombinationException(String errorMessage){
        super(errorMessage);
    }
}

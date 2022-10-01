package uk.ac.ed.inf;

public class InvalidPizzaCombinationException extends RuntimeException{
    public InvalidPizzaCombinationException(String errorMessage){
        super(errorMessage);
    }
}

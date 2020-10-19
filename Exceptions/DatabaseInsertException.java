package Exceptions;

/**
 * This class represents an Exception that occurs when an insert on the Database fails.
 */
public class DatabaseInsertException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new {@code DatabaseInsertException}.
     * 
     * @param message The error message.
     */
    public DatabaseInsertException(String message) {
        super(message);
    }

}
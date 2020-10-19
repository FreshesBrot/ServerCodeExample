package Exceptions;

/**
 * This Exception is thrown when the same user attempts to perform a redundant operation
 */
public class IdenticalUserException extends UserProfileException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new {@code IdenticalUserException}.
     * @param message  The Error message.
     */
    public IdenticalUserException(String message) {
        super(message);
    }

}
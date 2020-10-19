package Exceptions;

/**
 * This Exception is thrown when a user that cannot be verified tries to request, join or leave a {@code GameRoom}.
 */
public class UnverifiedUserException extends UserProfileException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new {@code UnverifiedUserexception}.
     * 
     * @param message  The Error message.
     */
    public UnverifiedUserException(String message) {
        super(message);
    }
}
package Exceptions;

/**
 * This class represents an exception that is thrown when a database lookup for a user was unsuccessful.
 */
public class UserNotFoundException extends UserProfileException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new {@code UserNotFoundException}.
     * @param message The Error message.
     */
    public UserNotFoundException(String message) {
        super(message);
    }

}
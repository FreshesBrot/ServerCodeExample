package Exceptions;

/**
 * This Exception is thrown when a user tries to register with a firebaseID that is already registered.
 * 
 */
public class UserAlreadyExistingException extends UserProfileException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new {@code UserAlreadyExistingException}.
     * @param message The Error message.
     */
    public UserAlreadyExistingException(String message) {
        super(message);
    }

}
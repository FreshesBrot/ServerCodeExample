package Exceptions;

/**
 * This class is the super class for all Exceptions that deal with {@code UserProfile} related errors.
 */
public class UserProfileException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new {@code UserProfileException}.
     * 
     * @param message The error message.
     */
    public UserProfileException(String message) {
        super(message);
    }

}
package Exceptions;

/**
 * This Exception is thrown when a user tries to invoke a method they dont have authorization for.
 */
public class InsufficientPermissionException extends GameStateException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    public InsufficientPermissionException(String message) {
        super(message);
    }

}
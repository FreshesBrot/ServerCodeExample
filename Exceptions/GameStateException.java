package Exceptions;

/**
 * This class is the super class for all Exceptions related to {@code GameState} errors.
 */
public class GameStateException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new {@code GameStateException}.
     * @param message  The Error message.
     */
    public GameStateException(String message) {
        super(message);
    }

}
package Exceptions;

/**
 * This Exception is thrown when a method is called that would transition the {@code State}, but the {@code State} transition would break the game.
 */
public class IllegalGameStateTransitionException extends GameStateException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    public IllegalGameStateTransitionException(String message) {
        super(message);
    }
}
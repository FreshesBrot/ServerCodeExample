package Exceptions;

public class NotEnoughPlayersException extends GameStateException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
        /**
     * Constructs a new {@code NotEnoughPlayersException}.
     * @param message  The Error message.
     */
    public NotEnoughPlayersException(String message) {
        super(message);
    }
}
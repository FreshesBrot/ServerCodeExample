package Exceptions;

/**
 * This class is the super class for all Exceptions related to {@code GameRoom} errors.
 */
public class GameRoomException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new {@code GameRoomException}.
     * 
     * @param message The error message.
     */
    public GameRoomException(String message ){
        super(message);
    }
}
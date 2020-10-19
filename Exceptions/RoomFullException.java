package Exceptions;

/**
 * This class represents an Exception that is thrown when a player attempts to join a room that is already full.
 */
public class RoomFullException extends GameRoomException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new {@code RoomFullException}.
     * @param message The Error message.
     */
    public RoomFullException(String message) {
        super(message);
    }

}

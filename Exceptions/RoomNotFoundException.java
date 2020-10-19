package Exceptions;

/**
 * This class represents an exception that is thrown when a game room could not be found by ID.
 */
public class RoomNotFoundException extends GameRoomException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new RoomNotFoudnException.
     * @param message The Error message.
     */
    public RoomNotFoundException(String message) {
        super(message);
    }
}

package Exceptions;

/**
 * This class represents an Exception that is thrown when a player tries to create a new room while all rooms are occupied
 */
public class NoRoomForRoomsException extends GameRoomException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code NoRoomForRoomsException}.
     * @param message  The Error message.
     */
    public NoRoomForRoomsException(String message) {
        super(message);
    }
}

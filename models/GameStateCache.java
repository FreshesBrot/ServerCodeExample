package models;

import models.GameState.State;
import java.util.*;
import models.Logger.Tags;

import Exceptions.GameStateException;

/**
 * This class stores all running games. This is a purely static class. 
 * This class also contains some methods to help instantiating, deconstructing a {@code GameState} and
 */
public class GameStateCache {

    /**
     * This method sets a {@code GameState} to unoccupied and clears all data from that instance.
     * 
     * @param roomID The ID of the corresponding {@code GameRoom}.
     */
    public static void unoccupyRoom(int roomID) {
        allGames[roomID] = null;
    }

    /**
     * This method sets a {@code GameState} as occupied.
     * 
     * @param roomID The ID of the corresponding {@code GameRoom}.
     * @throws GameStateException This Exception class is thrown when: The room is tried to be occupied while its not unoccupied. 
     */
    public static void occupyRoom(int roomID) throws GameStateException {
        Logger.Log(Tags.CLL,"occupying room "+roomID);
        GameState state;

        if((state = allGames[roomID]) == null) {
            state = GameState.CreateGame();
            allGames[roomID] = state;  
        }
        
        state.occupy();
    }


    /**
     * This method starts the Game cycle for a single {@code GameState} instance. There need to be enough players in order to start the game.
     * 
     * @param room The corresponding {@code GameRoom}.
     * @throws GameStateException This Exception class is thrown when: Not enough players in the {@code GameRoom}, not the room owner, wrong {@code State}.
     */
    public static void startGame(String firebaseID, GameRoom room) throws GameStateException {
        Logger.Log(Tags.CLL,"Start game called on room "+room.unique());
        
        GameState state;

        if((state = allGames[Integer.parseInt(room.unique())]) == null) 
            throw new GameStateException("There is no GameState instance at index "+room.unique()+".");
        //

        state.start(firebaseID, room.getUIDsRaw(), room.maximumPlayers(), room.isCheated());
        
        Logger.Log(Tags.SCS,"Game started!");
    }

    /**
     * This method returns a single specified {@code GameState} instance.
     * 
     * @param roomID The ID of the {@code GameState} and the corresponding {@code GameRoom}.
     * @return A single specified {@code GameState} instance.
     * @throws GameStateException if there is no {@code GameState} instance at the specified ID, an Exception is thrown.
     */
    public static GameState getGameInstance(int roomID) throws GameStateException {
        GameState state;

        if((state = allGames[roomID]) == null) 
            throw new GameStateException("There is no GameState instance at index "+roomID+".");

        return state;
    }

    /* PRIVATE FUNCTIONS */

    private static final GameState[] allGames = new GameState[99]; //storage for all gamestates

    /** Private Constructor */
    private GameStateCache() {}

}
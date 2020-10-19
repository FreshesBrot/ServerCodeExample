package db;

import models.GameRoom;
import models.Logger;
import models.Logger.Tags;

import org.bson.Document;

/**
 * This class represents the interface between the Backend Database and the API, specified for the {@code GameRoom} class.
 */
public class GameRoomDatabase extends Database<GameRoom> {

    public static final String collectionIdent = "gameroomCollection";
    private static GameRoomDatabase instance = null;

    /**
     * This method accesses the singleton instance of the {@code GameRoomDatabase}.
     * 
     * @return The singleton instance of the {@code GameRoomDatabase}.
     */
    public static GameRoomDatabase getInstance() {
        if(instance == null) instance = new GameRoomDatabase();
        return instance;
    }

    /**
     * Private constructor as this is a singleton class
     */
    private GameRoomDatabase() {
        super(collectionIdent);
    }

    /**
     * This method attempts to find the {@code GameRoom} instance specified by its {@code _id}.
     * 
     * @param id The id of the {@code GameRoom}.
     * @return The Document representation of the {@code GameRoom}.
     */
    public Document findOne(String id) {
        try {
            return findByValue("_id", id).get(0);

        } catch (IndexOutOfBoundsException e) {
            Logger.Log(Tags.ERR,e.getMessage());
            return null;
        
        }
    }

    /**
     * This method returns the first {@code GameRoom} instance that is not occupied as a {@code Document}.
     * Returns null if there are no free {@code GameRooms}.
     *
     * @return An unoccupied {@code GameRoom} instance.
     */
    public Document findFreeRoom() {
        try {
            return findByValue("occupied", "false").get(0);

        } catch(IndexOutOfBoundsException e) {
            Logger.Log(Tags.ERR,"No free rooms available!");

            return null;
        }
    }
    
}
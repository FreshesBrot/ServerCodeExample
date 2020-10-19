package models;

import java.util.*;
import java.util.stream.Stream;

import Exceptions.*;
import interfaces.*;
import models.Logger.Tags;
import db.GameRoomDatabase;

import org.bson.Document;

/**
 * This Class represents a Game room where players join before starting a game. Each {@code GameRoom} can hold up to 4 players. If a GameRoom has 0 players, it deconstructs itself.
 */
public class GameRoom implements IDocumentCoder<GameRoom> {
    /**
     * TODO:
     * optimize API-calls
     * clarify certain errors
     * implement check-update call
     * 
     */

    private int maxPlayers; //max number of players per room
    private static final int maxRooms = 99; //max number of concurrent rooms
    private static int occupiedRooms = 0;  //number of currently occupied rooms


    private int roomID; //this field identifies the current room in use
    private int numOfPlayers; //current number of players in a room
    private List<String> players; //the list of players currently in the room. room owner is head of the list
    private HashMap<String,Boolean> updateFlags; //a hashmap that contains the updateflags
    private boolean occupied; //flag that determines if the room is in use
    private boolean cheated = false;
    
    /* API IMPLEMENTATIONS */

    /**
     * This method lets a player join a specified room.
     *
     * @param roomID The ID of the {@code GameRoom} the player wants to join.
     * @param firebaseID The Firebase ID of the player that wants to join.
     * 
     * @throws GameRoomException Throws this Exception class when: The specified {@code GameRoom} does not exist, the specified {@code GameRoom} is already full.
     * @throws UserProfileException Throws this Exception class when: The joining user does not exist, the joining user is already in the {@code GameRoom}.
     */
    public static void joinRoom(int roomID, String firebaseID) throws GameRoomException, UserProfileException {
       UserProfile.verifiy(firebaseID);

       (new GameRoom(roomID)).join(firebaseID);
    }
    
    /**
     * This method lets a player leave a specified room.
     * 
     * @param roomID The ID of the {@code GameRoom}  the player wants to leave
     * @param firebaseID The Firebase ID of the player that wants to leave.
     * 
     * @throws GameRoomException Throws this Exception class when: The specified {@code GameRoom} does not exist.
     * @throws UserProfileException Throws this Exception class when: The leaving user does not exist, the leaving user was not in the {@code GameRoom}.
     */
    public static void leaveRoom(int roomID, String firebaseID) throws GameRoomException, UserProfileException { 
        UserProfile.verifiy(firebaseID);

        GameRoom room;
        (room = new GameRoom(roomID)).leave(firebaseID);
        if(!room.occupied) GameStateCache.unoccupyRoom(roomID);
    }
    
    /**
     * This method returns a valid roomID that will be set occupied if possible. The player that requested the room will join the new room as its owner.
     *
     * @param firebaseID The Firebase ID of the player that requested a room.
     * @param maxPlayers The maximum amount of players for this room.
     * @param cheated Wether the {@code GameRoom} is cheated or not.
     * @return Returns the ID of the {@code GameRoom} that has been occupied for the requesting player.
     * 
     * @throws GameRoomException This Exception class is thrown when: No new {@code GameRoom} can be constructed.
     * @throws UserProfileException This Exception class is thrown when: The requesting user does not exist.
     * @throws GameStateException This Exception class is thrown when: The room is tried to be occupied while its not unoccupied. 
     */
    public static int requestGameRoom(String firebaseID, int maxPlayers, boolean cheated) throws GameRoomException, UserProfileException, GameStateException {
        UserProfile.verifiy(firebaseID);    
        
        int newID;
        Document gameRoomInstance;
        newID = ((gameRoomInstance = GameRoomDatabase.getInstance().findFreeRoom()) == null) ? -1 : Integer.parseInt(gameRoomInstance.getString("_id"));
        
        GameRoom gm = setOccupiedFlag(newID, maxPlayers);
        gm.cheated = cheated;

        gm.join(firebaseID);
        GameStateCache.occupyRoom(gm.roomID);

        return gm.roomID;    
    }

        
    /**
     * This method retrieves all current users in the {@code GameRoom} specified by the room's ID.
     * The room owner is always the first entry.
     * 
     * @param roomID The ID of the room from which users are to be retrieved
     * @return An Array of Strings of all users in the room.
     * @throws RoomNotFoundExcetpion Throws an Exception when the requested room was either not found or is unoccupied.
     * 
     */
    public static List<String> getCurrentPlayers(int roomID) throws RoomNotFoundException {
            
        List<String> players = (new GameRoom(roomID)).players;
        List<String> names = new ArrayList<String>(4);
        
        for(String s : players) {
            try {
                names.add(UserProfile.findUserName(s));

            } catch (UserNotFoundException e) {
                //as usernames are verified before joining, there should never be an instance of an unknown user (unless something goes wrong)
                names.add("##UNKNOWN");

            }
        }
        
        return names;

    }
    
    /**
     * This method checks to see for a specific user if the room theyre in changed and requires an update on the client side.
     * 
     * @param firebaseID The Firebase ID of the specified user.
     * @param roomID The room ID of the {@code GameRoom} the user is in.
     * @return Whether an update is required or not.
     * 
     * @throws GameRoomException Throws this Exception class when: The {@code GameRoom} could not be found, 
     * @throws UserProfileException Throws this Exception class when: The user does not exist, The user is not in the specified {@code GameRoom}.
     */
    public static boolean checkUpdate(String firebaseID, int roomID) throws GameRoomException, UserProfileException {
        UserProfile.verifiy(firebaseID);
        
        return (new GameRoom(roomID)).checkUpdate(firebaseID);
    }

    /**
     * This method returns the raw Firebase IDs of the players in the {@code GameRoom}.
     */
    public List<String> getUIDsRaw() {
        return players;
    }

    /**
     * This method creates a Database instanced {@code GameRoom}.
     * 
     * @param roomID The ID of the room that is instanced from the {@code Database}.
     * @return The {@code GameRoom} instance.
     * @throws GameRoomException Throws this Exception class when: The roomID does not correspond to an existing {@code GameRoom}.
     */
    public static GameRoom GameRoomInstance(int roomID) throws GameRoomException {
        return new GameRoom(roomID);
    }

    /**
     * DEBUG FUNCTION: Flushes a {@code GameRoom}, removing all players and setting room to unoccupied.
     * 
     * @param roomID ID of the {@code GameRoom} that is flushed.
     * @throws GameRoomException Throws this Exception class when: The roomID does not correspond to an existing {@code GameRoom}.
     */
    public static void ForceFlush(int roomID) throws GameRoomException {
        (new GameRoom(roomID)).unsetOccupiedFlag();
    } 

    public int maximumPlayers() {
        return maxPlayers;
    }

    /**
     * This method returns the maximum amount of players for the {@code GameRoom} given by ID.
     * 
     * @param roomID The ID of the {@code GameRoom}.
     * @return The maximum amount of Players for the {@code GameRoom} with the given roomID.
     * @throws GameRoomException Throws this Exception class when: The roomID does not correspond to an existing {@code GameRoom}.
     */
    public static int maximumPlayers(int roomID) throws GameRoomException {
        return (new GameRoom(roomID)).maxPlayers;
    }

    public boolean isCheated() {
        return cheated;
    }

    /* INTERFACE IMPLEMENTATIONS */

    @Override
    public Document encode() {
        Document doc = new Document("_id",String.valueOf(roomID)); 
        List<String> flags = new ArrayList<>(4);

        for(String s : players) {
            flags.add(String.valueOf(updateFlags.get(s)));
        }
        
        doc.append("maxPlayers",String.valueOf(maxPlayers))
            .append("numPlayers",String.valueOf(numOfPlayers))
            .append("players",players)
            .append("update",flags)
            .append("occupied",String.valueOf(occupied))
            .append("cheated",String.valueOf(cheated));
            
            return doc;
            
    }
    
    @Override
    public GameRoom decode(Document doc) {
    
        roomID = Integer.parseInt(doc.getString("_id"));
        maxPlayers = Integer.parseInt(doc.getString("maxPlayers"));
        numOfPlayers = Integer.parseInt(doc.getString("numPlayers"));
        players = doc.<String>getList("players",String.class);
        List<String> flags = doc.<String>getList("update",String.class);        
        occupied = Boolean.parseBoolean(doc.getString("occupied"));
        cheated = Boolean.parseBoolean(doc.getString("cheated"));

        updateFlags = new HashMap<>();

        for(int i = 0; i < players.size();i++) {
            updateFlags.put(players.get(i),Boolean.parseBoolean(flags.get(i)));
        }
        
        return this;
    }
    
    
    @Override
    public String unique() {
        return String.valueOf(roomID);
    }
    
    /* PRIVATE FUNCTIONS */ 

    /**
     * The default constructor is used by the requestRoom function to create a blank {@code GameRoom} that it can update and write back to the Database.
     */
    private GameRoom() {
        defaultInit();
    }

    /**
     * Constructs a room containing all players that are currently in the room. Information is retrieved from the Database.
     * The Constructor is private because the {@code GameRoom} object is only needed for managing joining and leaving the room.
     *
     * @param roomID The ID of the room the player wants to join
     * @throws RoomNotFoundException Throws an exception when the specified room could not be found.
     */
    private GameRoom(int roomID) throws RoomNotFoundException {
        lookup(roomID);
    }

    /**
     * This method asks the DB for all current users in the {@code GameRoom} specified by its ID.
     *
     * @param roomID The ID of the room that is to be looked up.
     * @throws RoomNotFoundException Throws an Exception when the specified room could not be found.
     */ 
    private void lookup(int roomID) throws RoomNotFoundException {
        //Mongo DB lookup
        //initializes the users array and the number of players, as well as owner
        //roomnotfound is thrown when a room is not occupied
        Document doc;
        Logger.Log(Tags.INF,"Attemtping room lookup at "+roomID);
        
        if((doc = GameRoomDatabase.getInstance().findOne(String.valueOf(roomID))) == null) {
            Logger.Log(Tags.FLR,"Room lookup returned null.");

            throw new RoomNotFoundException("The room " + roomID + " could not be found.");
        }    
        
        Logger.Log(Tags.SCS,"Room successfully found and instantiated.");
        
        decode(doc);
        
    }    

    /**
     * This method default initializes a new {@code GameRoom} with default metrics.
     */ 
    private void defaultInit() {
        roomID = 0;
        numOfPlayers = 0;
        players = new ArrayList<String>();
        updateFlags = new HashMap<>();
        occupied = false;
    }    

    /**
     * This method is used to join a {@code GameRoom} on the Database side. If a new player joins, all other players' updateFlags are set to {@code true}.
     *
     * @param firebaseID The firebaseID of the user that joins the room
     * 
     * @throws RoomFullException Throws an Exception when the User wants to join a room that is already full.
     * @throws RoomNotFoundException Throws an Exception when the specified room is not occupied.
     * @throws IdenticalUserException Throws an Exception when a user tries to join the same room again.
     */  
    private void join(String firebaseID) throws RoomFullException, RoomNotFoundException, IdenticalUserException {
        if(numOfPlayers >= maxPlayers) throw new RoomFullException("The Room " + roomID + " is already full!");
        if(!occupied) throw new RoomNotFoundException("The Room "+roomID+" is not occupied!");
        if(players.indexOf(firebaseID) != -1) throw new IdenticalUserException("The user "+firebaseID+" is already in room "+roomID+"!");


        //write the new user to the DB
        numOfPlayers++;

       updateFlags.replaceAll((key,value) -> true);

        players.add(firebaseID);
        updateFlags.put(firebaseID,false);
        

        GameRoomDatabase.getInstance().update(this);
    }        

    /**
     * This method lets a player leave their current room and write it back to the Database. Once the player left, all other players' updateFlags are set to {@code true}.
     * If all players left the room, the room is set as unoccupied.
     * 
     * @param firebaseID The player that wants to leave the room.
     * 
     * @throws UserNotFoundException If the leaving user was not in the room to begin with, an exception is thrown.
     */ 
    private void leave(String firebaseID) throws UserNotFoundException {


        int userIndex = players.indexOf(firebaseID);
        //if userIndex is less than 0, the user was not in the gameroom

        if(userIndex < 0) {
            Logger.Log(Tags.ERR,"User "+firebaseID+" is not in room.");
            throw new UserNotFoundException("The user "+firebaseID+" is not in the room!");
        
        } else {
            
            //if the last user left, room can be cleared and marked unoccupied
            if(--numOfPlayers==0) {
                unsetOccupiedFlag();
                return;
            }
            

            players.remove(userIndex);
            updateFlags.remove(firebaseID);

            updateFlags.replaceAll((key,value) -> true);

            GameRoomDatabase.getInstance().update(this);
        }
    }    

    /**
     * This method flags the room as unoccupied and writes it back to the Database. The entire room is cleared.
     *
     */ 
    private void unsetOccupiedFlag() {
        //set flags on BD side
        Logger.Log(Tags.INF,"Room "+roomID+" empty. Flagging as unset.");

        occupiedRooms--;
        numOfPlayers = 0;
        players.clear();
        updateFlags.clear();
        occupied = false;
        GameRoomDatabase.getInstance().update(this);
    }    

    /**
     * This method occupies a room on the DB side. If the {@code roomID} is negative,
     * the method attempts to write a new {@code GameRoom} into the Database instead.
     * The method returns the newly created {@code GameRoom} for further processing.
     *
     * @param roomID The ID of the room that will be flagged as occupied.
     * @return The new {@code GameRoom} that is used for further processing.
     * @throws NoRoomForRoomsException Throws an exception when the maximum number of Rooms is occupied.
     */ 
    private static GameRoom setOccupiedFlag(int roomID, int maxPlayers) throws NoRoomForRoomsException {
        //set flags on DB side
        
        if(occupiedRooms<maxRooms) {
            occupiedRooms++;
            
            if(roomID<0) {
                //the player requested a gameroom while all existing gamerooms in the database were occupied.
                //but theres still room for more gamerooms
                //the trick is: if all rooms are occupied but there is still room for more rooms, take current occupiedRooms number as new roomID
                GameRoom newRoom = new GameRoom();
                newRoom.roomID = occupiedRooms;
                newRoom.maxPlayers = maxPlayers; 
                newRoom.occupied = true;
                GameRoomDatabase.getInstance().insert(newRoom);
                
                return newRoom;
            }    

            GameRoom room = new GameRoom();
            room.maxPlayers = maxPlayers;
            room.roomID = roomID;
            room.occupied = true;
            return room;    
        }    
        
        throw new NoRoomForRoomsException("The maximum number of " + maxRooms + "Rooms has been reached and no Room could be created!");
    }

    /**
     * This method returns the state of a users updateFlag. If the flag was {@code true},  the flag is set to {@code false} again.
     * 
     * @param firebaseID The Firebase ID of the user that is to be checked
     * @return The value of the flag.
     * 
     * @throws UserNotFoundException This Exception is thrown when the specified user is not in the room.
     */
    private boolean checkUpdate(String firebaseID) throws UserNotFoundException {
        
        if(!updateFlags.containsKey(firebaseID))
            throw new UserNotFoundException("The user "+firebaseID+" is not in room "+roomID+"!");

        boolean update = updateFlags.get(firebaseID);

        if(!update) return update;

        updateFlags.replace(firebaseID, false);

        GameRoomDatabase.getInstance().update(this);

        return update;
    }

}

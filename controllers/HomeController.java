package controllers;

import models.*;
import models.GameState.PlayerState;
import models.Logger.Tags;
import Exceptions.*;
import interfaces.ITryCatchExecution;
import db.TestDB;
import db.UserProfileDatabase;
import play.libs.Json;
import play.mvc.*;
import play.data.DynamicForm;
import play.data.FormFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.util.*;


/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {
    /** TODO:
     * Make Game Synchronization logic.
     * Make Game Synchronization API.
     * Optimize BSON-JSON-Document parsing (way later though)
     *  theres a problem with how objects are stored/returned from the DB:
     *   class instance is parsed to and from a bson document
     *   class instance is parsed to a json document
     *  maybe figure out a way to parse from bson to json without intermediate class instance?
     */

    //for testing
    private static int connections = 0;


    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        return ok(views.html.index.render());
    }

    //Test Methode für (S.40)
    public Result test() {
            Logger.Log(Tags.SCS,"request received");
            return ok("ok ok ok x"+(++connections));
            //return ok("Got Request");
    }

    public Result test2(int val,String s1, String s2) {
        Logger.Log(Tags.INF,"Running DB test...");
        Logger.Log(Tags.CLL,"Called /db/test/"+s1+"/"+s2);

       TestObject test = new TestObject(val);
       test.setStuff1(s1);
       test.setStuff2(s2);

       if(TestDB.getInstance().insert(test) != 0) {
            Logger.Log(Tags.FLR,"Insert failed. Returning false.");
            return ok("##ERR");
       }

       test = TestDB.getInstance().find(String.valueOf(val));

       return ok(test.toString());
    }

    public Result testFind(int val) {
        Logger.Log(Tags.INF,"Running DB test...");
        Logger.Log(Tags.CLL,"Called /db/test/"+val);

        TestObject test = TestDB.getInstance().find(String.valueOf(val));

        return ok(test.toString());

    }

    /**
     * This Section of the Server-API handles requests that are tied to user information, updates and registration.
     */

    /**
     * This Method registers a new user with their specified username. The new user is written to the Database and default-initialized.
     * If the user is already registered, the user will not be registered again and the registration is cancelled.
     *
     * @param firebaseID The unique Firebase ID of the user that wants to register.
     * @param username The specified username of the user that wants to register.
     *                 Unlike the Firebase ID, the username will be visible to everyone.
     * @return Returns the Result of the registration as a JSON-String, either successful or failed.
     *         Object Layout
     *         STATUS:
     *         MESSAGE:
     *         VALUE:
     *           username:
     *           tickets:
     *           friends:
     *             [..]
     *           rivals:
     *             [..]
     *           streaks:
     *             [..]          
     */
    public Result Register(String firebaseID, String username) {
        //registration can fail, needs to be caught
        Logger.Log(Tags.CLL, "called /db/register",firebaseID,username);
        
        return Execute(new ITryCatchExecution(){
            public Result Try() throws Exception  {
                return JsonFactory.toJson(new UserProfile(firebaseID,username));
            }
        });
    }

    /**
     * This Method retrieves all relevant user information that is to be displayed on a user's profile.
     * If the user could not be found, an error message is returned instead.
     *
     * @param firebaseID The unique Firebase ID used for the Database lookup
     * @return Returns a JSON-String that contains the result of the API-call and user information. If STATUS is 1, USER does not exist.
     *         Object Layout:
     *         STATUS:
     *         MESSAGE:
     *         VALUE:
     *          ...
     */
    public Result GetUserInfo(String firebaseID) {
        Logger.Log(Tags.CLL,"called /db/info",firebaseID);

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception  {
                return JsonFactory.toJson(UserProfile.findUser(firebaseID));
            }
        });
    }

    /**
     * This method searches for a specific user identifiable by their username in the {@code UserProfile} format. 
     * 
     * @param username The name of the user that is searched for.
     * @return Returns a JSON-String that contains the result of the API-call and user information. If STATUS is 1, USER does not exist.
     *         Object Layout:
     *         STATUS:
     *         MESSAGE:
     *         USER:
     *          ...
     */
    public Result SearchFriend(String username) {
        Logger.Log(Tags.CLL,"called /db/addFriend/search",username);

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception  {
                return JsonFactory.toJson((UserProfile.findUserByName(username)));
            }
        });
    }

    /**
     * This method adds a friend to the user specified by their Firebase ID.
     * 
     * @param firebaseID The user requesting the addfriend query.
     * @param username The name of the user the requesting user wants to add.
     * @return A JSON-String with a status code and a message.
     *         Object Layout:
     *         STATUS:
     *         MESSAGE:
     */ 
    public Result AddFriend(String firebaseID, String username) {
        Logger.Log(Tags.CLL, "called /db/addFriend",firebaseID,username);

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception  {
                UserProfile.findUser(firebaseID).addFriend(username);
                return JsonFactory.toJson();
            }
        });
    }

    public Result AddStreak(String firebaseID, String username, int streak) {
        return Execute(new ITryCatchExecution(){
            public Result Try() throws Exception {
                UserProfile.findUser(firebaseID).addStreak(username, streak);
                return JsonFactory.toJson();
            }
        });   
    }

    public Result UpdateStreak(String firebaseID, String username, int streak) {
        return Execute(new ITryCatchExecution(){
            public Result Try() throws Exception {
                UserProfile.findUser(firebaseID).updateStreak(username, streak);
                return JsonFactory.toJson();
            }
        });
    }

    public Result RemoveStreak(String firebaseID, String username) {
        return Execute(new ITryCatchExecution(){
            public Result Try() throws Exception {
                UserProfile.findUser(firebaseID).removeStreak(username);
                return JsonFactory.toJson();
            }
        });
    }

    /**
     * This section of the Server-API handles all the logic for joining, leaving, and creating a {@code GameRoom}.
     */

    /**
     * This Method lets a player join the specified room. Writes the user's Firebase ID to the Database.
     *
     * @param firebaseID The Firebase ID of the player that tries to join.
     * @param roomID The ID of the {@code GameRoom} the player wants to join.
     * @return A JSON-String with a status code and a message.
     *         Object Layout:
     *         STATUS:
     *         MESSAGE:
     */
    public Result JoinRoom(String firebaseID, int roomID) {
        Logger.Log(Tags.CLL,"called /gameSession/joinSession",firebaseID,String.valueOf(roomID));
        
        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception  {
                GameRoom.joinRoom(roomID, firebaseID);
                return JsonFactory.toJson();
            }
        });

    }

    /**
     * This Method lets a player leave the specified room. Removes the user's Firebase ID from the room entry and flags the room
     * unused if he was the last player to leave. If the owner of the room leaves, a new owner is established.
     *
     * @param firebaseID The Firebase ID of the player that wants to leave a room.
     * @param roomID The ID of the {@code GameRoom} the player wants to leave.
     * @return A JSON-String with a status code and a message.
     *         Object Layout:
     *         STATUS:
     *         MESSAGE:
     */
    public Result LeaveRoom(String firebaseID, int roomID) {
        Logger.Log(Tags.CLL,"called /gameSession/leaveSession",firebaseID,String.valueOf(roomID));
        
        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception  {
                GameRoom.leaveRoom(roomID, firebaseID);
                return JsonFactory.toJson();
            }
        });
    }

    /**
     * This method reserves a room for a player and returns a room ID other players can join. 
     * The player who is requesting a room joins the room as well.
     *
     * @param firebaseID The Firebase ID of the user that requested a Room.
     * @return  JSON-String containing a statuscode that is the room ID.
     *          Negative RoomID means a failed reservation.
     *          Object Layout:
     *          STATUS:
     *          MESSAGE:
     */
    public Result ReserveRoom(String firebaseID, int maxPlayers) {
        Logger.Log(Tags.CLL,"called /gameSession/createSession",firebaseID);
        
        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception  {
                return JsonFactory.toJson(GameRoom.requestGameRoom(firebaseID, maxPlayers,false));
            }
        });
        
    }

    /**
     * Retrieves the Maximum amount of players for the given {@code GameRoom} instance.
     * 
     * @param roomID ID of the {@code GameRoom}.
     * @return A JSON-String containing the maximum amount of players.
     */
    public Result MaximumPlayersOfRoom(int roomID) {
        //logger..

        return Execute(new ITryCatchExecution(){
            public Result Try() throws Exception {
                return JsonFactory.toJson(GameRoom.maximumPlayers(roomID));
            }
        });
    }

    /**
     * This method returns a JSON-String containing all current players of the room.
     *
     * @param roomID  The ID of the room the players are supposed to be retrieved from.
     * @return A JSON-Object containing all players as an array. 
     *         If an Exception occurs, there is no USERS JSON-Array.
     *         Object Layout:
     *         STATUS:
     *         MESSAGE:
     *         USERS:
     *           [..]
     * 
     */
    public Result currentPlayers(int roomID) {
        Logger.Log(Tags.CLL,"called /gameSession/curUsers",String.valueOf(roomID));

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception  {
                return JsonFactory.toJson(GameRoom.getCurrentPlayers(roomID));
            }
        });
    }

    /**
     * This method is used to ask if their {@code GameRoom} has updated.
     *
     * @param roomID The ID of the {@code GameRoom} that is to be checked.
     * @param firebaseID The Firebase ID of the user checking for an update.
     * @return A Status JSON-Object containing the update information.
     */
    public Result RoomUpdated(String firebaseID ,int roomID) {
        Logger.Log(Tags.CLL,"called /gameSession/update",String.valueOf(roomID));

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception  {
                return JsonFactory.toJson(GameRoom.checkUpdate(firebaseID, roomID));
            }
        });
    }


    /**
     * This section of the Server-API handles all the logic for syncing a game and all players in a game session.
     */

    /* ROOM OWNER CALLS */

    /**
     * Used by the room owner to start the game cycle.
     * 
     * @param firebaseID ID of the room owner.
     * @param roomID ID of the corresponding {@code GameState}.
     * @return JSON-String status message.
     */
    public Result StartGame(String firebaseID, int roomID) {
        Logger.Log(Tags.CLL,"called /ingame/start",firebaseID,String.valueOf(roomID));

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception  {
                GameStateCache.startGame(firebaseID, GameRoom.GameRoomInstance(roomID));
                return JsonFactory.toJson();
            }
        });
    }

    /* PLAYER CALLS */

    /* PRE-GAME FUNCTIONS */

    /**
     * Used to ask the server about the current state of the game.
     * 
     * @param roomID ID of the corresponding {@code GameState}.
     * @return JSON-String status message containing the current {@code State} of the game.
     */
    public Result AskState(int roomID) {
        Logger.Log(Tags.CLL,"called /ingame/askState",String.valueOf(roomID));

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception {
                return JsonFactory.toJson(GameStateCache.getGameInstance(roomID).getState());
            }
        });
    }

    /**
     * Retrieves the GMIndex randomly assigned by the Server.
     * 
     * @param roomID ID of the corresponding {@code GameState}.
     * @return JSON-String containing the GMIndex. 
     */
    public Result GetGMIndex(int roomID) {
        Logger.Log(Tags.CLL,"called /ingame/GMIndex",String.valueOf(roomID));

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception {
                return JsonFactory.toJson(GameStateCache.getGameInstance(roomID).getGMIndex());
            }
        });
    }

    /**
     * Retrieves the Minigame the GM chose.
     * 
     * @param firebaseID ID of the user asking for the Minigame - important for {@code GameState}
     * @param roomID ID of the corresponding {@code GameState}.
     * @return JSON-String containing the Minigame ID.
     */
    public Result GetMinigame(String firebaseID, int roomID) {
        Logger.Log(Tags.CLL,"called /ingame/minigame",firebaseID,String.valueOf(roomID));

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception {
                return JsonFactory.toJson(GameStateCache.getGameInstance(roomID).getMinigame(firebaseID));
            }
        });
    }

    /**
     * Retrieves the initial game values the GM calculated.
     * 
     * @param roomID ID of the corresponding {@code GameState}.
     * @return JSON-String containing a String that represents the data.
     */
    public Result GetInitValues(int roomID) {
        Logger.Log(Tags.CLL,"called /ingame/initValues",String.valueOf(roomID));

        return Execute(new ITryCatchExecution(){
            public Result Try() throws Exception {
                return JsonFactory.toJson(GameStateCache.getGameInstance(roomID).getInitialValues());
            }
        });
    }

    /**
     * Retrieve the sociality the GM calculated.
     * 
     * @param roomID ID of the corresponding {@code GameState}.
     * @return 
     */
    public Result GetSociality(int roomID) {
        Logger.Log(Tags.CLL,"called /ingame/getSociality",String.valueOf(roomID));

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception {
                return JsonFactory.toJson(GameStateCache.getGameInstance(roomID).getSociality());
            }
        });
    }

    /* MID-GAME FUNCTIONS */

    /**
     * Posts synchronization data of one player.
     * 
     * @param firebaseID The player thats posting their data.
     * @param roomID ID of the corresponding {@code GameState}.
     * @param data The synchronization data.
     * @return A JSON-String Status message.
     */
    public Result PostPlayerData(String firebaseID, int roomID, String data) {
        //logger..

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception {
                GameStateCache.getGameInstance(roomID).postPlayerData(firebaseID, data);
                return JsonFactory.toJson();
            }
        });
    }

    /**
     * Retrieves all players' relevant synchronization data.
     * 
     * @param roomID ID of the corresponding {@code GameState}.
     * @param playerIndex Index of the player with the required sync data.
     * @return JSON-String containing a specific player's sync data.
     */
    public Result GetPlayerData(int roomID, int playerIndex) {
        //logger..

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception {
                return JsonFactory.toJson(GameStateCache.getGameInstance(roomID).getPlayerData(playerIndex));
            }
        });
    }

    /* POST-GAME FUNCTIONS */

    /**
     * This method registers the minigame result of a single player.
     * 
     * @param firebaseID The Firebase ID of the player
     * @param roomID The ID of the corresponding {@code GameState}.
     * @return
     */ 
     public Result PostResult(String firebaseID, int roomID, String result) {
        Logger.Log(Tags.CLL, "called /game/postResult",firebaseID,String.valueOf(roomID),result); 

        return Execute(new ITryCatchExecution() {
           public Result Try() throws Exception {
               GameStateCache.getGameInstance(roomID).sync(firebaseID, result);
               return JsonFactory.toJson();
           } 
        });
    }    

    /**
     * 
     * 
     * @param firebaseID
     * @param roomID
     * @return
     */
    public Result Changed(String firebaseID, int roomID) {
        Logger.Log(Tags.CLL,"called /ingame/hasChanged",firebaseID,String.valueOf(roomID));

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception  {
                return JsonFactory.toJson(GameStateCache.getGameInstance(roomID).hasChanged(firebaseID));
            }
        });
    }

    public Result AllPlayerStates(int roomID) {
        Logger.Log(Tags.CLL,"called /ingame/playerStates",String.valueOf(roomID));
        
        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception {
                return JsonFactory.toJson(GameStateCache.getGameInstance(roomID).askPlayerStates());
            }
        });
    }

    /**
     * 
     * 
     * @param firebaseID
     * @param roomID
     * @return
     */ 
    public Result GetResults(int roomID) {
        Logger.Log(Tags.CLL,"called /ingame/allResults",String.valueOf(roomID));

        return Execute(new ITryCatchExecution(){
            public Result Try() throws Exception  {
                return JsonFactory.toJson(GameStateCache.getGameInstance(roomID).getResults());
            }
        });
    }    

    public Result PostReady(String firebaseID, int roomID) {
        Logger.Log(Tags.CLL,"called /ingame/ready",firebaseID,String.valueOf(roomID));

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception {
                GameStateCache.getGameInstance(roomID).postReady(firebaseID);
                return JsonFactory.toJson();
            }
        });
    }

    /* GAMEMASTER CALLS */
    
    /**
     * 
     * 
     * @param firebaseID
     * @param roomID
     * @param minigameID
     * @param sociality
     * @param initValues
     * @return
     */
    public Result SetMinigame(String firebaseID, int roomID, int minigameID, int sociality, String initValues) {
        Logger.Log(Tags.CLL,"called /ingame/setGame",
                firebaseID,String.valueOf(roomID),String.valueOf(sociality),
                String.valueOf(minigameID),"...");

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception {
                GameStateCache.getGameInstance(roomID).setMinigame(firebaseID, minigameID, sociality ,
                URLDecoder.decode(initValues, StandardCharsets.UTF_8.toString()));
                return JsonFactory.toJson();
            }
        });
    }

    /**
     * 
     * 
     * @param roomID
     * @param state
     * @return
     */
    public Result PlayersInState(int roomID, String state) {
        Logger.Log(Tags.CLL,"called /ingame/playersInState",String.valueOf(roomID),state);

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception {
                return JsonFactory.toJson(GameStateCache.getGameInstance(roomID).areInState(state));
            }
        });
    }

    /**
     * 
     * 
     * @param firebaseID
     * @param roomID
     * @return
     */
    public Result NextRound(String firebaseID, int roomID) {
        Logger.Log(Tags.CLL,"called /ingame/nextRound",firebaseID,String.valueOf(roomID));

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception{
                GameStateCache.getGameInstance(roomID).nextRound(firebaseID);
                return JsonFactory.toJson();
            }
        });
    }

    /**
     * 
     * 
     * @param firebaseID
     * @param roomID
     * @return
     */
    public Result GameOver(String firebaseID, int roomID) {
        Logger.Log(Tags.CLL,"called /ingame/gameOver",firebaseID,String.valueOf(roomID));

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception {
                GameStateCache.getGameInstance(roomID).setGameOver(firebaseID);
                return JsonFactory.toJson();
            }
        });
    }

    public Result BackToLobby(String firebaseID, int roomID) {
        //logger..

        return Execute(new ITryCatchExecution(){
            public Result Try() throws Exception {
                GameStateCache.getGameInstance(roomID).backToLobby(firebaseID);
                return JsonFactory.toJson();
            }
        });
    }

    /* DEBUG */

    public Result ReserveCheatedRoom(String firebaseID, int maxPlayers) {

        return Execute(new ITryCatchExecution() {
            public Result Try() throws Exception {
                return JsonFactory.toJson(GameRoom.requestGameRoom(firebaseID, maxPlayers,true));
            }
        });
    }



    public Result FlushRoom(int roomID) {
        Logger.Log(Tags.INF,"Flushing room"+roomID);

        return Execute(new ITryCatchExecution(){
            public Result Try() throws Exception {
                GameRoom.ForceFlush(roomID);
                GameStateCache.unoccupyRoom(roomID);
        
                return JsonFactory.toJson();        
            }
        });

    }

    public Result GameStateInfo(int roomID) {
        Logger.Log(Tags.INF,"Retrieving Info on a game state");

        return Execute(new ITryCatchExecution(){
            public Result Try() throws Exception {
                Logger.Log(Tags.INF,GameStateCache.getGameInstance(roomID).info());
                
                return JsonFactory.toJson();
            }
        });
    }



    /* PRIVATE FUNCTIONS */

    /**
     * This method serves as a shortcut for implementing a try-catch block that catches logs the error and returns the status message as a result.
     * 
     * @param t The unique {@code ITryCatchExecution} Interface instance
     * @return
     */
    private Result Execute(ITryCatchExecution t) {
        try {

            return t.Try();
        } catch (Exception e) {
            Logger.Log(Tags.ERR,e.getMessage());

            return JsonFactory.toJson(e);
        }
    }

}
   
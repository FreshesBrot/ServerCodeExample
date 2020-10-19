package models;

import java.util.*;
import models.Logger.Tags;

import Exceptions.*;

/**
 * This class represents the current state of a running game. It manages
 * synchronization of all players and handles game state transitions.
 */
public class GameState {

    /**TODO:
     * Figure out a way to store gamestates of each player
     * Figure out a way to sync everyplayer
     *  maybe with client-wise update pulls/pushes?
     *  probably have gamestate in memory; no writeback to database, too slow 
     * 
     * 
     */

    /**
     * This enumerator displays the current state of the game.
     */
    public enum State {
        /**The room for the Lobby is not in use. */
        UNOCCUPIED,
        
        /**The Game hasnt started yet; Gathering players / waiting to start the Game. */
        LOBBY,

        /**Before each Minigame, the Gamemaster is chosing a Game. */
        GM_CHOOSING,
        
        /**The Game is starting and all Players can retrieve initial values. */
        STARTING,
        
        /**Once all Players know the Game is starting, the Game will run. */
        RUNNING,
        
        /**If all Players are finished (everyone is waiting), results are displayed. */
        MINIGAME_END,
        
        /**If the GM has no HP left (or someone quits prematurely), the Game ends. */
        PARTY_END
    }

    /**
     * This enumerator is for tracking a player's state
     */
    public enum PlayerState {
        /**A general {@code PlayerState} that lets the {@code GameState} know the player received important information. */
        NOTIFIED,
        
        /** The player is still playing a game. */
        PLAYING,
    
        /**The player is done playing the game and posted their results. They will wait for all other players to finish. */
        WAITING, 
       
        /**The player is in the "Results Screen". */
        RESULTS, //
       
        /**  The player is ready to advance to the next round */
        READY    
       //if all players are ready (i.e everyone has seen the results) for the next minigame, the gamemaster can choose again (wrap back to GM_CHOOSING)
    }

    private State gameState; //current gamestate of the room
    private List<String> players; //players in the gameroom
    
    private int GMindex = -1; //index of the GM; -1 if no GM is assigned
    private int curMinigame = -1; //current running minigame; if -1, no minigame is played.
    private String initialValues = ""; //the initial values of the game
    private int sociality = -1; //determines how social the party is

    private HashMap<String,String> playerData; //hashmap to manage arbitrary synchronisation data
    private List<String> minigameResults; //results of minigame after each minigame
    private boolean initResults = true; //used to determine if the results need to be re-determined


    private HashMap<String,String> results; //player synchronized results
    private HashMap<String,PlayerState> states; //player syncrhonized states
    private HashMap<String,Boolean> changed; //flags to see if another player posted a result

    private static boolean firstTimeSetup = true; //check for initializing the transitions hashmap
    private static HashMap<State,State> transitions; //all legal transitions
    
    private boolean roomOwnerAlwaysGM = false; //if this flag is set, the room owner is always gamemaster

    /* API-IMPLEMENTATIONS */

    /*  ROOM OWNER RELEVANT FUNCTIONS */

    /**
     * This method creates a new {@code GameState}. Usually only needed by the {@code GameStateCache} to fill in a nullpointer.
     */
    public static GameState CreateGame() {
        Logger.Log(Tags.INF,"Creating a GameState instance");
        
        if(firstTimeSetup) {
            firstTimeSetup = false;
            initTransitions();
        }

        return new GameState();
    }
    
    /**
     * This method flags the {@code GameState} as occupied.
     * 
     * @throws IllegalGameStateTransitionException  Throws this Exception if the room is already occupied.
     */
    public void occupy() throws IllegalGameStateTransitionException {
        if(gameState != State.UNOCCUPIED)
            throw new IllegalGameStateTransitionException("The GameState of the Room is not UNOCCUPIED (gameState: " + String.valueOf(gameState) + ") and cannot be set to occupied!");
        //

        gameState = State.LOBBY;
    }

    /**
     * This method initiates a Game cycle - it starts the game and.
     * 
     * @param players The list of players from the {@code GameRoom} that want to play the game. Initializes the private field {@code players}. 
     * @throws GameStateException Throws this Exception class when: Less than 4 players in room, .
     */
    public void start(String firebaseID,List<String> players ,int numPlayers, boolean cheated) throws GameStateException {
        if(gameState != State.LOBBY)
            throw new IllegalGameStateTransitionException("You cannot start the game in "+String.valueOf(gameState)+" state!");
        if(!players.get(0).equals(firebaseID)) 
            throw new InsufficientPermissionException("You are not the room owner! Only the room owner can start the game.");
        if(players.size() < numPlayers)
            throw new NotEnoughPlayersException("Not enough Players in Room to start the Game!");
        //

        this.players = players;
        roomOwnerAlwaysGM = cheated;
        initGameValues();
        
        transit();
    }

    /* PLAYER RELEVANT FUNCTIONS */
    
    /**
     * This method returns the index of the player who was chosen as Gamemaster.
     * @return Index of the current GM.
     */
    public int getGMIndex() {
        return GMindex;
    }    
    
    /**
     * This method retrieves the minigame for the player that asked for it. The Firebase ID is important because it sets
     * that player's state to PLAYING, and only if all players are PLAYING should the game advance to the RUNNING state.
     *
     * @param firebaseID The Firebase ID of the player whos asking.
     * @return The ID of the minigame.
     * @throws GameStateException Throws this Exception when someones is asking for the Minigame at the wrong time.
     */
    public int getMinigame(String firebaseID) throws GameStateException {
        if(gameState != State.STARTING) 
            throw new GameStateException("Asking for the minigame in the wrong state!");
        //

        states.replace(firebaseID,PlayerState.PLAYING);
        if(allInState(PlayerState.PLAYING)) 
            transit();
        //

        return curMinigame;
    }

    /**
     * This method returns the initial values for every player.
     * 
     * @return Initial values to synchronize game start.
     */
    public String getInitialValues() {
        return initialValues;
    }

    public int getSociality() {
        return sociality;
    }

    /* GAMEMASTER RELEVANT FUNCTIONS */

    /**
     * This method can only be invoked by the GM. It sets the current minigame for all other players to retrieve. Can also only
     * be called if the current {@code State} is GM_CHOOSING.
     * 
     * @param firebaseID
     * @param minigame
     * @throws InsufficientPermissionException This Exception is thrown when anyone but the Gamemaster is calling the method.
     * @throws IllegalGameStateTransitionException This Exception is thrown when the method is called during the wrong {@code State}. 
     */ 
    public void setMinigame(String firebaseID, int minigame, int sociality, String initialValues) throws InsufficientPermissionException, IllegalGameStateTransitionException {
        Logger.Log(Tags.INF,"Setting minigame "+minigame);
        
        if(!isGM(firebaseID))
            throw new InsufficientPermissionException("You are not the Gamemaster! Only the Gamemaster can choose a minigame.");
        if(gameState != State.GM_CHOOSING)
            throw new IllegalGameStateTransitionException("You cannot set a Minigame in State "+String.valueOf(gameState)+"!");
        //
        
        curMinigame = minigame;
        this.sociality = sociality;
        this.initialValues = initialValues;

        transit();
    }

    /**
     * This method asks if all player's are in a specific {@code PlayerState}.
     *
     * @param state The specified state in String form.
     * @return Returns {@code true} if all players are in the specified state, and {@code false} otherwise.
     */
    public boolean areInState(String state) {
        return allInState(PlayerState.valueOf(state));
    }
    
    /**
     * This method can only be invoked by the Gamemaster.
     * This method starts a new minigame round and only resets all game relevant values.
     * 
     * @param firebaseID The Firebase ID of the calling user.
     * @throws InsufficientPermissionException This Exception is thrown when anyone but the Gamemaster is calling the method.
     * @throws IllegalGameStateTransitionException This Exception is thrown when the method is called during the wrong {@code State}. 
     */
    public void nextRound(String firebaseID) throws InsufficientPermissionException, IllegalGameStateTransitionException {
        if(!isGM(firebaseID))
            throw new InsufficientPermissionException("You are not the Gamemaster! Only the Gamemaster can advance the round");
        if(gameState != State.MINIGAME_END || !allInState(PlayerState.READY))
            throw new IllegalGameStateTransitionException("Cannot transition Gamestate! Either the Gamestate is wrong or not all players are ready.");
        //

        resetMinigameValues();
        transit();
    }

    /**
     * This method can only be invoked by the Gamemaster.
     * This method sets the Gamestate to PARTY_END, which signals the end of the minigame party. Winner is establishable for each client themself.
     * 
     * @param firebaseID The Firebase ID of the Gamemaster.
     * @throws InsufficientPermissionException  If anyone but the Gamemaster is calling this method, this Exception is thrown.
     * @throws IllegalGameStateTransitionException  If the Gamemaster tries to end the game prematurely, an Exception is thrown.
     */
    public void setGameOver(String firebaseID) throws InsufficientPermissionException, IllegalGameStateTransitionException {
        if(!isGM(firebaseID))
            throw new InsufficientPermissionException("You are not the Gamemaster! Only the Gamemaster can end the game.");
        if(gameState != State.MINIGAME_END)
            throw new IllegalGameStateTransitionException("Cannot end the Party mid-round!");
        //
        
        gameState = State.PARTY_END;
    }

    public void backToLobby(String firebaseID) throws IllegalGameStateTransitionException, InsufficientPermissionException {
        if(!isGM(firebaseID)) 
            throw new InsufficientPermissionException("You are not the GameMaster! You are not allowed to transition back to the Lobby.");
        if(gameState != State.PARTY_END)
            throw new IllegalGameStateTransitionException("Cannot go back to Lobby in GameState "+String.valueOf(gameState)+"!");
        //
    
        transit();
    }

    /* GAME RELEVANT FUNCTIONS */
    
    /**
     * This method returns the current {@code State} of the game. 
     * 
     * @return The current {@code State}. 
     */
    public State getState() {
        return gameState;
    }
    
    /**
     * This method syncs a single player and posts their result of a single minigame. It sets all changed flags to {@code true}.
     * If all players have posted their result, the {@code State} is changed to MINIGAME_END.
     * 
     * @param firebaseID
     * @param result
     */
    public void sync(String firebaseID, String result) {
        if(!players.contains(firebaseID)) return;
        //cannot post again!
        if(states.get(firebaseID) == PlayerState.WAITING) return;
    
        results.put(firebaseID, result);
        states.replace(firebaseID, PlayerState.WAITING);
        for(String s : players)
            changed.replace(s,true);
        //

        if(allInState(PlayerState.WAITING) && gameState == State.RUNNING) transit();
    }

    /**
     * This method returns a list of all player's current {@code PlayerState}.
     * @return List of current states of all players.
     */
    public List<String> askPlayerStates() {
        List<String> currentStates = new ArrayList<>(4);
        
        for(String s : players)
            currentStates.add(String.valueOf(states.get(s)));
        //
        
        return currentStates;
    }
    
    /**
     * This method returns for a single player if something has changed since the last time they asked.
     * 
     * @param firebaseID The Firebase ID of the asking player.
     * @return Returns {@code true} if a change has occured, and {@code false} if not. 
     *         Also returns {@code false} if the player is not part of the session.
     */
    public boolean hasChanged(String firebaseID) {
        if(!players.contains(firebaseID)) return false;

        boolean res = changed.get(firebaseID);
        if(res)
            changed.replace(firebaseID, false);
        //

        return res;
    }

    /**
     * This method posts a players game data that other players need to synchronize their game.
     * 
     * @param firebaseID The player that wants to synchronize their data.
     * @param data  The synchronization data.
     */
    public void postPlayerData(String firebaseID, String data) {
        if(!players.contains(firebaseID)) return;

        playerData.replace(firebaseID,data);
    }

    /**
     * This method retrieves a specific player's synchronization data for the player.  
     * 
     * @return A list of all sync data.
     */
    public String getPlayerData(int playerIndex) {
        if(playerIndex >= players.size()) 
            throw new IndexOutOfBoundsException("The playerIndex is outside the range [0,"+players.size()+"]!");
        //

        return playerData.get(players.get(playerIndex));
    }

    /**
     * This method returns a List of the Minigame results that is being used by the Client to determine if they won or not.
     * 
     * @return List of Strings that represent the result.
     */
    public List<String> getResults() {
        if(allInState(PlayerState.WAITING)) 
            initResults();
        //

        return minigameResults;
    }
    
    /**
     * This method tells the {@code GameState} that the specified player is ready for the next round. It sets the changed flag for all players except the calling player.
     * 
     * @param firebaseID The Firebase ID of the player that is ready.
     */
    public void postReady(String firebaseID) {
        if(!players.contains(firebaseID)) return;

        for(String s : players) {
            if(s.equals(firebaseID)) continue;
            changed.replace(s, true);
        }

        states.replace(firebaseID,PlayerState.READY);
    }

    public String info() {
        String result = "GAMESTATE INFO\n";
        
        result += "Current game state: "+String.valueOf(gameState)+"\n";
        
        switch(GMindex) {
            case -1:
            result += "GameMaster not established\n";
            break;
            default:
            result += "Gamemaster: "+players.get(GMindex)+" at index "+GMindex+"\n";
        }
        
        result += "Selected Minigame: "+curMinigame+"\n";
        result += "Initial Values: "+initialValues+"\n";
        result += "Sociality: "+sociality+"\n";
        
        result += "Player information: \n";
        
        for(String s : players) {
            result += "PLAYER: "+s+"\t";
            result += "STATE: "+states.get(s)+"\t";
            result += "RESULT: "+results.get(s)+"\t\n";
        }

        return result;
    }


    /* PRIVATE FUNCTIONS */

    private boolean isGM(String firebaseID) {
        return players.get(GMindex).equals(firebaseID);
    }

    /**
     * This method only resets minigame relevant values
     */
    private void resetMinigameValues() {
        curMinigame = -1;
        sociality = -1;
        initResults = true;
        
        minigameResults.clear();
        initialValues = "";

        for(String s : players) {
            results.replace(s,"");
            states.replace(s,PlayerState.NOTIFIED);
            changed.replace(s,false);
        }
    }

    /**
     * This method is used to initialize the hashmaps and lists when a game is started.
     */
    private void initGameValues() {
        if(roomOwnerAlwaysGM)
            GMindex = 0;
        else
            GMindex = (int) (Math.random() * (float) players.size());
        //

        for(String s : this.players) {    
            playerData.put(s,"");
            results.put(s,"");
            states.put(s,PlayerState.NOTIFIED);
            changed.put(s,false);
        }
    }

    /**
     * Method that initializes all transitions on first creation of a
     * {@code GameState} instance.
     */
    private static void initTransitions() {
        transitions = new HashMap<>();
        transitions.put(State.LOBBY,State.GM_CHOOSING);
        transitions.put(State.GM_CHOOSING, State.STARTING);
        transitions.put(State.STARTING, State.RUNNING);
        transitions.put(State.RUNNING, State.MINIGAME_END);
        transitions.put(State.MINIGAME_END, State.GM_CHOOSING);
        transitions.put(State.PARTY_END,State.LOBBY);
    }

    /**
     * This method determines if all players are in a specified state.
     * 
     * @param state The state all players are expected to be in.
     * @return Returns {@code true} if all players are in the specified state, and {@code false} otherwise.
     */
    private boolean allInState(PlayerState state) {
        boolean result = true;

        for(String s : players) 
            result = result && (states.get(s) == state);
        //

        return result;
    }

    /**
     * This method is responsible for managing all {@code GameState} transitions.
     */
    private void transit() {
        for(String s : players)
            changed.replace(s,true);
        gameState = transitions.get(gameState);
    }

    /**
     * This method initializes the minigame results once after each minigame if the {@code initResults} flag is set.
     * The method also sets every player's state to RESULTS, as they are able to view the results now, and the {@code GameState} is set to MINIGAME_END.
     */
    private void initResults() {
        Logger.Log(Tags.INF,"Initializing Game Results");

        if(initResults)
            for(String s : players) {
                minigameResults.add(results.get(s)); 
                states.replace(s, PlayerState.RESULTS);
            }
        //
        
        initResults = false;
    }

    /**This Constructor only intializes the hash maps and gamestate. */
    private GameState() {
        
        minigameResults = new ArrayList<>(4);
        playerData = new HashMap<>();
        results = new HashMap<>();
        states = new HashMap<>();
        changed = new HashMap<>();

        gameState = State.UNOCCUPIED;
    }


}
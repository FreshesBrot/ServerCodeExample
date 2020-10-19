package models;

import java.util.*;

import Exceptions.*;
import db.UserProfileDatabase;
import interfaces.IDocumentCoder;
import models.Logger.Tags;

import org.bson.Document;

/**
 * This Class represents a profile that is used to manage all relevant Data for
 * one user.
 * 
 */
public class UserProfile implements IDocumentCoder<UserProfile> {

    /**
     * TODO: optimize API-calls
     */ 
    
    private String firebaseID; // firebaseID of user, used to look up in DB
    private String username; // username (not firebaseID)
    private int tickets; // number of tickets the user has
    private List<String>  friends; // all friends of the user, saved and looked up via firebaseID String
    private List<String>  rivals; // array of all rivals names, saved as their firebase ID
    private HashMap<String,Integer> streaks; //all streaks per rival

    /* API IMPLEMENTATIONS */

    /**
     * This method does a shortened Database lookup and only retrieves the name. It
     * does not construct a new {@code UserProfile} object.
     *
     * @param firebaseID The specified user identified by their unique Firebase ID.
     * @return The Username as a String.
     * @throws UserNotFoundException An Exception is thrown if the specified User
     *                               could not be found.
     */ 
    public static String findUserName(String firebaseID) throws UserNotFoundException {
        return findUser(firebaseID).getUserName();
    }    

    /**
     * This method is used to verify if a specified user exists in the first place. 
     * This method throws an Exception in case the user could not be verified.
     * 
     * @param firebaseID The user that is to be verified.
     * @throws UnverifiedUserException Throws an Exception if the user could not be verified.
     */ 
    public static void verifiy(String firebaseID) throws UnverifiedUserException {
        if(!UserProfileDatabase.getInstance().verifyUser(firebaseID))
            throw new UnverifiedUserException("The user "+firebaseID+" does not exist!");
    }        

   /**
     * Looks up the User in the Database via their {@code firebaseID}.
     *
     * @param firebaseID The unique Firebase ID of a registered User.
     * @return Returns a Userprofile if one was found.
     * @throws UserNotFoundException Throws an Exception if the user could not be found.
     *                               
     */ 
    public static UserProfile findUser(String firebaseID) throws UserNotFoundException {
        Document lookup;

        if ((lookup = UserProfileDatabase.getInstance().findUser(firebaseID)) == null)
            throw new UserNotFoundException("The User with the ID "+firebaseID+" could not be found in the Database!");
        //

        return (new UserProfile()).decode(lookup);
    }    

    /**
     * 
     * 
     * @param username
     * @return
     * @throws UserNotFoundException
     */
    public static UserProfile findUserByName(String username) throws UserNotFoundException {
        Document lookup; 

        if ((lookup = UserProfileDatabase.getInstance().findUserByName(username)) == null)
            throw new UserNotFoundException("The User with useername " + username + " could not be found in the Database!");
        //

        return (new UserProfile()).decode(lookup);
    }

    /**
     * This method adds a friend to the friendslist. The specified username 
     * 
     * @param username The Friend specified by their username
     * @throws UserProfileException Throws this Exception class when: A user tried to add themself as a friend, 
     *                              a lookup returned no user, a user tried to add an existing again.
     */
    public void addFriend(String username) throws UserProfileException {
        UserProfile lookup;

        if (equals(lookup = findUserByName(username)))
            throw new IdenticalUserException("The user "+firebaseID+" tried adding themself as a friend!");
        //
        
        Logger.Log(Tags.INF,"Adding friend "+username);

        if(friends.indexOf(lookup.firebaseID) != -1)
            throw new UserAlreadyExistingException("The user " + firebaseID + " already has " + username + " as their friend!");
        //

        friends.add(lookup.firebaseID);

        update();

        Logger.Log(Tags.SCS,"Friend added.");
    }

    /**
     * Creates a new default Userprofile for a user that is registering to the
     * Database. The user can specify the Username they want.
     * 
     * @param firebaseID The newly created FirebaseID of the user.
     * @param username   The Username the user wants to use.
     * @throws UserAlreadyExistingException Exception is thrown when the user
     *                                      already exists.
     */
    public UserProfile(String firebaseID, String username) throws UserAlreadyExistingException, DatabaseInsertException {
        Logger.Log(Tags.INF,"Attempting to register new user "+firebaseID+"...");

        try {
            findUser(firebaseID);

            throw new UserAlreadyExistingException("The user " + firebaseID + " already exists!");
        } catch (UserNotFoundException e) {
            Logger.Log(Tags.SCS, "New user can be created.");

            // use default init for tickets and friends/rivals

            this.firebaseID = firebaseID;
            this.username = username;

            // default initialize user
            defaultInit();
            // write information to DB
            writeUserToDB();

        }
    }

    /* GETTER FUNCTIONS */
    
    /**
     * Get the username of the user.
     * 
     * @return Returns the user's username.
     */
    public String getUserName() {
        return username;
    }
    
    /**
     * Getter method for all friends. Exceptions are handled internally.
     * 
     * @return A list of Strings containing all friends
     */
    public List<String> getFriends() {
        return getUsernames(friends);
    }
    
    /**
     * Returns the number of Tickets of the user.
     * 
     * @return Number of Tickets
     */
    public int getTickets() {
        return tickets;
    }
    
    /**
     * Returns a list of the user's rivals .
     * The list's indices coincide with the indices of the streak, meaning a rival at index i is for the streak at index i.
     * 
     * @return Usernames of all rivals. A user that could not be resolved is called "##UNKNOWN"
     */ 
    public List<String> getRivals() {
        return getUsernames(rivals);
    }
    
    /**
     * This method returns a list of all streaks of the user. A loss streak is encoded as wins < 0.
     * The list's indices coincide with the indices of the rivals, meaning a streak at index i is for the rival at index i.
     * 
     * @return A list of all streaks.
     */
    public List<Integer> getStreaks() {
        List<Integer> allStreaks = new ArrayList<Integer>();

        for(String s : rivals) {
            allStreaks.add(streaks.get(s));
        }

        return allStreaks;
    }

    public int getStreak(String username) throws UserNotFoundException {
        UserProfile rival = findUserByName(username);

        if(!streaks.containsKey(rival.firebaseID)) 
            throw new UserNotFoundException("The user "+username+" is not a rival of "+this.username+"!");
        //

        return streaks.get(rival.firebaseID);
    }

    public void addStreak(String username, int streak) throws UserAlreadyExistingException, UserNotFoundException {
        Logger.Log(Tags.INF,"Adding a streak...");
        UserProfile rival = findUserByName(username);

        if(streaks.containsKey(rival.firebaseID)) 
            throw new UserAlreadyExistingException("There already is a streak for the rival "+username+"!");
        //

        Logger.Log(Tags.INF,"1");
        rivals.add(rival.firebaseID);
        Logger.Log(Tags.INF,"1");
        streaks.put(rival.firebaseID,streak);
        Logger.Log(Tags.INF,"1");
        update();
    }

    public void updateStreak(String username, int streak) throws UserNotFoundException {
        UserProfile rival = findUserByName(username);
        
        if(!streaks.containsKey(rival.firebaseID)) 
            throw new UserNotFoundException("The user "+username+" is not a rival of "+this.username+"!");
        //

        streaks.replace(rival.firebaseID, streak);
        update();
    }

    public void removeStreak(String firebaseID) throws UserNotFoundException {
        UserProfile rival = findUserByName(username);
        
        if(!streaks.containsKey(rival.firebaseID)) 
            throw new UserNotFoundException("The user "+rival.getUserName()+" is not a rival of "+this.username+"!");
        //
        
        streaks.remove(rival.firebaseID);
        rivals.remove(rival.firebaseID);
        update();
    }


    /**
     * Checks if a provided {@code UserProfile} is equal to this instance of a
     * {@code UserProfile}. Since user's are uniquely identified by their Firebase
     * ID, just checking for equality on the ID is enoug.
     *
     * @param user The {@code UserProfile} that is to be compared.
     * @return Whether the specified user and this instance are equal or not.
     */
    public boolean equals(UserProfile user) {
        return this.firebaseID == user.firebaseID;
    }
    
    
    /* INTERFACE IMPLEMENTATIONS */
    
    @Override
    public UserProfile decode(Document doc) {
        
        this.firebaseID = doc.getString("_id");
        this.username = doc.getString("username");
        this.tickets = Integer.parseInt(doc.getString("tickets"));
        this.friends = doc.<String>getList("friends",String.class);
        this.rivals = doc.<String>getList("rivals",String.class);
        List<Integer> streaksS = doc.<Integer>getList("streaks",Integer.class);
        
        for(int i = 0;i<rivals.size();i++) 
            streaks.put(rivals.get(i),streaksS.get(i));
        //

        return this;
    }
    
    @Override
    public Document encode() {
        Document doc = new Document("_id",firebaseID);
        
        //need to map to list
        List<Integer> streaksS = new ArrayList<>(rivals.size());
        for(String s : rivals)
            streaksS.add(streaks.get(s));
        

        doc.append("username",username)
        .append("tickets",String.valueOf(tickets))
        .append("friends", friends)
        .append("rivals",  rivals )
        .append("streaks", streaksS);
        
        return doc;
    }
    
    @Override
    public String unique() {
        return firebaseID;
    }
    
    /* PRIVATE FUNCTIONS */
    
    /**
     * This constructor is private and initializes only lists and hashmaps.
     */
    private UserProfile() { 
        friends = new ArrayList<>();
        rivals =  new ArrayList<>();
        streaks = new HashMap<>();
    }

    /**
     * This method creates a default instance of a {@code UserProfile}. Usually only
     * used when a new user is being registered.
     */
    private void defaultInit() {
        tickets = 100;
        
        friends = new ArrayList<>();
        rivals =  new ArrayList<>();
        streaks = new HashMap<>();
    }

    /**
     * This method writes the information of the {@code UserProfile} into the
     * Database. Only called from the Registration Constructor of this class.
     * 
     * @throws DatabaseInsertException Throws an exception when the insert on the DB failed.
     */
    private void writeUserToDB() throws DatabaseInsertException {
        if (UserProfileDatabase.getInstance().insert(this) != 0)
            throw new DatabaseInsertException("The insert on the Database failed.");
    }

    private static List<String> getUsernames(List<String> ids) {
        List<String> allNames = new ArrayList<String>(ids.size());

        // try to construct a user profile for each firebaseID string
        // catch the exception if a user has been deleted etc.

        for (String s : ids) {
            try {
                allNames.add(findUserName(s));
            } catch (UserNotFoundException e) {
                System.out.println(e.getMessage());
                // mark the user as an error
                allNames.add("##UNKNOWN");
            }
        }

        return allNames;
    }

    /**
     * A shortcut method for databse updates.
     */
    private void update() {
        UserProfileDatabase.getInstance().update(this);
    }

}
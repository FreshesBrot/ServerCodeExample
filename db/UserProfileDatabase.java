package db;

import models.*;
import models.Logger.Tags;

import org.bson.Document;

/**
 * This class represents the interface between the Backend Database and the API, specified for the {@code UserProfile} class.
 */
public class UserProfileDatabase extends Database<UserProfile> {

    private static UserProfileDatabase instance = null;
    private static final String collectionIdent = "userCollection"; //the collection name for the identifier, needed to create a MongoCollection. the string is unique to each database

    /**
     * This method is used to access the singleton instance of the {@code UserProfileDatabase}.
     * 
     * @return The singleton instance of the {@code UserProfileDatabase}.
     */
    public static UserProfileDatabase getInstance() {
        if(instance == null) instance = new UserProfileDatabase();
        return instance;
    }

    /**
     * This constructor is private as we only need one instance of the database throughout runtime.
     */
    private UserProfileDatabase() { 
        super(collectionIdent);
    }
    
    /**
     * This method checks if there is any instance of the specified user in the Database.
     * 
     * @param firebaseID The specified user.
     * @return Returns {@code true} if the user exists and {@code false} otherwise.
     */
    public boolean verifyUser(String firebaseID) {
        return exists("_id", firebaseID);
    }

    /**
     * This method returns the {@code Document} containing all of the users information.
     * 
     * @param firebaseID The Firebase ID of the user that is looked up.
     * @return A {@code Document} instance containing the user's information; needs to be decoded. Returns {@code null} if the user does not exist.
     */
    public Document findUser(String firebaseID) {
        try {
            return findByValue("_id", firebaseID).get(0);
        
        } catch (IndexOutOfBoundsException e) {
            
            return null;
        }
    }


    /**
     * This method is used to find a {@code UserProfile}s that is registered under the given name.
     * 
     * @param name The name that is searched for.
     * @return A {@code UserProfile} in {@code Document} form.
     */
    public Document findUserByName(String username) {
        try {
            return findByValue("username", username).get(0);
        
        } catch (IndexOutOfBoundsException e) {
            
            return null;
        }
    }
}
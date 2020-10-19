package models;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import play.mvc.*;

import models.*;
import models.Logger.Tags;
import Exceptions.*;

import java.util.List;

/**
 * This purely static class converts relevant Objects into JSON-String objects that are sent back to the client as a response to an API-call.
 * The JsonFactory creates a JSON-String with a status message at the very beginning for communicating the result of an API-call.
 */
public class JsonFactory {

    /**
     * The Constructor for the {@code JsonFactory} is private because this class does not need an instance.
     */
    private JsonFactory() {};

/*
    public static Result toJson(ValueContainer<?>... containers) {
        
        containers[0].getValue().toString();
    }

    public static Result toJson(ValueContainer<List<String>> list) {
        List<String> vals = list.getValue();


    }
*/

    /**
     * Creates a JSON-String for a simple OK status message.
     * 
     * @return A response-ready OK status message.
     */
    public static Result toJson() {
        return Results.ok(statusBody(0,"OPERATION OK"));
    }

    /**
     * Creates a JSON-String representation of a {@code UserProfile} that is returned to the client for further processing.
     * The JSON-String is a nested Object of both the status of the API-call, user information and arrays.
     *
     * @param user The {@code UserProfile} Object that is transformed.
     * @return A response-ready JSON-String format.
     */ 
    public static Result toJson(UserProfile user) {
        //status field
        Logger.Log(Tags.SCS, "User "+user.getUserName()+" found. Converting to JSON-String...");

        ObjectNode result = statusBody(0, "USER OK");
        result.set("VALUE", transformUserProfile(user));

        return Results.ok(result);
    }    

    /**
     * Creates a JSON-String containing a single value.
     * 
     * @param obj The object that is the value.
     * @return A response-ready status message with a single value.
     */
    public static Result toJson(Object obj) {
        ObjectNode result = statusBody(0, "VALUE OK");
        result.put("VALUE",String.valueOf(obj));

        return Results.ok(result);
    }    

    /**
     * Creates a JSON-String containig an array of values.
     * 
     * @param vals The List of values. Type needs to extend Object.
     * @return A response-ready status message with an array of values.
     */
    public static Result toJson(List<? extends Object> vals) {
        ObjectNode result = statusBody(0, "VALUES OK");

        ArrayNode strings = result.arrayNode();
        
        for(Object s : vals) 
            strings.add(String.valueOf(s));
        
        
        result.set("VALUES",strings);    

        return Results.ok(result);
    }    


    /**
     * Creates a JSON-String for when an API-call throws an exception.
     * 
     * @param e The Exception that is caught by the API-call.
     * @return A response-ready status message.
     */  
    public static Result toJson(Exception e) {
        Logger.Log(Tags.FLR,"API-call failed. Returning error message.");

        return Results.ok(statusBody(1, e.getMessage()));
    }        

    /**
     * This method handles the creation of the {@code UserProfile} JSON-Object Node.
     *
     * @param user The {@code UserProfile} that is to be transformed into a JSON-String.
     * @return The JSON-Object of a {@code UserProfile}.
     */
    private static ObjectNode transformUserProfile(UserProfile user) {

        //user information field
        ObjectNode userInfo = Json.newObject();
        ArrayNode friends = userInfo.arrayNode();
        ArrayNode rivals = userInfo.arrayNode();
        ArrayNode streaks = userInfo.arrayNode();

        //create user information object first before putting it into the status field
        userInfo.put("username", user.getUserName());
        userInfo.put("tickets",user.getTickets());

        //create json array for all friends, rivals and streaks
        List<String> friendsList = user.getFriends();

        for(String s : friendsList)
            friends.add(s);


        List<String> rivalsList = user.getRivals();
        List<Integer> streaksList = user.getStreaks();
        
        for(int i = 0; i < rivalsList.size();i++) {
            rivals.add(rivalsList.get(i));
            streaks.add(streaksList.get(i));
        }

        userInfo.set("friends", friends);
        userInfo.set("rivals",rivals);
        userInfo.set("streaks",streaks);

        return userInfo;
    }

    /**
     * This method creates a JSON-Object body for the status messages.
     * 
     * @param status The status code for the body.
     * @param statusMessage The custom status message that is set in the field MESSAGE
     */
    private static ObjectNode statusBody(int status,String statusMessage) {
        ObjectNode result = Json.newObject();

        result.put("STATUS",status);
        result.put("MESSAGE",statusMessage);

        return result;
    }
}
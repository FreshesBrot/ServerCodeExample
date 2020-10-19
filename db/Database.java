package db;

import java.util.*;


import com.mongodb.client.*;
import org.bson.Document;

import interfaces.*;
import models.Logger;
import models.Logger.Tags;

/**
 * This class represents the interface between the Backend Database and the API. 
 * This is the Baseclass for all Databases, taking a generic parameter to define what type of Database is to implemented.
 * The Database class should only be handled as a Singleton.
 * 
 * @param <T> The Type of Data the Database stores. T needs to implement the Interface {@code IDocumentable<T>}.
 * 
 */
public abstract class Database<T extends IDocumentCoder<T>> {

    private static final String uri = "mongodb://127.0.0.1:28000/?connectTimeoutMS=5000"; //the uri used to connect to a mongodb instance, includes a timeout
    private static final String db = "test"; //the string of the database
    protected String collection; //the name of the collection, specified by each database
    
    private MongoClient client; //the mongoclient to open a connection with
    protected MongoCollection<Document> mdbCollection; //the protected MongoCollection instance for subclasses to use
    
    /**
     * This method retrieves all objects that match the Key:Value pair specified by the method parameters.
     * 
     * @param key The Key of the pair
     * @param value The Value for the Key
     * @return Returns a list of {@code Document} that match the search criteria. If no element was found, returns an empty list.
     */
    protected List<Document> findByValue(String key, String value) {
        Logger.Log(Tags.CLL,"Called findByValue with "+key+":"+value);
        
        List<Document> list = new ArrayList<Document>();

       transaction(new ITransaction(){
        
            @Override
            public void commit() {
                //this find might cause trouble
                Iterable<Document> result = mdbCollection.find(new Document(key,value));
                
                result.forEach(s -> list.add(s));
            }
        });

        statusLog((list.isEmpty()) ? 1 : 0,"find");

        return list;
    }

    /**
     * This method takes an Object and updates it. If the object could not be found in the database, nothing is done.
     * This method returns 0 if the update was successful, and 1 otherwise.
     * 
     * @param object The object that gets updated.
     * @return A status code.
     */
    public int update(T object) {
        Logger.Log(Tags.CLL,"Called update on collection "+collection);

        int i = transaction(new ITransaction(){
        
            @Override
            public void commit() {
                mdbCollection.findOneAndReplace(new Document("_id",object.unique()), object.encode());
            }
        });

        statusLog(i, "update");

        return i;
    }

    /**
     * This method inserts a single item into the Database. If the insert was successful, returns 0, otherwise returns 1.
     * 
     * @param item The item that is to be inserted.
     * @return A status code.
     */
    public int insert(T item) {
        Logger.Log(Tags.CLL,"Called insert on collection "+collection);
        
        int i = transaction(new ITransaction(){
        
            @Override
            public void commit() {
                //decode item
                Document doc = item.encode();
                //commit to db
                mdbCollection.insertOne(doc);
            }
        });

        statusLog(i, "insert");

        return i;
    }

    /**
     * This method is used to remove the first matching result of the specified key:value pair.
     * 
     * @param key The key of the value.
     * @param value The value to the corresponding key.
     * @return The status code.
     */
    protected int remove(String key, String value) {
        Logger.Log(Tags.CLL,"Called remove on collection "+collection+" with "+key+":"+value);

        int i = transaction(new ITransaction(){
        
            @Override
            public void commit() {
                mdbCollection.deleteOne(new Document(key,value));
            }
        });

        statusLog(i, "remove");

        return i;
    }

    /**
     * This method returns whether there was any object matching the specified Key:Value pair.
     * 
     * @param key The key of the value.
     * @param value The value to the corresponding key.
     * @return Returns {@code true} if an object exists and {@code false} otherwise.
     */
    protected boolean exists(String key, String value) {
        Logger.Log(Tags.INF,"Looking up if objects match "+key+":"+value);

        int i = transaction(new ITransaction(){
        
            @Override
            public void commit() throws Exception {
                if((mdbCollection.countDocuments(new Document(key,value)) == 0))
                 throw new Exception("Element containing "+key+":"+value+" in collection "+collection+" could not be found.");
            }
        });

        return (i == 0) ? true : false;
    }

    /**
     * This method handles all transactions to the MongoDB database. Returns 0 if transaction was successful, and 1 otherwise.
     * 
     * @param transaction A {@code ITransaction} instance that contains code that deals with MongoDB CRUD-operations.
     * @return The status code.
     */
    private int transaction(ITransaction transaction) {
        try {
            client = MongoClients.create(uri);
            mdbCollection = client.getDatabase(db).getCollection(collection);
            
            Logger.Log(Tags.INF,"Starting Transaction...");
            transaction.commit();
            Logger.Log(Tags.SCS,"Transaction finished.");
    
            return 0;
        } catch (Exception e) {
            Logger.Log(Tags.ERR,e.getMessage());

            return 1;
        } finally {

            client.close();
        }
    }

    /**
     * This constructor intialized which collection the specific {@code Database} instance is adressing.
     * @param collectionIdent
     */
    protected Database(String collectionIdent) {
        collection = collectionIdent;
    }

    /**
     * This function logs the result of the transaction().
     *  
     * @param i The status code returned from transaction(). 
     * @param operation An operation specifier.
     */
    private void statusLog(int i, String operation) {
        if(i!=0)
            Logger.Log(Tags.ERR,operation+" failed!");
        else
            Logger.Log(Tags.SCS,operation+" successful!");
    }

}
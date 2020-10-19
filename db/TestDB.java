package db;

import models.TestObject;
import models.Logger;
import models.Logger.Tags;


public class TestDB extends Database<TestObject> {

    private static final String collectionIdent = "testCollection";
    private static TestDB instance = null;

    public static TestDB getInstance() {
        if(instance == null) instance = new TestDB();
        return instance;
    }

    private TestDB() {
        super(collectionIdent);
    }

    
    public TestObject find(String id) {
        try {
            return TestObject.construct(findByValue("_id",id).get(0));

        } catch (IndexOutOfBoundsException e) {
            Logger.Log(Tags.ERR,"No elements could be found.");

            return null;
        }
        
        
    }

}
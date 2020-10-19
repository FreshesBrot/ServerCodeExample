package models;

import interfaces.*;

import org.bson.Document;


public class TestObject implements IDocumentCoder<TestObject> {


    private int instance;

    private String stuff1;
    private String stuff2;

    public static TestObject construct(Document doc) {
        TestObject newTO = new TestObject();
        return newTO.decode(doc);
    }

    /**
     * Empty constructor used for initialization by the decoder
     */
    private TestObject() { }

    public TestObject(int instance) {
        this.instance = instance;
    }

    public void setStuff1(String stuff) {
        stuff1 = stuff;
    }

    public void setStuff2(String stuff) {
        stuff2 = stuff;
    }
    
    @Override
    public Document encode() {
        Document doc = new Document("_id",String.valueOf(instance));
        
        doc.append("stuff1",stuff1)
            .append("stuff2",stuff2);
        
        return doc;
    }

    @Override
    public TestObject decode(Document doc) {
        instance = Integer.parseInt(doc.getString("_id"));
        stuff1 = doc.getString("stuff1");
        stuff2 = doc.getString("stuff2");

        return this;
    }
    

    @Override
    public String unique() {
        return String.valueOf(instance);
    }

    @Override
    public String toString() {
        return instance+"::"+stuff1+"::"+stuff2;
    }

}
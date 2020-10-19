package interfaces;

import org.bson.Document;

/**
 * This interface provides methods to encode and decode to and from the BSON-Document type.
 *  
 * @param <T> The type of Object that is encoded and decoded.
 */
public interface IDocumentCoder<T> {
    
    /**
     * This method encodes the object of type T into a {@code Document}.
     * 
     * @return BSON-Document
     */
    public Document encode();

    /**
     * This method decodes a {@code Document} into an object of type T.
     * 
     * @param doc The BSON-Document holding the object information.
     */
    public T decode(Document doc);

    /**
     * This method returns the unique identifier of the object, useful for quick lookups.
     * 
     * @return The unique {@code String} identifier.
     */
    public String unique();

}
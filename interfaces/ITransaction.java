package interfaces;

/**
 * This interface provides a method for the {@code Database<T>} class to handle transactions to the database.
 */
public interface ITransaction {

    /**
     * The method that encapsulates database transactions.
     * 
     * @throws Exception Can throw Exceptions if implementation requires it.  
     */
    public void commit() throws Exception;

}
package interfaces;

import play.mvc.*;

/**
 * This interface is used to encapsulate a a method that throws an Exception that is supposed to be caught.
 */
public interface ITryCatchExecution {
    
    /**
     * This method encapsulates any API-Call on the server side.
     * 
     * @return A Result to be sent to the client.
     * @throws Exception Throws any Exceptions.
     */
    public Result Try() throws Exception;
}
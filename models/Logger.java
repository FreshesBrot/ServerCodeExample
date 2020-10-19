package models;

/**
 * This class prints logging Information to stdout with additional tag information
 */
public class Logger {
    
    //fields for shortcuts for tags
    public static final String CLL = "CALL"; //used for important function calls
    public static final String ERR = "ERROR"; //used for error logging
    public static final String SCS = "SUCCESS"; //used for when a certain operation succeeds
    public static final String FLR = "FAILURE"; //used for when a certain operation fails
    public static final String INF = "INFO"; //used for general information

    public static final boolean debug = false; //if this flag is true, logging is active. Set to false to reduce CPU-load from logging.

    /**
     * This enumerator is used to identify viable tags used for logging messages.
     */
    public enum Tags {
        /**
         * CALL-Tag, used for imoprtant function calls.
         */
        CLL,
        /**
         * ERROR-Tag, used for error and exception logging
         */
        ERR,
        /**
         * SUCCESS-Tag, used for operations that were successfull.
         */
        SCS,
        /**
         * FAILURE-Tag, used for operations that failed.
         */
        FLR,
        /**
         * INFO-Tag, used for general info
         */
        INF;
    }

    /**
     * The Logger class cannot be instantiated
     */
    private Logger() {}

    /**
     * This method prints a message to std out
     * 
     * @param tag The tag
     * @param info The message Info
     */
    public static void Log(Tags tag, String info) {
        if(debug)
            System.out.println(translateTag(tag)+": "+info);
    }

    /**
     * This method is specifically used to log url-calls with parameters
     * 
     * @param tag The tag
     * @param url  The base url
     * @param components The parameters of the url call
     */
    public static void Log(Tags tag, String url, String... components) {
        if(debug)
            Log(tag,buildUrl(url, components));
    }

    /**
     * This method takes a tag and returns an appropriate string
     * 
     * @param tag The tag that is to be translated
     * @return The corresponding string
     */
    private static String translateTag(Tags tag) {
        switch(tag) {
            case CLL: return CLL;
            case ERR: return ERR;
            case SCS: return SCS;
            case FLR: return FLR;
            case INF: return INF;
            default:  return ERR;
        }
    }

    /**
     * This method builds a URL from a base and from url-parameters.
     * 
     * @param baseURL The base url
     * @param components The url-parameters.
     * @return A complete url-string
     */
    private static String buildUrl(String baseURL, String... components) {
        String url = baseURL;
        for(String s : components) {
            url += "/"+s;
        }

        return url;
    }   
}
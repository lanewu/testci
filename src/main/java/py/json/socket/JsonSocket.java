package py.json.socket;

/**
 * An interface for json socket including some feature in common.
 * 
 * @author zjm
 *
 */
public interface JsonSocket {
    /**
     * Data in json socket is formatted as "HEAD JSON_STRING". "HEAD" means length of "JSON_STRING", and its length is 4
     * bytes. And reset of the data is "JSON_STRING".
     */
    public static final int HEAD_LEN = 4;
}

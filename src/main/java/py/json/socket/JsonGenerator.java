package py.json.socket;

/**
 * An interface to generate json string from specified object.
 * 
 * @author zjm
 *
 */
public interface JsonGenerator {
    /**
     * A json string will be returned if everything normal. Otherwise, null value will be returned.
     * 
     * @return
     */
    public String generate();
}

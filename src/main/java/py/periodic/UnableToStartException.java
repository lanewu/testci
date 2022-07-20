package py.periodic;

/**
 * Indicates that we can't start to execute periodic workers due to certain reasons.
 * 
 *  @author chenlia
 */
public class UnableToStartException extends Exception 
{

    private static final long serialVersionUID = 1L;

    public UnableToStartException(String message, Throwable cause) 
    {
        super(message, cause);
    }

    public UnableToStartException() 
    {
        super();
    }
    
    
    public UnableToStartException(String message) 
    {
        super(message);
    }
    
}
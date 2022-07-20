package py.periodic.impl;

/**
 * Indicates some execution options are invalid 
 * 
 *  @author chenlia
 */
public class InvalidExecutionOptionsException extends Exception 
{

    private static final long serialVersionUID = 1L;

    public InvalidExecutionOptionsException(String message, Throwable cause) 
    {
        super(message, cause);
    }

    public InvalidExecutionOptionsException() 
    {
        super();
    }
    
    public InvalidExecutionOptionsException(String message) 
    {
        super(message);
    }
    
}
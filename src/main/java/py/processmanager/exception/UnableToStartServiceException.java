package py.processmanager.exception;

public class UnableToStartServiceException extends Exception {
    
    public UnableToStartServiceException() {
        super();
    }
    
    public UnableToStartServiceException(String err) {
        super(err);
    }
    
    public UnableToStartServiceException(String err, Throwable throwable) {
        super(err, throwable);
    }
    
    public UnableToStartServiceException(Throwable throwable) {
        super(throwable);
    }

}

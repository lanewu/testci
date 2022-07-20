package py.processmanager.exception;

public class UnableToStartProcessManagerException extends Exception {
    
    public UnableToStartProcessManagerException() {
        super();
    }
    
    public UnableToStartProcessManagerException(String err) {
        super(err);
    }
    
    public UnableToStartProcessManagerException(String err, Throwable throwable) {
        super(err, throwable);
    }
    
    public UnableToStartProcessManagerException(Throwable throwable) {
        super(throwable);
    }

}

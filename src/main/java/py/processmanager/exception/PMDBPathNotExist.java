package py.processmanager.exception;

public class PMDBPathNotExist extends Exception {
    
    public PMDBPathNotExist() {
        super();
    }
    
    public PMDBPathNotExist(String err) {
        super(err);
    }
    
    public PMDBPathNotExist(String err, Throwable throwable) {
        super(err, throwable);
    }
    
    public PMDBPathNotExist(Throwable throwable) {
        super(throwable);
    }

}

package py.system.monitor;

public class ParseException extends Exception {
    public ParseException(){
        super();
    }
    
    public ParseException(String err) {
        super(err);
    }
    
    public ParseException(Throwable thr) {
        super(thr);
    }
    
    public ParseException(String err, Throwable thr) {
        super(err, thr);
    }
}

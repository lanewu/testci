package py.exception;

public class ArchiveStatusException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public ArchiveStatusException( String s ) {
        super( s );
    }
    
    public ArchiveStatusException( Throwable ex1 ) {
        super( ex1 );
    }

    public ArchiveStatusException( String s, Throwable ex1 ) {
        super( s, ex1 );
    }
}

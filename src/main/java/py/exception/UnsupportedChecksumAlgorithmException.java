package py.exception;

public class UnsupportedChecksumAlgorithmException extends Exception {
    private static final long serialVersionUID = 1L;

    public UnsupportedChecksumAlgorithmException( String s ) {
        super( s );
    }
    
    public UnsupportedChecksumAlgorithmException( Throwable ex1 ) {
        super( ex1 );
    }

    public UnsupportedChecksumAlgorithmException( String s, Throwable ex1 ) {
        super( s, ex1 );
    }
}

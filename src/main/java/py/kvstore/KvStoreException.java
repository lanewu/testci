package py.kvstore;

public class KvStoreException extends Exception{
    public KvStoreException(Exception e) {
        this.initCause(e);
    }
}

package py.kvstore.operation;
import static py.kvstore.KvConstants.*;

public abstract class Operation {

    protected Type type;
    protected byte[] key;

    protected Operation(Type type, byte[] key) {
        this.key = key;
        this.type = type;
    }

    public Type type() {return type;}

    public byte[] key() {return key;}

    public static PutOperation put(byte[] key, byte[] value) {
        return new PutOperation(key, value, NO_LEASE);
    }

    public static PutOperation put(byte[] key, byte[] value, long leaseId) {
        return new PutOperation(key, value, leaseId);
    }

    /************************** inner class *********************************************/
    public enum Type {
        PUT,
        DEL,
        TXN
    }

    public static final class PutOperation extends Operation {
        private final byte[] value;
        private final long leaseId;

        protected PutOperation(byte[] key, byte[] value, long leaseId) {
            super(Type.PUT, key);
            this.value = value;
            this.leaseId = leaseId;
        }

        public byte[] value() {return value;}

        public long leaseId() {return leaseId;}
    }
}

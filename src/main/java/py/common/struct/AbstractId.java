package py.common.struct;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;

import com.fasterxml.jackson.annotation.JsonProperty;

import py.exception.InvalidFormatException;

/**
 * The base class of various ids, such as volume id, container id and blids etc.
 *
 * Note that its subclass will use a JSON library to serialize/deserialize. Therefore, please don't add any
 * getter/setter functions, otherwise serialization/deserialization might go wrong.
 * 
 * @author chenlia
 */

public abstract class AbstractId extends SerializableObject implements Comparable<AbstractId> {
    private static final SecureRandom random = new SecureRandom();
    protected final long id;

    /**
     * Create a random and positive long ID.
     *
     * Long.toString() will print Longs with the highest bit set without a minus sign even though they are negative.
     * Long.parseString() refuses to parse these strings. So we limit ourselves to positive IDs and only 63 bits.
     */
    public AbstractId() {
        id = random.nextLong() & Long.MAX_VALUE;
    }

    public AbstractId(@JsonProperty("id") long id) {
        this.id = id;
    }

    public AbstractId(AbstractId copyFrom) {
        this.id = copyFrom.id;
    }

    public AbstractId(String string) throws InvalidFormatException {
        this(string, true);
    }

    /**
     * If raw is true, string is the output of Long.toString(id). If raw is false, string is getPrintablePrefix()
     * concatenated with "[", Long.toString(id), and "]".
     */
    public AbstractId(String string, boolean rawFormat) throws InvalidFormatException {
        String prefix = printablePrefix();
        if (!rawFormat) {
            if (!string.endsWith("]"))
                throw new InvalidFormatException("Your string needs to end with ']'; string:" + string);
            if (!string.startsWith(prefix))
                throw new InvalidFormatException("Your string needs to start with the prefix; string:" + string
                        + " prefix:" + prefix);
            if (string.charAt(prefix.length()) != '[')
                throw new InvalidFormatException("Your string needs a '[' after the prefix; string:" + string);
        }

        try {
            id = Long.parseLong(rawFormat ? string : string.substring(prefix.length() + 1, string.length() - 1));
        } catch (NumberFormatException e) {
            throw new InvalidFormatException("Bad number! string:" + string);
        }
    }

    /**
     * Read a long ID from buffer.
     * 
     * @param buffer
     *            something that has an 8 byte long in it
     */
    public AbstractId(ByteBuffer buffer) throws InvalidFormatException {
        id = buffer.getLong();
    }

    /**
     * Read a long ID from buffer.
     * 
     * @param bytes
     *            something that has an 8 byte long in it
     */
    public AbstractId(byte[] bytes) throws InvalidFormatException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        id = buffer.getLong();
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return printablePrefix() + "[" + Long.toString(id) + "]";
    }

    public String toRawString() {
        return Long.toString(id);
    }

    /**
     * Prefix used in toString() format.
     */
    public abstract String printablePrefix();

    /**
     * We take up one Long.
     */
    @Override
    public int sizeInByteBuffer() {
        return Long.SIZE / 8;
    }

    /**
     * Put our Long ID into the byteBuffer at its current position.
     */
    @Override
    public void toByteBuffer(ByteBuffer byteBuffer) {
        byteBuffer.putLong(id);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractId other = (AbstractId) obj;
        if (id != other.id)
            return false;
        return true;
    }

    @Override
    public int compareTo(AbstractId o) {
        if (id > o.id) {
            return 1;
        } else if (id < o.id) {
            return -1;
        }
        return 0;
    }
}

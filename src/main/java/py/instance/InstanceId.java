package py.instance;

import java.nio.ByteBuffer;

import com.fasterxml.jackson.annotation.JsonProperty;

import py.common.struct.AbstractId;
import py.exception.InvalidFormatException;

/**
 * Placehold for instance id
 * 
 * @author chenlia
 * 
 */
public class InstanceId extends AbstractId {
    public InstanceId() {
        super();
    }

    public InstanceId(@JsonProperty("id") long id) {
        super(id);
    }

    public InstanceId(InstanceId copyFrom) {
        super(copyFrom);
    }
    
    public InstanceId(String str) throws InvalidFormatException {
        super(str);
    }

    public InstanceId(byte [] bytes) throws InvalidFormatException {
        super(bytes);
    }

    public InstanceId(ByteBuffer buffer) throws InvalidFormatException {
        super(buffer);
    }

    @Override
    public String printablePrefix() {
        return "ii";
    }
}

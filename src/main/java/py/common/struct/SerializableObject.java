package py.common.struct;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Abstract base class for serializable objects into bytes 
 * (like ContainerId, InstanceId and etc.).
 */
public abstract class SerializableObject {

    public abstract void toByteBuffer(ByteBuffer byteBuffer);
    
    public abstract int sizeInByteBuffer();
    
    public byte [] toBytes() {
        byte [] bytes = new byte [sizeInByteBuffer()];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        toByteBuffer(byteBuffer);
        return bytes;
    }
}

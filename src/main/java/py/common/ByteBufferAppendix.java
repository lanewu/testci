package py.common;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * There are three types of bytebuffer: <br/>
 * 1. direct byte buffer who directly operate on memory <br/>
 * 2. byte buffer who has backup array <br/>
 * 3. other <br/>
 * For type 1 and type 2, we can copy data in bulk. For type 3, we just copy data byte by byte. When bulk copy data
 * between type1 and {@link Unsafe}, we need to know address in the buffer where the buffer array start from. The
 * appendix stores the address.
 * 
 * @author zjm
 *
 */
public class ByteBufferAppendix {
    private static final Logger logger = LoggerFactory.getLogger(ByteBufferAppendix.class);

    /**
     * The memory base address in direct byte buffer.
     */
    private long baseAddress;

    public static ByteBufferAppendix getAppendix(ByteBuffer buffer) {
        ByteBufferAppendix appendix = null;

        if (buffer.isDirect()) {
            try {
                appendix = new ByteBufferAppendix(buffer);
            } catch (Exception e) {
            }
        }

        return appendix;
    }

    private ByteBufferAppendix(ByteBuffer buffer) throws Exception {
        Validate.isTrue(buffer.isDirect());

        try {
            Method method = buffer.getClass().getDeclaredMethod("address");
            method.setAccessible(true);
            baseAddress = (long) method.invoke(buffer);
        } catch (Exception e) {
            logger.debug("Caught an exception", e);
            throw new RuntimeException("Unable to get array base address in direct buffer");
        }
    }

    public long getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(long baseAddress) {
        this.baseAddress = baseAddress;
    }
}

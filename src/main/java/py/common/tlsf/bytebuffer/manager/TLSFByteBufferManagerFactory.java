package py.common.tlsf.bytebuffer.manager;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This factory uses the singleton pattern to create TLSFByteBufferManager.  
 * 
 * @author chenlia 
 */
public class TLSFByteBufferManagerFactory {
    private static final Object lock = new Object();
    private static final Logger logger = LoggerFactory.getLogger(TLSFByteBufferManagerFactory.class);
    
    private static TLSFByteBufferManager tlsfByteBufferManager = null;

    public static void init(int sizeAlignment, int size, boolean addressAligned) {
        if (size < 0) {
            throw new RuntimeException("size is too large, and it is overflowed");
        }

        synchronized (lock) {
            if (tlsfByteBufferManager != null) {
                logger.warn("TLSFByteBufferManager has been initialized");
                return;
            }
        }

        tlsfByteBufferManager = new TLSFByteBufferManager(sizeAlignment, size, addressAligned);
        tlsfByteBufferManager.initAllocateAlignedBufferMetric(size);
    } 
    
    // get TLSFByteBufferManager. If init() is not called, then null is thrown
    public static TLSFByteBufferManager instance() {
        return tlsfByteBufferManager;
    }

    public static TLSFByteBufferManager build(int size, int sizeAlignment){
        Validate.isTrue(size > 0 && sizeAlignment > 0);
        TLSFByteBufferManager tlsfByteBufferManager = new TLSFByteBufferManager(sizeAlignment, size, true);
        tlsfByteBufferManager.initAllocateAlignedBufferMetric(size);
        return tlsfByteBufferManager;
    }
}
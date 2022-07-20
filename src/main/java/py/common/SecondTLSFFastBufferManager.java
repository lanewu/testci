package py.common;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.exception.NoAvailableBufferException;

public class SecondTLSFFastBufferManager extends AbstractTLSFastBufferManager {
    private static final Logger logger = LoggerFactory.getLogger(SecondTLSFFastBufferManager.class);

    public SecondTLSFFastBufferManager(long totalSize) {
        super(new TLSFFastBufferManager(totalSize, SecondTLSFFastBufferManager.class.getSimpleName()));
    }

    public SecondTLSFFastBufferManager(int alignmentSize, long totalSize) {
        super(new TLSFFastBufferManager(alignmentSize, totalSize, SecondTLSFFastBufferManager.class.getSimpleName()));
    }

    @Override
    public FastBuffer allocateBuffer(long size) throws NoAvailableBufferException {
        if (size <= 0) {
            logger.error("Invalid size {}, the requested size to allocate must be positive", size);
            return null;
        }
        return new SecondFastBufferImpl(super.allocateBuffers(size), size);
    }

    @Override
    public void releaseBuffer(FastBuffer retBuf) {
        Validate.isTrue(retBuf instanceof SecondFastBufferImpl);
        super.releaseBuffer(retBuf);
    }

}

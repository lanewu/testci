package py.common;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.exception.NoAvailableBufferException;

    public class PrimaryTLSFFastBufferManager extends AbstractTLSFastBufferManager {
        private static final Logger logger = LoggerFactory.getLogger(PrimaryTLSFFastBufferManager.class);

        public PrimaryTLSFFastBufferManager(long totalSize) {
            super(new TLSFFastBufferManager(totalSize, SyncLogTLSFFastBufferManager.class.getSimpleName()));
        }

        public PrimaryTLSFFastBufferManager(int alignmentSize, long totalSize) {
            super(new TLSFFastBufferManager(alignmentSize, totalSize, PrimaryTLSFFastBufferManager.class.getSimpleName()));
        }

        @Override
        public FastBuffer allocateBuffer(long size) throws NoAvailableBufferException {
            if (size <= 0) {
                logger.error("Invalid size {}, the requested size to allocate must be positive", size);
                return null;
            }
            return new PrimaryFastBufferImpl(super.allocateBuffers(size), size);
        }

        @Override
        public void releaseBuffer(FastBuffer retBuf) {
            Validate.isTrue(retBuf instanceof PrimaryFastBufferImpl);
            super.releaseBuffer(retBuf);
        }

    }

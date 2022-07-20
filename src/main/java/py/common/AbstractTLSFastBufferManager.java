package py.common;

import java.util.List;

import py.exception.NoAvailableBufferException;

public abstract class AbstractTLSFastBufferManager implements FastBufferManager {
    private TLSFFastBufferManager fastBufferManager;

    public AbstractTLSFastBufferManager(TLSFFastBufferManager fastBufferManager) {
        this.fastBufferManager = fastBufferManager;
    }

    @Override
    public void releaseBuffer(FastBuffer retbuf) {
        List<FastBuffer> fastBuffers = ((AbstractFastBuffer)retbuf).getFastBuffers();
        for (FastBuffer fastBuffer : fastBuffers) {
            fastBufferManager.releaseBuffer(fastBuffer);
        }
    }

    @Override
    public void close() {
        fastBufferManager.close();
    }

    @Override
    public long size() {
        return fastBufferManager.size();
    }

    public long getAlignmentSize() {
        return fastBufferManager.getAlignmentSize();
    }

    public List<FastBuffer> allocateBuffers(long size) throws NoAvailableBufferException {
        return fastBufferManager.allocateBuffers(size);
    }

}

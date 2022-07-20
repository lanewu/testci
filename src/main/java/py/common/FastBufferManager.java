package py.common;

import java.util.List;

import py.common.FastBuffer;
import py.exception.NoAvailableBufferException;

/**
 * A class to manage buffer's allocation and release.
 * 
 * @author zjm
 *
 */
public interface FastBufferManager {

    /**
     * Allocate buffer with specified size.
     * 
     * If available buffer size is less than specified size, this method will throw out
     * {@link NoAvailableBufferException}.
     * 
     * @param size
     * @return
     * @throws NoAvailableBufferException
     */
    public FastBuffer allocateBuffer(long size) throws NoAvailableBufferException;

    /**
     * Allocate multiple buffers with specified size.
     * 
     * @param size
     * @return
     * @throws NoAvailableBufferException
     */
    public List<FastBuffer> allocateBuffers(long size) throws NoAvailableBufferException;

    /**
     * Release buffer size.
     * 
     * @param retbuf
     */
    public void releaseBuffer(FastBuffer retbuf);

    /**
     * Close buffer.
     */
    public void close();

    /**
     * @return The total size of buffer 
     */
    public long size();

    /**
     * @return the alignment size of allocating size
     * @return
     */
    public long getAlignmentSize();
}
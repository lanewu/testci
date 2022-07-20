package py.common;

import java.nio.ByteBuffer;

import py.exception.BufferOverflowException;
import py.exception.BufferUnderflowException;

/**
 * 
 * @author wangzhiyu
 * 
 */
public interface FastBuffer {
    /**
     * This method transfers bytes from this buffer into the given destination array.
     * 
     * @param dst
     * @throws BufferUnderflowException
     *             If the size of this buffer is fewer than that of dst
     */
    public void get(byte[] dst) throws BufferUnderflowException;

    /**
     * similar to get(byte []).
     * 
     * @param dst
     * @param offset
     *            the start position in the dst array
     * @param length
     * @throws BufferUnderflowException
     */
    public void get(byte[] dst, int offset, int length) throws BufferUnderflowException;

    /**
     * transfer data from this buffer to {@link ByteBuffer}, if someone modify the position of the {@link ByteBuffer},
     * you will get wrong data or get a exception.
     * 
     * @param dst
     * @throws BufferUnderflowException
     */
    public void get(ByteBuffer dst) throws BufferUnderflowException;

    /**
     * similar to get(ByteBuffer), this method has a advantage that you needn't worry about the position of the
     * {@link ByteBuffer} modified, because the offset and length has specified, namely it is thread-safe for this
     * {@link ByteBuffer}.
     * 
     * @param dst
     * @param dstOffset
     * @param length
     * @throws BufferUnderflowException
     */
    public void get(ByteBuffer dst, int dstOffset, int length) throws BufferUnderflowException;

    /**
     * similar to get(ByteBuffer, int, int),namely it is thread-safe for this {@link ByteBuffer}.
     * 
     * @param dstBaseAddr
     * @param dstOff
     * @param dstLen
     * @throws BufferUnderflowException
     */
    public void get(long srcOffset, ByteBuffer dst, int dstOffset, int length) throws BufferUnderflowException;

    /**
     * transfer bytes from this buffer to the given destination array. The difference between this API and get(byte[],
     * int, int) is an offset for this buffer can be specified. Data beginning with myOffset is copied.
     */
    public void get(long srcOffset, byte[] dst, int dstOffset, int length) throws BufferUnderflowException;

    /**
     * This method transfers the bytes remaining in the given source buffer into this buffer. If there are more bytes in
     * the source buffer than in this buffer, then no bytes are transferred and a BufferOverflowException is thrown.
     * 
     * @param src
     */
    public void put(byte[] src) throws BufferOverflowException;

    /**
     * similar to put(byte [])
     * 
     * @param src
     * @param offset
     *            the start position in the src array
     * @param length
     * @throws BufferOverflowException
     */
    public void put(byte[] src, int srcOffset, int length) throws BufferOverflowException;

    /**
     * similar to put(byte [], int, int)
     * 
     * @param dstOffset
     * @param src
     * @param srcOffset
     * @param length
     * @throws BufferOverflowException
     */
    public void put(long dstOffset, byte[] src, int srcOffset, int length) throws BufferOverflowException;

    /**
     * similar to put(byte[] src), if someone modify the position of the {@link ByteBuffer}, you will get wrong data or
     * get a exception, namely it is not thread-safe for this {@link ByteBuffer}.
     * 
     * @param src
     */
    public void put(ByteBuffer src) throws BufferOverflowException;

    /**
     * similar to put(ByteBuffer), this method has a advantage that you needn't worry about the position of the
     * {@link ByteBuffer} modified, because the offset and length has specified, namely it is thread-safe for this
     * {@link ByteBuffer}.
     * 
     * @param src
     * @param srcOffset
     * @param length
     * @throws BufferOverflowException
     */
    public void put(ByteBuffer src, int srcOffset, int length) throws BufferOverflowException;

    /**
     * similar to put(ByteBuffer, int, int), for this {@link ByteBuffer}.
     * 
     * @param srcBaseAddr
     * @param srcOff
     * @param srcLen
     */
    public void put(long dstOffset, ByteBuffer src, int srcOffset, int length) throws BufferOverflowException;

    /**
     * the size of the buffer. The reason of returning long rather than int is that the first buffer before bing sliced
     * might be larger.
     * 
     * @return
     */
    public long size();

    /**
     * get the array of bytes out
     */
    public byte[] array();
}
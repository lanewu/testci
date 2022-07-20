package py.common;

import java.nio.ByteBuffer;
import java.util.List;

import sun.nio.ch.DirectBuffer;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import py.common.FastBuffer;
import py.exception.BufferOverflowException;
import py.exception.BufferUnderflowException;
import py.exception.NoAvailableBufferException;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;

@SuppressWarnings("restriction")
@Deprecated
/** 
 * this class is deprecated and TSLFFastBufferManager is the replacement
 * 
 * @author chenlia
 *
 */
public class FastBufferManagerLinearImpl implements FastBufferManager {
    private final Logger logger = LoggerFactory.getLogger(FastBufferManagerLinearImpl.class);
    private static final int MAGIC_NUMBER = 0x04202818;
    private FastBufferImpl buffHead;
    private long addressOfAllocatedMemory;
    private PYMetric counterNumAllocatedBuf;
    private final long size;

    private class FastBufferImpl implements FastBuffer {
        protected int magic = 0; // this member is used to prevent user to repeatedly release buffer
        protected long address;
        protected long size;
        protected FastBufferImpl next;

        public FastBufferImpl(long addr, long size, FastBufferImpl next) {
            this.address = addr;
            this.size = size;
            this.next = next;
            this.magic = MAGIC_NUMBER;
        }

        public long getAddress() {
            return address;
        }

        public void slice(long cutsize) {
            size -= cutsize;
            address += cutsize;
        }

        public void addSize(long addsize) {
            size += addsize;
        }

        @Override
        public long size() {
            return size;
        }

        @Override
        public void get(byte[] dst) throws BufferUnderflowException {
            get(dst, 0, dst.length);
        }

        @Override
        public void get(byte[] dst, int offset, int length) throws BufferUnderflowException {
            get(0, dst, offset, length);
        }

        @Override
        public void get(long srcOffset, byte[] dst, int dstOffset, int length) throws BufferUnderflowException {
            if (srcOffset + length > size) {
                throw new BufferUnderflowException("dst' length " + dst.length + " is larger than the buffer size "
                                + size + ", srcOffset: " + srcOffset);
            }

            DirectAlignedBufferAllocator.copyMemory(address + srcOffset, dst, dstOffset, length);
        }

        @Override
        public void get(ByteBuffer dst) throws BufferUnderflowException {
            get(dst, dst.position(), dst.remaining());
        }

        @Override
        public void get(ByteBuffer dst, int dstOffset, int length) throws BufferUnderflowException {
            get(0, dst, dstOffset, length);
        }

        @Override
        public void get(long srcOffset, ByteBuffer dst, int dstOffset, int length) throws BufferUnderflowException {
            if (srcOffset + length > size) {
                throw new BufferUnderflowException("dst' length " + length + " is larger than the buffer size " + size
                                + ", srcOffset: " + srcOffset);
            }

            if (dst.isDirect()) {
                DirectAlignedBufferAllocator.copyMemory(address + srcOffset,
                                ((DirectBuffer) dst).address() + dstOffset, length);
            } else {
                Validate.isTrue(dst.hasArray());
                get(srcOffset, dst.array(), dst.arrayOffset() + dstOffset, length);
            }
        }

        @Override
        public void put(byte[] src) throws BufferOverflowException {
            put(src, 0, src.length);
        }

        @Override
        public void put(byte[] src, int offset, int length) throws BufferOverflowException {
            put(0, src, offset, length);
        }

        @Override
        public void put(long dstOffset, byte[] src, int srcOffset, int length) throws BufferOverflowException {
            if (dstOffset + length > size) {
                throw new BufferOverflowException("length " + length + " is larger than the buffer size " + size
                                + ", offset: " + dstOffset);
            }

            DirectAlignedBufferAllocator.copyMemory(src, srcOffset, length, address + dstOffset);
        }

        @Override
        public void put(ByteBuffer src) throws BufferOverflowException {
            put(0, src, src.position(), src.remaining());
        }

        @Override
        public void put(ByteBuffer src, int srcOffset, int length) throws BufferOverflowException {
            put(0, src, srcOffset, length);
        }

        @Override
        public void put(long dstOffset, ByteBuffer src, int srcOffset, int length) throws BufferOverflowException {
            if (dstOffset + length > size) {
                throw new BufferOverflowException("src' length " + length + " is larger than the buffer size " + size
                                + ", dstOffset: " + dstOffset);
            }

            if (src.isDirect()) {
                DirectAlignedBufferAllocator.copyMemory(((DirectBuffer) src).address() + srcOffset,
                                address + dstOffset, length);
            } else {
                put(dstOffset, src.array(), src.arrayOffset() + srcOffset, length);
            }
        }

        @Override
        public byte[] array() {
            if (size == 0) {
                return null;
            }

            byte[] temp = new byte[(int) size];
            get(temp);
            return temp;
        }
    }

    // initializa fastbuffer ,allocate the biggest buffer once this time directly
    // from os,not from java virtual machine
    // create link head ,link to a node with the whole buffer
    public FastBufferManagerLinearImpl(long size) {
        // initialize link head
        buffHead = new FastBufferImpl(0, 0, null);
        addressOfAllocatedMemory = 0l;

        try {
            addressOfAllocatedMemory = DirectAlignedBufferAllocator.allocateMemory(size);
            this.size = size;
            logger.debug("the address of allocated memory is {} ", addressOfAllocatedMemory);
        } catch (OutOfMemoryError x) {
            logger.error("out of memory. " + size + " might be too large", x);
            throw x;
        }

        FastBufferImpl entry = new FastBufferImpl(addressOfAllocatedMemory, size, null);
        buffHead.next = entry;
        counterNumAllocatedBuf = PYMetricRegistry.getMetricRegistry().register(
                        MetricRegistry.name("FastBufferManager", "allocatedBuffers"), Counter.class);
    }

    @Override
	public synchronized FastBuffer allocateBuffer(long size) throws NoAvailableBufferException {
        if (size <= 0) {
            logger.error("can't assign a fast buffer with not-positive value {} ", size);
            return null;
        }

        FastBufferImpl buf = buffHead;
        FastBufferImpl pre = buffHead;
        do { // search first available buf
            if (buf.size >= size)
                break;
            pre = buf;
            buf = buf.next;
        } while (buf != null);

        if (buf == null) {
            // Can't satisfy the requested size
            throw new NoAvailableBufferException();
        }

        counterNumAllocatedBuf.incCounter();
        if (buf.size == size) {
            pre.next = buf.next;
            buf.next = null;
            buf.magic = MAGIC_NUMBER;
            return buf;
        } else {
            // the found buf has larger size. Slice it into two parts. return the first part and keep the second one
            FastBufferImpl newBuffer = new FastBufferImpl(buf.address, size, null);
            buf.slice(size);
            return newBuffer;
        }
    }

    @Override
    public synchronized void releaseBuffer(FastBuffer retbuf) {
        if (retbuf == null) {
            logger.trace("Don't need to release a null buffer");
            return;
        }

        if (!(retbuf instanceof FastBufferImpl)) {
            String err = "free a buffer wrong type";
            logger.warn(err);
            throw new RuntimeException(err);
        }

        FastBufferImpl retBufferImpl = (FastBufferImpl) retbuf;
        if (retBufferImpl.magic != MAGIC_NUMBER) {
            logger.error("magic # {} is error ,repeatly release buffer!", retBufferImpl.magic);
            throw new RuntimeException("wrong magic number " + retBufferImpl.magic);
        }

        logger.trace("releasing a buffer addresssed at {} ", retBufferImpl.address);
        retBufferImpl.magic = 0;

        long addressOfBufferToReturn = retBufferImpl.getAddress();
        long size = retBufferImpl.size();
        FastBufferImpl buf = buffHead;
        FastBufferImpl pre = buffHead;
        do { // search the position to add
            if (addressOfBufferToReturn < buf.address) {
                break;
            }
            pre = buf;
            buf = buf.next;
        } while (buf != null);

        // check if i can combine to pre buffer
        long gapBetweenTwoBuffers = (pre.address + pre.size) - addressOfBufferToReturn;
        if (gapBetweenTwoBuffers == 0) {// combine buffer now
            pre.addSize(size);
            retBufferImpl = pre;
        } else if (gapBetweenTwoBuffers < 0) { // can not combine, so link to pre
            retBufferImpl.next = pre.next;
            pre.next = retBufferImpl;// add a entry to list
        } else { // two buffer overlap
            String errStr = "when merging with a buffer, the buffer being released overlaps with its low-end neighbour";
            logger.error(errStr);
            throw new RuntimeException(errStr);
        }

        // check if i can combine to next buffer
        if (buf != null) {
            gapBetweenTwoBuffers = (retBufferImpl.address + retBufferImpl.size) - buf.address;
            if (gapBetweenTwoBuffers == 0) {
                // combine two buffers
                retBufferImpl.addSize(buf.size);
                retBufferImpl.next = buf.next;
                // release it so that the buffer object can be GCed
                buf = null;
            } else if (gapBetweenTwoBuffers > 0) {
                String errStr = "when merging with a buffer, the buffer being released overlaps with its high-end neighbour";
                logger.error(errStr);
                throw new RuntimeException(errStr);
            } else {// do nothing, just verify two buffers are linked
                Validate.isTrue(retBufferImpl.next == buf);
            }
        }
        counterNumAllocatedBuf.decCounter();
        logger.trace("successfully release buffer at {} ", retBufferImpl.address);
    }

    /**
     * get the numbers of available buffers
     * 
     * @return
     */
    public synchronized int getEntryNums() {
        FastBufferImpl buf = buffHead;
        int i = 0;
        do {
            if (buf.size > 0) {
                i++;
            }
            buf = buf.next;

        } while (buf != null);
        return i;
    }

    public synchronized long getEntryAddress(int entryId) {
        FastBufferImpl buf = buffHead;
        int i = 0;
        do {
            if (buf.size > 0) {
                i++;
                if (i == entryId)
                    return buf.address;
            }
            buf = buf.next;

        } while (buf != null);

        return 0;
    }

    public synchronized long getEntrySize(int entryId) {
        FastBufferImpl buf = buffHead;
        int i = 0;
        do {
            if (buf.size > 0) {
                i++;
                if (i == entryId)
                    return buf.size;
            }
            buf = buf.next;

        } while (buf != null);

        return 0l;
    }

    public synchronized void printBuffer(String args) {
        FastBufferImpl buf = buffHead;
        int i = 0;
        System.out.printf("%s << ", args);
        do {
            if (buf.size > 0) {
                System.out.printf("(addr=%d size=%d)->", buf.getAddress(), buf.size());
                i++;
            }
            buf = buf.next;

        } while (buf != null);
        System.out.printf(">> [%d] ", i);
    }

    @Override
    public void close() {
    }

    @Override
    public long size() {
        return this.size;
    }

    @Override
    public List<FastBuffer> allocateBuffers(long size) throws NoAvailableBufferException {
        throw new NotImplementedException("this is linear fast buffer");
    }

    @Override
    public long getAlignmentSize() {
        throw new NotImplementedException("this is linear fast buffer");
    }
}
package py.common;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.lang3.Validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

/**
 * Allocate aligned buffer by using java's unsafe API
 *
 * @author chenlia
 */
@SuppressWarnings("restriction")
public class DirectAlignedBufferAllocator {
    private static final Logger logger = LoggerFactory.getLogger(DirectAlignedBufferAllocator.class);
    // The default align offset to read/write memory
    private static final int MEMORY_ALIGN_OFFSET = 512;

    private static final long addressOffset;

    private static final long arrayBaseOffset;

    //the following two fields reflected from readonly HeapBuffer
    private static final long arrayInHeapBuffer;
    private static final long arrayOffsetInHeapBuffer;

    private static Unsafe unsafe;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            addressOffset = unsafe.objectFieldOffset(Buffer.class.getDeclaredField("address"));
            arrayInHeapBuffer = unsafe.objectFieldOffset(ByteBuffer.class.getDeclaredField("hb"));
            arrayOffsetInHeapBuffer = unsafe.objectFieldOffset(ByteBuffer.class.getDeclaredField("offset"));
            arrayBaseOffset = unsafe.arrayBaseOffset(byte[].class);
        } catch (Exception e) {
            // NOOP: throw exception later when allocating memory
            throw new RuntimeException(e);
        }
    }

    /**
     * The des remaining of {@link ByteBuffer } should be larger than the src remaining of {@link ByteBuffer}, the des
     * {@link ByteBuffer} should be not read-only, it will move the position of src buffer and des buffer with length
     * of scr.remaining().
     *
     * @param src
     * @param des
     */
    public static void copyMemory(ByteBuffer src, ByteBuffer des) {
        int sizeToCopy = src.remaining();
        Validate.isTrue(!des.isReadOnly(), "des is read only");
        if (des.remaining() < sizeToCopy) {
            logger.warn("src={}, des={}", src, des);
            Validate.isTrue(false);
        }

        if (src.isDirect()) {
            if (des.isDirect()) {
                unsafe.copyMemory(null, getIneternalAddress(src), null, getIneternalAddress(des), sizeToCopy << 0);
            } else {
                unsafe.copyMemory(null, getIneternalAddress(src), des.array(),
                        arrayBaseOffset + des.arrayOffset() + des.position(), sizeToCopy << 0);
            }
        } else {
            if (des.isDirect()) {
                unsafe.copyMemory(src.array(), src.arrayOffset() + arrayBaseOffset + src.position(), null,
                        getIneternalAddress(des), sizeToCopy << 0);
            } else {
                System.arraycopy(src.array(), src.arrayOffset() + src.position(), des,
                        des.arrayOffset() + des.position(), sizeToCopy);
            }
        }

        src.position(src.position() + sizeToCopy);
        des.position(des.position() + sizeToCopy);
    }

    private static long getIneternalAddress(ByteBuffer buffer) {
        return getAddress(buffer) + buffer.position();
    }

    public static void copyMemory(long srcAddr, byte[] dst, int offset, int length) {
        unsafe.copyMemory(null, srcAddr, dst, arrayBaseOffset + (offset << 0), (length << 0));
    }

    public static void copyMemory(byte[] src, int offset, int length, long desAddr) {
        unsafe.copyMemory(src, arrayBaseOffset + (offset << 0), null, desAddr, (length << 0));
    }

    /**
     * if the give buffer is readonly, we can not access to its byte[] unless reflect
     * @param buffer
     * @param length
     * @param desAddr
     */
    public static void copyMemory(ByteBuffer buffer, int length, long desAddr) {
        byte[] src;
        int offset;
        if (buffer.isDirect()) {
            throw new RuntimeException("use other method to copy direct to direct!");
        }
        if (!buffer.isReadOnly()) {
            src = buffer.array();
            offset = buffer.arrayOffset();
        } else {
            src = (byte[]) unsafe.getObject(buffer, arrayInHeapBuffer);
            offset = unsafe.getInt(buffer, arrayOffsetInHeapBuffer);
        }

        copyMemory(src, offset + buffer.position(), length, desAddr);
    }

    public static void copyMemory(long srcAddr, long desAddr, int length) {
        unsafe.copyMemory(null, srcAddr, null, desAddr, (length << 0));
    }

    public static byte getByte(long address) {
        return unsafe.getByte(address);
    }

    public static long getLong(long address) {
        return unsafe.getLong(address);
    }

    public static void putLong(long address, long value) {
        unsafe.putLong(address, value);
    }

    public static long getAddress(ByteBuffer buffer) {
        // steal the value of address field from Buffer, holding the memory address for this ByteBuffer
        return unsafe.getLong(buffer, addressOffset);
    }

    public static long allocateMemory(long capacity) {
        return unsafe.allocateMemory(capacity);
    }

    public static ByteBuffer allocateAlignedByteBuffer(int wantedCapacity) {
        return allocateAlignedByteBuffer(wantedCapacity, MEMORY_ALIGN_OFFSET);
    }

    public static ByteBuffer allocateAlignedByteBuffer(int wantedCapacity, int align) {
        // We over allocate by the alignment so we know we can have a large enough aligned
        // block of memory to use. Also set order to native while we are here.
        ByteBuffer buffer = ByteBuffer.allocateDirect((wantedCapacity + align));

        return alignByteBuffer(buffer, wantedCapacity, align);
    }

    public static ByteBuffer alignByteBuffer(ByteBuffer buffer, int wantedCapacity, int align) {
        // Power of 2 --> single bit, none power of 2 alignments are not allowed.
        if (Long.bitCount(align) != 1) {
            throw new IllegalArgumentException("Alignment must be a power of 2, and the specified align is " + align);
        }

        Validate.isTrue(buffer.capacity() == wantedCapacity + align);

        long address = getAddress(buffer);
        // check if we got lucky and the address is already aligned
        if ((address & (align - 1)) == 0) {
            // set the new limit to intended capacity
            buffer.limit(wantedCapacity);
            // the slice is now an aligned buffer of the required capacity
            return buffer.slice().order(ByteOrder.nativeOrder());
        } else {
            // we need to shift the start position to an aligned address --> address + (align - (address % align))
            // the modulo replacement with the & trick is valid for power of 2 values only
            int newPosition = (int) (align - (address & (align - 1)));
            // change the position
            buffer.position(newPosition);
            int newLimit = newPosition + wantedCapacity;
            // set the new limit to accommodate offset + intended capacity
            buffer.limit(newLimit);
            // the slice is now an aligned buffer of the required capacity
            return buffer.slice().order(ByteOrder.nativeOrder());
        }
    }
}

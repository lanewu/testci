package netty.transfer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.IllegalReferenceCountException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.test.TestBase;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by zhongyuan on 18-6-1.
 */
public class ByteBufUsageTest extends TestBase {
    private final Logger logger = LoggerFactory.getLogger(ByteBufUsageTest.class);

    @Test
    public void testByteBufUsage1() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);
        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1, byteBuf2);

        assertEquals(1, byteBuf1.refCnt());
        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());

        assertEquals(1, byteBuf2.refCnt());
        assertTrue(byteBuf2.release());
        assertEquals(0, byteBuf2.refCnt());

        assertEquals(1, byteBuf3.refCnt());

        boolean caughtException = false;
        try {
            byteBuf3.release();
        } catch (IllegalReferenceCountException e) {
            caughtException = true;
        }
        assertTrue(caughtException);
    }

    @Test
    public void testByteBufUsage1_0() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);
        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1.retain(), byteBuf2.retain());

        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());

        assertTrue(byteBuf3.release());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());

        assertTrue(byteBuf1.release());
        assertTrue(byteBuf2.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());

    }

    @Test
    public void testByteBufUsage1_0_0() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);

        ByteBuf byteBuf2 = byteBuf1.retainedSlice(0, length2);

        assertEquals(2, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf2.release();
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage1_0_1() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);

        byteBuf1.retain();
        assertEquals(2, byteBuf1.refCnt());

        ByteBuf byteBuf2 = byteBuf1.retainedSlice(0, length2);

        assertEquals(3, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf2.release();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage1_0_2() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);

        ByteBuf byteBuf2 = byteBuf1.slice(0, length2);
        byteBuf1.retain();

        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf2.release();
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage1_0_3() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);

        ByteBuf byteBuf2 = byteBuf1.slice(0, length2);
        byteBuf2.retain();

        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf2.release();
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage1_0_4() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);

        ByteBuf byteBuf2 = byteBuf1.slice(0, length2);
        byteBuf2.retain();

        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf2.release();
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage1_0_5() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);

        ByteBuf byteBuf2 = byteBuf1.retainedSlice(0, length2);
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf2.retain();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        byteBuf1.retain();
        assertEquals(3, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        byteBuf2.release();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf2.release();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage1_0_6() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);

        ByteBuf byteBuf2 = byteBuf1.retainedSlice(0, length2);
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf2.retain();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        byteBuf1.retain();
        assertEquals(3, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        byteBuf2.release();
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        boolean caughtException = false;
        try {
            byteBuf2.release();
        } catch (IllegalReferenceCountException e) {
            caughtException = true;
        }

        assertTrue(caughtException);
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage1_0_7() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);
        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1.retain(), byteBuf2.retain());

        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());

        ByteBuf byteBuf4 = byteBuf3.retainedSlice(0, length1);
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(2, byteBuf3.refCnt());
        assertEquals(2, byteBuf4.refCnt());

        byteBuf3.release();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        byteBuf4.release();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());
        assertEquals(0, byteBuf4.refCnt());

        byteBuf1.release();
        byteBuf2.release();
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());
        assertEquals(0, byteBuf4.refCnt());
    }

    @Test
    public void testByteBufUsage1_0_8() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);
        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1.retain(), byteBuf2.retain());

        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());

        ByteBuf byteBuf4 = byteBuf3.slice(0, length1);
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        byteBuf4.retain();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(2, byteBuf3.refCnt());
        assertEquals(2, byteBuf4.refCnt());

        byteBuf3.release();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        byteBuf4.release();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());
        assertEquals(0, byteBuf4.refCnt());

        byteBuf1.release();
        byteBuf2.release();
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());
        assertEquals(0, byteBuf4.refCnt());
    }

    @Test
    public void testByteBufUsage1_0_9() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);
        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1, byteBuf2);

        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());

        byteBuf2.retain();
        assertEquals(2, byteBuf2.refCnt());

        byteBuf3.release();
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());

        byteBuf2.release();
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage1_1() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);
        byteBuf1 = Unpooled.wrappedBuffer(byteBuf1, byteBuf2);

        assertEquals(1, byteBuf1.refCnt());
        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage2() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);
        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1, byteBuf2);

        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());

        assertTrue(byteBuf3.release());

        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());
    }

    @Test
    public void testByteBufUsage3() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf byteBuf3 = byteBuf1.duplicate();

        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf3.refCnt());

        ByteBuf byteBuf4 = byteBuf2.retainedDuplicate();

        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf4.refCnt());
        assertTrue(!byteBuf2.release());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(byteBuf4.release());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf4.refCnt());

    }

    @Test
    public void testByteBufUsage3_1() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf byteBuf3 = byteBuf1.duplicate();

        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf3.refCnt());

        ByteBuf byteBuf4 = byteBuf2.retainedDuplicate();

        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(byteBuf4.release());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(0, byteBuf4.refCnt());

        assertTrue(byteBuf2.release());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf4.refCnt());

    }

    @Test
    public void testByteBufUsage4() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf byteBuf3 = byteBuf1.slice();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf3.refCnt());

        ByteBuf byteBuf4 = byteBuf2.retainedSlice();

        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf4.refCnt());
        assertTrue(!byteBuf2.release());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(byteBuf4.release());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf4.refCnt());
    }

    @Test
    public void testByteBufUsage4_1() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf byteBuf3 = byteBuf1.slice();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf3.refCnt());

        ByteBuf byteBuf4 = byteBuf2.retainedSlice();

        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(byteBuf4.release());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(0, byteBuf4.refCnt());

        assertTrue(byteBuf2.release());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf4.refCnt());
    }

    @Test
    public void testByteBufUsage4_2() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1, byteBuf2);
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());

        ByteBuf byteBuf4 = byteBuf3.slice();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        byteBuf1.retain();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        byteBuf2.retain();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(!byteBuf1.release());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(!byteBuf2.release());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(byteBuf4.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());
        assertEquals(0, byteBuf4.refCnt());
    }

    @Test
    public void testByteBufUsage4_2_0() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1, byteBuf2);
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());

        ByteBuf byteBuf4 = byteBuf3.slice();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        byteBuf1.retain();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        byteBuf2.retain();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(!byteBuf1.release());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(!byteBuf2.release());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(byteBuf4.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());
        assertEquals(0, byteBuf4.refCnt());
    }

    @Test
    public void testByteBufUsage4_3() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1, byteBuf2);
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());

        ByteBuf byteBuf4 = byteBuf3.retainedSlice();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(2, byteBuf3.refCnt());
        assertEquals(2, byteBuf4.refCnt());

        assertTrue(!byteBuf4.release());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(byteBuf3.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());
        assertEquals(0, byteBuf4.refCnt());
    }

    @Test
    public void testByteBufUsage4_4() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1, byteBuf2);
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());

        ByteBuf byteBuf4 = byteBuf3.slice();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        byteBuf3.retain();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(2, byteBuf3.refCnt());
        assertEquals(2, byteBuf4.refCnt());

        byteBuf4.retain();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(3, byteBuf3.refCnt());
        assertEquals(3, byteBuf4.refCnt());

        assertTrue(!byteBuf4.release());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(2, byteBuf3.refCnt());
        assertEquals(2, byteBuf4.refCnt());

        assertTrue(!byteBuf3.release());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(byteBuf4.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());
        assertEquals(0, byteBuf4.refCnt());
    }

    @Test
    public void testByteBufUsage5() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf byteBuf3 = byteBuf1.retainedDuplicate();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertTrue(!byteBuf1.release());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf3.refCnt());

        ByteBuf byteBuf4 = byteBuf2.retainedSlice();
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf4.refCnt());
        assertTrue(!byteBuf2.release());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        ByteBuf byteBufDst = Unpooled.wrappedBuffer(byteBuf3, byteBuf4);
        assertEquals(1, byteBufDst.refCnt());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(byteBufDst.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());
        assertEquals(0, byteBuf4.refCnt());
        assertEquals(0, byteBufDst.refCnt());

    }

    @Test
    public void testByteBufUsage6() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);

        assertEquals(0, byteBuf1.readerIndex());
        assertEquals(length1, byteBuf1.writerIndex());

        int skipLength = 1024;
        byteBuf1.skipBytes(skipLength);
        assertEquals(skipLength, byteBuf1.readerIndex());
        assertEquals(length1, byteBuf1.writerIndex());

        ByteBuf byteBuf2 = byteBuf1.retainedDuplicate();
        assertEquals(byteBuf1.readerIndex(), byteBuf2.readerIndex());
        assertEquals(byteBuf1.writerIndex(), byteBuf2.writerIndex());

        assertTrue(!byteBuf1.release());
        assertTrue(byteBuf2.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage7() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        assertEquals(0, byteBuf1.readerIndex());
        assertEquals(length1, byteBuf1.writerIndex());

        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);
        assertEquals(0, byteBuf2.readerIndex());
        assertEquals(length2, byteBuf2.writerIndex());

        int skipLength1 = 1024;
        int skipLength2 = 2048;
        byteBuf1.skipBytes(skipLength1);
        assertEquals(skipLength1, byteBuf1.readerIndex());
        assertEquals(length1, byteBuf1.writerIndex());

        byteBuf2.skipBytes(skipLength2);
        assertEquals(skipLength2, byteBuf2.readerIndex());
        assertEquals(length2, byteBuf2.writerIndex());

        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1, byteBuf2);
        int totalReadableBytes = length1 + length2 - skipLength1 - skipLength2;
        assertEquals(totalReadableBytes, byteBuf3.readableBytes());
        assertEquals(0, byteBuf3.readerIndex());
        assertEquals(totalReadableBytes, byteBuf3.writerIndex());

        assertTrue(byteBuf3.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());

    }

    @Test
    public void testByteBufUsage8() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        byteBuf2 = Unpooled.wrappedBuffer(byteBuf1, byteBuf2);

        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        assertEquals(0, byteBuf2.readerIndex());
        assertEquals(length2 + length1, byteBuf2.writerIndex());
        assertEquals(length2 + length1, byteBuf2.readableBytes());

        assertTrue(byteBuf2.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage9() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        byteBuf1 = Unpooled.wrappedBuffer(byteBuf1.retainedDuplicate(), byteBuf2.retainedDuplicate());

        assertEquals(1, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        assertEquals(0, byteBuf1.readerIndex());
        assertEquals(length1 + length2, byteBuf1.writerIndex());
        assertEquals(length1 + length2, byteBuf1.readableBytes());

        assertTrue(!byteBuf2.release());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf1.refCnt());

        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());

    }

    @Test
    public void testByteBufUsage9_1() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf byteBufCopy = byteBuf1.duplicate();
        assertEquals(1, byteBufCopy.refCnt());

        byteBuf1 = Unpooled.wrappedBuffer(byteBuf1, byteBuf2.retainedDuplicate());

        assertEquals(1, byteBufCopy.refCnt());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        assertEquals(0, byteBuf1.readerIndex());
        assertEquals(length1 + length2, byteBuf1.writerIndex());
        assertEquals(length1 + length2, byteBuf1.readableBytes());

        assertTrue(!byteBuf2.release());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBufCopy.refCnt());

        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, byteBufCopy.refCnt());

    }

    @Test
    public void testByteBufUsage9_2() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf byteBufCopy = byteBuf1.duplicate();
        assertEquals(1, byteBufCopy.refCnt());

        byteBuf1 = Unpooled.wrappedBuffer(byteBuf1.retainedDuplicate(), byteBuf2.retainedDuplicate());

        assertEquals(2, byteBufCopy.refCnt());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        assertEquals(0, byteBuf1.readerIndex());
        assertEquals(length1 + length2, byteBuf1.writerIndex());
        assertEquals(length1 + length2, byteBuf1.readableBytes());

        assertTrue(!byteBuf2.release());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(2, byteBufCopy.refCnt());

        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(1, byteBufCopy.refCnt());

        assertTrue(byteBufCopy.release());
        assertEquals(0, byteBufCopy.refCnt());
    }

    @Test
    public void testByteBufUsage10() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);

        int skipLength = 1024;
        byteBuf1.skipBytes(skipLength);
        assertEquals(skipLength, byteBuf1.readerIndex());
        assertEquals(length1, byteBuf1.writerIndex());
        assertEquals(length1 - skipLength, byteBuf1.readableBytes());

        ByteBuf copyBuf = byteBuf1.copy();
        assertEquals(0, copyBuf.readerIndex());
        assertEquals(length1 - skipLength, copyBuf.writerIndex());
        assertEquals(length1 - skipLength, copyBuf.readableBytes());

        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, copyBuf.refCnt());

        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(1, copyBuf.refCnt());

        assertTrue(copyBuf.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, copyBuf.refCnt());
    }

    @Test
    public void testByteBufUsage11() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf copyBuf = byteBuf1.copy();

        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, copyBuf.refCnt());
        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, copyBuf.refCnt());

        byteBuf1 = Unpooled.wrappedBuffer(copyBuf, byteBuf2);
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, copyBuf.refCnt());

        assertEquals(length1 + length2, byteBuf1.readableBytes());
        assertEquals(0, byteBuf1.readerIndex());
        assertEquals(length1 + length2, byteBuf1.writerIndex());

        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, copyBuf.refCnt());
    }

    @Test
    public void testByteBufUsage12() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf copyBuf = byteBuf1.copy();

        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, copyBuf.refCnt());
        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, copyBuf.refCnt());

        byteBuf1 = Unpooled.wrappedBuffer(copyBuf.retainedDuplicate(), byteBuf2.retainedDuplicate());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(2, copyBuf.refCnt());

        assertEquals(length1 + length2, byteBuf1.readableBytes());
        assertEquals(0, byteBuf1.readerIndex());
        assertEquals(length1 + length2, byteBuf1.writerIndex());

        assertTrue(!copyBuf.release());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, copyBuf.refCnt());

        assertTrue(!byteBuf2.release());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(1, copyBuf.refCnt());

        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(0, copyBuf.refCnt());
    }

    @Test
    public void testByteBufUsage13() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);
        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1.retainedDuplicate(), byteBuf2.retainedDuplicate());

        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());

        assertTrue(byteBuf3.release());

        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());

        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());

        assertTrue(byteBuf2.release());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage14() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        byteBuf1.retain();
        byteBuf2.retain();
        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1.retainedDuplicate(), byteBuf2.retainedDuplicate());

        assertEquals(3, byteBuf1.refCnt());
        assertEquals(3, byteBuf2.refCnt());
        assertEquals(1, byteBuf3.refCnt());

        assertTrue(byteBuf3.release());

        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());
        assertEquals(0, byteBuf3.refCnt());

        assertTrue(!byteBuf1.release());
        assertEquals(1, byteBuf1.refCnt());
        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());

        assertTrue(!byteBuf2.release());
        assertEquals(1, byteBuf2.refCnt());
        assertTrue(byteBuf2.release());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage15() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        byte[] readBytes1 = new byte[length1];
        ByteBuf byteBuf3 = byteBuf1.readBytes(readBytes1, 0, length1);

        assertTrue(byteBuf1.release());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf3.refCnt());

        ByteBuf byteBuf4 = byteBuf2.readBytes(length2);

        assertTrue(byteBuf2.release());
        assertEquals(0, byteBuf2.refCnt());
        assertEquals(1, byteBuf4.refCnt());

        assertTrue(byteBuf4.release());
        assertEquals(0, byteBuf4.refCnt());

    }

    @Test
    public void testByteBufUsage16() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1, byteBuf2);

        byteBuf3.skipBytes(length1);
        assertEquals(0, byteBuf1.readerIndex());
        assertEquals(0, byteBuf2.readerIndex());
        assertEquals(length1, byteBuf3.readerIndex());

        byteBuf3.skipBytes(length2);
        assertEquals(0, byteBuf1.readerIndex());
        assertEquals(0, byteBuf2.readerIndex());
        assertEquals(length1 + length2, byteBuf3.readerIndex());

        assertTrue(byteBuf3.release());
        assertEquals(0, byteBuf3.refCnt());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage17() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);

        ByteBuf byteBuf3 = Unpooled.wrappedBuffer(byteBuf1, byteBuf2);

        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf3.retain();
        assertEquals(2, byteBuf3.refCnt());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        assertTrue(!byteBuf3.release());
        assertEquals(1, byteBuf3.refCnt());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        assertTrue(byteBuf3.release());
        assertEquals(0, byteBuf3.refCnt());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage18() {
        List<ByteBuf> byteBufList = new ArrayList<>();
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        int length2 = length1 / 2;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        byteBufList.add(byteBuf1);
        ByteBuf byteBuf2 = allocator.buffer(length2);
        byteBuf2.writeBytes(bytes, 0, length2);
        byteBufList.add(byteBuf2);

        ByteBuf wrapperBuf = byteBuf1;

        wrapperBuf = Unpooled.wrappedBuffer(wrapperBuf, byteBuf2);

        assertEquals(1, wrapperBuf.refCnt());
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        assertTrue(wrapperBuf.release());
        assertEquals(0, wrapperBuf.refCnt());
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage19() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        assertEquals(1, byteBuf1.refCnt());

        ByteBuf byteBuf2 = byteBuf1.readSlice(1);
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf1.retain();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    @Test
    public void testByteBufUsage20() {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        int length1 = 8 * 1024;
        byte[] bytes = new byte[length1];
        ByteBuf byteBuf1 = allocator.buffer(length1);
        byteBuf1.writeBytes(bytes);
        assertEquals(1, byteBuf1.refCnt());

        ByteBuf byteBuf2 = byteBuf1.readSlice(1);
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf2.retain();
        assertEquals(2, byteBuf1.refCnt());
        assertEquals(2, byteBuf2.refCnt());

        byteBuf1.release();
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(1, byteBuf2.refCnt());

        byteBuf2.release();
        assertEquals(0, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

}
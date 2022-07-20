package netty.transfer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.ResourceLeakDetector;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.*;

import py.netty.core.ByteToMessageDecoder;
import py.netty.memory.SimplePooledByteBufAllocator;
import py.netty.message.Header;
import py.netty.message.Message;
import py.netty.message.MessageImpl;
import py.test.TestBase;

public class ByteToMessageDecoderTest extends TestBase {
    private final static int POOL_SIZE = 1024 * 1024 * 128; // 128M
    private final static int PAGE_SIZE = 8 * 1024;  // 8K

    private SimplePooledByteBufAllocator allocator = new SimplePooledByteBufAllocator(POOL_SIZE, PAGE_SIZE);
    private int littlePageCount;
    private int pageCount;
    private ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);

    public ByteToMessageDecoderTest() {
        littlePageCount = allocator.getAvailableLittlePageCount();
        pageCount = allocator.getAvailableMediumPageCount();
    }

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("io.netty.leakDetectionLevel", ResourceLeakDetector.Level.PARANOID.name());
        System.setProperty("io.netty.leakDetection.targetRecords", "30");
        System.setProperty("io.netty.leakDetection.maxRecords", "100");
        System.setProperty("io.netty.leakDetection.acquireAndReleaseOnly", "true");
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.DEBUG);
    }

    @Before
    public void beforeMethod() {
        String ip = "127.0.0.1";
        InetSocketAddress inetSocketAddress = new InetSocketAddress(ip, 8080);
        Channel channel = mock(Channel.class);

        when(channel.remoteAddress()).thenReturn(inetSocketAddress);

        when(ctx.channel()).thenReturn(channel);
    }

    @After
    public void afterMethod() {
        assertEquals(littlePageCount, allocator.getAvailableLittlePageCount());
        assertEquals(pageCount, allocator.getAvailableMediumPageCount());
    }

    @Test
    public void testByteBufUsage18() {
        List<ByteBuf> byteBufList = new ArrayList<>();
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
        // different from ByteBufUsageTest.testByteBufUsage18, it's hard to understand why PyPooledDirectByteBuf.deallocate() set refCnt to 1
        assertEquals(1, byteBuf1.refCnt());
        assertEquals(0, byteBuf2.refCnt());
    }

    /**
     * decode the message one by one bytes.
     *
     * @throws Exception
     */
    @Test
    public void decodeMessageOneByteByOneByte() throws Exception {
        int count = 2;
        Message[] responses = new Message[count];

        ByteToMessageDecoder decode = new ByteToMessageDecoder() {
            int i = 0;

            public void fireChanelRead(ChannelHandlerContext ctx, Message msg) {
                // logger.warn("message: {}", msg);
                responses[i++] = msg;
                // msg.release();
                super.fireChanelRead(ctx, msg);
            }
        };
        Message[] requests = new Message[count];
        ByteBuf bufferToSend = null;
        Random random = new Random();
        List<Integer> randomLengthList = new ArrayList<>();
        // now random length list:[20, 21, 75, 29, 68, 79, 1, 82, 87, 0]
        // make sure random list contain '0', cause 0 will trigger bug
        int extraIndex = random.nextInt(count - 1);
        for (int i = 0; i < (count - 1); i++) {
            if (i == extraIndex) {
                randomLengthList.add(0);
            }
            randomLengthList.add(random.nextInt(100));
        }

        List<ByteBuf> messageBufList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int randomLength = randomLengthList.get(i);
            requests[i] = generateMessage(i, randomLength);
            messageBufList.add(requests[i].getBuffer());
            if (bufferToSend == null) {
                bufferToSend = requests[i].getBuffer();
            } else {
                bufferToSend = Unpooled.wrappedBuffer(bufferToSend, requests[i].getBuffer());
            }
        }

        logger.warn("now random length list:{}", randomLengthList);
        int index = 0;
        while (index < bufferToSend.readableBytes()) {
            ByteBuf buf = bufferToSend.slice(index++, 1);
            buf.retain();
            decode.channelRead(ctx, buf);
        }

        for (int i = 0; i < count; i++) {
            assertEquals(requests[i].getHeader().getMethodType(), responses[i].getHeader().getMethodType());
            int bodyLength = requests[i].getHeader().getLength();
            if (bodyLength == 0) {
                assertEquals(responses[i].getBuffer(), null);
            } else {
                ByteBuf src = requests[i].getBuffer().slice(Header.headerLength(), bodyLength);
                ByteBuf des = responses[i].getBuffer();
                for (int j = 0; j < bodyLength; j++) {
                    assertEquals(src.readByte(), des.readByte());
                }
                responses[i].release();
            }
        }

        assertTrue(bufferToSend.release());
        assertEquals(0, bufferToSend.refCnt());
    }

    /**
     * decode message block by block.
     *
     * @throws Exception
     */
    @Test
    public void decodeMessageBlockByBlock() throws Exception {
        int count = 100;
        Message[] responses = new Message[count];
        List<Integer> randomLengthList = new ArrayList<>();
        List<Integer> randomStepList = new ArrayList<>();

        ByteToMessageDecoder decode = new ByteToMessageDecoder() {
            int i = 0;

            @Override
            public void fireChanelRead(ChannelHandlerContext ctx, Message msg) {
                // logger.warn("message: {}", msg);
                if (msg.getHeader().getDataLength() == 810) {
                    logger.warn("breakpoint here");
                }
                responses[i++] = msg;
                super.fireChanelRead(ctx, msg);
            }
        };
        Message[] requests = new Message[count];
        ByteBuf bufferToSend = null;
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            int randomLength = random.nextInt(1011);
            randomLengthList.add(randomLength);

            requests[i] = generateMessage(i, randomLength);
            if (bufferToSend == null) {
                bufferToSend = requests[i].getBuffer();
            } else {
                bufferToSend = Unpooled.wrappedBuffer(bufferToSend, requests[i].getBuffer());
            }
        }

        int index = 0;
        while (index < bufferToSend.readableBytes()) {
            int randomStep = random.nextInt(PAGE_SIZE) + 1;
            randomStepList.add(randomStep);

            int leftSize = bufferToSend.readableBytes() - index;
            randomStep = Math.min(randomStep, leftSize);
            ByteBuf buf = bufferToSend.retainedSlice(index, randomStep);
            // buf.retain();
            decode.channelRead(ctx, buf);
            index += randomStep;
        }

        for (int i = 0; i < count; i++) {
            assertEquals(requests[i].getHeader().getMethodType(), responses[i].getHeader().getMethodType());
            int bodyLength = requests[i].getHeader().getLength();
            if (bodyLength == 0) {
                assertEquals(responses[i].getBuffer(), null);
            } else {
                ByteBuf src = requests[i].getBuffer().slice(Header.headerLength(), bodyLength);
                ByteBuf des = responses[i].getBuffer();
                for (int j = 0; j < bodyLength; j++) {
                    byte srcByte = src.readByte();
                    byte desByte = 0;
                    try {
                        desByte = des.readByte();
                    } catch (IllegalReferenceCountException e) {
                        logger.error("can not happen, random length list:{}, random step list:{}", randomLengthList,
                                randomStepList, e);
                    }
                    assertEquals(srcByte, desByte);
                }
            }
        }

        for (int i = 0; i < count; i++) {
            int bodyLength = responses[i].getHeader().getLength();
            if (bodyLength != 0) {
                responses[i].release();
            }
        }

        bufferToSend.release();
    }

    /**
     * decode message block by block specially.
     *
     * @throws Exception
     */
    @Test
    public void decodeMessageBlockByBlockSpecially() throws Exception {
        int count = 5;
        Message[] responses = new Message[count];
        List<Integer> randomLengthList = new ArrayList<>();
        List<Integer> randomStepList = new ArrayList<>();

        randomLengthList.add(237);
        randomLengthList.add(766);
        randomLengthList.add(691);
        randomLengthList.add(810);
        randomLengthList.add(651);

        randomStepList.add(1925);
        randomStepList.add(461);
        randomStepList.add(708);
        randomStepList.add(5885);

        ByteToMessageDecoder decode = new ByteToMessageDecoder() {
            int i = 0;

            @Override
            public void fireChanelRead(ChannelHandlerContext ctx, Message msg) {
                // logger.warn("message: {}", msg);
                if (msg.getHeader().getDataLength() == 810) {
                    logger.warn("breakpoint here");
                }
                responses[i++] = msg;
                super.fireChanelRead(ctx, msg);
            }
        };
        Message[] requests = new Message[count];
        ByteBuf bufferToSend = null;
        for (int i = 0; i < count; i++) {
            int randomLength = randomLengthList.get(i);

            requests[i] = generateMessage(i, randomLength);
            if (bufferToSend == null) {
                bufferToSend = requests[i].getBuffer();
            } else {
                bufferToSend = Unpooled.wrappedBuffer(bufferToSend, requests[i].getBuffer());
            }
        }

        int index = 0;
        int loopIndex = 0;
        while (index < bufferToSend.readableBytes()) {
            int randomStep = randomStepList.get(loopIndex);
            int leftSize = bufferToSend.readableBytes() - index;
            randomStep = Math.min(randomStep, leftSize);
            ByteBuf buf = bufferToSend.retainedSlice(index, randomStep);
            // buf.retain();
            decode.channelRead(ctx, buf);
            index += randomStep;
            loopIndex++;
        }

        for (int i = 0; i < count; i++) {
            assertEquals(requests[i].getHeader().getMethodType(), responses[i].getHeader().getMethodType());
            int bodyLength = requests[i].getHeader().getLength();
            if (bodyLength == 0) {
                assertEquals(responses[i].getBuffer(), null);
            } else {
                ByteBuf src = requests[i].getBuffer().slice(Header.headerLength(), bodyLength);
                ByteBuf des = responses[i].getBuffer();
                for (int j = 0; j < bodyLength; j++) {
                    byte srcByte = src.readByte();
                    byte desByte = 0;
                    try {
                        desByte = des.readByte();
                    } catch (IllegalReferenceCountException e) {
                        logger.error("can not happen, random length list:{}, random step list:{}", randomLengthList,
                                randomStepList, e);
                    }
                    assertEquals(srcByte, desByte);
                }
            }
        }

        for (int i = 0; i < count; i++) {
            int bodyLength = responses[i].getHeader().getLength();
            if (bodyLength != 0) {
                responses[i].release();
            }
        }

        bufferToSend.release();
    }

    /**
     * produce two kinds of message, and submit them to the decoder again and again, the message may be splited as
     * several parts.
     *
     * @throws Exception
     */
    @Test
    public void allKindsOfMessage() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        ByteToMessageDecoder decode = new ByteToMessageDecoder() {
            public void fireChanelRead(ChannelHandlerContext ctx, Message msg) {
                // logger.warn("message: {}", msg);
                if (msg.getHeader().getMethodType() == 10) {
                    assertEquals(msg.getHeader().getLength(), 1024 * 1024);
                } else if (msg.getHeader().getLength() == 11) {
                    assertEquals(msg.getHeader().getLength(), 57);
                }

                msg.release();
                count.incrementAndGet();
                super.fireChanelRead(ctx, msg);
            }
        };

        byte[] bigPack = generateMessageBytes(10, 1024 * 1024);
        byte[] litPack = generateMessageBytes(11, 57);

        Random random = new Random();
        int times = 1000;
        for (int i = 0; i < times; i++) {
            int useSize = 0;
            while (useSize < bigPack.length) {
                int size = PAGE_SIZE * random.nextInt(128) + 1;
                if (useSize + size >= bigPack.length) {
                    int litCount = random.nextInt(4) + 1;
                    int realSize = bigPack.length - useSize + litPack.length * litCount;
                    ByteBuf buffer = allocator.buffer(realSize);
                    buffer.writeBytes(bigPack, useSize, bigPack.length - useSize);
                    for (int j = 0; j < litCount; j++) {
                        buffer.writeBytes(litPack, 0, litPack.length);
                    }
                    decode.channelRead(ctx, buffer);
                    useSize = bigPack.length;
                    continue;
                }

                ByteBuf buffer = allocator.buffer(size);
                buffer.writeBytes(bigPack, useSize, size);
                decode.channelRead(ctx, buffer);
                useSize += size;
            }
        }
    }

    /**
     * step 1
     * -----------------
     * |               |
     * -----------------
     * Message A
     */
    @Test
    public void testChannelRead0() {
        int messageIndex = 10;
        int bodyLen = 1024;
        ByteBuf messageByteBuf = generateMessageByteBuf(messageIndex, bodyLen);

        AtomicInteger count = new AtomicInteger(0);
        ByteToMessageDecoder decoder = new ByteToMessageDecoder() {
            public void fireChanelRead(ChannelHandlerContext ctx, Message msg) {
                super.fireChanelRead(ctx, msg);
                count.incrementAndGet();

                assertEquals(messageIndex, msg.getHeader().getMethodType());
                assertEquals(bodyLen, msg.getHeader().getLength());
                assertEquals(bodyLen, msg.getBuffer().readableBytes());
                validateOriginalMessageBufferAndParsedBuffer(messageByteBuf, msg.getBuffer(), bodyLen);

                // write log release
                msg.release();
                assertEquals(1, messageByteBuf.refCnt());
            }
        };

        try {
            decoder.channelRead(ctx, messageByteBuf);
            assertTrue(decoder.getCumulation() == null);
            assertTrue(decoder.getHeader() == null);
            assertEquals(0, messageByteBuf.refCnt());
            assertEquals(1, count.get());
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * step 1
     * -----------------
     * |               |
     * -----------------
     * Message A
     */
    @Test
    public void testChannelRead0_0() {
        int messageIndex = 10;
        int bodyLen = 0;
        ByteBuf messageByteBuf = generateMessageByteBuf(messageIndex, bodyLen);

        AtomicInteger count = new AtomicInteger(0);
        ByteToMessageDecoder decoder = new ByteToMessageDecoder() {
            public void fireChanelRead(ChannelHandlerContext ctx, Message msg) {
                super.fireChanelRead(ctx, msg);
                count.incrementAndGet();

                assertEquals(messageIndex, msg.getHeader().getMethodType());
                assertEquals(bodyLen, msg.getHeader().getLength());
                assertEquals(null, msg.getBuffer());

                // write log release
                msg.release();
                assertEquals(1, messageByteBuf.refCnt());
            }
        };

        try {
            decoder.channelRead(ctx, messageByteBuf);
            assertTrue(decoder.getCumulation() == null);
            assertTrue(decoder.getHeader() == null);
            assertEquals(0, messageByteBuf.refCnt());
            assertEquals(1, count.get());
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * step 1     step 2
     * -------------------
     * |      |          |
     * | header |  body  |
     * -------------------
     * Message A
     */
    @Test
    public void testChannelRead1() {
        int messageIndex = 10;
        int bodyLen = 1024;
        ByteBuf messageByteBuf = generateMessageByteBuf(messageIndex, bodyLen);
        AtomicInteger count = new AtomicInteger(0);
        ByteToMessageDecoder decoder = new ByteToMessageDecoder() {
            public void fireChanelRead(ChannelHandlerContext ctx, Message msg) {
                super.fireChanelRead(ctx, msg);
                count.incrementAndGet();
                assertEquals(messageIndex, msg.getHeader().getMethodType());
                assertEquals(bodyLen, msg.getHeader().getLength());
                assertEquals(bodyLen, msg.getBuffer().readableBytes());

                validateOriginalMessageBufferAndParsedBuffer(messageByteBuf, msg.getBuffer(), bodyLen);

                // write log release
                assertTrue(getCumulation() == null);
                msg.release();

                assertTrue(getCumulation() == null);
                // cumulation has been released
                assertEquals(2, messageByteBuf.refCnt());
            }
        };

        int messageLen = Header.headerLength() + bodyLen;
        int reduceCountForHeaderLen = 2;
        int part1Len = Header.headerLength() - reduceCountForHeaderLen;
        int part2Len = messageLen - part1Len;

        ByteBuf byteBufPart1 = messageByteBuf.retainedSlice(0, part1Len);
        ByteBuf byteBufPart2 = messageByteBuf.retainedSlice(part1Len, part2Len);
        assertEquals(3, messageByteBuf.refCnt());
        assertEquals(1, byteBufPart1.refCnt());
        assertEquals(1, byteBufPart2.refCnt());

        // msg len less than header len, can not parse header this time
        try {
            decoder.channelRead(ctx, byteBufPart1);
            assertEquals(1, byteBufPart1.refCnt());
            assertEquals(3, messageByteBuf.refCnt());
            assertTrue(decoder.getHeader() == null);
            assertEquals(part1Len, decoder.getCumulation().readableBytes());
            assertEquals(1, decoder.getCumulation().refCnt());
            assertEquals(0, count.get());
        } catch (Exception e) {
            fail();
        }

        // cumulation + msg can build one whole message
        try {
            decoder.channelRead(ctx, byteBufPart2);
            assertEquals(0, byteBufPart1.refCnt());
            assertEquals(0, byteBufPart2.refCnt());
            assertEquals(1, messageByteBuf.refCnt());
            assertTrue(decoder.getCumulation() == null);
            assertTrue(decoder.getHeader() == null);
            assertEquals(1, count.get());
        } catch (Exception e) {
            fail();
        }

        assertTrue(messageByteBuf.release());
        assertEquals(0, messageByteBuf.refCnt());
    }

    private void testLeak() {
        for (int i = 0; i < 3000; i++) {
            ByteBuf messageByteBufTest = generateMessageByteBuf(10, 1024 * 128);
            assertEquals(1, messageByteBufTest.refCnt());
            messageByteBufTest = null;
        }
    }

    /**
     * step 1     step 2    step 3
     * ----------------------------
     * |         |        |       |
     * | header |         |       |
     * ----------------------------
     * Message A
     */

    @Test
    public void testChannelRead2() {
        int messageIndex = 10;
        int bodyLen = 1024;
        ByteBuf messageByteBuf = generateMessageByteBuf(messageIndex, bodyLen);
        List<Message> responseList = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(0);
        ByteToMessageDecoder decoder = new ByteToMessageDecoder() {
            public void fireChanelRead(ChannelHandlerContext ctx, Message msg) {
                super.fireChanelRead(ctx, msg);
                count.incrementAndGet();
                assertEquals(messageIndex, msg.getHeader().getMethodType());
                assertEquals(bodyLen, msg.getHeader().getLength());
                assertEquals(bodyLen, msg.getBuffer().readableBytes());

                validateOriginalMessageBufferAndParsedBuffer(messageByteBuf, msg.getBuffer(), bodyLen);

                // assertEquals(2, getCumulation().refCnt());
                assertNull(getCumulation());
                // write log release
                responseList.add(msg);
                // msg.release();
                assertEquals(2, getCumulation().refCnt());
                assertEquals(4, messageByteBuf.refCnt());
            }
        };

        int messageLen = Header.headerLength() + bodyLen;
        int lagerCountForHeaderLen = 2;
        int part1Len = Header.headerLength() + lagerCountForHeaderLen;
        int part2Len = 567;
        int part3Len = messageLen - part1Len - part2Len;

        ByteBuf byteBufPart1 = messageByteBuf.retainedSlice(0, part1Len);
        ByteBuf byteBufPart2 = messageByteBuf.retainedSlice(part1Len, part2Len);
        ByteBuf byteBufPart3 = messageByteBuf.retainedSlice(part1Len + part2Len, part3Len);

        byteBufPart1.markReaderIndex();
        validateOriginalMessageBufferAndParsedBuffer(messageByteBuf,
                Unpooled.wrappedBuffer(byteBufPart1.skipBytes(Header.headerLength()), byteBufPart2, byteBufPart3),
                bodyLen);
        byteBufPart1.resetReaderIndex();
        assertEquals(4, messageByteBuf.refCnt());
        assertEquals(1, byteBufPart1.refCnt());
        assertEquals(1, byteBufPart2.refCnt());
        assertEquals(1, byteBufPart3.refCnt());

        // msg len bigger than header len, can parse header but not body this time
        try {
            decoder.channelRead(ctx, byteBufPart1);
            assertEquals(1, byteBufPart1.refCnt());
            assertEquals(4, messageByteBuf.refCnt());
            assertTrue(decoder.getHeader() != null);
            assertEquals(lagerCountForHeaderLen, decoder.getCumulation().readableBytes());
            assertEquals(1, decoder.getCumulation().refCnt());
            assertEquals(0, count.get());
        } catch (Exception e) {
            fail();
        }

        // msg len bigger than header len, can parse header but not body this time
        try {
            decoder.channelRead(ctx, byteBufPart2);
            assertEquals(1, byteBufPart1.refCnt());
            assertEquals(1, byteBufPart2.refCnt());
            assertEquals(4, messageByteBuf.refCnt());
            assertTrue(decoder.getHeader() != null);
            assertEquals(lagerCountForHeaderLen + part2Len, decoder.getCumulation().readableBytes());
            assertEquals(1, decoder.getCumulation().refCnt());
            assertEquals(0, count.get());
        } catch (Exception e) {
            fail();
        }

        // cumulation + msg can build one whole message
        try {
            decoder.channelRead(ctx, byteBufPart3);

            responseList.get(0).getBuffer().resetReaderIndex();
            validateOriginalMessageBufferAndParsedBuffer(messageByteBuf, responseList.get(0).getBuffer(), bodyLen);

            responseList.get(0).release();

            assertEquals(0, byteBufPart1.refCnt());
            assertEquals(0, byteBufPart2.refCnt());
            assertEquals(0, byteBufPart3.refCnt());
            assertEquals(1, messageByteBuf.refCnt());
            assertTrue(decoder.getCumulation() == null);
            assertTrue(decoder.getHeader() == null);
            assertEquals(1, count.get());

        } catch (Exception e) {
            logger.error("caught an exception", e);
            fail();
        }

        assertTrue(messageByteBuf.release());
        assertEquals(0, messageByteBuf.refCnt());
    }

    /**
     * step 1    step 2       step 2
     * -----------------  --------------
     * |       |       |  |            |
     * | header |      |  |            |
     * -----------------  --------------
     * Message A            Message B
     */
    @Test
    public void testChannelRead3() {
        int messageAIndex = 10;
        int bodyALen = 1024;

        int messageBIndex = 11;
        int bodyBLen = 1024 * 2;

        ByteBuf messageAByteBuf = generateMessageByteBuf(messageAIndex, bodyALen);
        ByteBuf messageBByteBuf = generateMessageByteBuf(messageBIndex, bodyBLen);

        AtomicInteger count = new AtomicInteger(0);
        ByteToMessageDecoder decoder = new ByteToMessageDecoder() {
            public void fireChanelRead(ChannelHandlerContext ctx, Message msg) {
                super.fireChanelRead(ctx, msg);
                count.incrementAndGet();

                if (msg.getHeader().getMethodType() == messageAIndex) {
                    assertEquals(bodyALen, msg.getHeader().getLength());
                    assertEquals(bodyALen, msg.getBuffer().readableBytes());

                    validateOriginalMessageBufferAndParsedBuffer(messageAByteBuf, msg.getBuffer(), bodyALen);

                    assertTrue(getCumulation() == null);
                    msg.release();
                    assertTrue(getCumulation() == null);

                    assertEquals(2, messageAByteBuf.refCnt());
                    assertEquals(2, messageBByteBuf.refCnt());

                } else if (msg.getHeader().getMethodType() == messageBIndex) {
                    assertEquals(bodyBLen, msg.getHeader().getLength());
                    assertEquals(bodyBLen, msg.getBuffer().readableBytes());

                    validateOriginalMessageBufferAndParsedBuffer(messageBByteBuf, msg.getBuffer(), bodyBLen);

                    assertTrue(getCumulation() == null);
                    msg.release();
                    assertTrue(getCumulation() == null);

                    assertEquals(2, messageAByteBuf.refCnt());
                    assertEquals(2, messageBByteBuf.refCnt());
                } else {
                    fail();
                }
            }
        };

        int messageALen = Header.headerLength() + bodyALen;

        int lagerCountForHeaderLen = 2;
        int part1Len = Header.headerLength() - lagerCountForHeaderLen;

        ByteBuf byteBufPart1 = messageAByteBuf.retainedSlice(0, part1Len);
        ByteBuf byteBufPart2 = Unpooled.wrappedBuffer(messageAByteBuf.retainedSlice(part1Len, messageALen - part1Len),
                messageBByteBuf.retainedDuplicate());

        assertEquals(3, messageAByteBuf.refCnt());
        assertEquals(2, messageBByteBuf.refCnt());
        assertEquals(1, byteBufPart1.refCnt());
        assertEquals(1, byteBufPart2.refCnt());

        // msg len smaller than header len, can not parse A header
        try {
            decoder.channelRead(ctx, byteBufPart1);
            assertEquals(1, byteBufPart1.refCnt());
            assertEquals(1, byteBufPart2.refCnt());
            assertEquals(3, messageAByteBuf.refCnt());
            assertEquals(2, messageBByteBuf.refCnt());
            assertTrue(decoder.getHeader() == null);
            assertEquals(part1Len, decoder.getCumulation().readableBytes());
            assertEquals(1, decoder.getCumulation().refCnt());
            assertEquals(0, count.get());
        } catch (Exception e) {
            fail();
        }

        // can parse A message + B message, all messages will be fired
        try {
            decoder.channelRead(ctx, byteBufPart2);
            assertEquals(0, byteBufPart1.refCnt());
            assertEquals(0, byteBufPart2.refCnt());
            assertEquals(1, messageAByteBuf.refCnt());
            assertEquals(1, messageBByteBuf.refCnt());
            assertTrue(decoder.getCumulation() == null);
            assertTrue(decoder.getHeader() == null);
            assertEquals(2, count.get());
        } catch (Exception e) {
            fail();
        }

        assertTrue(messageAByteBuf.release());
        assertEquals(0, messageAByteBuf.refCnt());
        assertTrue(messageBByteBuf.release());
        assertEquals(0, messageBByteBuf.refCnt());
    }

    /**
     * step 1    step 2    step 2   step 3
     * -----------------  -------------------
     * |        |      |  |       |         |
     * | header|       |  | header |        |
     * -----------------  -------------------
     * Message A            Message B
     */
    @Test
    public void testChannelRead4() {
        int messageAIndex = 10;
        int bodyALen = 1024;

        int messageBIndex = 11;
        int bodyBLen = 1024 * 2;

        ByteBuf messageAByteBuf = generateMessageByteBuf(messageAIndex, bodyALen);
        ByteBuf messageBByteBuf = generateMessageByteBuf(messageBIndex, bodyBLen);

        AtomicInteger count = new AtomicInteger(0);
        ByteToMessageDecoder decoder = new ByteToMessageDecoder() {
            public void fireChanelRead(ChannelHandlerContext ctx, Message msg) {
                super.fireChanelRead(ctx, msg);
                count.incrementAndGet();

                if (msg.getHeader().getMethodType() == messageAIndex) {
                    assertEquals(bodyALen, msg.getHeader().getLength());
                    assertEquals(bodyALen, msg.getBuffer().readableBytes());

                    validateOriginalMessageBufferAndParsedBuffer(messageAByteBuf, msg.getBuffer(), bodyALen);

                    // assertEquals(2, getCumulation().refCnt());
                    assertNull(getCumulation());
                    assertEquals(3, messageAByteBuf.refCnt());
                    msg.release();
                    // assertEquals(1, getCumulation().refCnt());

                    assertEquals(2, messageAByteBuf.refCnt());
                    assertEquals(3, messageBByteBuf.refCnt());
                } else if (msg.getHeader().getMethodType() == messageBIndex) {
                    assertEquals(bodyBLen, msg.getHeader().getLength());
                    assertEquals(bodyBLen, msg.getBuffer().readableBytes());

                    validateOriginalMessageBufferAndParsedBuffer(messageBByteBuf, msg.getBuffer(), bodyBLen);

                    assertTrue(getCumulation() == null);
                    msg.release();
                    assertTrue(getCumulation() == null);

                    assertEquals(1, messageAByteBuf.refCnt());
                    assertEquals(2, messageBByteBuf.refCnt());
                } else {
                    fail();
                }
            }
        };

        int messageALen = Header.headerLength() + bodyALen;
        int messageBLen = Header.headerLength() + bodyBLen;
        int totalMessageLen = messageALen + messageBLen;

        int lagerCountForHeaderLen = 2;
        int part1Len = Header.headerLength() + lagerCountForHeaderLen;
        int part2LenInA = (messageALen - part1Len);
        int part2LenInB = (Header.headerLength() - lagerCountForHeaderLen);
        int part2Len = part2LenInA + part2LenInB;
        int part3Len = totalMessageLen - part1Len - part2Len;

        ByteBuf byteBufPart1 = messageAByteBuf.retainedSlice(0, part1Len);
        ByteBuf byteBufPart2 = Unpooled.wrappedBuffer(messageAByteBuf.retainedSlice(part1Len, part2LenInA),
                messageBByteBuf.retainedSlice(0, part2LenInB));
        ByteBuf byteBufPart3 = messageBByteBuf.retainedSlice(part2LenInB, part3Len);

        assertEquals(3, messageAByteBuf.refCnt());
        assertEquals(3, messageBByteBuf.refCnt());
        assertEquals(1, byteBufPart1.refCnt());
        assertEquals(1, byteBufPart2.refCnt());
        assertEquals(1, byteBufPart3.refCnt());

        // step 1: msg len bigger than header len, can parse A header but not body this time
        try {
            decoder.channelRead(ctx, byteBufPart1);
            assertEquals(1, byteBufPart1.refCnt());
            assertEquals(1, byteBufPart2.refCnt());
            assertEquals(1, byteBufPart3.refCnt());
            assertEquals(3, messageAByteBuf.refCnt());
            assertEquals(3, messageBByteBuf.refCnt());
            assertTrue(decoder.getHeader() != null);
            assertEquals(lagerCountForHeaderLen, decoder.getCumulation().readableBytes());
            assertEquals(1, decoder.getCumulation().refCnt());
            assertEquals(0, count.get());
        } catch (Exception e) {
            fail();
        }

        // step 2: can parse A message, B header can not parse this time, and A message will be fired
        try {
            decoder.channelRead(ctx, byteBufPart2);
            assertEquals(0, byteBufPart1.refCnt());
            assertEquals(1, byteBufPart2.refCnt());
            assertEquals(1, byteBufPart3.refCnt());
            assertEquals(2, messageAByteBuf.refCnt());
            assertEquals(3, messageBByteBuf.refCnt());
            assertTrue(decoder.getHeader() == null);
            assertEquals(part2LenInB, decoder.getCumulation().readableBytes());
            assertEquals(1, decoder.getCumulation().refCnt());
            assertEquals(1, count.get());
        } catch (Exception e) {
            fail();
        }

        // step 3: cumulation + msg can build one whole B message
        try {
            decoder.channelRead(ctx, byteBufPart3);
            assertEquals(0, byteBufPart1.refCnt());
            assertEquals(0, byteBufPart2.refCnt());
            assertEquals(0, byteBufPart3.refCnt());
            assertEquals(1, messageAByteBuf.refCnt());
            assertEquals(1, messageBByteBuf.refCnt());
            assertTrue(decoder.getCumulation() == null);
            assertTrue(decoder.getHeader() == null);
            assertEquals(2, count.get());
        } catch (Exception e) {
            fail();
        }

        assertTrue(messageAByteBuf.release());
        assertEquals(0, messageAByteBuf.refCnt());
        assertTrue(messageBByteBuf.release());
        assertEquals(0, messageBByteBuf.refCnt());
    }

    /**
     * step 1    step 2          step 2             step 3
     * -----------------  -------------------  --------------------
     * |         |     |  |                 |  |                  |
     * | header |      |  |                 |  |                  |
     * -----------------  -------------------  --------------------
     * Message A              Message B              Message C
     */
    @Test
    public void testChannelRead5() {
        int messageAIndex = 10;
        int bodyALen = 1024;

        int messageBIndex = 11;
        int bodyBLen = 1024 * 2;

        int messageCIndex = 12;
        int bodyCLen = 1024 * 3;

        ByteBuf messageAByteBuf = generateMessageByteBuf(messageAIndex, bodyALen);
        ByteBuf messageBByteBuf = generateMessageByteBuf(messageBIndex, bodyBLen);
        ByteBuf messageCByteBuf = generateMessageByteBuf(messageCIndex, bodyCLen);

        AtomicInteger count = new AtomicInteger(0);
        ByteToMessageDecoder decoder = new ByteToMessageDecoder() {
            public void fireChanelRead(ChannelHandlerContext ctx, Message msg) {

                super.fireChanelRead(ctx, msg);
                count.incrementAndGet();

                if (msg.getHeader().getMethodType() == messageAIndex) {
                    assertEquals(bodyALen, msg.getHeader().getLength());
                    assertEquals(bodyALen, msg.getBuffer().readableBytes());

                    validateOriginalMessageBufferAndParsedBuffer(messageAByteBuf, msg.getBuffer(), bodyALen);

                    // assertEquals(2, getCumulation().refCnt());
                    assertNull(getCumulation());

                    assertEquals(3, messageAByteBuf.refCnt());
                    msg.release();
                    assertEquals(2, messageAByteBuf.refCnt());
                    // assertEquals(1, getCumulation().refCnt());

                    assertEquals(2, messageBByteBuf.refCnt());
                    assertEquals(2, messageCByteBuf.refCnt());
                } else if (msg.getHeader().getMethodType() == messageBIndex) {
                    assertEquals(bodyBLen, msg.getHeader().getLength());
                    assertEquals(bodyBLen, msg.getBuffer().readableBytes());

                    validateOriginalMessageBufferAndParsedBuffer(messageBByteBuf, msg.getBuffer(), bodyBLen);

                    assertTrue(getCumulation() == null);
                    msg.release();
                    assertTrue(getCumulation() == null);

                    assertEquals(2, messageAByteBuf.refCnt());
                    assertEquals(2, messageBByteBuf.refCnt());
                    assertEquals(2, messageCByteBuf.refCnt());
                } else if (msg.getHeader().getMethodType() == messageCIndex) {
                    assertEquals(bodyCLen, msg.getHeader().getLength());
                    assertEquals(bodyCLen, msg.getBuffer().readableBytes());

                    validateOriginalMessageBufferAndParsedBuffer(messageCByteBuf, msg.getBuffer(), bodyCLen);

                    assertTrue(getCumulation() == null);
                    msg.release();
                    assertTrue(getCumulation() == null);

                    assertEquals(1, messageAByteBuf.refCnt());
                    assertEquals(1, messageBByteBuf.refCnt());
                    assertEquals(2, messageCByteBuf.refCnt());
                } else {
                    fail();
                }
            }
        };

        int messageALen = Header.headerLength() + bodyALen;

        int lagerCountForHeaderLen = 2;
        int part1Len = Header.headerLength() + lagerCountForHeaderLen;
        int part2LenInA = (messageALen - part1Len);

        ByteBuf byteBufPart1 = messageAByteBuf.retainedSlice(0, part1Len);
        ByteBuf byteBufPart2 = Unpooled.wrappedBuffer(messageAByteBuf.retainedSlice(part1Len, part2LenInA),
                messageBByteBuf.retainedDuplicate());
        ByteBuf byteBufPart3 = messageCByteBuf.retainedDuplicate();

        assertEquals(3, messageAByteBuf.refCnt());
        assertEquals(2, messageBByteBuf.refCnt());
        assertEquals(2, messageCByteBuf.refCnt());
        assertEquals(1, byteBufPart1.refCnt());
        assertEquals(1, byteBufPart2.refCnt());
        assertEquals(1, byteBufPart3.refCnt());

        // msg len bigger than header len, can parse A header but not body this time
        try {
            decoder.channelRead(ctx, byteBufPart1);
            assertEquals(1, byteBufPart1.refCnt());
            assertEquals(1, byteBufPart2.refCnt());
            assertEquals(1, byteBufPart3.refCnt());
            assertEquals(3, messageAByteBuf.refCnt());
            assertEquals(2, messageBByteBuf.refCnt());
            assertEquals(2, messageCByteBuf.refCnt());
            assertTrue(decoder.getHeader() != null);
            assertEquals(lagerCountForHeaderLen, decoder.getCumulation().readableBytes());
            assertEquals(1, decoder.getCumulation().refCnt());
            assertEquals(0, count.get());
        } catch (Exception e) {
            fail();
        }

        // can parse A message and B message this time, A and B message will be fired
        try {
            decoder.channelRead(ctx, byteBufPart2);
            assertEquals(0, byteBufPart1.refCnt());
            assertEquals(0, byteBufPart2.refCnt());
            assertEquals(1, byteBufPart3.refCnt());
            assertEquals(1, messageAByteBuf.refCnt());
            assertEquals(1, messageBByteBuf.refCnt());
            assertEquals(2, messageCByteBuf.refCnt());
            assertTrue(decoder.getHeader() == null);
            assertTrue(decoder.getCumulation() == null);
            assertEquals(2, count.get());
        } catch (Exception e) {
            fail();
        }

        // cumulation + msg can build one whole C message
        try {
            decoder.channelRead(ctx, byteBufPart3);
            assertEquals(0, byteBufPart1.refCnt());
            assertEquals(0, byteBufPart2.refCnt());
            assertEquals(0, byteBufPart3.refCnt());
            assertEquals(1, messageAByteBuf.refCnt());
            assertEquals(1, messageBByteBuf.refCnt());
            assertEquals(1, messageCByteBuf.refCnt());
            assertTrue(decoder.getCumulation() == null);
            assertTrue(decoder.getHeader() == null);
            assertEquals(3, count.get());
        } catch (Exception e) {
            fail();
        }

        assertTrue(messageAByteBuf.release());
        assertEquals(0, messageAByteBuf.refCnt());
        assertTrue(messageBByteBuf.release());
        assertEquals(0, messageBByteBuf.refCnt());
        assertTrue(messageCByteBuf.release());
        assertEquals(0, messageCByteBuf.refCnt());
    }

    /**
     * step 1    step 2          step 2          step 2    step 3
     * -----------------  -------------------  --------------------
     * |         |     |  |                 |  |      |           |
     * | header |      |  |                 |  | header |         |
     * -----------------  -------------------  --------------------
     * Message A              Message B              Message C
     */
    @Test
    public void testChannelRead6() {
        int messageAIndex = 10;
        int bodyALen = 1024;

        int messageBIndex = 11;
        int bodyBLen = 1024 * 2;

        int messageCIndex = 12;
        int bodyCLen = 1024 * 3;

        ByteBuf messageAByteBuf = generateMessageByteBuf(messageAIndex, bodyALen);
        ByteBuf messageBByteBuf = generateMessageByteBuf(messageBIndex, bodyBLen);
        ByteBuf messageCByteBuf = generateMessageByteBuf(messageCIndex, bodyCLen);

        AtomicInteger count = new AtomicInteger(0);
        ByteToMessageDecoder decoder = new ByteToMessageDecoder() {
            public void fireChanelRead(ChannelHandlerContext ctx, Message msg) {

                super.fireChanelRead(ctx, msg);
                count.incrementAndGet();

                if (msg.getHeader().getMethodType() == messageAIndex) {
                    assertEquals(bodyALen, msg.getHeader().getLength());
                    assertEquals(bodyALen, msg.getBuffer().readableBytes());

                    validateOriginalMessageBufferAndParsedBuffer(messageAByteBuf, msg.getBuffer(), bodyALen);

                    // assertEquals(2, getCumulation().refCnt());
                    assertNull(getCumulation());
                    assertEquals(3, messageAByteBuf.refCnt());
                    msg.release();
                    assertEquals(2, messageAByteBuf.refCnt());
                    // assertEquals(1, getCumulation().refCnt());

                    assertEquals(2, messageBByteBuf.refCnt());
                    assertEquals(3, messageCByteBuf.refCnt());
                } else if (msg.getHeader().getMethodType() == messageBIndex) {
                    assertEquals(bodyBLen, msg.getHeader().getLength());
                    assertEquals(bodyBLen, msg.getBuffer().readableBytes());

                    validateOriginalMessageBufferAndParsedBuffer(messageBByteBuf, msg.getBuffer(), bodyBLen);

                    assertTrue(getCumulation() == null);
                    msg.release();
                    assertTrue(getCumulation() == null);

                    assertEquals(2, messageAByteBuf.refCnt());
                    assertEquals(2, messageBByteBuf.refCnt());
                    assertEquals(3, messageCByteBuf.refCnt());
                } else if (msg.getHeader().getMethodType() == messageCIndex) {
                    assertEquals(bodyCLen, msg.getHeader().getLength());
                    assertEquals(bodyCLen, msg.getBuffer().readableBytes());

                    validateOriginalMessageBufferAndParsedBuffer(messageCByteBuf, msg.getBuffer(), bodyCLen);

                    assertTrue(getCumulation() == null);
                    msg.release();
                    assertTrue(getCumulation() == null);

                    assertEquals(1, messageAByteBuf.refCnt());
                    assertEquals(1, messageBByteBuf.refCnt());
                    assertEquals(2, messageCByteBuf.refCnt());
                } else {
                    fail();
                }
            }
        };

        int messageALen = Header.headerLength() + bodyALen;
        int messageBLen = Header.headerLength() + bodyBLen;
        int messageCLen = Header.headerLength() + bodyCLen;
        int totalMessageLen = messageALen + messageBLen + messageCLen;

        int lagerCountForHeaderLen = 2;
        int part1Len = Header.headerLength() + lagerCountForHeaderLen;
        int part2LenInA = (messageALen - part1Len);
        int part2LenInB = messageBLen;
        int part2LenInC = (Header.headerLength() - lagerCountForHeaderLen);
        int part2Len = part2LenInA + part2LenInB + part2LenInC;
        int part3Len = totalMessageLen - part1Len - part2Len;

        ByteBuf byteBufPart1 = messageAByteBuf.retainedSlice(0, part1Len);
        ByteBuf byteBufPart2 = Unpooled.wrappedBuffer(messageAByteBuf.retainedSlice(part1Len, part2LenInA),
                messageBByteBuf.retainedDuplicate(), messageCByteBuf.retainedSlice(0, part2LenInC));
        ByteBuf byteBufPart3 = messageCByteBuf.retainedSlice(part2LenInC, part3Len);

        assertEquals(3, messageAByteBuf.refCnt());
        assertEquals(2, messageBByteBuf.refCnt());
        assertEquals(3, messageCByteBuf.refCnt());
        assertEquals(1, byteBufPart1.refCnt());
        assertEquals(1, byteBufPart2.refCnt());
        assertEquals(1, byteBufPart3.refCnt());

        // msg len bigger than header len, can parse A header but not body this time
        try {
            decoder.channelRead(ctx, byteBufPart1);
            assertEquals(1, byteBufPart1.refCnt());
            assertEquals(1, byteBufPart2.refCnt());
            assertEquals(1, byteBufPart3.refCnt());
            assertEquals(3, messageAByteBuf.refCnt());
            assertEquals(2, messageBByteBuf.refCnt());
            assertEquals(3, messageCByteBuf.refCnt());
            assertTrue(decoder.getHeader() != null);
            assertEquals(lagerCountForHeaderLen, decoder.getCumulation().readableBytes());
            assertEquals(1, decoder.getCumulation().refCnt());
            assertEquals(0, count.get());
        } catch (Exception e) {
            fail();
        }

        // can parse A message and B message this time, A and B message will be fired, C message header can not parse
        try {
            decoder.channelRead(ctx, byteBufPart2);
            assertEquals(0, byteBufPart1.refCnt());
            assertEquals(1, byteBufPart2.refCnt());
            assertEquals(1, byteBufPart3.refCnt());
            assertEquals(2, messageAByteBuf.refCnt());
            assertEquals(2, messageBByteBuf.refCnt());
            assertEquals(3, messageCByteBuf.refCnt());
            assertTrue(decoder.getHeader() == null);
            assertEquals(part2LenInC, decoder.getCumulation().readableBytes());
            assertEquals(1, decoder.getCumulation().refCnt());
            assertEquals(2, count.get());
        } catch (Exception e) {
            fail();
        }

        // cumulation + msg can build one whole C message
        try {
            decoder.channelRead(ctx, byteBufPart3);
            assertEquals(0, byteBufPart1.refCnt());
            assertEquals(0, byteBufPart2.refCnt());
            assertEquals(0, byteBufPart3.refCnt());
            assertEquals(1, messageAByteBuf.refCnt());
            assertEquals(1, messageBByteBuf.refCnt());
            assertEquals(1, messageCByteBuf.refCnt());
            assertTrue(decoder.getCumulation() == null);
            assertTrue(decoder.getHeader() == null);
            assertEquals(3, count.get());
        } catch (Exception e) {
            fail();
        }

        assertTrue(messageAByteBuf.release());
        assertEquals(0, messageAByteBuf.refCnt());
        assertTrue(messageBByteBuf.release());
        assertEquals(0, messageBByteBuf.refCnt());
        assertTrue(messageCByteBuf.release());
        assertEquals(0, messageCByteBuf.refCnt());
    }

    /**
     * step 1    step 2    step 2   step 3
     * -----------------  -------------------
     * |        |      |  |       |         |
     * | header|       |  | header |        |
     * -----------------  -------------------
     * Message A            Message B
     */
    @Test
    public void testChannelRead7() {
        int messageAIndex = 2;
        int bodyALen = 1024;
        int dataALen = 0;

        int messageBIndex = 11;
        int bodyBLen = 1024 * 2;

        ByteBuf messageAByteBuf = generateMessageByteBuf(messageAIndex, bodyALen);
        ByteBuf messageBByteBuf = generateMessageByteBuf(messageBIndex, bodyBLen);

        AtomicInteger count = new AtomicInteger(0);
        ByteToMessageDecoder decoder = new ByteToMessageDecoder() {
            public void fireChanelRead(ChannelHandlerContext ctx, Message msg) {
                super.fireChanelRead(ctx, msg);
                count.incrementAndGet();

                if (msg.getHeader().getMethodType() == messageAIndex) {
                    assertEquals(bodyALen, msg.getHeader().getLength());
                    assertEquals(bodyALen, msg.getBuffer().readableBytes());

                    validateOriginalMessageBufferAndParsedBuffer(messageAByteBuf, msg.getBuffer(), bodyALen);

                    // assertEquals(2, getCumulation().refCnt());
                    assertNull(getCumulation());
                    assertEquals(3, messageAByteBuf.refCnt());
                    msg.release();
                    assertEquals(2, messageAByteBuf.refCnt());
                    // assertEquals(1, getCumulation().refCnt());

                    assertEquals(3, messageBByteBuf.refCnt());
                } else if (msg.getHeader().getMethodType() == messageBIndex) {
                    assertEquals(bodyBLen, msg.getHeader().getLength());
                    assertEquals(bodyBLen, msg.getBuffer().readableBytes());

                    validateOriginalMessageBufferAndParsedBuffer(messageBByteBuf, msg.getBuffer(), bodyBLen);

                    assertTrue(getCumulation() == null);
                    msg.release();
                    assertTrue(getCumulation() == null);

                    assertEquals(1, messageAByteBuf.refCnt());
                    assertEquals(2, messageBByteBuf.refCnt());
                } else {
                    fail();
                }
            }
        };

        int messageALen = Header.headerLength() + bodyALen;
        int messageBLen = Header.headerLength() + bodyBLen;
        int totalMessageLen = messageALen + messageBLen;

        int lagerCountForHeaderLen = 2;
        int part1Len = Header.headerLength() + lagerCountForHeaderLen;
        int part2LenInA = (messageALen - part1Len);
        int part2LenInB = (Header.headerLength() - lagerCountForHeaderLen);
        int part2Len = part2LenInA + part2LenInB;
        int part3Len = totalMessageLen - part1Len - part2Len;

        ByteBuf byteBufPart1 = messageAByteBuf.retainedSlice(0, part1Len);
        ByteBuf byteBufPart2 = Unpooled.wrappedBuffer(messageAByteBuf.retainedSlice(part1Len, part2LenInA),
                messageBByteBuf.retainedSlice(0, part2LenInB));
        ByteBuf byteBufPart3 = messageBByteBuf.retainedSlice(part2LenInB, part3Len);

        assertEquals(3, messageAByteBuf.refCnt());
        assertEquals(3, messageBByteBuf.refCnt());
        assertEquals(1, byteBufPart1.refCnt());
        assertEquals(1, byteBufPart2.refCnt());
        assertEquals(1, byteBufPart3.refCnt());

        // step 1: msg len bigger than header len, can parse A header but not body this time
        try {
            decoder.channelRead(ctx, byteBufPart1);
            assertEquals(1, byteBufPart1.refCnt());
            assertEquals(1, byteBufPart2.refCnt());
            assertEquals(1, byteBufPart3.refCnt());
            assertEquals(3, messageAByteBuf.refCnt());
            assertEquals(3, messageBByteBuf.refCnt());
            assertTrue(decoder.getHeader() != null);
            assertEquals(lagerCountForHeaderLen, decoder.getCumulation().readableBytes());
            assertEquals(1, decoder.getCumulation().refCnt());
            assertEquals(0, count.get());
        } catch (Exception e) {
            fail();
        }

        // step 2: can parse A message, B header can not parse this time, and A message will be fired
        try {
            decoder.channelRead(ctx, byteBufPart2);
            assertEquals(0, byteBufPart1.refCnt());
            assertEquals(1, byteBufPart2.refCnt());
            assertEquals(1, byteBufPart3.refCnt());
            assertEquals(2, messageAByteBuf.refCnt());
            assertEquals(3, messageBByteBuf.refCnt());
            assertTrue(decoder.getHeader() == null);
            assertEquals(part2LenInB, decoder.getCumulation().readableBytes());
            assertEquals(1, decoder.getCumulation().refCnt());
            assertEquals(1, count.get());
        } catch (Exception e) {
            fail();
        }

        // step 3: cumulation + msg can build one whole B message
        try {
            decoder.channelRead(ctx, byteBufPart3);
            assertEquals(0, byteBufPart1.refCnt());
            assertEquals(0, byteBufPart2.refCnt());
            assertEquals(0, byteBufPart3.refCnt());
            assertEquals(1, messageAByteBuf.refCnt());
            assertEquals(1, messageBByteBuf.refCnt());
            assertTrue(decoder.getCumulation() == null);
            assertTrue(decoder.getHeader() == null);
            assertEquals(2, count.get());
        } catch (Exception e) {
            fail();
        }

        assertTrue(messageAByteBuf.release());
        assertEquals(0, messageAByteBuf.refCnt());
        assertTrue(messageBByteBuf.release());
        assertEquals(0, messageBByteBuf.refCnt());
    }

    /**
     * @param index
     * @param bodySize
     * @return
     */
    private Message generateMessage(int index, int bodySize) {
        Header header = new Header((byte) index, 0, bodySize, index);
        ByteBuf buf = allocator.buffer(Header.headerLength() + bodySize);
        header.toBuffer(buf);
        for (int i = 0; i < bodySize; i++) {
            buf.writeByte(i);
        }

        return new MessageImpl(header, buf);
    }

    private byte[] generateMessageBytes(int index, int bodySize) {
        Header header = new Header((byte) index, 0, bodySize, index);
        byte[] buf = new byte[Header.headerLength() + bodySize];
        ByteBuffer buff = ByteBuffer.wrap(buf);
        header.toBuffer(buff);
        for (int i = 0; i < bodySize; i++) {
            buff.put((byte) i);
        }
        return buf;
    }

    private ByteBuf generateMessageByteBuf(int index, int bodySize) {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        Header header = new Header((byte) index, 0, bodySize, index);
        ByteBuf byteBuf = allocator.buffer(Header.headerLength() + bodySize);
        header.toBuffer(byteBuf);
        for (int i = 0; i < bodySize; i++) {
            byteBuf.writeByte((byte) i);
        }
        return byteBuf;
    }

    private void validateOriginalMessageBufferAndParsedBuffer(ByteBuf originalMessageBuffer,
            ByteBuf parsedMessageBuffer, int bodyLength) {
        ByteBuf src = originalMessageBuffer.slice(Header.headerLength(), bodyLength);
        ByteBuf des = parsedMessageBuffer;
        for (int j = 0; j < bodyLength; j++) {
            byte srcByte = src.readByte();
            byte desByte = des.readByte();
            assertEquals(srcByte, desByte);
        }
    }

    private void validateOriginalMessageAndParsedMessage(Message originalMessage, Message parsedMessage) {
        Validate.notNull(originalMessage);
        Validate.notNull(parsedMessage);
        Validate.isTrue(originalMessage.getHeader().equals(parsedMessage.getHeader()));
        int bodyLength = originalMessage.getHeader().getDataLength();
        validateOriginalMessageBufferAndParsedBuffer(originalMessage.getBuffer(), parsedMessage.getBuffer(),
                bodyLength);
    }

    private void validateTwoMessageList(List<Message> originalMessageList, List<Message> parsedMessageList) {
        Validate.notNull(originalMessageList);
        Validate.notNull(parsedMessageList);
        Validate.isTrue(originalMessageList.size() == parsedMessageList.size());

        int count = originalMessageList.size();

        if (count > 0) {
            for (int i = 0; i < count; i++) {
                validateOriginalMessageAndParsedMessage(originalMessageList.get(i), parsedMessageList.get(i));
            }
        }
    }

    @Test
    public void testHowToUseCompositeBuf() {
        // this test is used to test how a compositeBuf behaves, especially when its maxNumComponents are very small
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;

        CompositeByteBuf compositeByteBuf = new CompositeByteBuf(allocator, true, 5);
        ByteBuf [] bufs = new ByteBuf[20];

        for (int i = 0; i < 20; i ++ ) {
            logger.warn(" " + i);
            ByteBuf data = PooledByteBufAllocator.DEFAULT.buffer(100);
            bufs[i] = data;
            data.writerIndex(100);
            // we need to retain the data in case compositeByteBuf.addComponent release data
            data.retain();
            compositeByteBuf.addComponent(true, data);
            data.release();
        }

        compositeByteBuf.release();

        for (int i = 0; i < 20; i ++) {
            assertEquals(0, bufs[i].refCnt());
        }
    }
}

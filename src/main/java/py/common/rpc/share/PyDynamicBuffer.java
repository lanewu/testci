package py.common.rpc.share;

import org.apache.commons.lang.Validate;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

public class PyDynamicBuffer {
    private ChannelBuffer channelBuffer;
    private static final int DEFAULT_INIT_BUFFER_SIZE = 1024;
    private static final int MAX_THRESHOLD_SIZE_EXPONENTIAL_INCREASE = 1024 * 1024;
    private int maxThresholdSizeOfExponentialIncrease = MAX_THRESHOLD_SIZE_EXPONENTIAL_INCREASE;

    public PyDynamicBuffer(int initSize, int maxThresholdSizeOfExponentialIncrease) {
        Validate.isTrue(initSize <= maxThresholdSizeOfExponentialIncrease);
        channelBuffer = ChannelBuffers.buffer(((initSize >> 10) << 10));
        this.maxThresholdSizeOfExponentialIncrease = maxThresholdSizeOfExponentialIncrease;
    }

    public PyDynamicBuffer(int initSize) {
        this(initSize, MAX_THRESHOLD_SIZE_EXPONENTIAL_INCREASE);
    }

    public PyDynamicBuffer() {
        this(DEFAULT_INIT_BUFFER_SIZE, MAX_THRESHOLD_SIZE_EXPONENTIAL_INCREASE);
    }

    public void setWriteIndex(int writeIndex) {
        channelBuffer.writerIndex(writeIndex);
    }

    public void write(byte[] src, int offset, int length) {
        int canWriteBytes = channelBuffer.writableBytes();
        if (canWriteBytes < length) {
            // Keep the current write index, after the buffer is expanded, the write index should be recoveried.
            int writeIndex = channelBuffer.writerIndex();

            // For expanding the buffer, the write index should be moved to the capacity.
            channelBuffer.writerIndex(channelBuffer.capacity());

            // calculate the expend size of channelBuffer buffer
            int originSize = writeIndex;
            int increaseSize = channelBuffer.capacity();
            while ((increaseSize - originSize) < length) {
                if (increaseSize >= maxThresholdSizeOfExponentialIncrease) {
                    increaseSize += maxThresholdSizeOfExponentialIncrease;
                } else {
                    increaseSize <<= 1;
                }
            }

            ChannelBuffer expandBuffer = ChannelBuffers.buffer(increaseSize - originSize);
            expandBuffer.writerIndex(expandBuffer.capacity());
            channelBuffer = ChannelBuffers.wrappedBuffer(channelBuffer, expandBuffer);
            channelBuffer.writerIndex(writeIndex);
        }
        channelBuffer.writeBytes(src, offset, length);
    }

    public ChannelBuffer getBuffer() {
        return channelBuffer;
    }

    @Override
    public String toString() {
        return "PyDynamicBuffer [channelBuffer=" + channelBuffer + "]";
    }
}

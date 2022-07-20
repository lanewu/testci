package py.storage.async;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.tlsf.bytebuffer.manager.TLSFByteBufferManager;
import py.common.tlsf.bytebuffer.manager.TLSFByteBufferManagerFactory;
import sun.nio.ch.DirectBuffer;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

/**
 * A package-private class for async file accessor
 *
 * @author tyr
 */
class IOUtil {
    private static final Logger logger = LoggerFactory.getLogger(IOUtil.class);

    /**
     * Write data to the file from native buffer.
     *
     * @param buffer The data buffer, must be {@link java.nio.DirectByteBuffer}. And it's {@link Buffer#remaining()}
     *               must be positive
     */
    private static <A> void writeFromNativeBuffer(ByteBuffer buffer, long offset, AsyncFileAccessor accessor,
            A attachment, CompletionHandler<Integer, A> completionHandler) {
        // get the available length of buffer
        int length = buffer.remaining();
        int position = buffer.position();

        // length must be positive
        Validate.isTrue(length > 0);

        // write through accessor
        accessor.write(((DirectBuffer) buffer).address() + position, offset, length, attachment, completionHandler);
    }

    /**
     * Read data from the file into native buffer.
     *
     * @param buffer The data buffer, must be {@link java.nio.DirectByteBuffer}.
     */
    private static <A> void readIntoNativeBuffer(ByteBuffer buffer, long offset, AsyncFileAccessor accessor,
            A attachment, CompletionHandler<Integer, A> completionHandler) {

        int position = buffer.position();

        // get the available length of buffer
        int length = buffer.remaining();
        if (length <= 0) {
            logger.warn("reading into a empty buffer {} {} {}", accessor, offset, buffer);
            completionHandler.completed(0, attachment);
        } else {
            // read through accessor
            accessor.read(((DirectBuffer) buffer).address() + (long) position, offset, length, attachment,
                    new CompletionHandler<Integer, A>() {
                        @Override
                        public void completed(Integer result, A attachment) {
                            // set the buffer's position to a proper value.
                            // because the native accessor doesn't change buffer's position after using it.
                            if (result > 0) {
                                buffer.position(position + result);
                            }
                            completionHandler.completed(result, attachment);
                        }

                        @Override
                        public void failed(Throwable exc, A attachment) {
                            completionHandler.failed(exc, attachment);
                        }
                    });
        }
    }

    static <A> void read(ByteBuffer buffer, long offset, AsyncFileAccessor accessor, A attachment,
            CompletionHandler<Integer, A> completionHandler) {
        if (buffer.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        } else if (buffer instanceof DirectBuffer) {
            readIntoNativeBuffer(buffer, offset, accessor, attachment, completionHandler);
        } else {
            // if the buffer is not direct, we need a memory allocation and copy.
            // TODO allocate an aligned and controlled buffer
            //ByteBuffer directBuffer = ByteBuffer.allocateDirect(buffer.remaining());
            TLSFByteBufferManager tlsfByteBufferManager = TLSFByteBufferManagerFactory.instance();
            Validate.notNull(tlsfByteBufferManager);
            ByteBuffer directBuffer = tlsfByteBufferManager.blockingAllocate(buffer.remaining());

            readIntoNativeBuffer(directBuffer, offset, accessor, attachment, new CompletionHandler<Integer, A>() {
                @Override
                public void completed(Integer result, A attachment) {
                    // data was read into the direct buffer, now copy to the request buffer
                    directBuffer.flip();
                    if (result > 0) {
                        buffer.put(directBuffer);
                    }
                    tlsfByteBufferManager.release(directBuffer);
                    completionHandler.completed(result, attachment);
                }

                @Override
                public void failed(Throwable exc, A attachment) {
                    tlsfByteBufferManager.release(directBuffer);
                    completionHandler.failed(exc, attachment);
                }
            });
        }
    }

    static <A> void write(ByteBuffer buffer, long offset, AsyncFileAccessor accessor, A attachment,
            CompletionHandler<Integer, A> completionHandler) {
        if (buffer instanceof DirectBuffer) {
            writeFromNativeBuffer(buffer, offset, accessor, attachment, completionHandler);
        } else {

            int position = buffer.position();
            int limit = buffer.limit();

            int length = position <= limit ? limit - position : 0;
            if (length == 0) {
                logger.warn("writing from a 0 size buffer {} {} {}", accessor, offset, buffer);
                completionHandler.completed(0, attachment);
                return;
            }

            // if the buffer is not direct, we need a memory allocation and copy.
            // TODO allocate an aligned and controlled buffer
            //ByteBuffer directBuffer = ByteBuffer.allocateDirect(length);

            TLSFByteBufferManager tlsfByteBufferManager = TLSFByteBufferManagerFactory.instance();
            Validate.notNull(tlsfByteBufferManager);

            ByteBuffer directBuffer = tlsfByteBufferManager.blockingAllocate(length);
            // memory copy
            directBuffer.put(buffer);
            directBuffer.flip();

            // keep the original position for request buffer
            buffer.position(position);
            writeFromNativeBuffer(directBuffer, offset, accessor, attachment, new CompletionHandler<Integer, A>() {
                @Override
                public void completed(Integer result, A attachment) {
                    // update the position of request buffer to a proper value
                    if (result > 0) {
                        buffer.position(position + result);
                    }
                    tlsfByteBufferManager.release(directBuffer);
                    completionHandler.completed(result, attachment);
                }

                @Override
                public void failed(Throwable exc, A attachment) {
                    tlsfByteBufferManager.release(directBuffer);
                    completionHandler.failed(exc, attachment);
                }
            });
        }
    }

}

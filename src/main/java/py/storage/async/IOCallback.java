package py.storage.async;

import py.datanode.exception.StorageIOException;
import py.exception.StorageException;

import java.nio.channels.CompletionHandler;

/**
 * Created by xcs on 18-5-9.
 */
class IOCallback<A> implements Callback {

    private final long offset;
    private final int length;
    private final A attachment;
    private final CompletionHandler<Integer, A> completionHandler;

    IOCallback(long offset, int length, A attachment, CompletionHandler<Integer, A> completionHandler) {
        this.offset = offset;
        this.length = length;
        this.attachment = attachment;
        this.completionHandler = completionHandler;
    }

    @Override
    public void done(int errCode) {
        switch (errCode) {
        case AsyncFileAccessor.ERR_SUCCESS:
            completionHandler.completed(length, attachment);
            break;
        case AsyncFileAccessor.ERR_FAIL:
            completionHandler.failed(new StorageIOException(offset, length), attachment);
            break;
        default:
            completionHandler.failed(new StorageException("unknown error code"), attachment);
            break;
        }
    }
}

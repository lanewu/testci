package py.common;

import java.util.List;

public class SyncLogFastBufferImpl extends AbstractFastBuffer {

    public SyncLogFastBufferImpl(List<FastBuffer> fastBuffers, long totalSize) {
        super(fastBuffers, totalSize);
    }
}

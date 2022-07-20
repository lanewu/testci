package py.common;

import java.util.List;

public class SecondFastBufferImpl extends AbstractFastBuffer {

    public SecondFastBufferImpl(List<FastBuffer> fastBuffers, long totalSize) {
        super(fastBuffers, totalSize);
    }

}

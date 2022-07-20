package py.common;

import java.util.List;

public class PrimaryFastBufferImpl extends AbstractFastBuffer {

    PrimaryFastBufferImpl(List<FastBuffer> fastBuffers, long totalSize) {
        super(fastBuffers, totalSize);
    }

}

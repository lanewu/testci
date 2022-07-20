package py.netty.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.netty.core.nio.SingleThreadEventLoop;

/**
 * Created by zhongyuan on 18-2-25.
 */
public class PYTimeToFlushImpl implements PYTimeToFlush {
    private final static Logger logger = LoggerFactory.getLogger(PYTimeToFlushImpl.class);


    private SingleThreadEventLoop singleThreadEventLoop;
    private int requestCount;

    public PYTimeToFlushImpl(SingleThreadEventLoop singleThreadEventLoop) {
            this.singleThreadEventLoop = singleThreadEventLoop;
    }

    @Override
    public void call() {
        logger.debug("before flush, {} message have been written", this.requestCount);
        this.requestCount = 0;
        this.singleThreadEventLoop.flushAllChannels();
    }

    @Override
    public void incRequestCount() {
        this.requestCount++;
    }
}

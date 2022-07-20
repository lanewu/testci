package py.netty.client;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.netty.core.MethodCallback;
import py.netty.exception.TimeoutException;

/**
 * Created by zhongyuan on 18-6-26.
 */
public class MessageTimerTask<T> implements TimerTask {
    private final static Logger logger = LoggerFactory.getLogger(MessageTimerTask.class);
    private final int ioTimeout;
    public final long requestId;
    private TimerTaskRemover timerTaskRemover;

    private final MethodCallback<T> callback;

    public MessageTimerTask(Long requestId, MethodCallback<T> callback, TimerTaskRemover timerTaskRemover, int ioTimeout) {
        this.requestId = requestId;
        this.callback = callback;
        this.timerTaskRemover = timerTaskRemover;
        this.ioTimeout = ioTimeout;
    }

    public MethodCallback<T> getCallback() {
        return callback;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        MethodCallback<T> callback = timerTaskRemover.removeTimer(requestId);
        if (callback != null) {
            logger.warn("callback is not null. requestId:{}", requestId);
            callback.fail(new TimeoutException("cost time:" + ioTimeout + ", requestId:" + requestId));
        } else {
            logger.warn("callback is null. requestId:{}", requestId);
        }
    }
}

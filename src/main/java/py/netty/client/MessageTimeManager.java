package py.netty.client;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.netty.core.MethodCallback;
import py.netty.exception.DisconnectionException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MessageTimeManager<T> {
    private final static Logger logger = LoggerFactory.getLogger(MessageTimeManager.class);
    private final HashedWheelTimer timer;
    private final Map<Long, Timeout> messageTable;
    private final int ioTimeout;
    private static PYMetric counterPendingTimer;
    private volatile boolean broken;

    static {
        PYMetricRegistry registry = PYMetricRegistry.getMetricRegistry();
        counterPendingTimer = registry
                .register(MetricRegistry.name(MessageTimeManager.class.getSimpleName(), "counter.pending.timer.task"),
                        Counter.class);
    }

    public MessageTimeManager(HashedWheelTimer timer, int ioTimeout) {
        this.timer = timer;
        this.messageTable = new ConcurrentHashMap<>();
        this.ioTimeout = ioTimeout;
        this.broken = false;
    }

    public int getPendingMessageCount() {
        return messageTable.size();
    }

    public void addTimer(Long requestId, MethodCallback<T> callback) {
        MessageTimerTask task = new MessageTimerTask(requestId, callback, this::removeTimer, ioTimeout);
        if (messageTable.containsKey(requestId)) {
            logger.warn("already has request:{}, do nothing", requestId);
            return;
        }

        logger.debug("add timer requestId:{}", requestId);
        Timeout timeout = timer.newTimeout(task, ioTimeout, TimeUnit.MILLISECONDS);
        if (messageTable.putIfAbsent(requestId, timeout) != null) {
            timeout.cancel();
        } else {
            counterPendingTimer.incCounter();
        }
    }

    public MethodCallback<T> removeTimer(long requestId) {
        Timeout timeout = messageTable.remove(requestId);
        if (timeout == null) {
            return null;
        }

        logger.debug("remove timer requestId:{}", requestId);
        counterPendingTimer.decCounter();
        timeout.cancel();
        return ((MessageTimerTask) timeout.task()).getCallback();
    }

    public void fireChannelClose() {
        // TODO
        // if there are multiple threads , here (broken) MUST TO BE FIXED.
        broken = true;
        Set<Long> keys = new HashSet<>();
        try {
            for (Map.Entry<Long, Timeout> entry : messageTable.entrySet()) {
                keys.add(entry.getKey());
            }
        } catch (Exception e) {
            logger.warn("caught an exception when firing the channel close. messageTable:{}", messageTable, e);
        }

        for (Long requestId : keys) {
            try {
                MethodCallback<T> callback = removeTimer(requestId);
                if (callback != null) {
                    logger.warn(
                            "handle pending request on the time manager. notify the request id={} that the channel has broken",
                            requestId);
                    callback.fail(new DisconnectionException("request: " + requestId));
                } else {
                    logger.warn("handle pending request on the time manager. request id={} already been notify",
                            requestId);
                }
            } catch (Throwable e) {
                logger.error("request id={} caught an exception when calling notifyAllListeners", requestId, e);
            }
        }
        Validate.isTrue(messageTable.size() == 0);
    }

    public boolean isBroken() {
        return broken;
    }
}

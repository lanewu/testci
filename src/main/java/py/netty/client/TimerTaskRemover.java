package py.netty.client;

import py.netty.core.MethodCallback;

/**
 * Created by zhongyuan on 18-6-26.
 */
public interface TimerTaskRemover<T> {
    public MethodCallback<T> removeTimer(long requestId);
}

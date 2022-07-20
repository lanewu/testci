package py.netty.core;

/**
 * Created by zhongyuan on 18-2-25.
 */
public interface PYTimeToFlush {
    public void call();

    // just for logger
    public void incRequestCount();
}

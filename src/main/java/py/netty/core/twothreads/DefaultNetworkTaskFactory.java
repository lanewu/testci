package py.netty.core.twothreads;

import java.util.concurrent.TimeUnit;

public class DefaultNetworkTaskFactory implements NetworkTaskFactory {
    @Override
    public NetworkTask generate(Runnable runnable, long delayed, TimeUnit timeUnit) {
        return new DefaultNetworkTask(runnable, delayed, timeUnit);
    }
}

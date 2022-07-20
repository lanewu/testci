package py.netty.core.twothreads;

import java.util.concurrent.TimeUnit;

public interface NetworkTaskFactory {
    NetworkTask generate(Runnable runnable, long delay, TimeUnit timeUnit);
}

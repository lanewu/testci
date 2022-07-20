package py.common;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
    private AtomicInteger no = new AtomicInteger(1);
    private String threadName;
    private boolean isDaemon;

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public NamedThreadFactory(String name) {
      this(name, false);
    }

    public NamedThreadFactory(String name, boolean daemon) {
        this.threadName = name;
        this.isDaemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, threadName + no.getAndIncrement());
        t.setDaemon(isDaemon);
        return t;
    }
}

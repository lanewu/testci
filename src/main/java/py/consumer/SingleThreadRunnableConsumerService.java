package py.consumer;

import py.common.NamedThreadFactory;

import java.util.concurrent.*;

public class SingleThreadRunnableConsumerService<T extends Runnable> implements ConsumerService<T> {

    private final ExecutorService executor;

    public SingleThreadRunnableConsumerService(String name) {
        executor = Executors.newSingleThreadExecutor(new NamedThreadFactory(name));
    }

    public SingleThreadRunnableConsumerService(String name, BlockingQueue<Runnable> workQueue) {
        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, workQueue, new NamedThreadFactory(name));
    }

    @Override
    public void start() {
        // no need to start
    }

    @Override
    public void stop() {
        executor.shutdown();
        try {
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
    }

    @Override
    public boolean submit(T element) {
        executor.execute(element);
        return true;
    }
}

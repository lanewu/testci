package py.common.cache;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class CachedProducerImpl<T> implements CachedProducer<T> {

    private final BlockingQueue<T> cachedProducts = new LinkedBlockingQueue<>();
    private final AtomicLong cachePoolSize = new AtomicLong(0);
    private final ExecutorService producerExecutor;
    private final int cachedProductsLowerThreshold;
    private final int cachedProductsUpperThreshold;

    private final Callable<T> producer;

    public CachedProducerImpl(ExecutorService producerExecutor, int cachedProductsLowerThreshold,
            int cachedProductsUpperThreshold, Callable<T> producer) {
        this.producerExecutor = producerExecutor;
        this.cachedProductsLowerThreshold = cachedProductsLowerThreshold;
        this.cachedProductsUpperThreshold = cachedProductsUpperThreshold;
        this.producer = producer;
    }

    @Override
    public T poll() throws Exception {

        T address = cachedProducts.poll();
        long count;

        if (address == null) {
            address = producer.call();
            count = 0;
        } else {
            count = cachePoolSize.decrementAndGet();
        }

        if (count == cachedProductsLowerThreshold || count == 0) {
            producerExecutor.execute(() -> {
                for (int i = 0; i < cachedProductsUpperThreshold; i++) {
                    try {
                        cachedProducts.offer(producer.call());
                        if (cachePoolSize.incrementAndGet() >= cachedProductsUpperThreshold) {
                            break;
                        }
                    } catch (Exception e) {
                        break;
                    }
                }
            });
        }

        return address;
    }

}

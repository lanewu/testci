package py.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * <p>A dispatcher for multi consumer services.
 *
 * <p>Each element (E) has a type (T), we will create a consumer service for each T, and all elements with element
 * t1 will be submitted to t1's consumer service.
 *
 * @param <T> The element type, each element is supposed to belong to [one and only one] type
 * @param <E> The element
 */
public class ConsumerServiceDispatcher<T, E> implements ConsumerService<E> {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerServiceDispatcher.class);

    /**
     * Holder for all created consumer services.
     */
    private final ConcurrentHashMap<T, ConsumerService<E>> consumers = new ConcurrentHashMap<>();

    /**
     * Type identifier to find out which type an element belongs to.
     */
    private final Function<E, ? extends T> typeIdentifier;

    /**
     * Consumer service factory to create new working consumer services.
     */
    private final Function<? super T, ConsumerService<E>> consumerServiceFactory;

    /**
     * Stop signal
     */
    private volatile boolean isStopped;

    /**
     * Creates a {@code ConsumerServiceDispatcher} with the specified type identifier and consumer service factory
     *
     * @param typeIdentifier         type identifier to figure out which type an element belongs to
     * @param consumerServiceFactory consumer service factory to create working consumer services
     */
    public ConsumerServiceDispatcher(Function<E, ? extends T> typeIdentifier,
            Function<? super T, ConsumerService<E>> consumerServiceFactory) {
        this.typeIdentifier = typeIdentifier;
        this.consumerServiceFactory = consumerServiceFactory;
    }

    @Override
    public void start() {
        isStopped = false;
        // nothing to start
    }

    /**
     * Stop the consumer service, created consumer services should be stopped and removed before that. After stopped,
     * this dispatcher could be reopen.
     */
    @Override
    public synchronized void stop() {
        isStopped = true;
        List<T> stoppedKey = new ArrayList<>();
        for (Map.Entry<T, ConsumerService<E>> entry : consumers.entrySet()) {
            entry.getValue().stop();
            stoppedKey.add(entry.getKey());
        }

        for (T t : stoppedKey) {
            consumers.remove(t);
        }
    }

    /**
     * Submit an element to this consumer service.
     * Elements will be submitted to the certain consumer service according to their type(T) identified by the
     * {@link #typeIdentifier}
     *
     * @param element element to be submitted
     * @return true if succeeded
     */
    @Override
    public boolean submit(E element) {
        if (isStopped) {
            logger.warn("consumer service has been stopped, refuse the request {}", element);
            return false;
        }

        T type = typeIdentifier.apply(element);
        if (type == null) {
            return false;
        }

        ConsumerService<E> consumer = consumers.computeIfAbsent(type, t -> {
            ConsumerService<E> newConsumer = consumerServiceFactory.apply(t);
            if (newConsumer == null) {
                return null;
            }
            newConsumer.start();
            return newConsumer;
        });

        if (consumer == null) {
            logger.warn("got a null consumer {}, the consumers' map {}", element, consumers);
            return false;
        }

        try {
            return consumer.submit(element);
        } finally {
            if (isStopped) {
                stop();
            }
        }
    }

}

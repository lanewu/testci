package py.consumer;

import java.util.Collection;

/**
 * A consumer variant that accepts both single value and a collection of values.
 *
 * <p>
 * And can be started and stopped.
 * </p>
 *
 * @param <E>
 */
public interface ConsumerService<E> {

    /**
     * Start the consumer service
     */
    void start();

    /**
     * Stop the consumer service, already submitted elements should be consumed before stopped
     */
    void stop();

    /**
     * submit an element
     *
     * @param element element to be submitted
     * @return true if success
     */
    boolean submit(E element);

    /**
     * submit a collection of elements
     *
     * @param elements elements to be submitted
     * @return success count
     */
    default int submit(Collection<E> elements) {
        int successCount = 0;
        for (E element : elements) {
            successCount += submit(element) ? 1 : 0;
        }
        return successCount;
    }

}

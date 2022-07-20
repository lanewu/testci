package py.netty.core.twothreads;

import io.netty.util.internal.PlatformDependent;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * This class is similar to py.engine.TaskEngine while this class is more for netty
 */
public abstract class NetworkTaskEngineUsingBlockingQueue<E> extends NetworkTaskEngine<E> {
    final static Logger logger = LoggerFactory.getLogger(NetworkTaskEngineUsingBlockingQueue.class);
    final private BlockingQueue<E> queue;

    public NetworkTaskEngineUsingBlockingQueue(Consumer<E> consumer, String engineName) {
        super(consumer, engineName);
        queue = newTaskQueue();
    }

    protected BlockingQueue<E> newTaskQueue() {
        // This event loop never calls takeTask()
        return new LinkedBlockingQueue<E>();
    }

    @Override
    protected int size() {
        return queue.size();
    }

    @Override
    protected boolean enqueue(E element) {
        try {
            queue.put(element);
            return true;
        } catch (Throwable t) {
            logger.warn("failed to insert an element to the task engine", t);
            return false;
        }
    }

    @Override
    protected int drainElements(Collection<E> container) {
        Validate.notNull(container, "container is null at the operation of draining");

        return queue.drainTo(container);
    }

    /**
     * take element. blocking
     */
    @Override
    protected E takeElement() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            logger.warn("interrupted", e);
            return null;
        }
    }

    protected abstract void enqueueShutdownTask();

    protected abstract boolean isShutdownTask(E e);
}


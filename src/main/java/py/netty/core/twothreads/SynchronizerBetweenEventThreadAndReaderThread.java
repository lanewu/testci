package py.netty.core.twothreads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is a special class which is used to synchronize the task-execution thread and the reader thread.
 * <p>
 * There are some socket events that need to be executed by the task-execution thread. However, the reader thread is
 * responsible for process selection events of channals. These operations can block each other. For example, a
 * connection event being processed by the task thread can be blocked by the reader'For example, a connection event
 * being processed by the task thread can be blocked by the reader's select operation.
 * In order to unblock these socket events that are executed in the different threads, we can use this class. The usage
 * is as follows:
 * <p>
 * For task thread:
 * <p>
 * communicationViaChannel.clientWakingServerUpServer();       // the reader thread should be waken up from select() operation
 * <p>
 * communicationViaChannel.clientWaitOnBarrier();
 * <p>
 * <p>
 * For the reader thread:
 * <p>
 * communicationViaChannel.register(selector);
 * selector.select();
 * <p>
 * // check if it was just waken up by the task thread // if it was communicationViaChannel.serverWaitOnBarrier();
 */
public class SynchronizerBetweenEventThreadAndReaderThread {
    private static final Logger logger = LoggerFactory.getLogger(SynchronizerBetweenEventThreadAndReaderThread.class);
    private volatile boolean clientWakingServerUp = false;
    private Selector selector;

    protected static class MyCyclicBarrier extends CyclicBarrier {
        private final AtomicBoolean hasReset = new AtomicBoolean(false);

        public MyCyclicBarrier(int parties) {
            super(parties);
        }

        @Override public void reset() {
            // set the flag, so that any await() calls become invalid
            hasReset.set(true);
            super.reset();
        }

        @Override public int await() throws InterruptedException, BrokenBarrierException {
            if (!hasReset.get()) {
                return super.await();
            } else {
                // barrier has been reset, return 0 to indicate this thread is the last one to arrive
                return 0;
            }
        }

        @Override public int await(long timeout, TimeUnit unit)
                throws InterruptedException, BrokenBarrierException, TimeoutException {
            if (!hasReset.get()) {
                return super.await(timeout, unit);
            } else {
                // barrier has been reset, return 0 to indicate this thread is the last one to arrive
                return 0;
            }
        }
    }

    final AtomicReference<MyCyclicBarrier> barrierAtomicReference;

    public SynchronizerBetweenEventThreadAndReaderThread(int seqId, Selector selector) throws IOException {
        barrierAtomicReference = new AtomicReference<>(new MyCyclicBarrier(2));
        this.selector = selector;
    }

    /*
     * register the server channel
     */
    public void register(Selector selector) {
        // I am only interested in reading data
        logger.warn("register a new selector {} ", selector);
        this.selector = selector;
    }

    /**
     * someone (client) has waken the selector up. As the server, it will wait on the barrier
     */
    public void serverWaitOnBarrier() {
        if (clientWakingServerUp) {
            logger.warn("waken up by the client");
            clientWakingServerUp = false;
            CyclicBarrier barrier = barrierAtomicReference.get();
            try {
                if (barrier != null) {
                    barrier.await();
                    logger.warn("server finished waiting on the barrier");
                } else {
                    logger.warn("barrier is null while client might be waiting on the barrier");
                }
            } catch (Exception e) {
                logger.warn("the server side interrupted while waiting for the barrier", e);
            }
        }
    }

    /**
     * client wakes up the server
     *
     * @throws IOException
     */
    public void clientWakeupServer() {
        clientWakingServerUp = true;
        selector.wakeup();
    }

    // Notify the server that the client has done
    public void clientWaitOnBarrier() {
        CyclicBarrier barrier = barrierAtomicReference.get();
        try {
            if (barrier != null) {
                logger.warn("client starts to wait on barrier");
                barrier.await();
                logger.warn("client finished waiting on barrier");
            }
        } catch (Exception e) {
            logger.warn("the server side interrupted while waiting for the barrier");
        }
    }

    public void close() {
        // set barrier to null
        logger.debug("closing synchronizer");
        CyclicBarrier barrier = barrierAtomicReference.getAndUpdate(oldValue -> null);
        if (barrier != null) {
            // wake up all threads that wait on barrier.
            barrier.reset();
        }
    }
}

package py.netty.core.twothreads;

import io.netty.util.concurrent.*;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.PlatformDependent;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.NamedThreadFactory;
import py.consumer.ConsumerService;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;

/**
 * This class is similar to py.engine.TaskEngine while this class is more for netty
 */

public abstract class NetworkTaskEngine<E> implements ConsumerService<E> {
    final static Logger logger = LoggerFactory.getLogger(NetworkTaskEngine.class);

    private static final long START_TIME = System.nanoTime();
    private final String engineName;

    static long nanoTime() {
        return System.nanoTime() - START_TIME;
    }

    private static final int ST_NOT_STARTED = 1;
    private static final int ST_STARTED = 2;
    private static final int ST_SHUTTING_DOWN = 3;
    private static final int ST_TERMINATED = 5;

    private List<E> tasks = new ArrayList<>();
    private final ThreadFactory threadFactory;
    //This thread is created by threadFactory and used to poll tasks from the queue and execute them one by one.
    // The thread is blocked on the queue if the queue is empty
    private Thread currentThread;
    private boolean interrupted;
    //    private int maxDrainTo;

    /**
     * The consumer
     */
    private final Consumer<? super E> consumer;

    private static final AtomicIntegerFieldUpdater<NetworkTaskEngine> STATE_UPDATER = AtomicIntegerFieldUpdater
            .newUpdater(NetworkTaskEngine.class, "state");

    private final Semaphore threadLock = new Semaphore(0);
    private long lastExecutionTime;

    @SuppressWarnings({ "FieldMayBeFinal", "unused" })
    private volatile int state = ST_NOT_STARTED;
    private volatile long gracefulShutdownQuietPeriod;
    private volatile long gracefulShutdownTimeout;
    private long gracefulShutdownStartTime;
    private final Promise<?> terminationFuture = new DefaultPromise<Void>(GlobalEventExecutor.INSTANCE);

    public NetworkTaskEngine(Consumer<E> consumer, String engineName) {
        this.engineName = engineName;
        this.consumer = consumer;
        this.threadFactory = new NamedThreadFactory(engineName);
    }

    /**
     * Interrupt the current running {@link Thread}.
     */
    public void interruptThread() {
        Thread currentThread = this.currentThread;
        if (currentThread == null) {
            interrupted = true;
        } else {
            currentThread.interrupt();
        }
    }

    /**
     * Add a task to the task queue, or throws a {@link RejectedExecutionException} if this instance was shutdown
     * before.
     */
    private void addTask(E task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (!offerTask(task)) {
            reject();
        }
    }

    private final boolean offerTask(E task) {
        if (isTerminated()) {
            reject();
        }
        return enqueue(task);
    }

    /**
     * Poll all tasks from the task queue and run them via {@link Runnable#run()} method.
     */
    private boolean runAllTasks() {
        int nNormalTasks = 0;
        int nShutdownTasks = 0;
        int transferredElements = 0;
        tasks.clear();

        try {
            try {
                // block on the queue
                E element = takeElement();
                Validate.notNull(element);
                tasks.add(element);

                // drain as many tasks as we can
                transferredElements = drainElements(tasks);
            } catch (Exception e) {
                logger.warn("fail to take elements from the task queue");
            }

            for (E task : tasks) {
                if (isShutdownTask(task)) {
                    nShutdownTasks ++;
                    logger.warn("exiting {} engine", engineName);
                } else {
                    nNormalTasks++;
                    try {
                        safeExecute(task);
                    } catch (Exception e) {
                        logger.warn("fail to execute {} task", task);
                    }
                }
            }
        } finally {
            afterRunningAllTasks(nNormalTasks);
        }

        logger.debug("{} tasks have been processed. {} shutdown tasks", nNormalTasks, nShutdownTasks);
        Validate.isTrue(nNormalTasks + nShutdownTasks == transferredElements + 1);
        return nNormalTasks > 0;
    }

    /**
     * Invoked before returning from {@link #runAllTasks()}
     */
    public void afterRunningAllTasks(int runTasks) {
    }

    protected void safeExecute(E task) {
        try {
            consumer.accept(task);
        } catch (Throwable t) {
            logger.warn("A task raised an exception. Task: {}", task, t);
        }
    }

    public boolean inEventLoop(Thread thread) {
        return thread == this.currentThread;
    }

    private boolean inEventLoop() {
        return inEventLoop(Thread.currentThread());
    }

    @Override
    public void start() {
        startThread();
    }

    /**
     * Shutdown the engine gracefully.
     * <p>
     * This function is to mark the state of the engine thread to SHUTTING_DOWN and set the quiet period and timeout.
     * <p>
     * The engine will be shutdown when one of the following conditions is met:
     * <p>
     * 1. Enough time has elapsed (timeout occurred no matter if there are tasks in the queue) 2. No tasks are executed
     * within the quietPeriod.
     *
     * @param quietPeriod
     * @param timeout
     * @param unit
     * @return
     */
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        if (quietPeriod < 0) {
            throw new IllegalArgumentException("quietPeriod: " + quietPeriod + " (expected >= 0)");
        }
        if (timeout < quietPeriod) {
            throw new IllegalArgumentException(
                    "timeout: " + timeout + " (expected >= quietPeriod (" + quietPeriod + "))");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }

        logger.debug("in shutdown");
        if (isShuttingDown()) {
            return terminationFuture();
        }

        boolean inEventLoop = inEventLoop();
        boolean commandToExit;
        int oldState;
        logger.debug("to for loop");
        for (; ; ) {
            logger.debug("in the shutdown loop");
            if (isShuttingDown()) {
                return terminationFuture();
            }
            int newState;
            commandToExit = true;
            oldState = state;
            if (inEventLoop) {
                newState = ST_SHUTTING_DOWN;
            } else {
                switch (oldState) {
                case ST_NOT_STARTED:
                case ST_STARTED:
                    newState = ST_SHUTTING_DOWN;
                    break;
                default:
                    newState = oldState;
                    commandToExit = false;
                }
            }
            if (STATE_UPDATER.compareAndSet(this, oldState, newState)) {
                break;
            }
        }
        gracefulShutdownQuietPeriod = unit.toNanos(quietPeriod);
        gracefulShutdownTimeout = unit.toNanos(timeout);


        if (oldState == ST_NOT_STARTED) {
            try {
                doStartThread();
            } catch (Throwable cause) {
                STATE_UPDATER.set(this, ST_TERMINATED);
                terminationFuture.tryFailure(cause);

                if (!(cause instanceof Exception)) {
                    // Also rethrow as it may be an OOME for example
                    PlatformDependent.throwException(cause);
                }
                return terminationFuture;
            }
        }

        if (commandToExit) {
            logger.debug("exiting");
            exit(inEventLoop);
        }

        return terminationFuture();
    }

    private void exit(boolean inEventLoop) {
        if (!inEventLoop || state == ST_SHUTTING_DOWN) {
            // Use offer as we actually only need this to unblock the thread and if offer fails we do not care as there
            // is already something in the queue.
            enqueueShutdownTask();
        }
    }

    public Future<?> terminationFuture() {
        return terminationFuture;
    }

    public boolean isShuttingDown() {
        return state >= ST_SHUTTING_DOWN;
    }

    public boolean isTerminated() {
        return state == ST_TERMINATED;
    }

    /**
     * Confirm that the shutdown if the instance should be done now!
     */
    protected boolean confirmShutdown() {
        if (!isShuttingDown()) {
            return false;
        }

        if (!inEventLoop()) {
            throw new IllegalStateException("must be invoked from an event loop");
        }

        if (gracefulShutdownStartTime == 0) {
            gracefulShutdownStartTime = nanoTime();
        }

        // put a task to the engine to ensure runAllTasks() won't be blocked
        exit(true);
        if (runAllTasks()) {
            if (isTerminated()) {
                // Executor shut down - no new tasks anymore.
                return true;
            }

            // There were tasks in the queue. Wait a little bit more until no tasks are queued for the quiet period or
            // terminate if the quiet period is 0.
            // See https://github.com/netty/netty/issues/4241
            if (gracefulShutdownQuietPeriod == 0) {
                return true;
            }

            logger.debug("We still processed some tasks. We can't terminate the engine yet");
            return false;
        }

        // no tasks have been processed. Let's check grace period and time out can meet the crietial
        final long nanoTime = nanoTime();

        if (isTerminated() || nanoTime - gracefulShutdownStartTime > gracefulShutdownTimeout) {
            return true;
        }

        if (nanoTime - lastExecutionTime <= gracefulShutdownQuietPeriod) {
            logger.debug("We need to wait for a quiet period. wait {} ms", gracefulShutdownQuietPeriod / (1000 * 1000 ));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }

            return false;
        }

        // No tasks were added for last quiet period - hopefully safe to shut down.
        // (Hopefully because we really cannot make a guarantee that there will be no execute() calls by a user.)
        return true;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("unit");
        }

        if (inEventLoop()) {
            throw new IllegalStateException("cannot await termination of the current thread");
        }

        if (threadLock.tryAcquire(timeout, unit)) {
            threadLock.release();
        }

        return isTerminated();
    }

    @Override
    public boolean submit(E task) {
        if (isTerminated()) {
            reject();
        }

        boolean inEventLoop = inEventLoop();
        if (inEventLoop) {
            return enqueue(task);
        } else {
            startThread();
            return enqueue(task);
        }
    }

    @Override
    public void stop() {
        shutdownGracefully(1, 1, TimeUnit.SECONDS);
    }

    protected static void reject() {
        throw new RejectedExecutionException("event executor terminated");
    }

    private void startThread() {
        if (state == ST_NOT_STARTED) {
            if (STATE_UPDATER.compareAndSet(this, ST_NOT_STARTED, ST_STARTED)) {
                try {
                    doStartThread();
                } catch (Throwable cause) {
                    STATE_UPDATER.set(this, ST_NOT_STARTED);
                    PlatformDependent.throwException(cause);
                }
            }
        }
    }

    /**
     * Updates the internal timestamp that tells when a submitted task was executed most recently. {@link
     * #runAllTasks()} updates this timestamp automatically, and thus there's usually no need to call this method.
     */
    protected void updateLastExecutionTime() {
        lastExecutionTime = nanoTime();
    }

    private void doStartThread() {
        assert currentThread == null;
        currentThread = threadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                if (interrupted) {
                    currentThread.interrupt();
                }

                boolean success = false;
                updateLastExecutionTime();
                try {
                    infinitelyExecuteJobs();
                    success = true;
                } catch (Throwable t) {
                    logger.warn("Unexpected exception from an event executor: ", t);
                    logger.warn("Event loop {} is going to shutdown", this);
                } finally {
                    for (; ; ) {
                        int oldState = state;
                        if (oldState >= ST_SHUTTING_DOWN || STATE_UPDATER
                                .compareAndSet(NetworkTaskEngine.this, oldState, ST_SHUTTING_DOWN)) {
                            break;
                        }
                    }

                    // Check if confirmShutdown() was called at the end of the loop.
                    if (success && gracefulShutdownStartTime == 0) {
                        logger.error("Buggy " + EventExecutor.class.getSimpleName() + " implementation; "
                                + NetworkTaskEngine.class.getSimpleName() + ".confirmShutdown() must be called "
                                + "before run() implementation terminates.");
                    }

                    try {
                        // Run all remaining tasks and shutdown hooks.
                        for (; ; ) {
                            if (confirmShutdown()) {
                                logger.warn("confirming shutdown.");
                                break;
                            }
                        }
                    } finally {
                        try {
                            cleanup();
                        } finally {
                            STATE_UPDATER.set(NetworkTaskEngine.this, ST_TERMINATED);
                            threadLock.release();
                            int size = size();
                            if (size > 0) {
                                logger.warn(
                                        "An event executor terminated with " + "non-empty task queue (" + size + ')');
                            }

                            terminationFuture.setSuccess(null);
                            logger.warn("task engine has been shutdown.");
                        }
                    }
                }
            }
        });
        currentThread.setName(engineName);
        logger.debug("ready to start {} engine", engineName);
        currentThread.start();
    }

    private void infinitelyExecuteJobs() {
        boolean ranAtLeaseOnce;
        while (true) {
            ranAtLeaseOnce = runAllTasks();
            if (ranAtLeaseOnce) {
                updateLastExecutionTime();
            }

            // Always handle shutdown even if the loop processing threw an exception.
            try {
                if (isShuttingDown()) {
                    confirmShutdown();
                    break;
                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
        }
    }

    private void handleLoopException(Throwable t) {
        logger.warn("Unexpected exception in the selector loop.", t);

        // Prevent possible consecutive immediate failures that lead to
        // excessive CPU consumption.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore.
        }
    }

    // do nothing. It is up to my subclass to implement clean-up behaviors
    protected void cleanup() {
    }

    protected abstract int size();

    protected abstract boolean enqueue(E element);

    protected abstract void enqueueShutdownTask();

    protected abstract boolean isShutdownTask(E e);

    protected abstract int drainElements(Collection<E> container);

    /**
     * take element. blocking
     */
    protected abstract E takeElement() throws Exception;
}


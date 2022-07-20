package py.periodic.impl;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

import py.common.NamedThreadFactory;
import py.periodic.PeriodicWorkExecutor;
import py.periodic.UnableToStartException;
import py.periodic.Worker;
import py.periodic.WorkerFactory;

/**
 * One implementation of PeriodicWorkExecutor interface.
 * 
 * @author chenlia
 */
public class PeriodicWorkExecutorImpl implements PeriodicWorkExecutor {
    private static final Logger logger = Logger.getLogger(PeriodicWorkExecutorImpl.class);

    private WorkerFactory workerFactory;
    private ExecutionOptionsReader executionOptionsReader;
    private ScheduledThreadPoolExecutor executor;

    // The thread factory to create a new thread
    private ThreadFactory periodicThreadFactory;

    // If a thread throws an exception which is not caught, an error log will be written
    private UncaughtExceptionHandler uncaughtExceptionHandler = new UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            logger.error("Failure in thread " + t.getName(), e);
        }
    };

    public PeriodicWorkExecutorImpl(ExecutionOptionsReader optionReader, WorkerFactory factory) {
        this(optionReader, factory, "Periodic Worker");
    }

    public PeriodicWorkExecutorImpl(ExecutionOptionsReader optionReader, WorkerFactory factory, String workerThreadName) {
        workerFactory = factory;
        executionOptionsReader = optionReader;

        periodicThreadFactory = new NamedThreadFactory(workerThreadName) {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = super.newThread(r);
                t.setUncaughtExceptionHandler(uncaughtExceptionHandler);
                return t;
            }
        };
    }

    public PeriodicWorkExecutorImpl() {
        this(null, null);
    }

    @Override
	public void setWorkerFactory(WorkerFactory workerFactory) {
        this.workerFactory = workerFactory;
    }

    /**
     * Set the option reader to read execution options
     */
    public void setExecutionOptionsReader(ExecutionOptionsReader reader) {
        this.executionOptionsReader = reader;
    }

    @Override
	public void start() throws UnableToStartException {
        logger.trace("Periodic Work is kicked off");
        if (workerFactory == null || executionOptionsReader == null) {
            String errMsg = "workerFactory and executionOptionReader must be set before start workers";
            logger.error(errMsg);
            throw new UnableToStartException(errMsg);
        }

        ExecutionOptions executionOptions;
        try {
            executionOptions = executionOptionsReader.read();
        } catch (InvalidExecutionOptionsException e) {
            logger.error(e.getMessage());
            throw new UnableToStartException(e.getMessage(), e);
        }

        executor = new ScheduledThreadPoolExecutor(executionOptions.getMaxNumWorkers(), periodicThreadFactory);
        for (int i = 0; i < executionOptions.getNumWorkers(); i++) {
            if (executionOptions.getFixedDelay() != null) {
                // periodically execute worker's task with any delay.
                // Note that executionOptions.getFixedDelay could be 0, it is fine for the following statement
                executor.scheduleWithFixedDelay(new WorkerTask(workerFactory.createWorker()), 0,
                        executionOptions.getFixedDelay(), TimeUnit.MILLISECONDS);
            } else {
                executor.scheduleAtFixedRate(new WorkerTask(workerFactory.createWorker()), 0,
                        executionOptions.getFixedRate(), TimeUnit.MILLISECONDS);
            }
        }
    };

    @Override
	public void stop() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor == null ? true : executor.awaitTermination(timeout, unit);
    }

    @Override
	public void stopNow() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * @return the max number of workers that could concurrently run
     */
    public int getMaxWorkersCount() {
        return executor.getMaximumPoolSize();
    }

    /**
     * @return the number of workers currently working
     */
    public int getActiveWorkersCount() {
        return executor.getActiveCount();
    }

    private class WorkerTask implements Runnable {
        Worker worker;

        WorkerTask(Worker worker) {
            this.worker = worker;
        }

        @Override
        public void run() {
            try {
                if (executor.isShutdown()) {
                    // thread pool has shutdown, do not do anything and return
                    return;
                }
                worker.doWork();
            } catch (Exception e) {
                String errMsg = "An exception " + e.getClass().getName() + " thrown by the thread "
                        + Thread.currentThread().getName() + " is caught. The worker will be re-submitted.";
                logger.error(errMsg, e);
                // Since we have caught the exception, the Executor will reschedule this runnable again automatically
            }
        }
    }
}

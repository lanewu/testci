package py.periodic;

import java.util.concurrent.TimeUnit;

/**
 * PeriodicWorkExecutor concurrently and periodically runs multiple workers.
 * 
 * 
 * It is required to set a WorkerFactory used to generate workers. Once the factory is 
 * start() method can start to run workers.
 * 
 *  @author chenlia
 */
public interface PeriodicWorkExecutor {

    /**
     * Set the worker factory used to spawn new workers
     */
    public void setWorkerFactory(WorkerFactory workerFactory);

    /**
     * Start to run workers. 
     */
    public void start() throws UnableToStartException;
    
    /**
     * Stop all workers gracefully. The executor will wait until workers complete their work
     * before it shutdowns them down
     */
    public void stop();
    
    /**
     * Stop all workers abruptly. The executor will try to interrupt what workers are done and
     * shut everything down.  
     */
    public void stopNow();
    
    /**
     * Wait until all workers are terminated
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
}

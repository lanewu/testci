package py.periodic;


/**
 * A worker is an abstract of a task that does a unit of work.
 * 
 * The implementation of this interface needs to use neither a loop to 
 * repeat work nor a thread to make it concurrent. Instead, PeriodicWorkExecutor 
 * runs the work within a thread and infinitely repeats the work.
 * 
 * 
 * @author chenlia
 */
public interface Worker {
    /**
     * Do a unit of work. If it succeeds, returns nothing, otherwise
     * throw an exception.
     * 
     * doWork() will be executed within one thread. In other words, 
     * there are no two threads concurrently access doWork. 
     * Therefore, there is no need to consider to synchronize this function.  
     * 
     * @throws Exception
     */
    void doWork() throws Exception;
}

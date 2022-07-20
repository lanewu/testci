package py.periodic.impl;

/**
 * A place holder for execution options. This is an immutable class.
 * 
 * @author chenlia
 */
public final class ExecutionOptions {
    // The maximum number of workers
    private final int maxNumWorkers;
    
    // The number of active workers
    private final int numWorkers;
    
    /**
     *  Workers will be executed in a fixed frequency specified by fixedRate in milliseconds. 
     *  
     *  For instance, if fixedRate is 10, then workers will be executed every 10 milliseconds.
     *  If it takes a worker 11 seconds to complete its work, then the worker will be executed
     *  immediately after the previous run.
     *  
     *  Note fixedRate must be positive or null, otherwise InvalidExecutionOptionsException will be thrown   
     */
    private final Integer fixedRate;
    
    /**
     * Workers will be executed with the given delay after the termination of one execution. 
     * fixedDelay specifies this delay in milliseconds.    
     *    
     * For instance, if fixedDelay is 10, then workers will be executed 10 milliseconds later after 
     * the termination of the previous run.
     *  
     * Note fixedDelay must be positive or null, otherwise InvalidExecutionOptionsException will be thrown  
     */
    private final Integer fixedDelay;
    
    public ExecutionOptions(
            int maxNumWorkers, 
            int numWorkers, 
            Integer fixedRate, 
            Integer fixedDelay) 
    throws InvalidExecutionOptionsException
    {
        if (maxNumWorkers <= 0 || numWorkers <= 0 || maxNumWorkers < numWorkers) 
        {
            throw new InvalidExecutionOptionsException("maxNumWorkers and numWorkers must be positive and " +
            "maxNumWorkers may not be less than numWorkers");
        }
        
        if (!((fixedRate != null && fixedDelay == null) || 
                (fixedRate == null && fixedDelay != null)))
        {
            throw new InvalidExecutionOptionsException("Either fixedRate or fixedDelay must be set");
        }
        
        if (fixedRate != null && fixedRate < 0)
        {
            throw new InvalidExecutionOptionsException("fixedRate must be postive");
        }
        
        if (fixedDelay != null && fixedDelay < 0)
        {
            throw new InvalidExecutionOptionsException("fixedDelay must be postive");
        }
        
        this.maxNumWorkers = maxNumWorkers;
        this.numWorkers = numWorkers;
        this.fixedRate = fixedRate;
        this.fixedDelay = fixedDelay;
    }
    
    public int getMaxNumWorkers()
    {
        return maxNumWorkers;
    }

    public int getNumWorkers()
    {
        return numWorkers;
    }

    public Integer getFixedRate()
    {
        return fixedRate;
    }
    
    public Integer getFixedDelay()
    {
        return fixedDelay;
    }
}

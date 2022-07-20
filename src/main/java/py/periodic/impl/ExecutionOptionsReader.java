package py.periodic.impl;


/**
 * A factory class that is able to retrieve execution options
 * 
 * @author chenlia
 */
public class ExecutionOptionsReader {
    private int maxNumWorkers;
    private int numWorkers;
    private Integer fixedRate;
    private Integer fixedDelay;

    public ExecutionOptionsReader(int maxNumWorkers, 
            int numWorkers, 
            Integer fixedRate, 
            Integer fixedDelay) {
        this.maxNumWorkers = maxNumWorkers;
        this.numWorkers = numWorkers;
        this.fixedRate = fixedRate;
        this.fixedDelay = fixedDelay;
    }
    
    public ExecutionOptions read() throws InvalidExecutionOptionsException
    {
        return new ExecutionOptions(maxNumWorkers, numWorkers, fixedRate, fixedDelay);
    }


    public void setMaxNumWorkers(int maxNumWorkers)
    {
        this.maxNumWorkers = maxNumWorkers;
    }

    public void setNumWorkers(int numWorkers)
    {
        this.numWorkers = numWorkers;
    }

    public void setFixedRate(Integer fixedRate)
    {
        this.fixedRate = fixedRate;
    }

    public void setFixedDelay(Integer fixedDelay)
    {
        this.fixedDelay = fixedDelay;
    }
}

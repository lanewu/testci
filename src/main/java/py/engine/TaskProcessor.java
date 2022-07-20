package py.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Created by zhongyuan on 17-5-23.
 */
public class TaskProcessor implements Callable<Result> {
    private final static Logger logger = LoggerFactory.getLogger(TaskProcessor.class);
    private Task task;

    TaskProcessor(Task task) {
        this.task = task;
    }

    @Override
    public Result call() throws Exception {
        try {
            this.task.doWork();
        } catch (Throwable t) {
            logger.error("caught an exception", t);
        }
        return null;
    }
}

package py.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 
 * @author lx
 *
 */
public class StopTask extends DelayedTask {
    private static final Logger logger = LoggerFactory.getLogger(StopTask.class);

    public StopTask() {
        super(0);
    }

    @Override
    public void doWork() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCancel() {
        return true;
    }

    @Override
    public void setToken(int expectedToken) {
    }

    @Override
    public int getToken() {
        return 1;
    }

    @Override
    public Result work() {
        logger.warn("this is a stop task");
        return null;
    }
}

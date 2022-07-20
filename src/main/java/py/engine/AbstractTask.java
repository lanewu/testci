package py.engine;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author lx
 *
 */
public abstract class AbstractTask implements Task {
    private final static Logger logger = LoggerFactory.getLogger(AbstractTask.class);
    public final static TaskListener DEFAULTLISTENER = new DefaultListener();
    private final TaskListener taskListener;

    private volatile boolean isCancelled = false;
    private int token = 1;

    public AbstractTask() {
        this(DEFAULTLISTENER);
    }

    public AbstractTask(TaskListener resultListener) {
        this.taskListener = resultListener;
    }

    public TaskListener getTaskListener() {
        return taskListener;
    }

    public abstract Result work();

    public void doWork() {
        try {
            taskListener.response(work());
        } catch (Exception e) {
            taskListener.response(new ResultImpl(e));
        }
    }

    public void cancel() {
        logger.warn("cancel the task={}", this);
        isCancelled = true;
    }

    public boolean isCancel() {
        return isCancelled;
    }

    public int getToken() {
        return token;
    }

    public void setToken(int token) {
        this.token = token;
    }

    @Override
    public void destroy() {
    }

    @Override
    public long getDelay(TimeUnit unit) {
        throw new NotImplementedException("");
    }

    @Override
    public int compareTo(Delayed o) {
        throw new NotImplementedException("");
    }

    private static class DefaultListener implements TaskListener {
        @Override
        public void response(Result result) {
            if (result != null && result.cause() != null) {
                logger.error("default listener get a cause", result.cause());
            }
        }
    }
}

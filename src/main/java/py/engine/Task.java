package py.engine;

import java.util.Comparator;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * It is an interface class, which is used in {@link TaskEngine}.
 * <p>
 * if you want to implements the compareTo method of {@link Task}, you should implement a object of {@link Comparator}
 * instead, because the compareTo method has been implements for the interface {@link Delayed}.
 *
 * @author lx
 */
public interface Task extends Delayed {
    /**
     * Call this method to release the resource for this task if you don't need do this task.
     */
    public void destroy();

    /**
     * The method will be called when task engine schedules to this task.
     */
    public void doWork();

    /**
     * Call this method to cancel this task, it only works when the task was not scheduled.
     */
    public void cancel();

    /**
     * Check if the task has been cancelled.
     *
     * @return
     */
    public boolean isCancel();

    /**
     * For improving the performance, you can get more than one token to do more thing.
     *
     * @param token
     */
    public void setToken(int token);

    public int getToken();

    public long getDelay(TimeUnit unit);

    public int compareTo(Delayed o);
}

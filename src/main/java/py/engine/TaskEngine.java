package py.engine;

import java.util.concurrent.TimeUnit;

import py.token.controller.TokenController;

/**
 * 
 * @author lx
 *
 */
public interface TaskEngine {

    /**
     * Start the engine, and then you can submit the task or control the speed
     */
    public void start();

    /**
     * It will submit a {@link Task} to the engine and return immediately, if submitting successfully, it will be
     * executed automatically.
     * 
     * @param task
     * @return if the {@link Task} has been submitted successfully, it will return True, otherwise it will return false
     */
    public boolean drive(Task task);

    /**
     * It will submit a {@link Task} to the engine and wait for the timeout to submit {@link Task}.
     * 
     * @param task
     * @param timeout
     * @param timeUnit
     * @return
     */
    public boolean drive(Task task, int timeout, TimeUnit timeUnit);

    /**
     * Stop the engine, it will refuse new {@link Task}.
     */
    public void stop();

    /**
     * @return the size of queue
     */
    public int getPendingTask();

    /**
     * 
     * @return which will be used to control the speed of doing task.
     */
    public TokenController getTokenController();

    public void setTokenController(TokenController ioController);
}

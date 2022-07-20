package py.token.controller;

import java.util.concurrent.TimeUnit;

/**
 * It is used for controlling the speed of doing some work and must be used with {@link TokenControllerCenter}. After
 * creating an instance of {@link TokenController}, you must register it to {@link TokenControllerCenter}, then the
 * Controller will work fine. If you no longer use it, you can deregister it.
 * 
 * @author lx
 *
 */
public interface TokenController {
    public long getId();

    /**
     * It is called by {@link TokenControllerCenter} to reset the number of token.
     */
    public void reset();

    public boolean acquireToken(long timeout, TimeUnit timeUnit);

    public boolean acquireToken(int tokenCount, long timeout, TimeUnit timeUnit);

    public boolean acquireToken();

    public boolean acquireToken(int tokenCount);

    /**
     * Trying to get token of specified number, and you at least get a token when it works fine, maybe nothing.
     * 
     * @param tokenCount
     *            the token you get
     * @return
     */
    public int tryAcquireToken(int tokenCount);

    public void updateToken(int bucketCapacity);
}

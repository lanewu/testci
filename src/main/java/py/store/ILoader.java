package py.store;

import py.monitor.exception.EmptyStoreException;

/**
 * 
 * @author shixulu
 *
 * @param <TPath>
 *            the path may be {@code java.lang.String} or any other classes.
 */
public interface ILoader<TPath> {
    public void from(TPath path) throws EmptyStoreException, Exception;
}

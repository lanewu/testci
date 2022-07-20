package py.netty.core.twothreads;

import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ThrowableUtil;
import org.slf4j.Logger;

public final class PromiseLoggingUtil {

    /**
     * Internal utilities to notify {@link Promise}s.
     */

    private PromiseLoggingUtil() {
    }

    /**
     * Try to cancel the {@link Promise} and log if {@code logger} is not {@code null} in case this fails.
     */
    public static void tryCancel(Promise<?> p, Logger logger) {
        if (!p.cancel(false) && logger != null) {
            Throwable err = p.cause();
            if (err == null) {
                logger.warn("Failed to cancel promise because it has succeeded already: {}", p);
            } else {
                logger.warn("Failed to cancel promise because it has failed already: {}, unnotified cause:", p, err);
            }
        }
    }

    /**
     * Try to mark the {@link Promise} as success and log if {@code logger} is not {@code null} in case this fails.
     */
    public static <V> void trySuccess(Promise<? super V> p, V result, Logger logger) {
        if (!p.trySuccess(result) && logger != null) {
            Throwable err = p.cause();
            if (err == null) {
                logger.warn("Failed to mark a promise as success because it has succeeded already: {}", p);
            } else {
                logger.warn("Failed to mark a promise as success because it has failed already: {}, unnotified cause:",
                        p, err);
            }
        }
    }

    /**
     * Try to mark the {@link Promise} as failure and log if {@code logger} is not {@code null} in case this fails.
     */
    public static void tryFailure(Promise<?> p, Throwable cause, Logger logger) {
        if (!p.tryFailure(cause) && logger != null) {
            Throwable err = p.cause();
            if (err == null) {
                logger.warn("Failed to mark a promise as failure because it has succeeded already: {}", p, cause);
            } else {
                logger.warn(
                        "Failed to mark a promise as failure because it has failed already: {}, unnotified cause: {}",
                        p, ThrowableUtil.stackTraceToString(err), cause);
            }
        }
    }

}


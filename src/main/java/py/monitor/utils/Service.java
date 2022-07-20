package py.monitor.utils;

import static py.monitor.utils.Service.State.NEW;
import static py.monitor.utils.Service.State.PAUSED;
import static py.monitor.utils.Service.State.RUNNING;
import static py.monitor.utils.Service.State.STARTING;
import static py.monitor.utils.Service.State.STOPPED;
import static py.monitor.utils.Service.State.FAILED;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author sxl
 * 
 */
public abstract class Service {
    private static final Logger logger = LoggerFactory.getLogger(Service.class);

    public static enum State {
        NEW {
            @Override
            protected Set<State> getAllowed() {
                return EnumSet.of(STARTING, RUNNING, STOPPED, FAILED, TERMINATED);
            }
        },
        STARTING {
            @Override
            protected Set<State> getAllowed() {
                return EnumSet.of(RUNNING, STOPPED, FAILED, TERMINATED);
            }
        },
        RUNNING {
            @Override
            protected Set<State> getAllowed() {
                return EnumSet.of(PAUSED, STOPPED, FAILED, TERMINATED);
            }
        },
        PAUSED {
            @Override
            protected Set<State> getAllowed() {
                return EnumSet.of(RUNNING, STOPPED, FAILED, TERMINATED);
            }
        },
        STOPPED {
            @Override
            protected Set<State> getAllowed() {
                return EnumSet.of(RUNNING, FAILED, TERMINATED);
            }
        },
        FAILED {
            @Override
            protected Set<State> getAllowed() {
                return EnumSet.of(TERMINATED);
            }
        },
        TERMINATED {
            @Override
            protected Set<State> getAllowed() {
                return EnumSet.noneOf(State.class);
            }
        };

        protected abstract Set<State> getAllowed();

        public State next(State next) {
            Set<State> allowed = getAllowed();
            if (!allowed.contains(next)) {
                logger.error("Unable to change state from {} to {} (allowed: {})", this, next, allowed);
                throw new IllegalArgumentException();
            }
            return next;
        }
    }

    private final AtomicReference<State> state = new AtomicReference<State>(NEW);

    public String getState() {
        return state.toString();
    }

    /**
     * Transition NEW -> STARTING
     */
    final public void start() throws Exception {
        if(!isJmxAgentSwitcherOn()) {
            logger.warn("JmxAgentSwitcher is off");
            return;
        }

        if (!(state.compareAndSet(NEW, STARTING) || state.compareAndSet(STOPPED, STARTING))) {
            logger.error("Unable to start service with state {}", state);
            throw new IllegalStateException();
        }

        try {
            logger.debug("Current service status: {}", state);
            onStart();
        } catch (Exception e) {
            logger.error("Caught an exception", e);
            if (!(state.compareAndSet(STARTING, FAILED))) {
                throw new IllegalStateException(
                        "Fail to set service status to FAILED when failed to start the service");
            }
            throw e;
        }

        if (!state.compareAndSet(STARTING, RUNNING)) {
            logger.error("Service has been started success, but unable to set service state to {}", state);
            throw new IllegalStateException();
        }
    }

    final public void run() throws Exception {
        // state checking not atomic
        switch (state.get()) {
        case STARTING:
        case PAUSED:
        case STOPPED:
        case FAILED:
            state.set(RUNNING);
            break;
        default:
            throw new IllegalStateException("Unable to run service with state " + state);
        }
    }

    final public void pause() throws Exception {
        if (!state.compareAndSet(RUNNING, PAUSED)) {
            logger.error("Unable to start service with state {}", state);
            throw new IllegalStateException();
        }
        onPause();
    }

    final public void resume() throws Exception {
        if (!state.compareAndSet(PAUSED, RUNNING)) {
            logger.error("Unable to start service with state {}", state);
            throw new IllegalStateException();
        }
        onResume();
    }

    final public void stop() throws Exception {
        // non atomic state change
        if (!(state.compareAndSet(PAUSED, STOPPED) || state.compareAndSet(RUNNING, STOPPED))) {
            logger.error("Unable to stop service with state {}", state);
            throw new IllegalStateException();
        }
        onStop();
    }

    final public void terminate() throws Exception {
        if (state.get() == State.TERMINATED) {
            logger.error("Unable to start service with state {}", state);
            throw new IllegalStateException();
        }
        state.set(State.TERMINATED);
    }

    protected boolean isJmxAgentSwitcherOn() throws Exception {
        return true;
    }
    /**
     * Override to implement actions to be taken to start a service.
     */
    protected abstract void onStart() throws Exception;

    protected abstract void onStop() throws Exception;

    protected abstract void onPause() throws Exception;

    protected abstract void onResume() throws Exception;
}

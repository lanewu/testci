package py.token.controller;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.periodic.UnableToStartException;
import py.periodic.Worker;
import py.periodic.WorkerFactory;
import py.periodic.impl.ExecutionOptionsReader;
import py.periodic.impl.PeriodicWorkExecutorImpl;

/**
 * The class is used for managing all of {@link TokenController}.
 * 
 * @author lx
 *
 */
public class TokenControllerCenter {
    private final static Logger logger = LoggerFactory.getLogger(TokenControllerCenter.class);

    private final static TokenControllerCenter center = new TokenControllerCenter();
    private final static int FIXED_DELAY_MS = 1000;
    private final Map<Long, TokenController> mapIdToIOController;
    private PeriodicWorkExecutorImpl executor;
    private int maxWorkerCount;
    private int workerCount;

    private TokenControllerCenter() {
        this.mapIdToIOController = new ConcurrentHashMap<Long, TokenController>();
        this.maxWorkerCount = 1;
        this.workerCount = 1;
    }

    public static TokenControllerCenter getInstance() {
        return center;
    }

    public void setMaxWorkerCount(int maxWorkerCount) {
        this.maxWorkerCount = maxWorkerCount;
    }

    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    public void start() throws UnableToStartException {
        Validate.isTrue(executor == null);
        ExecutionOptionsReader reader = new ExecutionOptionsReader(maxWorkerCount, workerCount, FIXED_DELAY_MS, null);
        executor = new PeriodicWorkExecutorImpl(reader, new ControlIOWorkerFactory(), "io-controller");
        executor.start();

    }

    public void register(TokenController controller) {
        logger.info("register controller: {}", controller);
        mapIdToIOController.put(controller.getId(), controller);
    }

    public void deregister(TokenController controller) {
        logger.info("unregister controller: {}", controller);
        mapIdToIOController.remove(controller.getId());
    }

    public void stop() {
        if (executor != null) {
            executor.stop();
            executor = null;
        }
    }

    private class ControlIOWorkerFactory implements WorkerFactory {

        @Override
        public Worker createWorker() {
            return new Worker() {
                @Override
                public void doWork() throws Exception {
                    for (Entry<Long, TokenController> entry : mapIdToIOController.entrySet()) {
                        TokenController controller = entry.getValue();
                        try {
                            controller.reset();
                            logger.trace("reset controller: {}", controller);
                        } catch (Throwable t) {
                            logger.error("can not reset controller: {}", controller);
                        }
                    }
                }
            };
        }

    }

    public PeriodicWorkExecutorImpl getExecutor() {
        return executor;
    }
}

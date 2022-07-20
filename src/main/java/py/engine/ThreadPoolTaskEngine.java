package py.engine;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.NamedThreadFactory;
import py.token.controller.TokenControllerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by zhongyuan on 17-5-23.
 */
public class ThreadPoolTaskEngine extends AbstractTaskEngine {
    private final static Logger logger = LoggerFactory.getLogger(ThreadPoolTaskEngine.class);
    private final BlockingQueue<Task> queue;
    private Thread pullerThread;
    private final ThreadPoolExecutor workerPool;

    public ThreadPoolTaskEngine(int corePoolSize, int maxPoolSize) {
        this(corePoolSize, maxPoolSize, false);
    }

    public ThreadPoolTaskEngine(int corePoolSize, int maxPoolSize, boolean allowCoreThreadTimeOut) {
        queue = new LinkedBlockingQueue<>();
        workerPool = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60, TimeUnit.SECONDS, new SynchronousQueue<>(),
                new NamedThreadFactory("worker-pool"));
        workerPool.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
        this.tokenController = TokenControllerUtils.generateBogusController();
    }

    @Override
    public void start() {
        pullerThread = new Thread(prefix + "puller-Thread") {
            public void run() {
                try {
                    pullWork();
                } catch (Throwable t) {
                    logger.warn("caught an exception", t);
                }
            }
        };

        logger.info("start puller thread:{}", pullerThread.getName());
        pullerThread.start();

    }

    private void pullWork() {
        List<Task> tasks = new ArrayList<Task>();
        while (true) {
            tasks.clear();
            if (0 == queue.drainTo(tasks, maxDrainTo)) {
                try {
                    tasks.add(queue.take());
                } catch (Exception e) {
                    logger.warn("fail to task from queue", e);
                }
            }

            for (Task task : tasks) {
                if (task instanceof StopTask) {
                    logger.warn("exit {} queue", prefix);
                    return;
                }

                if (task.isCancel()) {
                    logger.warn("task: {} has been cancelled", task);
                    continue;
                }

                // control the speed of doing working
                // int token = getTokenController().tryAcquireToken(task.getToken());
                // if (token == 0) {
                //    logger.warn("fail to control {} engine, just going on, expect token: {}", prefix, task.getToken());
                // }
                // task.setToken(token);

                boolean needPutBack = false;
                try {
                    TaskProcessor taskProcessor = new TaskProcessor(task);
                    workerPool.execute(new FutureTask<>(taskProcessor));
                } catch (RejectedExecutionException re) {
                    needPutBack = true;
                    logger.info("Because of RejectedExecutionException, Can't submit a task to work threads");
                } catch (Exception e) {
                    logger.error("submit task, caught an exception", e);
                    needPutBack = true;
                } finally {
                    if (needPutBack) {
                        drive(task);
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
        try {
            queue.put(new StopTask());
            Validate.notNull(pullerThread);
            pullerThread.join();
            workerPool.shutdown();
            workerPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (Throwable t) {
            logger.warn("caught an exception", t);
        }
    }

    @Override
    public boolean drive(Task task) {
        return queue.offer(task);
    }

    @Override
    public boolean drive(Task task, int timeout, TimeUnit timeUnit) {
        try {
            return queue.offer(task, timeout, timeUnit);
        } catch (InterruptedException e) {
            logger.error("caught an exception", e);
            return false;
        }
    }

    @Override
    public int getPendingTask() {
        return queue.size();
    }
}

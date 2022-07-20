package py.common;

import com.lmax.disruptor.WorkHandler;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.struct.LimitQueue;
import py.common.struct.PYRingBuffer;
import py.engine.TaskEngine;
import py.metrics.PYTimerContext;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zhongyuan on 17-6-5.
 */
public class LoggerWorkHandler implements WorkHandler<LoggerEvent> {
    private final static Logger logger = LoggerFactory.getLogger(LoggerWorkHandler.class);
    private Map<Long, StartTimeAndLogs> traceRecordMap;
    private TaskEngine delayEngine;
    private AtomicInteger allMarkCount;
    private final AtomicBoolean enableLoggerTracer;

    public LoggerWorkHandler(TaskEngine delayEngine, Map<Long, StartTimeAndLogs> traceRecordMap,
            AtomicBoolean enableLoggerTracer) {
        this.enableLoggerTracer = enableLoggerTracer;
        this.traceRecordMap = traceRecordMap;
        this.delayEngine = delayEngine;
        this.allMarkCount = new AtomicInteger(0);
    }

    @Override
    public void onEvent(LoggerEvent loggerEvent) throws Exception {
        try {
            Validate.notNull(loggerEvent);
            loggerEvent.stopFromPublishToHandle();
            if (loggerEvent.isMark()) {
                if (!enableLoggerTracer.get()) {
                    return;
                }
                PYTimerContext pyTimerContext = null;
                try {
                    pyTimerContext = LoggerTracer.timerHandlerProcess.time();
                    StartTimeAndLogs startTimeAndLogs;
                    if (!traceRecordMap.containsKey(loggerEvent.getTraceId())) {
                        StartTimeAndLogs preValue = traceRecordMap.putIfAbsent(loggerEvent.getTraceId(),
                                new StartTimeAndLogs(System.currentTimeMillis()));
                        if (preValue == null) {
                            CleanZombieLogTask cleanZombieLogTask = new CleanZombieLogTask(
                                    LoggerTracer.CLEAN_ZOMBIE_TRACE_LOG_DELAY_TIME_MS, loggerEvent.getTraceId());
                            delayEngine.drive(cleanZombieLogTask);
                        }
                    }
                    startTimeAndLogs = traceRecordMap.get(loggerEvent.getTraceId());
                    if (startTimeAndLogs == null) {
                        // means object has been removed
                        return;
                    }
                    boolean offer = startTimeAndLogs.offerLog(loggerEvent.formatLogger());
                    if (!offer && !startTimeAndLogs.isFull()) {
                        logger.info("please look at the way U use tracer, still has:{} request not done",
                                traceRecordMap.keySet().size());
                    }
                } finally {
                    if (pyTimerContext != null) {
                        pyTimerContext.stop();
                    }
                }
            } else if (loggerEvent.isDoneTrace()) {
                PYTimerContext pyTimerContext = null;
                try {
                    pyTimerContext = LoggerTracer.timerHandlerProcessDoneTrace.time();
                    int timeoutMs = loggerEvent.getTimeoutMs();
                    // determine if need to print all mark points by time out(MS)
                    StartTimeAndLogs startTimeAndLogs = traceRecordMap.remove(loggerEvent.getTraceId());
                    if (startTimeAndLogs != null) {
                        long costTime = loggerEvent.getEndTime() - startTimeAndLogs.getStartTime();
                        if (costTime > timeoutMs) {
                            startTimeAndLogs.printLogs();
                            logger.warn("ori:{} {} cost time:{}ms longer than timeout:{}", loggerEvent.getTraceId(),
                                    loggerEvent.getTraceAction(), costTime, timeoutMs);
                        }

                        int markCount = startTimeAndLogs.size();
                        // dec size of mark point count
                        allMarkCount.addAndGet(-markCount);
                        startTimeAndLogs.release();
                    }
                } finally {
                    if (pyTimerContext != null) {
                        pyTimerContext.stop();
                    }
                }
            } else {
                throw new IllegalArgumentException("can not show up:" + loggerEvent);
            }
        } catch (Throwable t) {
            logger.error("logger event handler caught an exception", t);
        } finally {
            loggerEvent.release();
        }
    }

    class StartTimeAndLogs {
        private final Long startTime;
        private PYRingBuffer<String> logs;

        public StartTimeAndLogs(Long startTime) {
            this.startTime = startTime;
            this.logs = new PYRingBuffer(LoggerTracer.ONE_OBJECT_MAX_MARK_NUMBER);
        }

        public boolean offerLog(String log) {
            long currentMarkCount = allMarkCount.get();
            if (++currentMarkCount > LoggerTracer.MAX_MARK_COUNT) {
                return false;
            }

            boolean isFull = logs.isFull();
            logs.offer(log);
            if (!isFull) {
                allMarkCount.incrementAndGet();
                return true;
            }
            return false;
        }

        public void printLogs() {
            try {
                logger.warn("{}", logs.buildString());
            } catch (Exception e) {
                logger.error("caught an exception while print logs:{}", logs.size(), e);
            }
        }

        public int size() {
            return logs.size();
        }

        public Long getStartTime() {
            return startTime;
        }

        public boolean isFull() {
            return logs.isFull();
        }

        private void release() {
            logs.release();
            logs = null;
        }
    }
}

package py.common;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import py.common.log.info.carrier.*;
import py.engine.DelayedTaskEngine;
import py.engine.TaskEngine;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYTimerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zhongyuan on 17-3-30.
 */
public class LoggerTracer {
    private static final Logger logger = LoggerFactory.getLogger(LoggerTracer.class);
    // change to public for unit test
    public static final int ONE_OBJECT_MAX_MARK_NUMBER = 100;
    public static int MAX_MARK_COUNT = 12800 * ONE_OBJECT_MAX_MARK_NUMBER;

    public static int CLEAN_ZOMBIE_TRACE_LOG_DELAY_TIME_MS = 35000;

    public static int DONE_TRACE_DELAY_TIME_MS = 1000;

    // <objectId, StartTimeAndLogs>
    private Map<Long, LoggerWorkHandler.StartTimeAndLogs> traceRecordMap;

    private static final AtomicBoolean enableLoggerTracer = new AtomicBoolean(false);

    private static final AtomicBoolean startFlag = new AtomicBoolean(false);
    private static final AtomicBoolean stopFlag = new AtomicBoolean(false);

    private Disruptor<LoggerEvent> disruptor;

    public static PYMetric timerPublish;
    public static PYMetric timerPublishDoneTrace;
    public static PYMetric timerHandlerProcess;
    public static PYMetric timerHandlerProcessDoneTrace;

    public static PYMetric timerFromPublishToHandle;

    /**
     * delay engine to clean which can not be done trace job, also can delay done trace to wait close mark point put into container.
     */
    private TaskEngine delayEngine;

    private LoggerTracer() {
        this.delayEngine = new DelayedTaskEngine();
        this.traceRecordMap = new ConcurrentHashMap<>();
    }

    private void intMetrics() {
        PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
        timerPublish = metricRegistry
                .register(MetricRegistry.name(LoggerTracer.class.getSimpleName(), "timer_publish"), Timer.class);
        timerPublishDoneTrace = metricRegistry
                .register(MetricRegistry.name(LoggerTracer.class.getSimpleName(), "timer_publish_done_trace"), Timer.class);
        timerHandlerProcess = metricRegistry
                .register(MetricRegistry.name(LoggerTracer.class.getSimpleName(), "timer_handler_process"),
                        Timer.class);
        timerHandlerProcessDoneTrace = metricRegistry
                .register(MetricRegistry.name(LoggerTracer.class.getSimpleName(), "timer_handler_process_done_trace"),
                        Timer.class);

        timerFromPublishToHandle = metricRegistry
                .register(MetricRegistry.name(LoggerTracer.class.getSimpleName(), "timer_from_publish_to_handle"),
                        Timer.class);
    }

    private void initDisruptor() {
        intMetrics();
        EventFactory<LoggerEvent> eventFactory = new LoggerEventFactory();
        int ringBufferSize = 1024 * 128;
        disruptor = new Disruptor<>(eventFactory, ringBufferSize, Executors.defaultThreadFactory(), ProducerType.MULTI,
                new BlockingWaitStrategy());
        LoggerWorkHandler loggerWorkHandler1 = new LoggerWorkHandler(delayEngine, traceRecordMap, enableLoggerTracer);

        LoggerWorkHandler loggerWorkHandler2 = new LoggerWorkHandler(delayEngine, traceRecordMap, enableLoggerTracer);
        LoggerWorkHandler loggerWorkHandler3 = new LoggerWorkHandler(delayEngine, traceRecordMap, enableLoggerTracer);
        LoggerWorkHandler loggerWorkHandler4 = new LoggerWorkHandler(delayEngine, traceRecordMap, enableLoggerTracer);
        LoggerWorkHandler loggerWorkHandler5 = new LoggerWorkHandler(delayEngine, traceRecordMap, enableLoggerTracer);
        LoggerWorkHandler loggerWorkHandler6 = new LoggerWorkHandler(delayEngine, traceRecordMap, enableLoggerTracer);
        LoggerWorkHandler loggerWorkHandler7 = new LoggerWorkHandler(delayEngine, traceRecordMap, enableLoggerTracer);
        LoggerWorkHandler loggerWorkHandler8 = new LoggerWorkHandler(delayEngine, traceRecordMap, enableLoggerTracer);

        disruptor.handleEventsWithWorkerPool(loggerWorkHandler1, loggerWorkHandler2, loggerWorkHandler3,
                loggerWorkHandler4, loggerWorkHandler5, loggerWorkHandler6, loggerWorkHandler7, loggerWorkHandler8);
    }

    /**
     * once you stop logger tracer, can not enable it again. and if you have not started it, can not enable it neither
     */
    public void enableLoggerTracer() {
        if (!stopFlag.get() && startFlag.get()) {
            this.enableLoggerTracer.set(true);
            logger.warn("enable logger tracer successfully");
        } else {
            logger.error("can not enable logger tracer, because start flag:{}, stop flag:{}", startFlag.get(),
                    stopFlag.get());
        }
    }

    public void disableLoggerTracer() {
        this.enableLoggerTracer.set(false);
    }

    /**
     * usually only start logger tracer when service startup, can not support start more than one time
     */
    public void start() {
        if (stopFlag.get()) {
            logger.warn("logger tracer had been stopped by someone before, won't start it");
            return;
        }

        if (startFlag.compareAndSet(false, true)) {
            logger.warn("going to start logger tracer");
            try {
                initDisruptor();
                this.delayEngine.start();
                this.disruptor.start();
                /**
                 * clean map for record
                 */
                this.traceRecordMap.clear();
                logger.warn("successfully start logger tracer process");
            } catch (Throwable t) {
                logger.error("failed to start logger tracer", t);
                throw new RuntimeException();
            }
        } else {
            logger.warn("logger tracer had been started by someone before, won't start it again");
        }
    }

    public void stop() {
        // first set enable logger tracer to false, no effective for now
        this.enableLoggerTracer.set(false);

        if (!startFlag.get()) {
            logger.warn("we didn't start logger tracer before, so DO NOT need to stop it");
            return;
        }

        if (stopFlag.compareAndSet(false, true)) {
            logger.warn("going to stop logger tracer");
            try {
                disruptor.shutdown();
            } catch (Exception e) {
                logger.error("failed to stop disruptor", e);
            }
            try {
                this.delayEngine.stop();
            } catch (Exception e) {
                logger.error("failed to stop delay engine", e);
            }
            /**
             * clear tracer map for now
             */
            this.traceRecordMap.clear();
            logger.warn("successfully stop logger tracer process");
        } else {
            logger.warn("we had stop logger tracer before, won't stop it again");
        }
    }

    private final static LoggerTracer loggerTracerSingleton = new LoggerTracer();

    public static LoggerTracer getInstance() {
        return loggerTracerSingleton;
    }

    /**
     * only msg need to be record
     *
     * @param objectId
     * @param className
     * @param msg
     */
    public void mark(long objectId, String className, String msg) {
        if (enableLoggerTracer.get()) {
            LogInfoCarrier logInfoCarrier = new SimpleLogInfoCarrier(msg);
            markProcess(objectId, className, logInfoCarrier);
        }
    }

    public void mark(long objectId, String className, String format, Object arg) {
        if (enableLoggerTracer.get()) {
            LogInfoCarrier logInfoCarrier = new OneLogInfoCarrier(format, arg);
            markProcess(objectId, className, logInfoCarrier);
        }
    }

    public void mark(long objectId, String className, String format, Object arg1, Object arg2) {
        if (enableLoggerTracer.get()) {
            LogInfoCarrier logInfoCarrier = new TwoLogInfoCarrier(format, arg1, arg2);
            markProcess(objectId, className, logInfoCarrier);
        }
    }

    public void mark(long objectId, String className, String format, Object... arguments) {
        if (enableLoggerTracer.get()) {
            LogInfoCarrier logInfoCarrier = new MultiLogInfoCarrier(format, arguments);
            markProcess(objectId, className, logInfoCarrier);
        }
    }

    public void mark(long objectId, String className, String msg, Throwable t) {
        if (enableLoggerTracer.get()) {
            LogInfoCarrier logInfoCarrier = new ThrowableLogInfoCarrier(msg, t);
            markProcess(objectId, className, logInfoCarrier);
        }
    }

    public void doneTrace(long objectId, TraceAction traceAction, int timeoutMs) {
        if (enableLoggerTracer.get()) {
            // should delay done trace
            LoggerEvent loggerEvent = buildLoggerEventFrom(TraceType.DONETRACE, objectId, traceAction, null, null,
                    timeoutMs, System.currentTimeMillis());
            DoneTraceTask doneTraceTask = new DoneTraceTask(DONE_TRACE_DELAY_TIME_MS, disruptor, loggerEvent);
            delayEngine.drive(doneTraceTask);
        }
    }

    public static String buildString(FormattingTuple ft) {
        StringBuilder stringBuilder = new StringBuilder();
        if (ft.getMessage() != null) {
            stringBuilder.append(ft.getMessage());
        }
        if (ft.getThrowable() != null) {
            stringBuilder.append(Utils.LINE_SEPARATOR);
            stringBuilder.append(ft.getThrowable());
        }
        return stringBuilder.toString();
    }

    public static String buildString(String msg, Throwable t) {
        StringBuilder stringBuilder = new StringBuilder();
        if (msg != null) {
            stringBuilder.append(msg);
        }
        if (t != null) {
            stringBuilder.append(Utils.LINE_SEPARATOR);
            stringBuilder.append(t);
        }
        return stringBuilder.toString();
    }

    private void markProcess(long objectId, String className, LogInfoCarrier logInfoCarrier) {
        publishForMark(TraceType.MARK, objectId, null, logInfoCarrier, className);
    }

    public void publishForMark(TraceType traceType, long traceId, TraceAction traceAction,
            LogInfoCarrier logInfoCarrier, String className) {
        PYTimerContext pyTimerContext = timerPublish.time();
        RingBuffer<LoggerEvent> ringBuffer = disruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try {
            LoggerEvent loggerEvent = ringBuffer.get(sequence);
            loggerEvent.setTraceType(traceType);
            loggerEvent.setTraceId(traceId);
            loggerEvent.setTraceAction(traceAction);
            loggerEvent.setLogInfoCarrier(logInfoCarrier);
            loggerEvent.setClassName(className);
            loggerEvent.setFromPublishToHandle(timerFromPublishToHandle.time());
        } finally {
            ringBuffer.publish(sequence);
            pyTimerContext.stop();
        }
    }

    public static void publishForDoneTrace(Disruptor<LoggerEvent> disruptor, LoggerEvent loggerEvent) {
        RingBuffer<LoggerEvent> ringBuffer = disruptor.getRingBuffer();
        PYTimerContext pyTimerContext = timerPublishDoneTrace.time();
        long sequence = ringBuffer.next();
        try {
            LoggerEvent publishLoggerEvent = ringBuffer.get(sequence);
            publishLoggerEvent.deepCopy(loggerEvent);
            loggerEvent.setFromPublishToHandle(timerFromPublishToHandle.time());
        } finally {
            ringBuffer.publish(sequence);
            pyTimerContext.stop();
        }
    }

    private LoggerEvent buildLoggerEventFrom(TraceType traceType, long traceId, TraceAction traceAction,
            LogInfoCarrier logInfoCarrier, String className, int timeoutMs, long endTime) {
        LoggerEvent loggerEvent = new LoggerEvent();
        loggerEvent.setTraceType(traceType);
        loggerEvent.setTraceId(traceId);
        loggerEvent.setTraceAction(traceAction);
        loggerEvent.setLogInfoCarrier(logInfoCarrier);
        loggerEvent.setClassName(className);
        loggerEvent.setTimeoutMs(timeoutMs);
        loggerEvent.setEndTime(endTime);
        return loggerEvent;
    }

    public int getLoggerCount(Long requestId) {
        int count = 0;
        LoggerWorkHandler.StartTimeAndLogs startTimeAndLogs = traceRecordMap.get(requestId);
        if (startTimeAndLogs != null) {
            count = startTimeAndLogs.size();
        }
        return count;
    }

}

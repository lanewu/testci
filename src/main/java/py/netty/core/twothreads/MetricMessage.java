package py.netty.core.twothreads;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYTimerContext;
import py.netty.message.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * This message
 */
public class MetricMessage {

    // The data wrapped
    private final Object data;

    private static Map<String, PYMetric> timers;
    private static Map<String, PYTimerContext> contexts;

    private static PYMetric timer1, timer2, timer3, timer4;

    static String TIMER_IN_TASK_QUEUE = "timer_in_task_queue";
    static String TIMER_EXECUTE_REQUEST_TIMEOUT_HANDLER = "timer_execute_handlers";
    static String TIMER_IN_OUTPUT_BUFFER = "timer_in_output_buffer";
    static String TIMER_TAKEN_OUTPUT_BUFFER = "timer_taken_output_buffer";

    static {
        timers = new HashMap<>(10);
        contexts = new HashMap<>(10);

        PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();

        timer1 = metricRegistry
                .register(MetricRegistry.name(Message.class.getSimpleName(), TIMER_IN_TASK_QUEUE), Timer
                        .class);

        timer2 = metricRegistry.register(
                MetricRegistry.name(Message.class.getSimpleName(), TIMER_EXECUTE_REQUEST_TIMEOUT_HANDLER),
                Timer.class);


        timer3 = metricRegistry.register(
                MetricRegistry.name(Message.class.getSimpleName(), TIMER_IN_OUTPUT_BUFFER),
                Timer.class);

        timer4 = metricRegistry.register(
                MetricRegistry.name(Message.class.getSimpleName(), TIMER_TAKEN_OUTPUT_BUFFER),
                Timer.class);

        timers.put(TIMER_IN_TASK_QUEUE, timer1);
        timers.put(TIMER_EXECUTE_REQUEST_TIMEOUT_HANDLER, timer2);
        timers.put(TIMER_IN_OUTPUT_BUFFER, timer3);
        timers.put(TIMER_TAKEN_OUTPUT_BUFFER, timer4);
    }


    public void startTimer(String name) {
        PYMetric timer = timers.get(name);
        if (timer != null) {
            contexts.put(name, timer.time());
        }
    }

    public void stopTimer(String name) {
        PYTimerContext context = contexts.get(name);
        if (context != null) {
            context.stop();
        }
    }

    public MetricMessage(Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }
}

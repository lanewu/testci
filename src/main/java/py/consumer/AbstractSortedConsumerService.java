package py.consumer;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import com.codahale.metrics.Timer;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYNullMetric;
import py.metrics.PYTimerContext;

public abstract class AbstractSortedConsumerService<E> implements ConsumerService<E> {
    private final static Logger logger = LoggerFactory.getLogger(AbstractSortedConsumerService.class);

    protected final String name;

    private volatile boolean shutdown = false;

    private volatile ConcurrentSkipListSet<E> idleSet;
    private AtomicReference<ConcurrentSkipListSet<E>> atomicReference;
    private List<E> elements;
    private Comparator<E> comparator;
    private Puller puller;
    private Semaphore semaphore;

    private PYMetric counterWaitForSchedule;
    private PYMetric histoPullPerSchedule;

    private PYMetric timerWaitForTasks;
    private PYMetric timerSortTasks;

    public AbstractSortedConsumerService(Comparator<E> comparator, String name) {
        this.atomicReference = new AtomicReference<>();
        this.idleSet = new ConcurrentSkipListSet<>();
        Validate.isTrue(this.atomicReference.getAndSet(new ConcurrentSkipListSet<E>()) == null);
        this.semaphore = new Semaphore(0);
        this.elements = new LinkedList<E>();
        this.comparator = comparator;
        this.counterWaitForSchedule = new PYNullMetric();
        this.histoPullPerSchedule = new PYNullMetric();
        this.name = name;
    }

    public void initMetric(Class<?> clazz, String prefix) {
        PYMetricRegistry registry = PYMetricRegistry.getMetricRegistry();
        histoPullPerSchedule = registry.register(
                MetricRegistry.name(clazz.getSimpleName(), "puller_histo_pull_per_schedule", prefix), Histogram.class);
        counterWaitForSchedule = registry.register(
                MetricRegistry.name(clazz.getSimpleName(), "puller_counter_wait_for_schedule", prefix), Counter.class);
        timerWaitForTasks = registry
                .register(MetricRegistry.name(clazz.getSimpleName(), "puller_timer_wait_for_tasks", prefix), Timer.class);
        timerSortTasks = registry
                .register(MetricRegistry.name(clazz.getSimpleName(), "puller_timer_sort_tasks", prefix), Timer.class);
    }

    public void start() {
        puller = new Puller(name);
        puller.start();
    }

    private class Puller extends Thread {
        private E firstElement = null;
        public Puller(String name) {
            super(name);
        }

        public void run() {
            while (!shutdown) {
                try {
                    elements.clear();
                    puller();
                } catch (Throwable t) {
                    logger.error("fail to start scheduler thread", t);
                }
            }

            logger.info("exit the thread, name={}", getName());
        }

        private void puller() throws Exception {
            PYTimerContext contextWaitForTasks = timerWaitForTasks.time();
            semaphore.acquire();
            contextWaitForTasks.stop();
            firstElement = null;

            PYTimerContext contextSortTasks = timerSortTasks.time();
            while ((firstElement = idleSet.pollFirst()) != null) {
                elements.add(firstElement);
            }

            idleSet = atomicReference.getAndSet(idleSet);
            while ((firstElement = idleSet.pollFirst()) != null) {
                elements.add(firstElement);
            }

            int count = elements.size();
            logger.info("pool size={} every time, semaphore={}", count, semaphore.availablePermits());
            if (count < 1) {
                throw new IllegalArgumentException("count = " + count);
            }

            elements.sort(comparator);
            contextSortTasks.stop();
            try {
                consume(elements);
            } finally {
                if (count > 1) {
                    semaphore.acquire(count - 1);
                }
            }

            histoPullPerSchedule.updateHistogram(count);
            counterWaitForSchedule.decCounter(count);
        }
    }

    @Override
    public boolean submit(E e) {
        boolean success = atomicReference.get().add(e);
        if (success) {
            semaphore.release();
            counterWaitForSchedule.incCounter();
        } else {
            logger.debug("can not add element={}", e);
        }

        return success;
    }

    protected void shutdown() {
        shutdown = true;
    }

    protected void join() {
        try {
            puller.join();
        } catch (Exception e) {
            logger.warn("fail to stop scheduler", e);
        }
    }

    abstract protected void consume(Collection<E> elements);
}

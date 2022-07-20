package py.metrics;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Timer;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

import py.monitor.jmx.server.ResourceType;

/**
 * Wrapper of codahale's metric registry. We can disable/enable metric by using this class.
 *
 * @author chenlia
 * @history 20160113 sxl add method "nameEx" to create metric name with resource type and id in it.
 */
public class PYMetricRegistry extends MetricRegistry {
    private static final Logger logger = LoggerFactory.getLogger(PYMetricRegistry.class);

    private boolean enableMetric = false;
    private List<Closeable> reporterList = new ArrayList<Closeable>();

    private final Supplier<Reservoir> reservoirSupplier;

    private PYMetricRegistry(Supplier<Reservoir> reservoirSupplier) {
        this.reservoirSupplier = reservoirSupplier;
    }

    private static class LazyHolder {
        private static final AtomicBoolean initialized = new AtomicBoolean(false);
        private static final AtomicReference<PYMetricRegistry> register = new AtomicReference<>();

        private synchronized static void  initRegister(Supplier<Reservoir> reservoirSupplier) {
            if (!initialized.get()) {
                register.set(new PYMetricRegistry(reservoirSupplier));
                initialized.compareAndSet(false, true);
            }
        }

        private static PYMetricRegistry getRegister() {
            if (!initialized.get()) {
                initRegister(ExponentiallyDecayingReservoir::new);
            }
            return register.get();
        }
    }

    public static void initRegister(Supplier<Reservoir> reservoirSupplier) {
        LazyHolder.initRegister(reservoirSupplier);
    }

    public static PYMetricRegistry getMetricRegistry() {
        return LazyHolder.getRegister();
    }

    private static void append(StringBuilder builder, String part) {
        if (part != null && !part.isEmpty()) {
            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(part);
        }
    }

    /**
     * we use this class to create the metric name with resource type & id much more easier.
     *
     * @author sxl
     */
    public static class PYMetricNameBuilder {
        public static final String EMPTY_ID = "0";
        private final String originalName;
        private ResourceType type;
        private String id;

        public PYMetricNameBuilder(String originalName) {
            this.originalName = originalName;
        }

        public PYMetricNameBuilder type(ResourceType resourceType) {
            this.type = resourceType;
            return this;
        }

        public PYMetricNameBuilder id(String id) {
            this.id = id;
            return this;
        }

        public String build() throws Exception {
            // give default resource type to a metric if it has not been set
            type = (type == null) ? ResourceType.NONE : type;

            // give default id to a metric if it has not been set
            id = (id == null) ? EMPTY_ID : id;

            // if (type == ResourceType.NONE && id == EMPTY_ID) {
            // return originalName;
            // } else {
            PYMetricNameHelper<String> pyMetricNameHelper = new PYMetricNameHelper<String>(originalName, type, id);
            return pyMetricNameHelper.generate();
            // }
        }

    }

    /**
     * for example: if you want to create an IOPS metrics on a volume whose volume id is 12345l, you should create it
     * like this:
     * <p>
     * <code>
     * private PYMetric demoMetric;
     * PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
     * demoMetric = metricRegistry.register(MetricRegistry.nameEx("xxxx","xxxx")
     * .type(ResourceType.VOLUME).id(1234l).build(), Meter.class);
     * </code>
     * <p>
     * if you don't care the resource type & id. you can use it like this:
     * <p>
     * <code>
     * private PYMetric demoMetric;
     * PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
     * demoMetric = metricRegistry.register(MetricRegistry.nameEx("xxxx","xxxx").build(), Meter.class);
     * </code>
     * <p>
     * Or,just use the old method like this:
     * <p>
     * <code>
     * PYMetric demoMetric;
     * PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
     * demoMetric = metricRegistry.register(MetricRegistry.name("xxxx","xxxx"), Meter.class);
     * </code>
     *
     * @param name
     * @param names
     * @return
     * @author sxl
     */
    public static PYMetricNameBuilder nameEx(String name, String... names) {
        final StringBuilder builder = new StringBuilder();
        append(builder, name);
        if (names != null) {
            for (String s : names) {
                append(builder, s);
            }
        }
        String originalName = builder.toString();
        return new PYMetricNameBuilder(originalName);
    }

    /**
     * Register the given metric instance.
     * 
     * @param name
     *            metric register name
     * @param metricInstance
     *            customized metric
     * @return {@link PYNullMetric#defaultNullMetric} if option in {@link PYMetricRegistry} for metric functionality is
     *         disabled. Otherwise, a metric will be returned if being registered successfully, or null if not.
     */
    public PYMetric registerInstance(String name, Metric metricInstance) {
        return registerInstance(name, metricInstance, enableMetric);
    }

    /**
     * Register the given metric instance.
     * 
     * @param name
     *            metric register name
     * @param metricInstance
     *            customized metric
     * @param enableMetric
     *            a flag to tell whether metric functionality is enabled.
     *            <p>
     *            Note: this flag has higher priority than option for metric functionality in {@link PYMetricRegistry}.
     *            That is the latter is ignored.
     * @return {@link PYNullMetric#defaultNullMetric} if the given flag for metric functionality is disabled. Otherwise,
     *         a metric will be returned if being registered successfully, or null if not.
     */
    public PYMetric registerInstance(String name, Metric metricInstance, boolean enableMetric) {
        if (!enableMetric) {
            return PYNullMetric.defaultNullMetric;
        }

        Metric metric = null;
        try {
            metric = super.register(name, metricInstance);
        } catch (IllegalArgumentException e) {
            metric = getMetricRegistry().getMetrics().get(name);
        } catch (Exception e) {
            logger.error("Can't register name {} as metric {} due to exception", name,
                    metricInstance.getClass().getName(), e);
            return null;
        }

        if (metric == null) {
            logger.error("Can't register a valid metric for use");
            return null;
        }
        // If the name to registered has registered before, but the registered metric type is not the type to
        // register, then return null to fail registering. Otherwise, return the registered metric.
        if (!metric.getClass().equals(metricInstance.getClass())) {
            logger.error("Cannot register the used name {} whose type is {} as another type {} of metric", name,
                    metric.getClass().getName(), metricInstance.getClass().getName());
            return null;
        } else {
            logger.debug("Register name {} as metric {} successfully", name, metricInstance.getClass().getName());
            return new PYMetricImpl(metric);
        }
    }

    /**
     * Register metric in type of the given class.
     * 
     * @param name
     *            metric register name
     * @param clazz
     *            metric type
     * @return {@link PYNullMetric#defaultNullMetric} if option in {@link PYMetricRegistry} for metric functionality is
     *         disabled. Otherwise, a metric will be returned if being registered successfully, or null if not.
     */
    public PYMetric register(String name, Class<? extends Metric> clazz) {
        return register(name, clazz, enableMetric);
    }

    private static final AtomicLong counter = new AtomicLong(0);
    /**
     * Register metric in type of the given class.
     * 
     * @param name
     *            metric register name
     * @param clazz
     *            metric type
     * @param enableMetric
     *            a flag to tell whether metric functionality is enabled.
     *            <p>
     *            Note: this flag has higher priority than option for metric functionality in {@link PYMetricRegistry}.
     *            That is the latter is ignored.
     * @return {@link PYNullMetric#defaultNullMetric} if the given flag for metric functionality is disabled. Otherwise,
     *         a metric will be returned if being registered successfully, or null if not.
     */
    public PYMetric register(String name, Class<? extends Metric> clazz, boolean enableMetric) {
        if (!enableMetric) {
            return PYNullMetric.defaultNullMetric;
        }

        // close debug metric
        Metric metric = null;
        try {
            // check if the name to register was registered before, if not, register it
            if (clazz.isAssignableFrom(Histogram.class)) {
                metric = super.histogram(name);
            } else {
                Metric instance;
                if (Timer.class.equals(clazz)) {
                    instance = new Timer(reservoirSupplier.get());
                } else if (Histogram.class.equals(clazz)) {
                    instance = new Histogram(reservoirSupplier.get());
                } else {
                    instance = clazz.newInstance();
                }
                metric = super.register(name, instance);
            }
        } catch (IllegalArgumentException e) {
            metric = getMetricRegistry().getMetrics().get(name);
        } catch (Exception e) {
            logger.error("Can't register name {} as metric {} due to exception", name, clazz.getName(), e);
            return null;
        }
        if (metric == null) {
            logger.error("Can't register a valid metric for use");
            return null;
        }
        // If the name to registered has registered before, but the registered metric type is not the type to
        // register, then return null to fail registering. Otherwise, return the registered metric.
        if (!metric.getClass().equals(clazz)) {
            logger.error("Cannot register the used name {} whose type is {} as another type {} of metric", name,
                    metric.getClass().getName(), clazz.getName());
            return null;
        } else {
            logger.debug("Register name {} as metric {} successfully", name, clazz.getName());
            return new PYMetricImpl(metric);
        }

    }

    public boolean isEnableMetric() {
        return enableMetric;
    }

    public PYMetricRegistry setEnableMetric(boolean enableMetric) {
        this.enableMetric = enableMetric;
        return this;
    }

    public void addReporter(Closeable reporter) {
        this.reporterList.add(reporter);
    }

    public void stopReporter() {
        for (Closeable reporter : reporterList) {
            try {
                reporter.close();
            } catch (Exception e) {
                logger.error("Caught an exception when stop reporter {}", reporter.getClass().getName(), e);
            }
        }
    }
}

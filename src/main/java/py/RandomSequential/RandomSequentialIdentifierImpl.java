package py.RandomSequential;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;

public class RandomSequentialIdentifierImpl implements RandomSequentialIdentifier {

    private final IOStateMachine ioStateMachine;
    private static PYMetric meterSequential;
    private static PYMetric meterNotSequential;

    static {

        PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
        String className = "RandomSequentialIdentifierImpl";

        meterSequential = metricRegistry
                .register(MetricRegistry.name(className, "meter_sequential"), Meter.class);
        meterNotSequential = metricRegistry
                .register(MetricRegistry.name(className, "meter_not_sequential"), Meter.class);

    }

    public RandomSequentialIdentifierImpl(int sequentialConditionThreshold, int randomConditionThreshold) {
        ioStateMachine = new IOStateMachine(sequentialConditionThreshold, randomConditionThreshold);
    }

    @Override
    public synchronized boolean updateLastOffsetAndIsSequential(long offset, int length) {

        IOStateMachine.State state = ioStateMachine.process(offset, length);

        if (state == IOStateMachine.State.IO_RANDOM) {
            meterNotSequential.mark();
            return false;
        } else {
            meterSequential.mark();
            return true;
        }
    }

}

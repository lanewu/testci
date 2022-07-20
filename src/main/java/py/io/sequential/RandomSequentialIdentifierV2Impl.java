package py.io.sequential;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.io.sequential.IOSequentialTypeHolder.IOSequentialType;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;

public class RandomSequentialIdentifierV2Impl implements RandomSequentialIdentifierV2 {

  private static final Logger logger = LoggerFactory.getLogger(RandomSequentialIdentifierV2Impl.class);
  private int sequentialLimiting;
  private long lastOffset;
  IOState currentState;  // : sequential, random
  private final String prefix;

  private static PYMetric meterSequential;
  private static PYMetric meterRandom;


  enum IOState {
    IO_RANDOM, IO_SEQUENTIAL,
  }

  public RandomSequentialIdentifierV2Impl(int sequentialLimiting, String prefix) {
    this.sequentialLimiting = sequentialLimiting;
    this.currentState = IOState.IO_RANDOM;
    this.prefix = prefix;
    initMetric();
    logger.warn("sequentialLimiting:{} prefix:{}", this.sequentialLimiting, this.prefix);
  }

  private static void initMetric() {

    PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
    String className = "RandomSequentialIdentifierImpl";

    meterSequential = metricRegistry
        .register(MetricRegistry.name(className, "meter_sequential"), Meter.class);
    meterRandom = metricRegistry
        .register(MetricRegistry.name(className, "meter_random"), Meter.class);

  }

  private boolean matchTowIOContextOffset(IOSequentialTypeHolder previous, IOSequentialTypeHolder next) {
    if (previous.getOffset() + previous.getLength() == next.getOffset()) {
      return true;
    } else {
      return false;
    }
  }

  private void setTypeToSubIOContextList(int head, int tail, List<? extends IOSequentialTypeHolder> ioSequentialTypeHolders,
      IOSequentialType ioSequentialType) {
    for (int i = head; i <= tail; i++) {
      IOSequentialTypeHolder ioHolder = ioSequentialTypeHolders.get(i);
      ioHolder.setIoSequentialType(ioSequentialType);
      if (ioSequentialType == IOSequentialType.RANDOM_TYPE) {
        meterRandom.mark();
      } else {
        meterSequential.mark();
      }
    }
  }

  @Override
  public void judgeIOIsSequential(List<? extends IOSequentialTypeHolder> ioSequentialTypeHolders) {
    int size = ioSequentialTypeHolders.size();
    int ioContextSize = ioSequentialTypeHolders.size();
    int continuousCount = 0;
    for (int i = 0; i < size; i++) {
      IOSequentialTypeHolder ioContext = ioSequentialTypeHolders.get(i);
      logger.debug("io coming {}, {}, {}", lastOffset, ioContext.getLength(), ioContext.getOffset());
      // continuous with previous
      if (ioContext.getOffset() == lastOffset) {
        if(continuousCount == 0) {
          ioContext.setIoSequentialType(IOSequentialType.SEQUENTIAL_TYPE);
        } else if (continuousCount > 0) {
          setTypeToSubIOContextList(i - continuousCount, i, ioSequentialTypeHolders, IOSequentialType.SEQUENTIAL_TYPE );
          continuousCount = 0;
        } else {
          logger.error("lastOffset:{} continuous:{} i:{} list:{}", lastOffset, continuousCount, i, ioSequentialTypeHolders);
        }
        lastOffset += ioContext.getLength();
        currentState = IOState.IO_SEQUENTIAL;
        meterSequential.mark();
        continue;
      } else {
        if (i + 1 >= ioContextSize) {
          break;
        }
        if (matchTowIOContextOffset(ioContext, ioSequentialTypeHolders.get(i + 1))) {
          // don't judge sequential
          continuousCount++;
          // if this io context was the second from tail, then set SEQUENTIAL to it.
          if (continuousCount >= sequentialLimiting || (i + 1) == (size - 1) ) {
            setTypeToSubIOContextList(i + 1 - continuousCount, i + 1, ioSequentialTypeHolders,
                IOSequentialType.SEQUENTIAL_TYPE);
            lastOffset = ioSequentialTypeHolders.get(i + 1).getOffset() + ioContext.getLength();
            i++;
            continuousCount = 0;
            logger
                .info("set sub list to sequential. index:{} continuous:{} list size:{}", i,
                    continuousCount, ioContextSize);
          }
        } else {
          setTypeToSubIOContextList(i - continuousCount, i, ioSequentialTypeHolders,
              IOSequentialType.RANDOM_TYPE);
          logger.info("set sub list to random. index:{} continuous:{} list size:{} offset:{}", i,
              continuousCount, ioContextSize, ioSequentialTypeHolders.get(i + 1).getOffset() - ioContext.getOffset());
          currentState = IOState.IO_RANDOM;
          continuousCount = 0;
          continue;
        }
      }
    }
  }
}

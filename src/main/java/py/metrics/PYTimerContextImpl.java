package py.metrics;

import com.codahale.metrics.Timer.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a class that wraps around timer's context
 *
 * @author chenlia
 */
public class PYTimerContextImpl implements PYTimerContext {
  private static final Logger logger = LoggerFactory.getLogger(PYTimerContextImpl.class);

  private final Context context;

  public PYTimerContextImpl(Context context) {
    if (context == null) {
      // Keep a validation here because this happened once, but not reproducing in the
      // following days --tyr 2018.12.18
      logger.error("given a null context !! {}", context, new Exception());
    }
    this.context = context;
  }

  @Override
  public long stop() {
    try {
      return context.stop();
    } catch (Throwable t) {
      // Keep a validation here because this happened once, but not reproducing in the
      // following days --tyr 2018.12.18
      logger.error("an error ?! {}", context, t);
      return -1;
    }
  }

}

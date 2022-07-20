package py.metrics;

/** 
 * a class that simulates timer's context and does nothing
 * @author chenlia
 */

public class PYNullTimerContext implements PYTimerContext {
    public static final PYTimerContext defaultNullTimerContext = new PYNullTimerContext();

    @Override
	public long stop() {
        return -1;
    }

}

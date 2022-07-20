package py.engine;

/**
 * Created by kobofare on 2017/6/29.
 */
public class BogusLatency implements Latency {
    public static final BogusLatency DEFAULT = new BogusLatency();

    @Override
    public void mark() {
    }

    @Override
    public boolean isSafe() {
        return true;
    }

    @Override
    public Latency createBranch() {
        return new BogusLatency();
    }

    @Override
    public String print() {
        return "";
    }
}

package py.engine;

/**
 * Created by kobofare on 2017/6/28.
 */
public interface Latency {
    void mark();
    boolean isSafe();
    Latency createBranch();
    String print();
}

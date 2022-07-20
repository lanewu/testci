package py.engine;

/**
 * 
 * @author lx
 *
 */
public interface TaskListener<R extends Result> {
    public void response(R result);
}

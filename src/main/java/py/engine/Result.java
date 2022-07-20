package py.engine;

/**
 * 
 * @author lx
 *
 */
public interface Result {
    public boolean isSuccess();

    public Exception cause();
}

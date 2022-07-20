package py.engine;

/**
 * @author lx
 */
public class ResultImpl implements Result {
    public static final Result DEFAULT = new ResultImpl();
    private Exception e;

    public ResultImpl() {
        this(null);
    }

    public ResultImpl(Exception e) {
        this.e = e;
    }

    @Override
    public boolean isSuccess() {
        return e == null;
    }

    @Override
    public Exception cause() {
        return e;
    }

    public void setCause(Exception e) {
        this.e = e;
    }

    @Override
    public String toString() {
        return "ResultImpl [e=" + e + "]";
    }
}

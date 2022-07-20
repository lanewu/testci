package py.function;

public interface Callback {
    void completed();

    void failed(Throwable e);
}

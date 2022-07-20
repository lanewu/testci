package py.common;

public interface VoidCallback {
    VoidCallback EMPTY_VOID_CALL_BACK = () -> {};

    void call();
}

package py.netty.server;

import py.netty.core.IOEventThreadsMode;
import py.netty.core.TransferenceOption;

/**
 * Option for I/O transference server.
 * 
 * @author zjm
 *
 * @param <T>
 */
public class TransferenceServerOption<T> extends TransferenceOption<T> {

    public static final TransferenceServerOption<Integer> IO_SERVER_SO_BACKLOG = valueOf("IO_SERVER_SO_BACKLOG");

    public static final TransferenceOption<IOEventThreadsMode> IO_EVENT_GROUP_THREADS_MODE = valueOf(
        "IO_EVENT_GROUP_THREADS_MODE");
    public static final TransferenceOption<Float> IO_EVENT_GROUP_THREADS_PARAMETER = valueOf(
        "IO_EVENT_GROUP_THREADS_PARAMETER");
    public static final TransferenceOption<IOEventThreadsMode> IO_EVENT_HANDLE_THREADS_MODE = valueOf(
        "IO_EVENT_HANDLE_THREADS_MODE");
    public static final TransferenceOption<Float> IO_EVENT_HANDLE_THREADS_PARAMETER = valueOf(
        "IO_EVENT_HANDLE_THREADS_PARAMETER");

    private static <T> TransferenceServerOption<T> valueOf(String name) {
        return new TransferenceServerOption<T>(name);
    }

    protected TransferenceServerOption(String name) {
        super(name);
    }

}

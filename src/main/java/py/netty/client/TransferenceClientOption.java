package py.netty.client;

import py.netty.core.IOEventThreadsMode;
import py.netty.core.TransferenceOption;

/**
 * Option for I/O transference client.
 * 
 * @author zjm
 *
 * @param <T>
 */
public class TransferenceClientOption<T> extends TransferenceOption<T> {

    public static final TransferenceClientOption<Integer> IO_TIMEOUT_MS = valueOf("IO_TIMEOUT_MS");
    public static final TransferenceClientOption<Integer> IO_CONNECTION_TIMEOUT_MS = valueOf(
            "IO_CONNECTION_TIMEOUT_MS");
    public static final TransferenceClientOption<Integer> MAX_LAST_TIME_FOR_CONNECTION_MS = valueOf(
            "MAX_LAST_TIME_FOR_CONNECTION_MS");
    public static final TransferenceClientOption<Integer> CONNECTION_COUNT_PER_ENDPOINT = valueOf(
            "CONNECTION_COUNT_PER_ENDPOINT");

    public static final TransferenceOption<IOEventThreadsMode> CLIENT_IO_EVENT_GROUP_THREADS_MODE = valueOf(
        "CLIENT_IO_EVENT_GROUP_THREADS_MODE");
    public static final TransferenceOption<Float> CLIENT_IO_EVENT_GROUP_THREADS_PARAMETER = valueOf(
        "CLIENT_IO_EVENT_GROUP_THREADS_PARAMETER");
    public static final TransferenceOption<IOEventThreadsMode> CLIENT_IO_EVENT_HANDLE_THREADS_MODE = valueOf(
        "CLIENT_IO_EVENT_HANDLE_THREADS_MODE");
    public static final TransferenceOption<Float> CLIENT_IO_EVENT_HANDLE_THREADS_PARAMETER = valueOf(
        "CLIENT_IO_EVENT_HANDLE_THREADS_PARAMETER");

    private static <T> TransferenceClientOption<T> valueOf(String name) {
        return new TransferenceClientOption<T>(name);
    }

    protected TransferenceClientOption(String name) {
        super(name);
    }
}

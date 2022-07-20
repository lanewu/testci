package py.netty.core;

import py.netty.client.TransferenceClientOption;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import py.netty.server.TransferenceServerOption;

/**
 * Configuration for I/O transference.
 * 
 * @author zjm
 *
 */
public class TransferenceConfiguration {
    public TransferenceConfiguration() {
        option(TransferenceOption.MAX_MESSAGE_LENGTH, 5 * 1024 * 1024);
    }

    /*
     * Configuration options.
     */
    private Map<TransferenceOption<?>, Object> options = new ConcurrentHashMap<TransferenceOption<?>, Object>();

    /**
     * Configure some specified option.
     * 
     * @param option
     * @param value
     * @return
     */
    public <T> TransferenceConfiguration option(TransferenceOption<T> option, T value) {
        options.put(option, value);
        return this;
    }

    /**
     * Get value of specified option.
     * 
     * @param option
     * @return
     */
    public <T> Object valueOf(TransferenceOption<T> option) {
        return options.get(option);
    }

    /**
     * Get default configuration for I/O transference client.
     *
     * @return
     */
    public static TransferenceConfiguration defaultConfiguration() {
        TransferenceConfiguration clientConfiguartion = new TransferenceConfiguration();
        clientConfiguartion.option(TransferenceClientOption.IO_CONNECTION_TIMEOUT_MS, 3000);
        clientConfiguartion.option(TransferenceClientOption.IO_TIMEOUT_MS, 10000);
        clientConfiguartion.option(TransferenceClientOption.CONNECTION_COUNT_PER_ENDPOINT, 1);
        clientConfiguartion.option(TransferenceClientOption.MAX_LAST_TIME_FOR_CONNECTION_MS, 60 * 1000 * 15);

        clientConfiguartion.option(TransferenceOption.MAX_BYTES_ONCE_ALLOCATE, 16 * 1024);

        clientConfiguartion.option(TransferenceClientOption.CLIENT_IO_EVENT_GROUP_THREADS_MODE, IOEventThreadsMode.Fix_Threads_Mode);
        clientConfiguartion.option(TransferenceClientOption.CLIENT_IO_EVENT_GROUP_THREADS_PARAMETER, 2.0f);
        clientConfiguartion.option(TransferenceClientOption.CLIENT_IO_EVENT_HANDLE_THREADS_MODE, IOEventThreadsMode.Fix_Threads_Mode);
        clientConfiguartion.option(TransferenceClientOption.CLIENT_IO_EVENT_HANDLE_THREADS_PARAMETER, 4.0f);

        clientConfiguartion.option(TransferenceServerOption.IO_EVENT_GROUP_THREADS_MODE, IOEventThreadsMode.Fix_Threads_Mode);
        clientConfiguartion.option(TransferenceServerOption.IO_EVENT_GROUP_THREADS_PARAMETER, 2.0f);
        clientConfiguartion.option(TransferenceServerOption.IO_EVENT_HANDLE_THREADS_MODE, IOEventThreadsMode.Fix_Threads_Mode);
        clientConfiguartion.option(TransferenceServerOption.IO_EVENT_HANDLE_THREADS_PARAMETER, 4.0f);
        return clientConfiguartion;
    }
}

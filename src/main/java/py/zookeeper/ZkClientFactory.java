package py.zookeeper;

import java.util.ArrayList;
import java.util.List;

public class ZkClientFactory {
    private int sessionTimeout;
    private String serverAddress;

    public ZkClientFactory(String serverAddress, int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
        this.serverAddress = serverAddress;
    }

    public ZkClient generate(List<ZkListener> listeners) throws ZkException {
        return new ZkClientImpl(serverAddress, sessionTimeout, listeners);
    }

    public ZkClient generate(ZkListener listener) throws ZkException {
        List<ZkListener> listeners = new ArrayList<ZkListener>();
        listeners.add(listener);
        return new ZkClientImpl(serverAddress, sessionTimeout, listeners);
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }
    
}

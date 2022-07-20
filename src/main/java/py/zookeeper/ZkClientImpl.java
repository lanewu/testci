package py.zookeeper;

import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It is wrapped for zookeeper
 * 
 * @author lx
 *
 */
public class ZkClientImpl implements ZkClient {
    private final static Logger logger = LoggerFactory.getLogger(ZkClientImpl.class);
    private final ZooKeeper zooKeeper;
    private List<ZkListener> listeners;
    private String serverAddress;
    private int sessionTimeout;
    
    public ZkClientImpl(String serverAddress, int sessionTimeout, List<ZkListener> listeners) throws ZkException {
        try {
            this.serverAddress = serverAddress;
            this.sessionTimeout = sessionTimeout;
            this.listeners = listeners;
            this.zooKeeper = new ZooKeeper(serverAddress, sessionTimeout, new ZooKeeperWatcher());
            logger.info("try to build a session with target:{}", serverAddress);
        } catch (Exception e) {
            throw new ZkException(
                            "can not connect to serverAddress: " + serverAddress + ", timeout: " + sessionTimeout, e);
        }
    }

    private class ZooKeeperWatcher implements Watcher {
        @Override
        public void process(WatchedEvent event) {
            logger.info("process the event: {}", event);
            if (Event.EventType.None.equals(event.getType())) {
                switch (event.getState()) {
                case SyncConnected:
                    logger.warn("connected to the zk server, session:{}", Long.toHexString(zooKeeper.getSessionId()));
                    if (listeners != null) {
                        for (ZkListener listener : listeners) {
                            try {
                                listener.connected(zooKeeper.getSessionId());
                            } catch (Exception e) {
                            }
                        }
                    }
                    break;
                case Expired:
                    logger.warn("The zk service has expired the connection: {}. Reconnect to it.",
                                    Long.toHexString(zooKeeper.getSessionId()));
                    try {
                        zooKeeper.close();
                    } catch (InterruptedException e1) {
                        logger.error("close the zookeeper failure");
                    }

                    // notify all listeners
                    if (listeners != null) {
                        for (ZkListener listener : listeners) {
                            try {
                                listener.expired();
                            } catch (Exception e) {
                            }
                        }
                    }
                    break;
                case Disconnected:
                    logger.warn("Zk client:{} is disconnected from server", Long.toHexString(zooKeeper.getSessionId()));
                    // notify all listeners
                    if (listeners != null) {
                        for (ZkListener listener : listeners) {
                            try {
                                listener.disconnected();
                            } catch (Exception e) {
                            }
                        }
                    }
                    break;
                case AuthFailed:
                    logger.error("auth failure");
                    break;
                default:
                    break;
                }
            } else if (Event.EventType.NodeDeleted.equals(event.getType())) {
                String path = event.getPath();
                logger.warn("node:{} has deleted", path);
                if (listeners != null) {
                    for (ZkListener listener : listeners) {
                        try {
                            listener.pathDeleted(path);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<String> getFiles(String path) throws ZkException {
        return getFiles(path, false);
    }

    @Override
    public List<String> getFiles(String path, boolean watch) throws ZkException {
        try {
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return zooKeeper.getChildren(path, watch);
        } catch (Exception e) {
            throw new ZkException("can not get files in dir: " + path, e);
        }
    }

    @Override
    public void monitor(String path) throws ZkException {
        try {
            zooKeeper.exists(path, true);
        } catch (KeeperException | InterruptedException e) {
            throw new ZkException("can not watch path: " + path, e);
        }
    }

    @Override
    public void createPath(String path) throws ZkException {
        String[] dirs = path.split("/");
        String newDir = "";
        for (int i = 0; i < dirs.length; i++) {
            if (dirs[i].isEmpty()) {
                continue;
            }

            newDir += "/" + dirs[i];
            try {
                if (!exist(newDir)) {
                    zooKeeper.create(newDir, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            } catch (NodeExistsException e) {
                // just skip
            } catch (Exception e) {
                throw new ZkException("can not create directory: " + newDir + ", because: ", e);
            }
        }
    }

    @Override
    public String createFile(String path, byte[] data, boolean ephemeral) throws ZkException {
        try {
            return zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                            ephemeral ? CreateMode.EPHEMERAL_SEQUENTIAL : CreateMode.PERSISTENT);
        } catch (Exception e) {
            throw new ZkException("can not create file: " + path + ", because: ", e);
        }
    }

    @Override
    public byte[] readData(String path) throws ZkException {
        try {
            if (zooKeeper.exists(path, false) == null) {
                return null;
            }

            Stat stat = new Stat();
            return zooKeeper.getData(path, false, stat);
        } catch (Exception e) {
            throw new ZkException("can not get data from file: " + path, e);
        }
    }

    @Override
    public void writeData(String path, byte[] data) throws ZkException {
        try {
            if (zooKeeper.exists(path, false) == null) {
                zooKeeper.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

            zooKeeper.setData(path, data, -1);
        } catch (Exception e) {
            throw new ZkException("can not write data to file: " + path, e);
        }
    }

    @Override
    public void close() {
        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                logger.error("can not close zookeeper");
            }
        }
    }

    @Override
    public void deleteFile(String path) throws ZkException {
        try {
            if (zooKeeper.exists(path, false) == null) {
                return;
            }
            zooKeeper.delete(path, -1);
        } catch (InterruptedException | KeeperException e) {
            throw new ZkException("can not delete file: " + path, e);
        }
    }

    public boolean exist(String path) throws ZkException {
        try {
            if (zooKeeper.exists(path, false) == null) {
                return false;
            } else {
                return true;
            }
        } catch (InterruptedException | KeeperException e) {
            throw new ZkException("can not delete file: " + path, e);
        }
    }
    
    @Override
    public String toString() {
        return "ZkClientImpl [zooKeeper=" + zooKeeper + ", listeners=" + listeners + ", serverAddress=" + serverAddress
                        + ", sessionTimeout=" + sessionTimeout + "]";
    }
}

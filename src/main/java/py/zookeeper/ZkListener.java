package py.zookeeper;

public interface ZkListener {
    public void pathDeleted(String path);
    public void disconnected();
    public void expired();
    public void connected(long sessionId);
}

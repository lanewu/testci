package py.connection.pool;


/**
 * it is a async operator, if you want to wait for the connection to build successfully or timeout, you should
 * pass
 * Created by kobofare on 2017/3/4.
 */
public interface ReconnectionHandler {
    public void reconnect(ConnectionRequest connectionRequest);
}

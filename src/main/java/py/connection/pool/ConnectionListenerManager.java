package py.connection.pool;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by kobofare on 2017/3/5.
 */
public class ConnectionListenerManager implements GenericFutureListener<ChannelFuture> {
    private final static Logger logger = LoggerFactory.getLogger(ConnectionListenerManager.class);

    // JUST for unit test
    public List<ConnectionRequest> getRequests() {
        return requests;
    }

    private List<ConnectionRequest> requests;
    private PYChannel pyChannel;
    private volatile boolean connecting;

    public ConnectionListenerManager(PYChannel pyChannel) {
        this.requests = new LinkedList<>();
        this.pyChannel = pyChannel;
        this.connecting = false;
    }

    public void add(ConnectionRequest request) {
        requests.add(request);
    }

    public void notifyAllListeners() {
        Channel channel = pyChannel.get();
        logger.warn("notifyAllListeners channel={}", channel);

        for (ConnectionRequest listener : requests) {
            try {
                logger.warn("notifyAllListeners listener={}", listener);
                listener.complete(channel);
            } catch (Exception e) {
                logger.warn("fail to notifyAllListeners connection listener, channel={}", pyChannel);
            }
        }

        requests.clear();
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        Channel channel = future.channel();
        if (future.isSuccess()) {
            logger.warn("connection successfully, channel={}", channel);
            pyChannel.set(channel);
        } else {
            logger.info("fail to connection to {}, cause:", pyChannel.getEndPoint(), future.cause());
            pyChannel.set(null);
        }
    }

    public boolean isConnecting() {
        return connecting;
    }

    public void setConnecting(boolean connecting) {
        this.connecting = connecting;
    }


    @Override
    public String toString() {
        return "ConnectionListenerManager{" + "requests=" + requests.size() + ", pyChannel=" + pyChannel
                + ", connecting=" + connecting + '}';
    }
}

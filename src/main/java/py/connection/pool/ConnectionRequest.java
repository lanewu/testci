package py.connection.pool;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.netty.message.SendMessage;

/**
 * Created by kobofare on 2017/3/4.
 */
public class ConnectionRequest implements ConnectionListener {
    public static final Logger logger = LoggerFactory.getLogger(ConnectionRequest.class);
    public final PYChannel pyChannel;

    public ConnectionRequest(PYChannel pyChannel) {
       this.pyChannel = pyChannel;
    }

    public PYChannel getPyChannel() {
        return pyChannel;
    }

    @Override
    public void complete(Channel channel) {

    }

    @Override
    public String toString() {
        return "ConnectionRequest{" + "pyChannel=" + pyChannel + '}';
    }
}

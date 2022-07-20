package py.connection.pool;

import io.netty.channel.Channel;

/**
 * Created by kobofare on 2017/3/5.
 */
public interface ConnectionListener {
    /**
     * @param channel after reconnecting to peer successfully, the channel will be available, otherwise it will be null.
     */
    public void complete(Channel channel);
}

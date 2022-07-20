package py.connection.pool;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.struct.EndPoint;
import py.netty.core.NetworkDetect;
import py.netty.exception.DisconnectionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by kobofare on 2017/3/4.
 */
public class PYChannelPool {
    private static final Logger logger = LoggerFactory.getLogger(PYChannelPool.class);
    private final PYChannel[] pyChannels;
    private int robin;
    private int times;
    private final EndPoint endpoint;
    private ReconnectionHandler reconnectionHandler;

    public PYChannelPool(int robin, EndPoint endPoint,
            ReconnectionHandler reconnectionHandler) {//, int pingHostTimeoutMs) {
        this.pyChannels = new PYChannel[robin];
        this.robin = robin;
        this.times = 0;
        this.endpoint = endPoint;
        //   this.reachable = true;
        this.reconnectionHandler = reconnectionHandler;
        //   this.pingHostTimeoutMs = pingHostTimeoutMs;

        for (int i = 0; i < robin; i++) {
            pyChannels[i] = null;
        }
    }

    public EndPoint getEndpoint() {
        return endpoint;
    }

    /**
     * the method is wrapped by synchronized, because multi thread call this method maybe cause problem.
     *
     * @param endPoint
     * @return
     */
    public synchronized PYChannel get(EndPoint endPoint) {

        int index = times % robin;

        // if a good channel exist, then attach the channel to connection.
        times = index + 1;
        if (pyChannels[index] != null) {
            return pyChannels[index];
        }

        logger.warn("build new connection with end point: {}", endPoint);
        pyChannels[index] = new PYChannel(endPoint);
        reconnectionHandler.reconnect(new ConnectionRequest(pyChannels[index]));
        return pyChannels[index];
    }

    public List<PYChannel> getAll() {
        List<PYChannel> channels = new ArrayList<>();
        for (int i = 0; i < pyChannels.length; i++) {
            if (pyChannels[i] != null) {
                channels.add(pyChannels[i]);
            }
        }

        return channels;
    }

    public void close() {
        for (int i = 0; i < pyChannels.length; i++) {
            if (pyChannels[i] == null) {
                continue;
            }

            Channel old = pyChannels[i].set(null);
            if (old != null) {
                old.close();
            }
        }
    }

    @Override
    public String toString() {
        return "PYChannelPool{" + "pyChannels=" + Arrays.toString(pyChannels) + ", robin=" + robin + ", times=" + times
                + ", endpoint=" + endpoint + ", reconnectionHandler=" + reconnectionHandler + '}';
    }
}

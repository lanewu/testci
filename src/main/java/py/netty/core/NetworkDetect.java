package py.netty.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.netty.exception.DisconnectionException;

import java.io.IOException;
import java.net.*;

/**
 * This is a helper class for checking if the target is reachable.
 * <p>
 * Created by kobofare on 2017/2/20.
 */
public class NetworkDetect {
    private final static Logger logger = LoggerFactory.getLogger(NetworkDetect.class);

    /**
     * send a icmp package and expect a response
     *
     * @param ip
     * @param timeout unit: ms
     * @throws DisconnectionException
     */
    public static void ping(String ip, int timeout) throws DisconnectionException {
        try {
            if (!InetAddress.getByName(ip).isReachable(timeout)) {
                logger.warn("ip: {} is not reachable, timeout: {}", ip, timeout);
                throw new DisconnectionException("ip: " + ip + ", timeout: " + timeout);
            }
        } catch (UnknownHostException e) {
            logger.warn("there is no host: {}, {}", ip, timeout, e);
            throw new DisconnectionException("ip: " + ip + ", timeout: " + timeout);
        } catch (IOException e) {
            logger.warn("io exception: {}, {}", ip, timeout, e);
            throw new DisconnectionException("ip: " + ip + ", timeout: " + timeout);
        }
    }

    /**
     * try to connect to target.
     *
     * @param ip
     * @param port
     * @param timeout unit: ms
     * @throws DisconnectionException
     */
    public static void connect(String ip, int port, int timeout) throws DisconnectionException {

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(ip, port), timeout);
        } catch (Exception e) {
            logger.warn("caught an exception when connecting", e);
            throw new DisconnectionException("ip: " + ip + ", port: " + port + ", timeout: " + timeout);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.warn("caught an exception when closing the socket", e);
            }
        }
    }

    /**
     * ping first, if it is ok, then try to connect to target.
     *
     * @param ip
     * @param port
     * @param timeout
     * @throws DisconnectionException
     */
    public static void ping(String ip, int port, int timeout) throws DisconnectionException {
        // ping first, then connection to the port.
        ping(ip, timeout);
        connect(ip, port, timeout);
    }

    public static boolean myselfAlive(String hostName) {
        InetAddress inetAddress = null;
        try {
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(hostName));
            inetAddress = InetAddress.getLocalHost();
            if (networkInterface == null || !networkInterface.isUp()) {
                logger.error("the hostname is {}, the address is {}",hostName,inetAddress);
                return false;
            }
        } catch (SocketException | UnknownHostException e) {
            logger.error("the hostname is {}, the address is {}",hostName,inetAddress);
            return false;
        }
        return true;
    }

}

package py.zookeeper;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.app.context.AppContext;
import py.instance.InstanceStatus;

/**
 * 
 * @author lx
 * 
 */
public class ZkElectionLeader implements ZkListener {
    private final static Logger logger = LoggerFactory.getLogger(ZkElectionLeader.class);
    private final ZkClientFactory zkClientFactory;
    private String balletBox;
    private String ticket;
    private byte[] data;
    private String TICKET_PREFIX = "ticket_";
    private ZkClient zkClient;
    private final AppContext context;
    private String watchPath;

    public ZkElectionLeader(ZkClientFactory zkClientFactory, String balletBox, AppContext context) {
        this.zkClientFactory = zkClientFactory;
        this.balletBox = balletBox;
        if (balletBox.endsWith("/")) {
            this.balletBox = balletBox.substring(0, balletBox.length() - 1);
        }
        this.context = context;
    }

    public AppContext getAppContext() {
        return context;
    }

    public void startElection() throws ZkException {
        synchronized (this) {
            zkClient = zkClientFactory.generate(this);
        }
    }

    private void init() throws ZkException {
        synchronized (this) {
            // Create root directory if it does't exist.
            zkClient.createPath(balletBox);
            if (ticket == null) {
                ticket = zkClient.createFile(balletBox + "/" + TICKET_PREFIX, data, true);
                logger.warn("balletBox: {}, ticket: {}", balletBox, ticket);
            }

            if (election(ticket)) {
                context.setStatus(InstanceStatus.OK);
            } else {
                context.setStatus(InstanceStatus.SUSPEND);
            }
        }
    }

    private boolean election(String currentTicket) {
        List<String> children;
        try {
            children = zkClient.getFiles(balletBox);
        } catch (ZkException e) {
            logger.error("can not get ticket from box:{}, current ticket", balletBox, currentTicket);
            return false;
        }

        logger.info("++++++ children: {}, currentTicket: {}", children, currentTicket);
        Collections.sort(children);
        int index = children.indexOf(currentTicket.substring(currentTicket.lastIndexOf('/') + 1));
        if (index == 0) {
            logger.warn("now i am leader: {}, ticket: {}, children: {}", zkClientFactory.getServerAddress(), currentTicket, children);
            return true;
        } else {
            boolean success = false;
            while (index > 0) {
                try {
                    final String watchNode = children.get(--index);
                    String watchPath = balletBox + "/" + watchNode;
                    zkClient.monitor(watchPath);
                    this.watchPath = watchPath;
                    success = true;
                    logger.warn("now i am fellower, so monitor: {}, my: {}, children: {}", watchPath, currentTicket, children);
                    break;
                } catch (Exception e) {
                    logger.warn("watch node failure, current tick: {}, watch path: {}, all path: {}", currentTicket,
                                    watchPath, children, e);
                }
            }

            if (!success && index == 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void pathDeleted(String path) {
        if (!watchPath.equals(path)) {
            throw new IllegalArgumentException("watchpath: " + watchPath + ", path: " + path);
        }

        logger.info("monitor node: {} is deleted, so became OK", path);
        if (election(ticket)) {
            context.setStatus(InstanceStatus.OK);
        } else {
            context.setStatus(InstanceStatus.SUSPEND);
        }
    }

    public void close() {
        if (zkClient != null) {
            zkClient.close();
        }
    }

    @Override
    public void expired() {
        try {
            logger.warn("client:{}, ticket:{}, expired", zkClientFactory.getServerAddress(), ticket);
            /**
             * Only when the session is expired, the current ticket is deleted from zookeeper system, so now 
             * we should reset the ticket.
             */
            ticket = null;
            startElection();
        } catch (Exception e) {
            logger.error("caught an exception", e);
        }
    }

    @Override
    public void disconnected() {
        logger.warn("client:{}, disconnected", zkClientFactory.getServerAddress());
        context.setStatus(InstanceStatus.SUSPEND);
    }

    @Override
    public void connected(long sessionId) {
        logger.warn("client: {}, connection successfully, sessionId: {}", zkClientFactory.getServerAddress(),
                        Long.toHexString(sessionId));
        try {
            init();
        } catch (Exception e) {
            logger.error("caught an exception", e);
        }
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}

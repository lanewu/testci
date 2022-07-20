package py.app.context;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.struct.EndPoint;
import py.instance.Group;
import py.instance.InstanceDomain;
import py.instance.InstanceId;
import py.instance.InstanceStatus;
import py.instance.Location;
import py.instance.PortType;

public class AppContextImpl implements AppContext {

    private final Logger logger = LoggerFactory.getLogger(AppContextImpl.class);
    protected final Map<PortType, EndPoint> endPoints;

    private String instanceName;
    private InstanceIdStore instanceIdStore;
    private GroupStore groupStore = new GroupStore.NullGroupStore();
    private InstanceStatus status;
    private Location location;
    private InstanceDomainFileStore domainFileStore = new InstanceDomainFileStore.NullInstanceDomainFileStore();

    public AppContextImpl(String name) {
        this(name, InstanceStatus.OK);
    }

    public AppContextImpl(String name, InstanceStatus instanceStatus) {
        endPoints = new HashMap<>();
        this.instanceName = name;
        this.status = instanceStatus;
    }

    /**
     * A convenient API for putEndPoint(). It maps the specified end point to a MAIN service of the app
     * 
     */
    public void putEndPoint(EndPoint endPoint) {
        putEndPoint(PortType.CONTROL, endPoint);
    }

    /**
     * maybe a process contain several services, so we should set the address (equal to EndPoint) to supply service, and
     * when we not set the host name, then we will select a default host name
     * 
     * @param serviceNameEnumValue
     * @param endPoint
     */
    public void putEndPoint(PortType serviceNameEnumValue, EndPoint endPoint) {
        if (endPoint.getHostName() == null || endPoint.getHostName().isEmpty()) {
            try {
                final InetAddress inetAddress = InetAddress.getLocalHost();
                String hostName = inetAddress.getHostAddress();
                endPoint.setHostName(hostName);

            } catch (Exception e) {
                String errMsg = "Can't start the application";
                logger.info("cant get host name", e);
                throw new RuntimeException(errMsg, e);
            }
        }

        endPoints.put(serviceNameEnumValue, endPoint);
    }

    @Override
    public EndPoint getEndPointByServiceName(PortType serviceNameEnumValue) {
        return endPoints.get(serviceNameEnumValue);
    }

    @Override
    public InstanceId getInstanceId() {
        return instanceIdStore.getInstanceId();
    }
     
    @Override
    public EndPoint getMainEndPoint() {
        return getEndPointByServiceName(PortType.CONTROL);
    }

    @Override
    public EndPoint getHeartbeatMyselfEndPoint() {
        return getMainEndPoint();
    }

    @Override
    public void setInstanceId(InstanceId instanceId) {
        instanceIdStore.persistInstanceId(instanceId);
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public Map<PortType, EndPoint> getEndPoints() {
        return endPoints;
    }

    public InstanceIdStore getInstanceIdStore() {
        return instanceIdStore;
    }

    public void setInstanceIdStore(InstanceIdStore instanceIdStore) {
        this.instanceIdStore = instanceIdStore;
    }

    public void setLocation(String location) {
        this.location = Location.fromString(location);
    }

    @Override
    public Location getLocation() {
        return this.location;
    }

    @Override
    public InstanceStatus getStatus() {
        return this.status;
    }

    public GroupStore getGroupStore() {
        return groupStore;
    }

    public void setGroupStore(GroupStore groupStore) {
        this.groupStore = groupStore;
    }

    public InstanceDomainFileStore getDomainFileStore() {
        return domainFileStore;
    }

    public void setDomainFileStore(InstanceDomainFileStore domainFileStore) {
        this.domainFileStore = domainFileStore;
    }

    @Override
    public void setStatus(InstanceStatus status) {
        if (this.status != status) {
            logger.warn("this instance status is chang from {} to {}, instance is {}", this.status, status, this);
            this.status = status;
        }
    }

    @Override
    public Group getGroup() {
        return groupStore.getGroup();
    }

    @Override
    public void setGroupInfo(Group group) throws Exception {
        try {
            if (groupStore.canUpdateGroup(group)) {
                logger.warn("update group, old group:{}, new group:{}", getGroup(), group);
                groupStore.persistGroup(group);
            }
        } catch (Exception e) {
            logger.error("caught an exception", e);
            throw new Exception();
        }
    }

    /**
     * By default, we only need to listen to the main EndPoint. For those services who have more than one EndPoints to
     * listen to, they should have there own AppContext who overrides this function
     */
    @Override
    public Map<PortType, EndPoint> getEndPointsThrift() {
        Map<PortType, EndPoint> endPointsToListenTo = new HashMap<>();
        endPointsToListenTo.put(PortType.CONTROL, getMainEndPoint());
        return endPointsToListenTo;
    }

    @Override
    public InstanceDomain getInstanceDomain() {
        return domainFileStore.getInstanceDomain();
    }

    @Override
    public void setInstanceDomain(InstanceDomain instanceDomain) {
        domainFileStore.persistInstanceDomain(instanceDomain);
    }

    @Override
    public String toString() {
        return "AppContextImpl [logger=" + logger + ", endPoints=" + endPoints + ", instanceName=" + instanceName
                + ", instanceIdStore=" + instanceIdStore + ", groupStore=" + groupStore + ", status=" + status
                + ", location=" + location + ", domainFileStore=" + domainFileStore + "]";
    }

}

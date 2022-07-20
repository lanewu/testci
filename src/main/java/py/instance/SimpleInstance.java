package py.instance;

import py.common.struct.EndPoint;

/**
 * Created by zhongyuan on 17-4-8.
 */
public class SimpleInstance {

    private final InstanceId instanceId;
    private final EndPoint endPoint;

    public SimpleInstance(InstanceId instanceId, EndPoint endPoint) {
        this.instanceId = instanceId;
        this.endPoint = endPoint;
    }

    public InstanceId getInstanceId() {
        return instanceId;
    }

    public EndPoint getEndPoint() {
        return endPoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SimpleInstance))
            return false;

        SimpleInstance that = (SimpleInstance) o;

        return instanceId != null ? instanceId.equals(that.instanceId) : that.instanceId == null;
    }

    @Override
    public int hashCode() {
        return instanceId != null ? instanceId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SimpleInstance{" + "instanceId=" + instanceId + ", endPoint=" + endPoint + '}';
    }
}

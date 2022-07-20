package py.app.context;

import py.instance.InstanceId;

// Return an instance id
public class DummyInstanceIdStore implements InstanceIdStore {

    private InstanceId instanceId;

    @Override
    public InstanceId getInstanceId() {
        return instanceId;
    }

    @Override
    public void persistInstanceId(InstanceId id) {
        instanceId = id;
    }

    public void setInstanceId(InstanceId instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public String toString() {
        return "DummyInstanceIdStore [instanceId=" + instanceId + "]";
    }
}

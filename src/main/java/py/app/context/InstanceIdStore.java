package py.app.context;

import py.instance.InstanceId;

// Return an instance id
public interface InstanceIdStore {
    public InstanceId getInstanceId();
    public void persistInstanceId(InstanceId id);
}

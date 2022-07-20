package py.instance;

import java.util.HashSet;
import java.util.Set;

import py.common.struct.EndPoint;
import py.exception.FilterDeniedException;
import py.filter.InstanceFilter;
import py.instance.InstanceStatus;

/**
 * In memory instance store for testing purpose
 * 
 * @author chenlia
 */
public class DummyInstanceStore implements InstanceStore {
    private Set<Instance> instances = new HashSet<Instance>();

    public Set<Instance> getInstances() {
        return instances;
    }

    public void setInstances(Set<Instance> instances) {
        this.instances = instances;
    }

    @Override
    public synchronized void save(Instance instance) {
        instances.add(instance);
    }

    @Override
    public synchronized Set<Instance> getAll(String name, InstanceStatus status) {
        Set<Instance> returnedSet = new HashSet<Instance>();
        for (Instance instance : instances) {
            if (name.equals(instance.getName()) && status.equals(instance.getStatus())) {
                returnedSet.add(instance);
            }
        }

        return returnedSet;
    }

    @Override
    public synchronized Set<Instance> getAll(InstanceStatus status) {
        Set<Instance> returnedSet = new HashSet<Instance>();
        for (Instance instance : instances) {
            if (status.equals(instance.getStatus())) {
                returnedSet.add(instance);
            }
        }

        return returnedSet;
    }

    @Override
    public synchronized Set<Instance> getAll(String name) {
        Set<Instance> returnedSet = new HashSet<Instance>();
        for (Instance instance : instances) {
            if (name.equals(instance.getName())) {
                returnedSet.add(instance);
            }
        }

        return returnedSet;
    }

    @Override
    public synchronized Set<Instance> getAll() {
        return new HashSet<>(instances);
    }

    @Override
    public synchronized Instance get(EndPoint endPoint) {
        for (Instance instance : instances) {
            for (EndPoint tmp : instance.getEndPoints().values()) {
                if (tmp != null && tmp.equals(endPoint)) {
                    return instance;
                }
            }
        }
        return null;
    }

    @Override
    public synchronized Instance get(InstanceId id) {
        for (Instance instance : instances) {
            if (instance.getId().equals(id)) {
                return instance;
            }
        }
        return null;
    }

    @Override
    public synchronized void delete(Instance instance) {
        instances.remove(instance);
    }

    @Override
    public Instance getByHostNameAndServiceName(String hostName, String name) {
        return null;
    }

    @Override
    public void close() {
    }

}

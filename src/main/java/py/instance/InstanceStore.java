package py.instance;

import java.util.Set;

import py.common.struct.EndPoint;
import py.filter.InstanceFilter;
import py.exception.FilterDeniedException;

public interface InstanceStore {
    public Set<Instance> getAll();

    public Set<Instance> getAll(String name);

    public Set<Instance> getAll(InstanceStatus status);

    public Set<Instance> getAll(String name, InstanceStatus status);

    public Instance get(InstanceId id);

    public Instance get(EndPoint endPoint);

    public void delete(Instance instance);

    public Instance getByHostNameAndServiceName(String hostName, String name);
    /**
     * insert the instance to the store if it doesn't exist. Otherwise, update it.
     * sxl turned it to 'Deprecated'. because it can be instead by another method
     * :"public void save(Instance instance, Filter<Instance> filter) throws FilterDeniedException;"
     * 
     * @param instance
     *            instance to be saved
     */
    public void save(Instance instance);

    // Some Instance store implement may have workers, so need a interface to shutdown
    public void close();

}

package py.monitor.utils;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.PyService;
import py.common.struct.EndPoint;
import py.instance.Instance;
import py.instance.InstanceStatus;
import py.instance.InstanceStore;

/**
 * 
 * @author shixulu
 *
 */

public class Utils {
    private static Logger logger = LoggerFactory.getLogger(Utils.class);

    public static String getServiceNameByEndPoint(EndPoint endpoint, InstanceStore instanceStore) throws Exception {
        String serviceName = "";
        Set<Instance> instances = instanceStore.getAll();
        boolean beFound = false;
        for (Instance instance : instances) {
            logger.debug("Current instance in instance-store is : {}", instance);
            if (instance.getEndPoint().equals(endpoint)) {
                serviceName = instance.getName();
                beFound = true;
                break;
            }
        }

        if (!beFound) {
            logger.warn("Can't find endpoint {} in instance-store : {}", endpoint, instances);
            throw new Exception();
        }
        return serviceName;
    }

    public static Set<Instance> getInstancesByServiceNames(Set<String> serviceNames, InstanceStore instanceStore) {
        Set<Instance> instances = new HashSet<Instance>();
        for (String serviceName : serviceNames) {
            instances.addAll(instanceStore.getAll(serviceName, InstanceStatus.OK));
        }
        return instances;
    }

    public static Set<EndPoint> getEndPointByServiceNames(Set<String> serviceNames, InstanceStore instanceStore)
            throws Exception {
        Set<EndPoint> endpoints = new HashSet<EndPoint>();
        for (String serviceName : serviceNames) {
            endpoints.addAll(getEndPointByServiceName(serviceName, instanceStore));
        }
        return endpoints;
    }

    public static Set<EndPoint> getEndPointByServiceName(String serviceName, InstanceStore instanceStore)
            throws Exception {
        if (!PyService.is(serviceName).legal()) {
            logger.error("Illegal service name: {}", serviceName);
            throw new Exception();
        }

        Set<EndPoint> endpoints = new HashSet<EndPoint>();
        Set<Instance> instances = instanceStore.getAll(serviceName, InstanceStatus.OK);

        for (Instance instance : instances) {
            endpoints.add(instance.getEndPoint());
        }

        return endpoints;
    }

}

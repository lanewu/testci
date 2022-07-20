package py.filter;

import py.instance.Instance;
import py.instance.InstanceStatus;

/***
 * This class provides a bunch of useful instance filters that can be used to filter out the instance records
 * 
 * @chenlia
 * 
 */
public class UtilityInstanceFilters {

    /**
     * A filter that is the logical AND of its child filters.
     */
    public static class AndFilter implements InstanceFilter {
        final InstanceFilter[] filters;

        /**
         * Creates a new filter that is the logical AND of the specified child filters.
         *
         * @param filters
         *            the child filters.
         */
        public AndFilter(InstanceFilter... filters) {
            this.filters = new InstanceFilter[filters.length];
            for (int i = 0; i < filters.length; i++) {
                this.filters[i] = filters[i];
            }
        }

        /**
         * Invokes the child filters' {@link #passed} methods in order. If any child filter's {@code passed} method
         * returns {@code false}, this method short circuits subsequent invocations and immediately returns
         * {@code false}. If none of the child filters' {@code passed} methods return {@code false}, this method returns
         * {@code true}.
         *
         * @return {@code true} if, and only if, no child filter's {@code passed} method returned {@code false}.
         */
        public boolean passed(Instance instance) {
            for (InstanceFilter filter : filters) {
                if (!filter.passed(instance)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * A filter that is the logical OR of its child filters.
     */
    public static class OrFilter implements InstanceFilter {
        final InstanceFilter[] filters;

        /**
         * Creates a new filter that is the logical OR of the specified child filters.
         *
         * @param filters
         *            the child filters.
         */
        public OrFilter(InstanceFilter... filters) {
            this.filters = filters;
        }

        /**
         * Invokes the child filters' {@link #passed} methods in order. If any child filter's {@code passed} method
         * returns {@code true}, this method short circuits subsequent invocations and immediately returns {@code true}.
         * If none of the child filters' {@code passed} methods return {@code true}, this method returns {@code false}.
         *
         * @return {@code true} if, and only if, any child filter's {@code passed} method returned {@code true}.
         */
        public boolean passed(Instance instance) {
            for (InstanceFilter filter : filters) {
                if (filter.passed(instance)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Not Filter
     */
    public static class NotFilter implements InstanceFilter {
        final InstanceFilter filter;

        public NotFilter(InstanceFilter filter) {
            this.filter = filter;
        }

        public boolean passed(Instance instance) {
            return !filter.passed(instance);
        }
    }

    /**
     * Filter instances by DFDD {@link InstanceStatus}
     *
     */
    public static class InstanceStatusFilter implements InstanceFilter {

        final InstanceStatus status;

        public InstanceStatusFilter(InstanceStatus state) {
            this.status = state;
        }

        public boolean passed(Instance instance) {
            if (instance == null || instance.getStatus() == null)
                return false;
            return instance.getStatus() == status;
        }
    }

    /**
     * Filter instances by {@link InstanceStatus} and prefix that the instanceId starts with
     */
    public static class InstanceStatusPrefixFilter implements InstanceFilter {

        final InstanceStatus state;
        final String prefix;

        public InstanceStatusPrefixFilter(InstanceStatus state, String prefix) {
            this.state = state;
            this.prefix = prefix;
        }

        public boolean passed(Instance instance) {
            if (instance == null || instance.getStatus() == null || instance.getId() == null)
                return false;
            return instance.getStatus() == state && instance.getId().toString().startsWith(prefix);
        }
    }

    /**
     * Filters by instances in the local dc and a given {@link InstanceStatus}
     */
    public static class LocalDcInstanceStatusFilter implements InstanceFilter {

        final InstanceStatus state;
        final String dc;

        public LocalDcInstanceStatusFilter(String dc, InstanceStatus state) {
            this.dc = dc;
            this.state = state;
        }

        public boolean passed(Instance instance) {
            if (instance == null || instance.getLocation() == null || instance.getLocation().getDc() == null
                    || instance.getStatus() == null)
                return false;
            return instance.getLocation().getDc().equals(this.dc) && instance.getStatus() == state;
        }
    }

    /**
     * Filter by the data cluster the instance is in, and its {@link InstanceStatus}
     */
    public static class ClusterInstanceStatusFilter implements InstanceFilter {

        final InstanceStatus state;
        final String cluster;

        public ClusterInstanceStatusFilter(String cluster, InstanceStatus state) {
            this.cluster = cluster;
            this.state = state;
        }

        public boolean passed(Instance instance) {
            if (instance == null || instance.getLocation() == null || instance.getLocation().getCluster() == null
                    || instance.getStatus() == null)
                return false;
            return instance.getLocation().getCluster().equals(cluster) && instance.getStatus().equals(state);
        }
    }

    /**
     * Filter by the cluster only.
     */
    public static class ClusterFilter implements InstanceFilter {

        final String cluster;

        public ClusterFilter(String cluster) {
            if (cluster == null)
                throw new IllegalArgumentException("You can't pass a null cluster");
            this.cluster = cluster;
        }

        public boolean passed(Instance instance) {
            if (instance == null || instance.getLocation() == null || instance.getLocation().getCluster() == null)
                return false;
            return instance.getLocation().getCluster().equals(cluster);
        }
    }

    /**
     * Filter by dc only
     */
    public static class DCFilter implements InstanceFilter {

        final String dc;

        public DCFilter(String dc) {
            if (dc == null || dc.equals(""))
                throw new IllegalArgumentException("You can't pass a null or empty dc string");
            this.dc = dc;
        }

        public boolean passed(Instance instance) {
            if (instance == null || instance.getLocation() == null || instance.getLocation().getDc() == null)
                return false;
            else
                return instance.getLocation().getDc().equals(dc);
        }
    }

    /**
     * Filter by racks only
     */
    public static class RackFilter implements InstanceFilter {

        final String rack;

        public RackFilter(String rack) {
            if (rack == null || rack.equals(""))
                throw new IllegalArgumentException("You can't pass a null or empty rack string");
            this.rack = rack;
        }

        public boolean passed(Instance instance) {
            if (instance == null || instance.getLocation() == null || instance.getLocation().getRack() == null)
                return false;
            else
                return instance.getLocation().getRack().equals(rack);
        }
    }

    /**
     * Filter by hosts only
     */
    public static class HostFilter implements InstanceFilter {

        final String host;

        public HostFilter(String host) {
            if (host == null || host.equals(""))
                throw new IllegalArgumentException("You can't pass a null or empty host string");
            this.host = host;
        }

        public boolean passed(Instance instance) {
            if (instance == null || instance.getLocation() == null || instance.getLocation().getHost() == null)
                return false;
            else
                return instance.getLocation().getHost().equals(host);
        }
    }

    /**
     * Filter by instanceId only
     */
    public static class InstanceIdFilter implements InstanceFilter {

        final String instanceId;

        public InstanceIdFilter(String instanceId) {
            if (instanceId == null || instanceId.equals(""))
                throw new IllegalArgumentException("You can't pass a null or empty instanceId string");
            this.instanceId = instanceId;
        }

        public boolean passed(Instance instance) {
            if (instance == null || instance.getLocation() == null || instance.getId() == null)
                return false;
            else
                return instance.getId().equals(instanceId);
        }
    }
}

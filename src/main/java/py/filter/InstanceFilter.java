package py.filter;

import py.instance.Instance;

public interface InstanceFilter {
    /***
     * indicates whether the instance record is passed the tests according to the filter
     */
    public boolean passed(Instance instance);
}

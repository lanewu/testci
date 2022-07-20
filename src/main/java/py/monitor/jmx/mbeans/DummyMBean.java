package py.monitor.jmx.mbeans;

import py.monitor.pojo.management.PeriodicPojo;
import py.monitor.pojo.management.annotation.Description;
import py.monitor.pojo.management.annotation.MBean;
import py.monitor.pojo.management.annotation.ManagedAttribute;

@MBean(objectName = "dummy-mbean:name=dummyMbean")
@Description("dummy mbean for test case")
public class DummyMBean extends PeriodicPojo{
    
    public static String OBJECT_NAME = "dummy-mbean:name=dummyMbean";

    private String name;

    private String description;

    @ManagedAttribute
    public String getName() {
        return name;
    }

    @ManagedAttribute
    public void setName(String name) {
        this.name = name;
    }

    @ManagedAttribute
    public String getDescription() {
        return description;
    }

    @ManagedAttribute
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    protected void modifyData() throws Exception {
        // TODO Auto-generated method stub
        
    }

}

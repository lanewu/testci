package py.monitor.customizable;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

public class StandardMBeanEx extends StandardMBean{

    protected StandardMBeanEx(Class<?> mbeanInterface) throws NotCompliantMBeanException {
        super(mbeanInterface);
        // TODO Auto-generated constructor stub
    }

}

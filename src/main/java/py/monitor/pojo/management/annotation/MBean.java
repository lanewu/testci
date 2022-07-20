package py.monitor.pojo.management.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import py.monitor.jmx.server.ResourceType;

/**
 *
 * @author sxl
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface MBean {
    public static enum AutomaticType {
        ATTRIBUTE, OPERATION
    };

    /**
     * return the ObjectName with which the MBean should be registered with the MBean server.
     * <P>
     * Refer to {@link javax.management.ObjectName} for details of objectname syntax Sample object names:<br>
     * py.monitor.aop.pojo.management:name=MyBean,type=py.monitor.aop.pojo.management.ProcessingMonitor
     * py.monitor.aop.pojo.management:application=ESB,name=MyBean,type=py.monitor.aop.pojo.management.ProcessingMonitor
     */
    String objectName() default "";

    ResourceType resourceType() default ResourceType.NONE;

    AutomaticType[] automatic() default {};
}

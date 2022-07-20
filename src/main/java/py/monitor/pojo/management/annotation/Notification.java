package py.monitor.pojo.management.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to specify the method of the MBean class whom is going to send the attribution to JMX client
 * 
 * @author sxl
 * 
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Notification {
    public String rule() default "";

    public int period() default 1;
}

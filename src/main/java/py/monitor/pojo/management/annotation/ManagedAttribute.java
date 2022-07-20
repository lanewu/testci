package py.monitor.pojo.management.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for an MBean attributes.
 * <p>
 *
 * May be applied to one or both of:
 * <ul>
 * <li>An MBean attribute getter: getXxx(), or boolean isXxx()</li>
 * <li>An MBean attribute setter: setXxx()</li>
 * </ul>
 * 
 * @author sxl
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface ManagedAttribute {
    String range() default "";

    String unitOfMeasurement() default "";
}

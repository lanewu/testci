package py.monitor.pojo.management.helper;

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.struct.EndPoint;
import py.metrics.PYMetricRegistry.PYMetricNameBuilder;
import py.monitor.pojo.management.annotation.MBean;
import py.monitor.pojo.util.Preconditions;

public class ObjectNameBuilder {
    private static final Logger logger = LoggerFactory.getLogger(ObjectNameBuilder.class);
    private static final String KEY_NAME = "name";
    private static final String KEY_TYPE = "type";
    private static final String KEY_APPLICATION = "application";

    private ObjectName objectName;
    private String domain;
    private Map<String, String> properties = new LinkedHashMap<String, String>();
    private EndPoint endpoint;

    public ObjectNameBuilder() {
    }

    private void normalize() {
        if (objectName != null) {
            this.domain = objectName.getDomain();
            this.properties.putAll(objectName.getKeyPropertyList());
            this.objectName = null;
        }
    }

    /**
     * Use introspection to obtain the objectName from the {@link MBean#objectName()} annotation attribute.
     * 
     * @param mBeanClass
     *            a type annotated with {@link MBean}
     * @return an ObjectNameBuilder
     * @throws MalformedObjectNameException
     */
    public ObjectNameBuilder withObjectName(Class<?> mBeanClass) throws MalformedObjectNameException {
        MBean annotation = mBeanClass.getAnnotation(MBean.class);
        Preconditions.notNull(annotation, mBeanClass + " is not annotated with " + MBean.class);
        return withObjectName(annotation);
    }

    public ObjectNameBuilder withObjectName(MBean annotation) throws MalformedObjectNameException {
        String annotatedObjectName = annotation.objectName();
        String finalObjectName = null;
        try {
            logger.debug("endpoint:{}", endpoint);
            String resourceId = null;
            switch (annotation.resourceType()) {
            case MACHINE:
                resourceId = this.endpoint.getHostName();
                break;
            case JVM:
                resourceId = String.format("%s[%d]", endpoint.getHostName(), endpoint.getPort());
                break;
            default:
                resourceId = PYMetricNameBuilder.EMPTY_ID;
                break;
            }
            finalObjectName = new PYMetricNameBuilder(annotatedObjectName).type(annotation.resourceType())
                    .id(resourceId).build();
        } catch (Exception e) {
            logger.error("Caught an exception", e);
            throw new MalformedObjectNameException();
        }

        Preconditions.notEmpty(finalObjectName,
                String.format("@{} annotation does not specify objectName", MBean.class.getName()));
        return withObjectName(ObjectName.getInstance(finalObjectName));
    }

    /**
     *
     * @param objectName
     *            an objectName that should replace domain and all properties of the ObjectName contained by this
     *            builder
     */
    public ObjectNameBuilder withObjectName(ObjectName objectName) {
        this.objectName = objectName;
        return this;
    }

    /**
     * @param mBeanClass
     *            the class that will contribute its attributes
     * @return an ObjectNameBuilder with a domain matching {@code mBeanClass}' package, and type attribute matching
     *         {@code mBeanClass}' fully qualified name.
     * @throws MalformedObjectNameException
     */
    public ObjectNameBuilder withDomainAndType(Class<?> mBeanClass) throws MalformedObjectNameException {
        return withDomain(mBeanClass.getPackage().getName()).withType(mBeanClass.getName());
    }

    public ObjectNameBuilder withDomain(String domain) {
        normalize();
        this.domain = domain;
        return this;
    }

    public ObjectNameBuilder withName(String name) {
        return withProperty(KEY_NAME, name);
    }

    public ObjectNameBuilder withType(String type) {
        return withProperty(KEY_TYPE, type);
    }

    public ObjectNameBuilder withEndpoint(EndPoint endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public ObjectNameBuilder withApplication(String app) {
        return withProperty(KEY_APPLICATION, app);
    }

    public ObjectNameBuilder withProperty(String key, String value) {
        normalize();
        properties.put(key, value);
        return this;
    }

    public ObjectName build() throws MalformedObjectNameException {
        return (objectName != null) ? objectName
                : ObjectName.getInstance(domain, new Hashtable<String, String>(properties));
    }
}

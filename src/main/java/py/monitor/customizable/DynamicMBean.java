package py.monitor.customizable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.monitor.pojo.management.helper.NotificableDynamicMBean;

/**
 * {@code DynamicMBean} will be used as a template to create a dynamic MBean by a lots of MBean attributes that had been
 * restored in the {@code AttributeStore}.
 * 
 * After the derived class has been created, we can place it into {@code py.monitor.jmx.repoter.Reporter} so that the
 * {@code Reporter} can support 2 kinds of MBean:
 * 1. Annotated POJO MBeans that are defined by java code.(POJO is just a definition of MBean in JMX side)
 * 2. Customisable MBeans that are defined by a lots of attributes.(we can customize all performance task by this way!!)
 * 
 * 
 * <pre class="code">
 * public class Bean {
 *     private Foo foo;
 * 
 *     public Foo getFoo() {
 *         return this.foo;
 *     }
 * 
 *     public Bean setFoo(Foo foo) {
 *         this.foo = foo;
 *         return this;
 *     }
 * }
 * </pre>
 * 
 * @author sxl
 * 
 * @remark
 *         POJO means "Plain Ordinary Java Object". And it always be used to create a bean object in many architecture,
 *         such as JMX,Spring and so on
 */
public class DynamicMBean extends NotificableDynamicMBean {
    private static final Logger logger = LoggerFactory.getLogger(DynamicMBean.class);
    private final Map<String, Method> getters;
    private final Map<String, Method> setters;
    private final Set<Method> operations;
    private final Object resource;
    private final MBeanInfo info;

    /* Creates a new instance of DynamicPOJOMBean */
    public DynamicMBean(Object obj) {
        getters = new LinkedHashMap<String, Method>();
        setters = new LinkedHashMap<String, Method>();
        operations = new LinkedHashSet<Method>();
        resource = obj;
        try {
            info = initialize();
        } catch (IntrospectionException ex) {
            throw new IllegalArgumentException(obj.getClass().getName(), ex);
        }
    }

    private MBeanInfo initialize() throws IntrospectionException {
        final List<MBeanAttributeInfo> attributesInfo = new ArrayList<MBeanAttributeInfo>();
        final List<MBeanOperationInfo> operationsInfo = new ArrayList<MBeanOperationInfo>();
        final Set<String> attributesName = new HashSet<String>();
        final ArrayList<Method> ops = new ArrayList<Method>();
        for (Method m : resource.getClass().getMethods()) {
            if (m.getDeclaringClass().equals(Object.class))
                continue;
            if (m.getName().startsWith("get") && !m.getName().equals("get") && !m.getName().equals("getClass")
                    && m.getParameterTypes().length == 0 && m.getReturnType() != void.class) {
                getters.put(m.getName().substring(3), m);
            } else if (m.getName().startsWith("is") && !m.getName().equals("is") && m.getParameterTypes().length == 0
                    && m.getReturnType() == boolean.class) {
                getters.put(m.getName().substring(2), m);
            } else if (m.getName().startsWith("set") && !m.getName().equals("set") && m.getParameterTypes().length == 1
                    && m.getReturnType().equals(void.class)) {
                setters.put(m.getName().substring(3), m);
            } else {
                ops.add(m);
            }
        }

        attributesName.addAll(getters.keySet());
        attributesName.addAll(setters.keySet());

        for (String attrName : attributesName) {
            final Method get = getters.get(attrName);
            Method set = setters.get(attrName);
            if (get != null && set != null && get.getReturnType() != set.getParameterTypes()[0]) {
                set = null;
                ops.add(setters.remove(attrName));
            }
            final MBeanAttributeInfo mbi = getAttributeInfo(attrName, get, set);
            if (mbi == null && get != null) {
                ops.add(getters.remove(attrName));
            }
            if (mbi == null && set != null) {
                ops.add(setters.remove(attrName));
            }
            if (mbi != null)
                attributesInfo.add(mbi);
        }

        for (Method m : ops) {
            final MBeanOperationInfo opi = getOperationInfo(m);
            if (opi == null)
                continue;
            operations.add(m);
            operationsInfo.add(opi);
        }
        return getMBeanInfo(resource, attributesInfo.toArray(new MBeanAttributeInfo[attributesInfo.size()]),
                operationsInfo.toArray(new MBeanOperationInfo[operationsInfo.size()]));
    }

    protected MBeanAttributeInfo getAttributeInfo(String attrName, Method get, Method set)
            throws IntrospectionException {
        return new MBeanAttributeInfo(attrName, attrName, get, set);
    }

    protected MBeanOperationInfo getOperationInfo(Method m) {
        if (m.getDeclaringClass() == Object.class)
            return null;
        return new MBeanOperationInfo(m.getName(), m);
    }

    protected MBeanInfo getMBeanInfo(Object resource, MBeanAttributeInfo[] attrs, MBeanOperationInfo[] ops) {
        return new MBeanInfo(resource.getClass().getName(), resource.getClass().getName(), attrs, null, ops, null);
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        final Method get = getters.get(attribute);
        if (get == null)
            throw new AttributeNotFoundException(attribute);
        try {
            return get.invoke(resource);
        } catch (IllegalArgumentException ex) {
            throw new ReflectionException(ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof Exception)
                throw new MBeanException((Exception) cause);
            throw new RuntimeErrorException((Error) cause);
        } catch (IllegalAccessException ex) {
            throw new ReflectionException(ex);
        }
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        final Method set = setters.get(attribute);
        if (set == null)
            throw new AttributeNotFoundException(attribute.getName());
        try {
            set.invoke(resource, attribute.getValue());
        } catch (IllegalArgumentException ex) {
            throw new ReflectionException(ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof Exception)
                throw new MBeanException((Exception) cause);
            throw new RuntimeErrorException((Error) cause);
        } catch (IllegalAccessException ex) {
            throw new ReflectionException(ex);
        }
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        if (attributes == null)
            return new AttributeList();
        final List<Attribute> result = new ArrayList<Attribute>(attributes.length);
        for (String attr : attributes) {
            final Method get = getters.get(attr);
            try {
                result.add(new Attribute(attr, get.invoke(resource)));
            } catch (Exception x) {
                continue;
            }
        }
        return new AttributeList(result);
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        if (attributes == null)
            return new AttributeList();
        final List<Attribute> result = new ArrayList<Attribute>(attributes.size());
        for (Object item : attributes) {
            final Attribute attr = (Attribute) item;
            final String name = attr.getName();
            final Method set = setters.get(name);
            try {
                set.invoke(resource, attr.getValue());
                final Method get = getters.get(name);
                final Object newval = (get == null) ? attr.getValue() : get.invoke(resource);
                result.add(new Attribute(name, newval));
            } catch (Exception x) {
                continue;
            }
        }
        return new AttributeList(result);
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException,
            ReflectionException {
        Method toInvoke = null;
        if (params == null)
            params = new Object[0];
        if (signature == null)
            signature = new String[0];
        for (Method m : operations) {
            if (!m.getName().equals(actionName))
                continue;
            final Class[] sig = m.getParameterTypes();
            if (sig.length == params.length) {
                if (sig.length == 0)
                    toInvoke = m;
                else if (signature.length == sig.length) {
                    toInvoke = m;
                    for (int i = 0; i < sig.length; i++) {
                        if (!sig[i].getName().equals(signature[i])) {
                            toInvoke = null;
                            break;
                        }
                    }
                }
            }
            if (toInvoke != null)
                break;
        }
        if (toInvoke == null)
            throw new ReflectionException(new NoSuchMethodException(actionName));
        try {
            return toInvoke.invoke(resource, params);
        } catch (IllegalArgumentException ex) {
            throw new ReflectionException(ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof Exception)
                throw new MBeanException((Exception) cause);
            throw new RuntimeErrorException((Error) cause);
        } catch (IllegalAccessException ex) {
            throw new ReflectionException(ex);
        }
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return info;
    }

    @Override
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void postRegister(Boolean registrationDone) {
        // TODO Auto-generated method stub

    }

    @Override
    public void preDeregister() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void postDeregister() {
        // TODO Auto-generated method stub

    }

}

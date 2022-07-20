package py.monitor.pojo.management.helper;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.DynamicMBean;

import py.monitor.pojo.util.Preconditions;

public class DynamicProxyFactory {
    /**
     * Non instantiable
     */
    private DynamicProxyFactory() {

    }

    @SuppressWarnings("unchecked")
    public static <T> T createDynamicProxy(DynamicMBean dynamicMBean, Class<T> type) throws IntrospectionException {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type},
                new DynamicMBeanInvokationHandler(dynamicMBean, type));
    }

    private static class DynamicMBeanInvokationHandler implements InvocationHandler {
        private Map<Method, String[]> operationSignatures = new HashMap<Method, String[]>();
        private Map<Method, String> getters = new HashMap<Method, String>();
        private Map<Method, String> setters = new HashMap<Method, String>();
        final DynamicMBean dynamicMBean;

        public DynamicMBeanInvokationHandler(DynamicMBean dynamicMBean, Class<?> type) {
            Preconditions.notNull(dynamicMBean);
            Preconditions.notNull(type);

            this.dynamicMBean = dynamicMBean;

            initializeSignatures(type);
        }

        private void initializeSignatures(Class<?> type) {
            BeanInfo beanInfo;
            try {
                beanInfo = Introspector.getBeanInfo(type);
            } catch (IntrospectionException e) {
                throw new RuntimeException(e);
            }
            // identify getters and setters (potential MBean attributes)
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                String property = propertyDescriptor.getName();
                addNotNull(getters, propertyDescriptor.getReadMethod(), property);
                addNotNull(setters, propertyDescriptor.getWriteMethod(), property);
            }

            // identify potential MBean operations
            // We could exclude getter and setter methods, but theoretically, they could be operations
            MethodDescriptor[] methodDescriptors = beanInfo.getMethodDescriptors();
            for (MethodDescriptor methodDescriptor : methodDescriptors) {
                Method method = methodDescriptor.getMethod();
                operationSignatures.put(method, createSignature(method.getParameterTypes()));
            }
        }

        private <T> void addNotNull(Map<Method, String> properties, Method method, String name) {
            if (method != null) {
                properties.put(method, name);
            }
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String getterAttribute = getters.get(method);
            if (getterAttribute != null) {
                return dynamicMBean.getAttribute(getterAttribute);
            }
            String setterAttribute = setters.get(method);
            if (setterAttribute != null) {
                if (args == null || args.length != 1) {
                    throw new IllegalArgumentException(
                            String.format("Setter method was called with != 1 arguments: %s", method));
                }
                dynamicMBean.setAttribute(new Attribute(setterAttribute, args[0]));
                return null; // return void
            }

            String[] operationSignature = operationSignatures.get(method);
            if (operationSignature != null) {
                return dynamicMBean.invoke(method.getName(), args, operationSignature);
            }
            throw new IllegalArgumentException("Unknown method: " + method);
        }

        /**
         * Convert the parameter types of Method.invoke() into the signature of DynamicMBean.invoke()
         * @param parameterTypes parameter types of Method.invoke()
         * @return signature of DynamicMBean.invoke()
         */
        private String[] createSignature(Class<?>[] parameterTypes) {
            // create signature from class array and insert in cache
            String[] signature = new String[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                signature[i] = parameterTypes[i].getName();
            }
            return signature;
        }

    }
}

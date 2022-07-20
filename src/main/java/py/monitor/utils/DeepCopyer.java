package py.monitor.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DeepCopyer {
    private static final Logger logger = LoggerFactory.getLogger(DeepCopyer.class);

    // Map for mapping object references between <i>original</i> and <i>clone</i>
    private final static ThreadLocal<Map<String, Object>> references = new ThreadLocal<>();

    // Map which contains a default wrapped values for a primitive types
    private final static Map<Class<?>, Object> primitiveWrappersMap = new HashMap<>();

    // An un-instantiable classes
    private final static Set<Class<?>> uninstantiableClasses = new HashSet<>();

    static {
        primitiveWrappersMap.put(boolean.class, Boolean.FALSE);
        primitiveWrappersMap.put(byte.class, (byte) 0);
        primitiveWrappersMap.put(short.class, (short) 0);
        primitiveWrappersMap.put(char.class, (char) 0);
        primitiveWrappersMap.put(int.class, 0);
        primitiveWrappersMap.put(long.class, 0L);
        primitiveWrappersMap.put(float.class, 0f);
        primitiveWrappersMap.put(double.class, (double) 0);

        uninstantiableClasses.add(Class.class);
        uninstantiableClasses.add(Void.class);
    }

    /**
     * Create a deep copy of the given {@code obj} by using reflections
     * 
     * @param obj
     *            object for copying
     * @return a deep copy of the given object
     * @throws ReflectiveOperationException
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(final T obj) throws ReflectiveOperationException {
        try {
            // set a new threadlocal references map
            references.set(new HashMap<String, Object>());
            Class<T> clazz = (Class<T>) obj.getClass();
            return getClone(obj, clazz);
        } finally {
            // helps to GC
            references.get().clear();
            references.remove();
        }
    }

    /**
     * Try to create a deep copy of the given {@code obj} by the following ways:<br>
     * - in first, by calling clone() method,<br>
     * - in second, try to use a copy constructor<br>
     * - and, lastly, by calling deepCopy(obj)
     * 
     * @param obj
     *            object for copying
     * @return a deep copy of the given object
     * @throws ReflectiveOperationException
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T cloneByCommonWay(final T obj) throws ReflectiveOperationException {
        if (obj == null)
            return null;

        Class<?> clazz = obj.getClass();

        // try to use a clone() method
        if (obj instanceof Cloneable) {
            try {
                // find appropriate clone(), takes no parameters,
                final Method m = clazz.getDeclaredMethod("clone");
                // and returns type instance of the obj
                if (m.getReturnType().isInstance(obj)) {
                    // for using protected method clone()
                    SetMethodAccessible<T> accessible = new SetMethodAccessible<>(m, obj);
                    T copy = AccessController.doPrivileged(accessible);
                    logger.debug("Returns a copy by clone method");
                    return copy;
                }
            } catch (PrivilegedActionException e) {
                logger.error("Attempt to use a clone method failed: {}", e);
            }
        }

        // Try to use a copy constructor
        final Constructor<T> ctor;
        final Class<?>[] paramTypes = new Class[] { clazz };
        try {
            ctor = (Constructor<T>) clazz.getDeclaredConstructor(paramTypes);
            // for using private constructor
            ctor.setAccessible(true);
            T copy = ctor.newInstance(obj);
            logger.debug("Returns a copy by copy constructor");
            return copy;
        } catch (ReflectiveOperationException e) {
            logger.error("Attempt to use a copy constructor failed: {}", e);
        }
        return clone(obj);
    }

    private DeepCopyer() {
    }

    /**
     * Add object references between <i>original</i> and <i>clone</i> to the {@link #references} map.
     * 
     * @param original
     *            original object
     * @param copy
     *            clone object
     */
    private static void addToReferencesMap(Object original, Object copy) {
        String key = getUniqueName(original);
        references.get().put(key, copy);
    }

    /**
     * Check if that class is uninstantiable
     * 
     * @param clazz
     *            checked class
     * @return {@code true} if that class is uninstantiable
     */
    private static boolean isUninstantiable(Class<?> clazz) {
        return uninstantiableClasses.contains(clazz);
    }

    /**
     * Constructs a new object instance using the constructor of a given class
     * 
     * @param clazz
     *            class of a new object
     * @return a new object instance of a given class
     * @throws ReflectiveOperationException
     */
    private static Object constractNewObject(Class<?> clazz) throws ReflectiveOperationException {
        // try to use default constructor (it's cached in class)
        try {
            return clazz.newInstance();
        } catch (ReflectiveOperationException e) {
            // do nothing
        }

        // gets all the declared constructors and try to use them in turn
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        if (constructors.length > 0) {
            for (Constructor<?> constructor : constructors) {
                Class<?>[] parameters = constructor.getParameterTypes();
                Object[] args = new Object[parameters.length];

                for (int i = 0; i < parameters.length; i++) {
                    // null or a primitive defaults
                    args[i] = parameters[i].isPrimitive() ? getPrimitiveDefault(parameters[i]) : null;
                }

                try {
                    // for using private constructors
                    constructor.setAccessible(true);
                    return constructor.newInstance(args);
                } catch (ReflectiveOperationException e) {
                    // go to next constructor
                    continue;
                }
            }
        }
        return null;
    }

    /**
     * Creates a copy of the given array
     * 
     * @param array
     *            array for copying
     * @return copy of the given array
     * @throws ReflectiveOperationException
     */
    private static Object copyArray(Object array) throws ReflectiveOperationException {
        Class<?> arrayType = array.getClass().getComponentType();
        int length = Array.getLength(array);
        Object arrayCopy = Array.newInstance(arrayType, length);

        // arrays is the objects too
        addToReferencesMap(array, arrayCopy);

        for (int i = 0; i < length; i++) {
            Object value = Array.get(array, i);
            Object cloneValue = getClone(value, arrayType);
            Array.set(arrayCopy, i, cloneValue);
        }
        return arrayCopy;
    }

    /**
     * Deep copy the values from the given {@code fields} of the {@code fromObj} to the {@code toObj}.
     * 
     * @param fields
     *            array of the fields declared by the class of the {@code fromObj}
     * @param fromObj
     *            source for reading values
     * @param toObj
     *            destination for writing values
     * @throws ReflectiveOperationException
     */
    private static <T> void copyFieldValues(Field[] fields, T fromObj, T toObj) throws ReflectiveOperationException {
        Object value;
        Object cloneValue;
        for (Field field : fields) {
            // for settings private fields
            field.setAccessible(true);
            Class<?> fieldClazz = field.getType();

            value = field.get(fromObj);
            cloneValue = getClone(value, fieldClazz);
            logger.debug("{}", field);
            field.set(toObj, cloneValue);
        }
    }

    /**
     * Creates a copy of the given object.
     * 
     * @param obj
     *            object for copying
     * @param clazz
     *            class of the object
     * @return a deep copy of the given object
     * @throws ReflectiveOperationException
     */
    private static Object copyObject(Object obj, Class<?> clazz) throws ReflectiveOperationException {
        Object copy = constractNewObject(clazz);

        addToReferencesMap(obj, copy);

        Field[] fields = getFields(clazz);
        copyFieldValues(fields, obj, copy);
        return copy;
    }

    /**
     * Returns a clone of the given object
     * 
     * @param original
     *            object for copying
     * @param clazz
     *            class of the {@code original} object
     * @return a deep copy of the given object
     * @throws ReflectiveOperationException
     */
    @SuppressWarnings("unchecked")
    private static <T> T getClone(T original, Class<?> clazz) throws ReflectiveOperationException {
        if (original == null)
            return null;

        Object cloneValue;
        boolean isPrimitive = clazz.isPrimitive();

        if (!isPrimitive) {
            // for objects - trying to give value from the references map
            if ((cloneValue = getFromReferencesMap(original)) != null)
                return (T) cloneValue;
        }

        // workaround for suppressing calling of a Wrappers
        Class<?> valueType = isPrimitive ? clazz : original.getClass();

        if (valueType.isArray()) {
            cloneValue = copyArray(original);
        } else if (isPrimitive || valueType.isEnum() || isUninstantiable(valueType)) {
            cloneValue = original;
        } else {
            cloneValue = copyObject(original, valueType);
        }
        return (T) cloneValue;
    }

    /**
     * Returns an array of {@code Field} objects reflecting all the fields
     * declared by the given {@code class}
     * 
     * @param clazz
     *            {@code Class} object
     * @return array of {@code Field} objects
     */
    private static Field[] getFields(Class<?> clazz) {
        List<Field> result = new LinkedList<>();
        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                // static fields are not needed
                if (!Modifier.isStatic(field.getModifiers())) {
                    result.add(field);
                }
            }
            // get up to the Superclass
            clazz = clazz.getSuperclass();
        }
        return result.toArray(new Field[result.size()]);
    }

    /**
     * Returns the object referenced by the given {@code obj}, or {@code null} if the references map contains no mapping
     * for the {@code obj}.
     * 
     * @param obj
     *            the key object
     * @return referenced object
     */
    private static Object getFromReferencesMap(Object obj) {
        String key = getUniqueName(obj);
        Object result = references.get().get(key);

        return result; // obj.getClass().isInstance(result) ? result : null;
    }

    /**
     * Returns the wrapper object of a given primitive {@code Class} with the
     * default value
     * 
     * @param primitiveClazz
     *            primitive {@code Class}
     * @return the wrapper object of a primitive with the default value
     */
    private static Object getPrimitiveDefault(Class<?> primitiveClazz) {
        return primitiveWrappersMap.get(primitiveClazz);
    }

    /**
     * Returns the unique name of the given object
     * 
     * @param obj
     *            the object
     * @return the unique name of the given object as a {@code String}
     */
    private static String getUniqueName(Object obj) {
        return obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
    }

    private final static class SetMethodAccessible<T> implements PrivilegedExceptionAction<T> {
        final private Method method;
        final private Object args;

        SetMethodAccessible(Method method, Object args) {
            this.method = method;
            this.args = args;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T run() throws ReflectiveOperationException {
            method.setAccessible(true);
            return (T) method.invoke(args);
        }
    }
}

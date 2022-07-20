package py.common;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

public class ReflectionUtils {
    // private static final Log logger = LogFactory.getLog(Utils.class);
    private static final Logger logger = Logger.getLogger(ReflectionUtils.class);

    /**
     * When {@code Type} initialized with a value of an object, its fully qualified class name
     * will be prefixed with this.
     * 
     * @see {@link ReflectionUtil#getClassName(Type)}
     */
    private static final String TYPE_CLASS_NAME_PREFIX = "class ";
    private static final String TYPE_INTERFACE_NAME_PREFIX = "interface ";

    /*
     * Utility class with static access methods, no need for constructor.
     */
    private ReflectionUtils() {
    }

    /**
     * {@link Type#toString()} value is the fully qualified class name prefixed
     * with {@link ReflectionUtil#TYPE_NAME_PREFIX}. This method will substring it, for it to be eligible
     * for {@link Class#forName(String)}.
     * 
     * @param type
     *            the {@code Type} value whose class name is needed.
     * @return {@code String} class name of the invoked {@code type}.
     * 
     * @see {@link ReflectionUtil#getClass()}
     */
    public static String getClassName(Type type) {
        if (type == null) {
            return "";
        }
        String className = type.toString();
        if (className.startsWith(TYPE_CLASS_NAME_PREFIX)) {
            className = className.substring(TYPE_CLASS_NAME_PREFIX.length());
        } else if (className.startsWith(TYPE_INTERFACE_NAME_PREFIX)) {
            className = className.substring(TYPE_INTERFACE_NAME_PREFIX.length());
        }
        return className;
    }

    /**
     * Returns the {@code Class} object associated with the given {@link Type} depending on its fully qualified name.
     * 
     * @param type
     *            the {@code Type} whose {@code Class} is needed.
     * @return the {@code Class} object for the class with the specified name.
     * 
     * @throws ClassNotFoundException
     *             if the class cannot be located.
     * 
     * @see {@link ReflectionUtil#getClassName(Type)}
     */
    public static Class<?> getClass(Type type) throws ClassNotFoundException {
        String className = getClassName(type);
        if (className == null || className.isEmpty()) {
            return null;
        }
        return Class.forName(className);
    }

    /**
     * Creates a new instance of the class represented by this {@code Type} object.
     * 
     * @param type
     *            the {@code Type} object whose its representing {@code Class} object
     *            will be instantiated.
     * @return a newly allocated instance of the class represented by
     *         the invoked {@code Type} object.
     * 
     * @throws ClassNotFoundException
     *             if the class represented by this {@code Type} object
     *             cannot be located.
     * @throws InstantiationException
     *             if this {@code Type} represents an abstract class,
     *             an interface, an array class, a primitive type, or void;
     *             or if the class has no nullary constructor;
     *             or if the instantiation fails for some other reason.
     * @throws IllegalAccessException
     *             if the class or its nullary constructor is not accessible.
     * 
     * @see {@link Class#newInstance()}
     */
    public static Object newInstance(Type type) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        Class<?> clazz = getClass(type);
        if (clazz == null) {
            return null;
        }
        return clazz.newInstance();
    }

    /**
     * Returns an array of {@code Type} objects representing the actual type
     * arguments to this object.
     * If the returned value is null, then this object represents a non-parameterized
     * object.
     * 
     * @param object
     *            the {@code object} whose type arguments are needed.
     * @return an array of {@code Type} objects representing the actual type
     *         arguments to this object.
     * 
     * @see {@link Class#getGenericSuperclass()}
     * @see {@link ParameterizedType#getActualTypeArguments()}
     */
    public static Type[] getParameterizedTypes(Object object) {
        Type superclassType = object.getClass().getGenericSuperclass();
        if (!ParameterizedType.class.isAssignableFrom(superclassType.getClass())) {
            return null;
        }

        return ((ParameterizedType) superclassType).getActualTypeArguments();
    }

    /**
     * Checks whether a {@code Constructor} object with no parameter types is specified
     * by the invoked {@code Class} object or not.
     * 
     * @param clazz
     *            the {@code Class} object whose constructors are checked.
     * @return {@code true} if a {@code Constructor} object with no parameter types is specified.
     * @throws SecurityException
     *             If a security manager, <i>s</i> is present and any of the
     *             following conditions is met:
     *             <ul>
     *             <li>invocation of {@link SecurityManager#checkMemberAccess
     *             s.checkMemberAccess(this, Member.PUBLIC)} denies access to the constructor
     * 
     *             <li>the caller's class loader is not the same as or an ancestor of the class loader for the current
     *             class and invocation of {@link SecurityManager#checkPackageAccess
     *             s.checkPackageAccess()} denies access to the package of this class
     *             </ul>
     * 
     * @see {@link Class#getConstructor(Class...)}
     */
    public static boolean hasDefaultConstructor(Class<?> clazz) throws SecurityException {
        Class<?>[] empty = {};
        try {
            clazz.getConstructor(empty);
        } catch (NoSuchMethodException e) {
            return false;
        }
        return true;
    }

    /**
     * Returns a {@code Class} object that identifies the
     * declared class for the field represented by the given {@code String name} parameter inside
     * the invoked {@code Class<?> clazz} parameter.
     * 
     * @param clazz
     *            the {@code Class} object whose declared fields to be
     *            checked for a certain field.
     * @param name
     *            the field name as {@code String} to be
     *            compared with {@link Field#getName()}
     * @return the {@code Class} object representing the type of given field name.
     * 
     * @see {@link Class#getDeclaredFields()}
     * @see {@link Field#getType()}
     */
    public static Class<?> getFieldClass(Class<?> clazz, String name) {
        if (clazz == null || name == null || name.isEmpty()) {
            return null;
        }

        Class<?> propertyClass = null;

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getName().equalsIgnoreCase(name)) {
                propertyClass = field.getType();
                break;
            }
        }

        return propertyClass;
    }

    /**
     * Returns a {@code Class} object that identifies the
     * declared class as a return type for the method represented by the given {@code String name} parameter inside the
     * invoked {@code Class<?> clazz} parameter.
     * 
     * @param clazz
     *            the {@code Class} object whose declared methods to be
     *            checked for the wanted method name.
     * @param name
     *            the method name as {@code String} to be
     *            compared with {@link Method#getName()}
     * @return the {@code Class} object representing the return type of the given method name.
     * 
     * @see {@link Class#getDeclaredMethods()}
     * @see {@link Method#getReturnType()}
     */
    public static Class<?> getMethodReturnType(Class<?> clazz, String name) {
        if (clazz == null || name == null || name.isEmpty()) {
            return null;
        }

        name = name.toLowerCase();
        Class<?> returnType = null;

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                returnType = method.getReturnType();
                break;
            }
        }

        return returnType;
    }

    /**
     * Extracts the enum constant of the specified enum class with the
     * specified name. The name must match exactly an identifier used
     * to declare an enum constant in the given class.
     * 
     * @param clazz
     *            the {@code Class} object of the enum type from which
     *            to return a constant.
     * @param name
     *            the name of the constant to return.
     * @return the enum constant of the specified enum type with the
     *         specified name.
     * 
     * @throws IllegalArgumentException
     *             if the specified enum type has
     *             no constant with the specified name, or the specified
     *             class object does not represent an enum type.
     * 
     * @see {@link Enum#valueOf(Class, String)}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object getEnumConstant(Class<?> clazz, String name) {
        if (clazz == null || name == null || name.isEmpty()) {
            return null;
        }
        return Enum.valueOf((Class<Enum>) clazz, name);
    }

    /**
     * find a class method by its name and its arguments. If checkArgs is false, then no args will be checked.
     * 
     * @param clazz
     * @param methodName
     * @param args
     * @param checkInheritance
     * @param checkArgs
     * @param strictArgs
     * @return
     * @throws NoSuchMethodException
     */
    public static Method findMethod(Class clazz, String methodName, Class[] args, boolean checkInheritance,
            boolean checkArgs, boolean strictArgs) throws NoSuchMethodException {
        if (clazz == null)
            throw new NoSuchMethodException("No class");
        if (methodName == null || methodName.trim().equals(""))
            throw new NoSuchMethodException("No method name");

        Method method = null;
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length && method == null; i++) {
            if (methods[i].getName().equals(methodName)
                    && (!checkArgs || (checkParams(methods[i].getParameterTypes(), (args == null ? new Class[] {}
                            : args), strictArgs)))) {
                method = methods[i];
            }
        }

        if (method != null) {
            return method;
        } else if (checkInheritance)
            return findInheritedMethod(clazz.getPackage(), clazz.getSuperclass(), methodName, args, strictArgs);
        else
            throw new NoSuchMethodException("No such method " + methodName + " on class " + clazz.getName());

    }

    public static Field findField(Class clazz, String targetName, Class targetType, boolean checkInheritance,
            boolean strictType) throws NoSuchFieldException {
        if (clazz == null)
            throw new NoSuchFieldException("No class");
        if (targetName == null)
            throw new NoSuchFieldException("No field name");

        try {
            Field field = clazz.getDeclaredField(targetName);
            if (strictType) {
                if (field.getType().equals(targetType))
                    return field;
            } else {
                if (field.getType().isAssignableFrom(targetType))
                    return field;
            }
            if (checkInheritance) {
                return findInheritedField(clazz.getPackage(), clazz.getSuperclass(), targetName, targetType, strictType);
            } else
                throw new NoSuchFieldException("No field with name " + targetName + " in class " + clazz.getName()
                        + " of type " + targetType);
        } catch (NoSuchFieldException e) {
            return findInheritedField(clazz.getPackage(), clazz.getSuperclass(), targetName, targetType, strictType);
        }
    }

    public static boolean isInheritable(Package pack, Member member) {
        if (pack == null)
            return false;
        if (member == null)
            return false;

        int modifiers = member.getModifiers();
        if (Modifier.isPublic(modifiers))
            return true;
        if (Modifier.isProtected(modifiers))
            return true;
        if (!Modifier.isPrivate(modifiers) && pack.equals(member.getDeclaringClass().getPackage()))
            return true;

        return false;
    }

    public static boolean checkParams(Class[] formalParams, Class[] actualParams, boolean strict) {
        if (formalParams == null && actualParams == null)
            return true;
        if (formalParams == null && actualParams != null)
            return false;
        if (formalParams != null && actualParams == null)
            return false;

        if (formalParams.length != actualParams.length)
            return false;

        if (formalParams.length == 0)
            return true;

        int j = 0;
        if (strict) {
            while (j < formalParams.length && formalParams[j].equals(actualParams[j]))
                j++;
        } else {
            while ((j < formalParams.length) && (formalParams[j].isAssignableFrom(actualParams[j]))) {
                j++;
            }
        }

        if (j != formalParams.length) {
            return false;
        }

        return true;
    }

    public static boolean isSameSignature(Method methodA, Method methodB) {
        if (methodA == null)
            return false;
        if (methodB == null)
            return false;

        List parameterTypesA = Arrays.asList(methodA.getParameterTypes());
        List parameterTypesB = Arrays.asList(methodB.getParameterTypes());

        if (methodA.getName().equals(methodB.getName()) && parameterTypesA.containsAll(parameterTypesB))
            return true;

        return false;
    }

    public static boolean isTypeCompatible(Class formalType, Class actualType, boolean strict) {
        if (formalType == null && actualType != null)
            return false;
        if (formalType != null && actualType == null)
            return false;
        if (formalType == null && actualType == null)
            return true;

        if (strict)
            return formalType.equals(actualType);
        else
            return formalType.isAssignableFrom(actualType);
    }

    public static boolean containsSameMethodSignature(Method method, Class c, boolean checkPackage) {
        if (checkPackage) {
            if (!c.getPackage().equals(method.getDeclaringClass().getPackage()))
                return false;
        }

        boolean samesig = false;
        Method[] methods = c.getDeclaredMethods();
        for (int i = 0; i < methods.length && !samesig; i++) {
            if (isSameSignature(method, methods[i]))
                samesig = true;
        }
        return samesig;
    }

    public static boolean containsSameFieldName(Field field, Class c, boolean checkPackage) {
        if (checkPackage) {
            if (!c.getPackage().equals(field.getDeclaringClass().getPackage()))
                return false;
        }

        boolean sameName = false;
        Field[] fields = c.getDeclaredFields();
        for (int i = 0; i < fields.length && !sameName; i++) {
            if (fields[i].getName().equals(field.getName()))
                sameName = true;
        }
        return sameName;
    }

    protected static Method findInheritedMethod(Package pack, Class clazz, String methodName, Class[] args,
            boolean strictArgs) throws NoSuchMethodException {
        if (clazz == null)
            throw new NoSuchMethodException("No class");
        if (methodName == null)
            throw new NoSuchMethodException("No method name");

        Method method = null;
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length && method == null; i++) {
            if (methods[i].getName().equals(methodName) && isInheritable(pack, methods[i])
                    && checkParams(methods[i].getParameterTypes(), args, strictArgs))
                method = methods[i];
        }
        if (method != null) {
            return method;
        } else
            return findInheritedMethod(clazz.getPackage(), clazz.getSuperclass(), methodName, args, strictArgs);
    }

    protected static Field findInheritedField(Package pack, Class clazz, String fieldName, Class fieldType,
            boolean strictType) throws NoSuchFieldException {
        if (clazz == null)
            throw new NoSuchFieldException("No class");
        if (fieldName == null)
            throw new NoSuchFieldException("No field name");
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if (isInheritable(pack, field) && isTypeCompatible(fieldType, field.getType(), strictType))
                return field;
            else
                return findInheritedField(clazz.getPackage(), clazz.getSuperclass(), fieldName, fieldType, strictType);
        } catch (NoSuchFieldException e) {
            return findInheritedField(clazz.getPackage(), clazz.getSuperclass(), fieldName, fieldType, strictType);
        }
    }

}

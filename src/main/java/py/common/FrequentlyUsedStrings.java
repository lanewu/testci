package py.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Similar to String.intern() but it stores String objects besides String literal.
 * 
 * This class can be used for various AppName, and Location.Cluster & Location.Group.
 * 
 * @author chenlia 
 */
public class FrequentlyUsedStrings {
    protected static final Logger log = LoggerFactory.getLogger(FrequentlyUsedStrings.class);

    protected static final int MAX_ENTRIES = 200;

    private static int maxEntries = MAX_ENTRIES;

    private static ConcurrentMap<String, String> stringPoool = new ConcurrentHashMap<String, String>();

    /***
     * If provided string is a frequently used string, return the shared String object
     * for that value, otherwise return the provided value.
     * @param s
     * @return Shared String object if <code>s</code> is a well-known value.
     */
    static public String get(String s) {
        if (s == null)
            return null;
        String known = stringPoool.get(s);
        if (known != null)
            return known;
        // Although we don't synchronize maxEntries, that is ok. We don't need to be perfectly accurate.
        if (stringPoool.size() < maxEntries) {
            known = stringPoool.putIfAbsent(s, s);
            if (known != null)
                return known;
            // else return s
        }
        return s;
    }

    /***
     * @return maximum count of distinct values that can be pooled.
     */
    static public int getMaxEntries() {
        return maxEntries;
    }

    /***
     * Set the cap for the max number of pooled strings.
     */
    static public void setMaxEntries(int max) {
        maxEntries = max;
    }
}

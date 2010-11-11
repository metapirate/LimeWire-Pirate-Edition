package org.limewire.logging;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Mimics and wraps {@link org.apache.commons.logging.LogFactory} and implements a
 * subset of its methods, but returns {@link Log} instead of 
 * {@link org.apache.commons.logging.Log} instances.
 */
public class LogFactory {
    
    private static Map<org.apache.commons.logging.Log, Log> logs = new IdentityHashMap<org.apache.commons.logging.Log, Log>();

    /**
     * @param clazz can not be null
     * @return a Log named by the fully qualifed class name of clazz
     */
    public static Log getLog(Class clazz) {
        return getLog(clazz, null);
    }

    /**
     * @param key can not be null
     * @return a Log named by the key
     */    
    public static Log getLog(String key) {
        return getLog(null, key);
    }

    /**
     * @param clazz can be null if keyOrCategory is non-null
     * @param keyOrCategory can be null if clazz is non-null
     * @return a log with the name <code>keyOrCategory.classname</code>
     * where <code>classname</code> is the non-qualified name of the 
     * class.
     */
    public static Log getLog(Class clazz, String keyOrCategory) {
        assert clazz != null || keyOrCategory != null;
        
        // not synchronized as LogFactory is also not synchronized, Logs are created
        // at class load time which is synchronized
        org.apache.commons.logging.Log log;
        if (keyOrCategory == null) {
            log = org.apache.commons.logging.LogFactory.getLog(clazz);
        } else if(clazz == null) {
            log = org.apache.commons.logging.LogFactory.getLog(keyOrCategory);
        } else {
            log = org.apache.commons.logging.LogFactory.getLog(keyOrCategory + "." + clazz.getSimpleName());
        }
        Log decorator = logs.get(log);
        if (decorator == null) {
            decorator = new LogImpl(log);
            logs.put(log, decorator);
        }
        return decorator;
    }
    
}

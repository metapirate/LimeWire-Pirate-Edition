package org.limewire.util;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

/**
 * A utility class to register and unregister JMX Beans
 */
public class JmxUtils {

    private static final Log LOG = LogFactory.getLog(JmxUtils.class);
    
    private JmxUtils() {}
    
    /**
     * Creates and returns a JMX {@link ObjectName} from the given arguments.
     */
    public static ObjectName createName(Class<?> clazz, String property) 
            throws MalformedObjectNameException {
        
        Package pkg = clazz.getPackage();
        String type = clazz.getSimpleName();
        
        if (pkg != null) {
            return ObjectName.getInstance(pkg.getName() 
                    + ":type=" + type + ",name=" + property);
        }
        
        return ObjectName.getInstance(type + ":name=" + property);
    }
    
    /**
     * Registers the given JMX Bean
     */
    public static boolean add(Class<?> clazz, String property, Object bean) {
        try {
            return add(createName(clazz, property), bean);
        } catch (MalformedObjectNameException e) {
            LOG.error("MalformedObjectNameException", e);
        }
        return false;
    }
    
    /**
     * Registers the given JMX Bean
     */
    public static boolean add(String name, Object bean) {
        try {
            return add(ObjectName.getInstance(name), bean);
        } catch (MalformedObjectNameException e) {
            LOG.error("MalformedObjectNameException", e);
        }
        return false;
    }
    
    /**
     * Registers the given JMX Bean
     */
    public static boolean add(ObjectName name, Object bean) {
        Objects.nonNull(name, "name");
        Objects.nonNull(bean, "bean");
        
        MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
        
        try {
            beanServer.registerMBean(bean, name);
            return true;
        } catch (InstanceAlreadyExistsException e) {
            LOG.error("InstanceAlreadyExistsException", e);
        } catch (MBeanRegistrationException e) {
            LOG.error("MBeanRegistrationException", e);
        } catch (NotCompliantMBeanException e) {
            LOG.error("NotCompliantMBeanException", e);
        }
        
        return false;
    }
    
    /**
     * Unregisters the given JMX Bean
     */
    public static boolean remove(Class<?> clazz, String property) {
        try {
            return remove(createName(clazz, property));
        } catch (MalformedObjectNameException e) {
            LOG.error("MalformedObjectNameException", e);
        }
        return false;
    }
    
    /**
     * Unregisters the given JMX Bean
     */
    public static boolean remove(String property) {
        try {
            return remove(ObjectName.getInstance(property));
        } catch (MalformedObjectNameException e) {
            LOG.error("MalformedObjectNameException", e);
        }
        return false;
    }
    
    /**
     * Unregisters the given JMX Bean
     */
    public static boolean remove(ObjectName name) {
        Objects.nonNull(name, "name");
        
        MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
        
        try {
            beanServer.unregisterMBean(name);
            return true;
        } catch (MBeanRegistrationException e) {
            LOG.error("MBeanRegistrationException", e);
        } catch (InstanceNotFoundException e) {
            LOG.error("InstanceNotFoundException", e);
        }
        
        return false;
    }
}

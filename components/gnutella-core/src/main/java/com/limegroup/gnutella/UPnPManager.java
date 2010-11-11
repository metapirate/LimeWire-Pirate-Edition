package com.limegroup.gnutella;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.device.DeviceChangeListener;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.NetworkUtils;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.service.ErrorService;

import com.google.inject.Inject;


/**
 * Manages the mapping of ports to limewire on UPnP-enabled routers.  
 * 
 * According to the UPnP Standards, Internet Gateway Devices must have a
 * specific hierarchy.  The parts of that hierarchy that we care about are:
 * 
 * Device: urn:schemas-upnp-org:device:InternetGatewayDevice:1
 * 	 SubDevice: urn:schemas-upnp-org:device:WANDevice:1
 *     SubDevice: urn:schemas-upnp-org:device:WANConnectionDevice:1
 *        Service: urn:schemas-upnp-org:service:WANIPConnection:1
 * 
 * Every port mapping is a tuple of:
 *  - External address ("" is wildcard)
 *  - External port
 *  - Internal address
 *  - Internal port
 *  - Protocol (TCP|UDP)
 *  - Description
 * 
 * Port mappings can be removed, but that is a blocking network operation which will
 * slow down the shutdown process of Limewire.  It is safe to let port mappings persist 
 * between limewire sessions. In the meantime however, the NAT may assign a different 
 * ip address to the local node.  That's why we need to find any previous mappings 
 * the node has created and update them with our new address. In order to uniquely 
 * distinguish which mappings were made by us, we put part of our client GUID in the 
 * description field.  
 * 
 * For the TCP mapping, we use the following description: "Lime/TCP:<cliengGUID>"
 * For the UDP mapping, we use "Lime/UDP:<clientGUID>"
 * 
 * NOTES:
 * 
 * Not all NATs support mappings with different external port and internal ports. Therefore
 * if we were unable to map our desired port but were able to map another one, we should
 * pass this information on to Acceptor. 
 * 
 * Some buggy NATs do not distinguish mappings by the Protocol field.  Therefore
 * we first map the UDP port, and then the TCP port since it is more important should the
 * first mapping get overwritten.
 * 
 * The cyberlink library uses an internal thread that tries to discover any UPnP devices.  
 * After we discover a router or give up on trying to, we should call stop().
 * 
 */
@EagerSingleton
public class UPnPManager implements org.limewire.lifecycle.Service {
    // TODO move to net
    private static final Log LOG = LogFactory.getLog(UPnPManager.class);
	
	/** some schemas */
	private static final String ROUTER_DEVICE= 
		"urn:schemas-upnp-org:device:InternetGatewayDevice:1";
	private static final String WAN_DEVICE = 
		"urn:schemas-upnp-org:device:WANDevice:1";
	private static final String WANCON_DEVICE=
		"urn:schemas-upnp-org:device:WANConnectionDevice:1";
	private static final String SERVICE_TYPE = 
		"urn:schemas-upnp-org:service:WANIPConnection:1";
	
	/** prefixes and a suffix for the descriptions of our TCP and UDP mappings */
	private static final String TCP_PREFIX = "LimeTCP";
	private static final String UDP_PREFIX = "LimeUDP";
	private String _guidSuffix;
	
	/** 
	 * the router we have and the sub-device necessary for port mapping 
	 *  LOCKING: DEVICE_LOCK
	 */
	private volatile Device _router;
	
	/**
	 * The port-mapping service we'll use.  LOCKING: DEVICE_LOCK
	 */
	private volatile Service _service;
	
	/** The tcp and udp mappings created this session */
	private volatile Mapping _tcp, _udp;
	
	/**
	 * Lock that everything uses.
	 */
	private final Object DEVICE_LOCK = new Object();
	
	private final AtomicBoolean started = new AtomicBoolean(false);
	
	private final ControlPoint controlPoint;
	
	private final CopyOnWriteArrayList<UPnPListener> listeners = new CopyOnWriteArrayList<UPnPListener>();
    
    private final UPnPManagerConfiguration configuration;
	
	@Inject
	UPnPManager(UPnPManagerConfiguration configuration) {
        this.configuration = configuration;
        this.controlPoint = new ControlPoint();
    }
	
	public void addListener(UPnPListener uPnPListener) {
	    listeners.add(uPnPListener);
	}
	
	private void notifyListeners() {
	    for(UPnPListener listener : listeners) 
	        listener.natFound();
	}

    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this).in(ServiceStage.VERY_LATE);
    }

    @Override
    public void initialize() {
    }

    @Override
    public String getServiceName() {
        return "UPnPManager";
    }

    public void start() {
        if (!ConnectionSettings.DISABLE_UPNP.getValue()) {
            if (!started.getAndSet(true)) {
                LOG.debug("Starting UPnP Manager.");
                controlPoint.addDeviceChangeListener(new DeviceListener());

                synchronized (DEVICE_LOCK) {
                    try {
                        controlPoint.start();
                    } catch (Exception bad) {
                        configuration.setEnabled(false);
                        ErrorService.error(bad);
                    }
                }
            }
        }
    }
    
    public void stop() {
        controlPoint.stop();
    }
	
	/**
     * @return whether we are behind an UPnP-enabled NAT/router
     */
	public boolean isNATPresent() {
	    return _router != null && _service != null;
	}

	/**
	 * @return whether we have created mappings this session
	 */
	public boolean mappingsExist() {
	    return _tcp != null || _udp != null;
	}
	
	/**
	 * @return the external address the NAT thinks we have.  Blocking.
	 * null if we can't find it.
	 */
	public InetAddress getNATAddress() throws UnknownHostException {
		
        if (!isNATPresent())
            return null;
        
        Action getIP = getActionFromService(_service, "GetExternalIPAddress");
		if(getIP == null) {
		    LOG.debug("Couldn't find GetExternalIPAddress action!");
		    return null;
		}
		    
		
		if (!getIP.postControlAction()) {
			LOG.debug("couldn't get our external address");
			return null;
		}
		
		Argument ret = getIP.getOutputArgumentList().getArgument("NewExternalIPAddress");
		return InetAddress.getByName(ret.getValue());
	}
	
	/**
	 * Traverses the structure of the router device looking for 
	 * the port mapping service.
	 */
	private void discoverService() {
		
		for (Iterator iter = _router.getDeviceList().iterator();iter.hasNext();) {
			Device current = (Device)iter.next();
			if (!current.getDeviceType().equals(WAN_DEVICE))
				continue;
			
			DeviceList l = current.getDeviceList();
			if (LOG.isDebugEnabled())
				LOG.debug("found "+current.getDeviceType()+", size: "+l.size() + ", on: " + current.getFriendlyName());
			
			for (int i=0;i<current.getDeviceList().size();i++) {
				Device current2 = l.getDevice(i);
				
				if (!current2.getDeviceType().equals(WANCON_DEVICE))
					continue;
			
				if (LOG.isDebugEnabled())
					LOG.debug("found "+current2.getDeviceType() + ", on: " + current2.getFriendlyName());
				
				_service = current2.getService(SERVICE_TYPE);
				return;
			}
		}
	}
	
	/**
	 * adds a mapping on the router to the specified port
	 * @return the external port that was actually mapped. 0 if failed
	 */
	public int mapPort(int port, byte[] address) {
	    if(LOG.isTraceEnabled())
	        LOG.trace("Attempting to map port: " + port);
		
		Random gen=null;
		
		String localAddress = NetworkUtils.ip2string(address);
		int localPort = port;
	
		// try adding new mappings with the same port
		Mapping udp = new Mapping("",
				port,
				localAddress,
				localPort,
				"UDP",
				UDP_PREFIX + getGUIDSuffix());
		
		// add udp first in case it gets overwritten.
		// if we can't add, update or find an appropriate port
		// give up after 20 tries
		int tries = 20;
		while (!addMapping(udp)) {
			if (tries<0)
				break;
			tries--;
			
			// try a random port
			if (gen == null)
				gen = new Random();
			port = gen.nextInt(50000)+2000;
			udp = new Mapping("",
					port,
					localAddress,
					localPort,
					"UDP",
					UDP_PREFIX + getGUIDSuffix());
		}
		
		if (tries < 0) {
			LOG.debug("couldn't map a port :(");
			return 0;
		}
		
		// at this stage, the variable port will point to the port the UDP mapping
		// got mapped to.  Since we have to have the same port for UDP and tcp,
		// we can't afford to change the port here.  So if mapping to this port on tcp
		// fails, we give up and clean up the udp mapping.
		// Note: Phillipe reported that on some routers adding an UDP mapping will also
		// create a TCP mapping.  So we no longer delete the UDP mapping if the TCP one 
		// fails.
		Mapping tcp = new Mapping("",
				port,
				localAddress,
				localPort,
				"TCP",
				TCP_PREFIX + getGUIDSuffix());
		if (!addMapping(tcp)) {
			LOG.debug(" couldn't map tcp to whatever udp was mapped. leaving udp around...");
			tcp = null;
		}
		
		// save a ref to the mappings
		synchronized(DEVICE_LOCK) {
			_tcp = tcp;
			_udp = udp;
		}
		
		// we're good - start a thread to clean up any potentially stale mappings
        ThreadExecutor.startThread(new StaleCleaner(), "Stale Mapping Cleaner");
		return port;
	}

    /**
     * Gets an action from a service, trimming whitespace if necessary.
     * see: http://forum.limewire.org/showpost.php?p=21952&postcount=1
     * for an example router that adds whitespace
     */
    private Action getActionFromService(Service service, String actionName) {
        Action action = service.getAction(actionName);
	    if(action != null) {
	        return action;
	    }
	    
	    if(LOG.isDebugEnabled())
	        LOG.debug("Couldn't find action: " + actionName + ", from direct lookup");
	    
	    
        for(Object actionObj : _service.getActionList()) {
            if(actionObj instanceof Action) {
                action = (Action)actionObj;
                if(action.getName() != null && actionName.equals(action.getName().trim())) {
                    return action;
	            }
            }
	    }
        
        if(LOG.isDebugEnabled())
            LOG.debug("Couldn't find action: " + actionName + " after iterating");
        
        return null;
	}
	
	/**
	 * @param m Port mapping to send to the NAT
	 * @return the error code
	 */
	private boolean addMapping(Mapping m) {
		
		if (LOG.isDebugEnabled())
			LOG.debug("adding "+m);
		
		Action add = getActionFromService(_service, "AddPortMapping");
		
		if(add == null) {
		    return false;
		}
		
		add.setArgumentValue("NewRemoteHost",m._externalAddress);
		add.setArgumentValue("NewExternalPort",m._externalPort);
		add.setArgumentValue("NewInternalClient",m._internalAddress);
		add.setArgumentValue("NewInternalPort",m._internalPort);
		add.setArgumentValue("NewProtocol",m._protocol);
		add.setArgumentValue("NewPortMappingDescription",m._description);
		add.setArgumentValue("NewEnabled","1");
		add.setArgumentValue("NewLeaseDuration",0);
		
		boolean success = add.postControlAction();
		if(LOG.isTraceEnabled())
		    LOG.trace("Post succeeded: " + success);
		return success;
	}
	
	/**
	 * @param m the mapping to remove from the NAT
	 * @return whether it worked or not
	 */
	private boolean removeMapping(Mapping m) {
		
		if (LOG.isDebugEnabled())
			LOG.debug("removing "+m);
		
		Action remove = getActionFromService(_service, "DeletePortMapping");
		
		if(remove == null) {
		    LOG.debug("Couldn't find DeletePortMapping action!");
		    return false;
	    }
		
		remove.setArgumentValue("NewRemoteHost",m._externalAddress);
		remove.setArgumentValue("NewExternalPort",m._externalPort);
		remove.setArgumentValue("NewProtocol",m._protocol);
		
		boolean success = remove.postControlAction();
		if(LOG.isDebugEnabled())
		    LOG.debug("Remove succeeded: " + success);
		return success;
	}

	/**
	 * schedules a shutdown hook which will clear the mappings created
	 * this session. 
	 */
	public void clearMappings() {
        // TODO Service.stop()
        synchronized(DEVICE_LOCK) {
            LOG.debug("start cleaning");
            if (_tcp != null) removeMapping(_tcp);
            if (_udp != null) removeMapping(_udp);
            LOG.debug("done cleaning");
        }
	}
	
	@Override
    public void finalize() {
        // TODO Service.stop()
        stop();
	}

	private String getGUIDSuffix() {
	    synchronized(DEVICE_LOCK) {
    	    if (_guidSuffix == null)
    			_guidSuffix = configuration.getClientID();
    	    return _guidSuffix;
        }
	}
	
	private class DeviceListener implements DeviceChangeListener {
        /** this method will be called when we discover a UPnP device. */
        public void deviceAdded(Device dev) {
            if (isNATPresent())
                return;
            synchronized(DEVICE_LOCK) {
                if(LOG.isTraceEnabled())
                    LOG.trace("Device added: " + dev.getFriendlyName());
                
                // did we find a router?
                if (dev.getDeviceType().equals(ROUTER_DEVICE) && dev.isRootDevice())
                    _router = dev;
                
                if (_router != null) {
                    discoverService();
                    
                    // did we find the service we need?
                    if (_service == null) {
                        LOG.debug("couldn't find service");
                        _router=null;
                    } else {
                        if(LOG.isDebugEnabled())
                            LOG.debug("Found service, router: " + _router.getFriendlyName() + ", service: " + _service);
                        stop();
                    }
                } else {
                    LOG.debug("didn't get router device");
                }
            }
            
            if(isNATPresent())
                notifyListeners();
        }
    	
    	public void deviceRemoved(Device dev) {}
	}
	
	private final static class Mapping {
		public final String _externalAddress;
		public final int _externalPort;
		public final String _internalAddress;
		public final int _internalPort;
		public final String _protocol,_description;
		
		// network constructor
		public Mapping(String externalAddress,String externalPort,
				String internalAddress, String internalPort,
				String protocol, String description) throws NumberFormatException{
			_externalAddress=externalAddress;
			_externalPort=Integer.parseInt(externalPort);
			_internalAddress=internalAddress;
			_internalPort=Integer.parseInt(internalPort);
			_protocol=protocol;
			_description=description;
		}
		
		// internal constructor
		public Mapping(String externalAddress,int externalPort,
				String internalAddress, int internalPort,
				String protocol, String description) {

			if ( !NetworkUtils.isValidPort(externalPort) ||
				!NetworkUtils.isValidPort(internalPort))
			    throw new IllegalArgumentException();

			_externalAddress=externalAddress;
			_externalPort=externalPort;
			_internalAddress=internalAddress;
			_internalPort=internalPort;
			_protocol=protocol;
			_description=description;
		}
		
		@Override
        public String toString() {
			return _externalAddress+":"+_externalPort+"->"+_internalAddress+":"+_internalPort+
				"@"+_protocol+" desc: "+_description;
		}
		
	}
	
	/**
	 * This thread reads all the existing mappings on the NAT and if it finds
	 * a mapping which appears to be created by us but points to a different
	 * address (i.e. is stale) it removes the mapping.
	 * 
	 * It can take several minutes to finish, depending on how many mappings there are.  
	 */
	private class StaleCleaner implements Runnable {
	    
	    // TODO: remove
	    private String list(List l) {
	        String s = "";
	        for(Iterator i = l.iterator(); i.hasNext(); ) {
	            Argument next = (Argument)i.next();
	            s += next.getName() + "->" + next.getValue() + ", ";
	        }
	        return s;
	    }
	    
		public void run() {
		    
		    LOG.debug("Looking for stale mappings...");
		    
			Set<Mapping> mappings = new HashSet<Mapping>();
			Action getGeneric = getActionFromService(_service, "GetGenericPortMappingEntry");
			
			if(getGeneric == null) {
			    LOG.debug("Couldn't find GetGenericPortMappingEntry action!");
			    return;
			}
			
			// get all the mappings
			try {
				for (int i=0;;i++) {
    				getGeneric.setArgumentValue("NewPortMappingIndex",i);
    				if(LOG.isDebugEnabled())
				        LOG.debug("Stale Iteration: " + i + ", generic.input: " + list(getGeneric.getInputArgumentList()) + ", generic.output: " + list(getGeneric.getOutputArgumentList()));
					
					if (!getGeneric.postControlAction())
						break;
					
					mappings.add(new Mapping(
							getGeneric.getArgumentValue("NewRemoteHost"),
							getGeneric.getArgumentValue("NewExternalPort"),
							getGeneric.getArgumentValue("NewInternalClient"),
							getGeneric.getArgumentValue("NewInternalPort"),
							getGeneric.getArgumentValue("NewProtocol"),
							getGeneric.getArgumentValue("NewPortMappingDescription")));
				    // TODO: erase output arguments.
				
				}
			}catch(NumberFormatException bad) {
			    LOG.error("NFE reading mappings!", bad);
				//router broke.. can't do anything.
				return;
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug("Stale cleaner found "+mappings.size()+ " total mappings");
			
			// iterate and clean up
            for(Mapping current : mappings) {
				if(LOG.isDebugEnabled())
				    LOG.debug("Analyzing: " + current);
				    
				if(current._description == null || current._internalAddress == null)
				    continue;
				
				// does it have our description?
				if (current._description.equals(TCP_PREFIX+getGUIDSuffix()) ||
						current._description.equals(UDP_PREFIX+getGUIDSuffix())) {
					
					// is it not the same as the mappings we created this session?
					synchronized(DEVICE_LOCK) {
						
						if (_udp != null &&
								current._externalPort == _udp._externalPort &&
								current._internalAddress.equals(_udp._internalAddress) &&
								current._internalPort == _udp._internalPort)
							continue;
					}
					
					// remove it.
					if (LOG.isDebugEnabled())
						LOG.debug("mapping "+current+" appears to be stale");
					removeMapping(current);
				}
			}
		}
	}
}

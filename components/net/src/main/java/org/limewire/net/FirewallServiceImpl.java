package org.limewire.net;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;
import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;

import com.google.inject.Inject;

@EagerSingleton
public class FirewallServiceImpl implements FirewallService, Service {
    
    public static final File LIMEWIRE_EXE_FILE = new File("LimeWire.exe").getAbsoluteFile();
    
    /** The name of this program, "LimeWire". */
	private static String name = "LimeWire";
    
    private AtomicBoolean needsCleanup = new AtomicBoolean(false);
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this); // TODO .in("Stage") ???
    }

    @Override
    public String getServiceName() {
        return I18nMarker.marktr("Firewall Manager");
    }

    @Override
    @Asynchronous (daemon = false) // TODO is daemon = false correct?
    public void stop() {
        if(needsCleanup.get()) {
            if (!OSUtils.isWindows())
                return;
    
            // Get the path of this running instance, like "C:\Program Files\LimeWire\LimeWire.exe"
            String path = SystemUtils.getRunningPath();
    
            // Only do something if the LimeWire Windows launcher ran, not Java in a development environment
            if (!path.equalsIgnoreCase(LIMEWIRE_EXE_FILE.getPath()))
                return;
    
            // Only remove our listing if it's there
            if (SystemUtils.isProgramListedOnFirewall(path)) {
                SystemUtils.removeProgramFromFirewall(path);
            }
        }
    }

    public boolean isProgrammaticallyConfigurable() {
        if(!OSUtils.isWindows() || !SystemUtils.isFirewallPresent() || !SystemUtils.isFirewallEnabled())
            return false;
        
        String path = SystemUtils.getRunningPath();
        return path.equalsIgnoreCase(LIMEWIRE_EXE_FILE.getPath());
    }

    /**
     * Add ourselves to the firewall exceptions list.
     * This will let code in this process listen on a socket without the firewall showing the user a security warning.
     * Call this method on startup before the program listens on a socket.
     * Unlike UPnP, this returns quickly and works very reliably.
     * 
     * Returns true if this is added (or already on) the firewall exception list.
     */
    public boolean addToFirewall() {
        if (!OSUtils.isWindows())
    		return false;

    	// Get the path of this running instance, like "C:\Program Files\LimeWire\LimeWire.exe"
    	String path = SystemUtils.getRunningPath();
        if (path == null)
            return false;

    	// Only add us if the LimeWire Windows launcher ran, not Java in a development environment
    	if (!path.equalsIgnoreCase(LIMEWIRE_EXE_FILE.getAbsolutePath()))
    		return false;

    	// Only add a listing for us if the Windows Firewall Exceptions list doesn't have one yet
    	if (SystemUtils.isProgramListedOnFirewall(path))
            return true;
      
		if(SystemUtils.addProgramToFirewall(path, name)) {
            needsCleanup.set(true);
            return true;
        }
        
        return false;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
    }
}

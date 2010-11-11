package org.limewire.ui.swing.shell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.jdic.filetypes.Action;
import org.jdesktop.jdic.filetypes.Association;
import org.jdesktop.jdic.filetypes.AssociationAlreadyRegisteredException;
import org.jdesktop.jdic.filetypes.AssociationNotRegisteredException;
import org.jdesktop.jdic.filetypes.AssociationService;
import org.jdesktop.jdic.filetypes.RegisterFailedException;
import org.limewire.util.SystemUtils;


public class FileTypeAssociation implements ShellAssociation {
    
    private static final Log LOG = LogFactory.getLog(FileTypeAssociation.class);

	private static final AssociationService SERVICE = new AssociationService();
	
	private final String extention;
    private final String mimeType;
    private final String executable;
    private final String verb;
    
	private final Association association = new Association();
	
	public FileTypeAssociation(String extention, 
			String mimeType, String executable, String verb, 
			String description, String iconPath) {
		this.extention = extention;
		this.mimeType = mimeType;
		this.executable = executable;
		this.verb = verb;
		Action action = new Action(verb, executable);
		association.addAction(action);
		association.addFileExtension(extention);
		association.setMimeType(mimeType);
		association.setName(description); // only used on unix
		association.setDescription(description);
		if (iconPath != null) // don't chance passing null to jdic
			association.setIconFileName(iconPath);
	}
	
	public boolean isAvailable() {
	    try {
    		// if no association at all, then it is available
    		Association f = SERVICE.getFileExtensionAssociation(extention); 
    		if (f == null && f == SERVICE.getMimeTypeAssociation(mimeType))
    			return true;
	    } catch(IllegalArgumentException iae) {
	        // SEE: LWC-1170
	        // If JDIC bails on us, the registry might be a little confused...
	        // so let's fix it by inserting ours.
	        LOG.warn("Can't check availability!", iae);
	        return true;
	    }
		
		// still check for a default handler.
		String extHandler = SystemUtils.getDefaultExtentionHandler(extention);
		return ("".equals(extHandler) && 
				"".equals(SystemUtils.getDefaultMimeHandler(mimeType)));
	}

	public boolean isRegistered() {
		Association f;
		try {
		    f = SERVICE.getFileExtensionAssociation(extention);
		} catch(IllegalArgumentException iae) {
            // SEE: LWC-1170
		    LOG.warn("Can't check registration!", iae);
		    return false;
		}
		if (f == null)
			return false;
		Action open = f.getActionByVerb(verb);
		if (open == null)
			return false;
		if (executable.equals(open.getCommand()))
			return true;
		return executable.equals(SystemUtils.getDefaultExtentionHandler(extention)) &&
				executable.equals(SystemUtils.getDefaultMimeHandler(mimeType));
	}

	public void register() {
		try {
			SERVICE.registerUserAssociation(association);
            SystemUtils.flushIconCache();
        } catch (AssociationAlreadyRegisteredException ignore){
            LOG.error("can't register", ignore);
		} catch (RegisterFailedException ignore){
            LOG.error("can't register", ignore);
		}
        
	}

	public boolean canUnregister() {
	    return true;
	}
	
	public void unregister() {
	    try {
    		forceUnregister(SERVICE.getFileExtensionAssociation(extention));
    		forceUnregister(SERVICE.getMimeTypeAssociation(extention));
	    } catch(IllegalArgumentException ignored) {
	        //SEE: LWC-1170
	        LOG.warn("Can't unregister!", ignored);
	    }
	}
	
	private void forceUnregister(Association f) {
		if (f == null)
			return;
		try {
			SERVICE.unregisterUserAssociation(f);
            SystemUtils.flushIconCache();
		} catch (AssociationNotRegisteredException ignore) { 
            LOG.error("can't unregister", ignore);
        } catch (RegisterFailedException ignore) { 
            LOG.error("can't unregister", ignore);
        }
	}
	
	@Override
    public String toString() {
		return extention+":"+mimeType+":"+executable+":"+verb;
	}
}

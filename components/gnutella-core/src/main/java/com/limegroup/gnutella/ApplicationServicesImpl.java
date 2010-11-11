package com.limegroup.gnutella;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.InstallSettings;
import org.limewire.io.GUID;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
public class ApplicationServicesImpl implements ApplicationServices {
   
    private final byte[] bittorrentGUID;
    private final byte[] limewireGUID;
    private final boolean newInstall;
    private final boolean newJavaVersion;
    
    @Inject
    ApplicationServicesImpl() {
        byte [] myguid=null;
        try {
            myguid = GUID.fromHexString(ApplicationSettings.CLIENT_ID.get());
        }catch(IllegalArgumentException iae) {
            myguid = GUID.makeGuid();
            ApplicationSettings.CLIENT_ID.set((new GUID(myguid)).toHexString());
        }
        limewireGUID = myguid;
        
        byte []mybtguid = new byte[20];
        mybtguid[0] = 0x2D; // - 
        mybtguid[1] = 0x4C; // L
        mybtguid[2] = 0x57; // W
        System.arraycopy(StringUtils.toAsciiBytes(LimeWireUtils.BT_REVISION),0, mybtguid,3, 4);
        mybtguid[7] = 0x2D; // -
        System.arraycopy(limewireGUID,0,mybtguid,8,12);
        bittorrentGUID = mybtguid;
        
        String lastRunVersion = getVersionNoProModifier(InstallSettings.LAST_VERSION_RUN.get());
        String limewireVersion = getVersionNoProModifier(LimeWireUtils.getLimeWireVersion());
        newInstall = lastRunVersion == null || !lastRunVersion.equals(limewireVersion);
        
        String lastJavaVersion = InstallSettings.LAST_JAVA_VERSION_RUN.get();
        String currentJavaVersion = System.getProperty("java.version");
        newJavaVersion = lastJavaVersion == null || !lastJavaVersion.equals(currentJavaVersion);
        InstallSettings.LAST_JAVA_VERSION_RUN.set(currentJavaVersion);
    }
    
    /**
     * @param fullVersion full version string, such as "5.1.1", "5.1.1 Pro"
     * @return the version string, stripping out any "Pro" modifiers.
     * If the version string is null, or does not end in "Pro", returns
     * the argument which was passed in.
     */
    private static String getVersionNoProModifier(String fullVersion) {
        String fullVersionNoModifiers = null;
        if (fullVersion != null) {
            fullVersionNoModifiers = fullVersion.endsWith("Pro") ? 
                fullVersion.substring(0, fullVersion.length()-4) : fullVersion; 
        }
        return fullVersionNoModifiers;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ApplicationServices#getMyBTGUID()
     */
    public byte [] getMyBTGUID() {
    	return bittorrentGUID;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ApplicationServices#getMyGUID()
     */
    public byte [] getMyGUID() {
        return limewireGUID;
    }

    @Override
    public boolean isNewInstall() {
        return newInstall;
    }

    @Override
    public boolean isNewJavaVersion() {
        return newJavaVersion;
    }
}

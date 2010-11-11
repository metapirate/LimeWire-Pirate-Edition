package org.limewire.core.api;

/** Contains methods relating to the overall application. */
public interface Application {

    /** Returns true if the application is in a 'testing' version. */
    boolean isTestingVersion();

    /**
     * Starts the core services of the application.
     */
    void startCore();

    /**
     * Stops the core services of the application.
     */
    void stopCore();
    
    /**
     * Sets a command than will be executed after shutdown.
     */
    void setShutdownFlag(String flag);

    /** Returns the version of the program. */
    String getVersion();

    /** 
     * Returns true if this version of LimeWire is a beta version.
     */
    public boolean isBetaVersion();

    /**
     * Returns true if there was no previously installed version, 
     * or the current version is differant than the previous version. 
     */
    public boolean isNewInstall();
    
    /**
     * Returns true if the java version is differant that the one used to last run limewire. 
     */
    public boolean isNewJavaVersion();
}

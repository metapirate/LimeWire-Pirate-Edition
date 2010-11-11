package com.limegroup.gnutella.licenses;

/** A license that can have its name set. */
public interface MutableLicense extends License {
    
    void setLicenseName(String name);
    
}
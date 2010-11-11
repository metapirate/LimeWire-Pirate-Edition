package org.limewire.lifecycle;

public interface StagedRegisterBuilder {
    
    public void in(ServiceStage stage);
    
    public void in(Object customStage);

}

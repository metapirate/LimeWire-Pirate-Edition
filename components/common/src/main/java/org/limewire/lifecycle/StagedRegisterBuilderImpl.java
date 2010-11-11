package org.limewire.lifecycle;

import org.limewire.util.Objects;

class StagedRegisterBuilderImpl implements StagedRegisterBuilder {
    
    private final Service service;
    private ServiceStage stage = ServiceStage.NORMAL;
    private Object customStage = null;
    
    public StagedRegisterBuilderImpl(Service service) {
        this.service = Objects.nonNull(service, "service");
    }

    public void in(ServiceStage stage) {
        this.stage = Objects.nonNull(stage, "stage");
    }
    
    public void in(Object stage) {
        this.customStage = Objects.nonNull(stage, "stage");
    }
    
    ServiceStage getStage() { return stage; }
    Service getService() { return service; }
    Object getCustomStage() { return customStage; }

}

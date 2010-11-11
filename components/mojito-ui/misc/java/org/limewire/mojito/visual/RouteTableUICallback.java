package org.limewire.mojito.visual;

import org.limewire.mojito.routing.Bucket;

public interface RouteTableUICallback {
    
    public void handleBucketSelected(Bucket bucket);
    
    public void handleNodeGraphRootSelected();

}

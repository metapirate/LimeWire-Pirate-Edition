package org.limewire.mojito.visual.components;

import org.limewire.mojito.routing.Bucket;

public class BucketVertex extends RouteTableVertex<Bucket>{

    private Bucket bucket;
    private boolean isLocalBucket;
    
    public BucketVertex(Bucket bucket, boolean isLocalBucket) {
        super();
        this.bucket = bucket;
        this.isLocalBucket = isLocalBucket;
    }
    
    @Override
    public boolean isLocal() {
        return isLocalBucket;
    }
    
    @Override
    public Bucket getNode() {
        return bucket;
    }

    @Override
    public String toString() {
        return bucket.getBucketID().toBinString().substring(0, bucket.getDepth());
    }
}

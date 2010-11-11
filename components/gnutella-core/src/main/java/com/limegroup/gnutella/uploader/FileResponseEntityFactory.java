package com.limegroup.gnutella.uploader;

import java.io.File;

public interface FileResponseEntityFactory {

    public abstract FileResponseEntity createFileResponseEntity(
            HTTPUploader uploader, File file);

}
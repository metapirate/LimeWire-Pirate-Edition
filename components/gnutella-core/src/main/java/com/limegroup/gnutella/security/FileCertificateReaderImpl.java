package com.limegroup.gnutella.security;

import java.io.File;
import java.io.IOException;

import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;

public class FileCertificateReaderImpl implements FileCertificateReader {
    
    private final CertificateParser certificateParser;

    @Inject
    public FileCertificateReaderImpl(CertificateParser certificateParser) {
        this.certificateParser = certificateParser;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.security.FileCertificateReader#read(java.io.File)
     */
    public Certificate read(File file) throws IOException {
        if (file.length() > 10000) {
            throw new IOException("file too big, corrupted");
        }
        byte[] data = FileUtils.readFileFully(file);
        if (data == null) {
            throw new IOException("file could not be read");
        }
        return certificateParser.parseCertificate(StringUtils.toUTF8String(data));
    }

    @Override
    public boolean write(Certificate certificate, File file) {
        return FileUtils.verySafeSave(file.getParentFile(), file.getName(), StringUtils.toUTF8Bytes(certificate.getCertificateString()));
    }

}

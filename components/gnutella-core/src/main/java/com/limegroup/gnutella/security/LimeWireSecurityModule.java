package com.limegroup.gnutella.security;

import com.google.inject.AbstractModule;

public class LimeWireSecurityModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(CertificateParser.class).to(CertificateParserImpl.class);
        bind(CertificateVerifier.class).to(CertificateVerifierImpl.class);
        bind(FileCertificateReader.class).to(FileCertificateReaderImpl.class);
        bind(HttpCertificateReader.class).to(HttpCertificateReaderImpl.class);
    }

}

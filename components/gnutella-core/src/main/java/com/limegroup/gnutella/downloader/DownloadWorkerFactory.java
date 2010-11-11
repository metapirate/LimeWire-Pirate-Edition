package com.limegroup.gnutella.downloader;


interface DownloadWorkerFactory {

    DownloadWorker create(DownloadWorkerSupport manager, RemoteFileDescContext rfdContext,
            VerifyingFile vf);

}
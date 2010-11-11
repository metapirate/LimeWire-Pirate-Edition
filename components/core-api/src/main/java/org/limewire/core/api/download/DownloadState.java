package org.limewire.core.api.download;

/**
 * An enum to describe the various general download states and their capabilities. 
 */
public enum DownloadState {
    /** Download finished and scanned. */
	DONE(false, false, true), 
	CONNECTING(true, false, false), 
	DOWNLOADING(true, false, false), 
	PAUSED(false, true, false), 
	FINISHING(false, false, false), 
    LOCAL_QUEUED(true, false, false), 
    REMOTE_QUEUED(true, false, false), 
	CANCELLED(false, false, false), 
	STALLED(false, false, false),
	TRYING_AGAIN(true, false, false),
	ERROR(false, false, false),
	RESUMING(false, false, false),
	/** Threat detected by dangerous file checker. */
	DANGEROUS(false, false, true),
    /** Anti-virus scan in progress for finished download. */
    SCANNING(false, false, false),
    /** Anti-virus scan in progress for file fragment. */
    SCANNING_FRAGMENT(false, false, false),
    /** Threat detected by anti-virus scan. */
    THREAT_FOUND(false, false, true),
	/** Anti-virus scan failed. */
	SCAN_FAILED(false, false, true),
	/**Applying the downloaded anti-virus update*/
	APPLYING_DEFINITION_UPDATE(false, false, false);

	private final boolean pausable;
	private final boolean resumable;
	private final boolean finished;

	DownloadState(boolean pausable, boolean resumable, boolean finished) {
		this.pausable = pausable;
		this.resumable = resumable;
		this.finished = finished;
	}

	public boolean isPausable() {
		return pausable;
	}

	public boolean isResumable() {
		return resumable;
	}
	
	/**
	 * Returns true if the state represents a finished condition.
	 */
	public boolean isFinished() {
	    return finished;
	}
}

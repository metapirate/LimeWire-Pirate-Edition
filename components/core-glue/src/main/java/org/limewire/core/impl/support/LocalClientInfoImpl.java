package org.limewire.core.impl.support;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.limewire.core.api.support.LocalClientInfo;
import org.limewire.core.api.support.SessionInfo;
import org.limewire.core.settings.LimeProps;
import org.limewire.core.settings.SharingSettings;
import org.limewire.mojito.settings.MojitoProps;
import org.limewire.setting.Setting;
import org.limewire.setting.SettingsFactory;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.VersionUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.internal.Nullable;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * This class encapsulates all of the data for an individual client machine for
 * an individual bug report.
 * <p>
 * 
 * This class collects all of the data for the local machine and provides access
 * to that data in url-encoded form.
 */
// 2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class LocalClientInfoImpl extends LocalAbstractInfo implements LocalClientInfo {

    /**
     * Creates information about this bug from the bug, thread, and detail.
     */
    @Inject
    public LocalClientInfoImpl(@Assisted Throwable bug,
            @Nullable @Assisted("threadName") String threadName,
            @Nullable @Assisted("detail") String detail, @Assisted boolean fatal,
            SessionInfo sessionInfo) {
        // Store the basic information ...
        _limewireVersion = LimeWireUtils.getLimeWireVersion();
        _javaVersion = VersionUtils.getJavaVersion();
        _javaVendor = System.getProperty("java.vendor", "?");
        _os = OSUtils.getOS();
        _osVersion = System.getProperty("os.version", "?");
        _architecture = System.getProperty("os.arch", "?");
        _freeMemory = "" + Runtime.getRuntime().freeMemory();
        _totalMemory = "" + Runtime.getRuntime().totalMemory();
        _peakThreads = "" + ManagementFactory.getThreadMXBean().getPeakThreadCount();
        _loadAverage = "" + ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        _pendingObjects = ""
                + ManagementFactory.getMemoryMXBean().getObjectPendingFinalizationCount();
        _heapUsage = "" + ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        _nonHeapUsage = "" + ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        _settingsFreeSpace = "" + CommonUtils.getUserSettingsDir().getUsableSpace();
        _incompleteFreeSpace = "" + SharingSettings.INCOMPLETE_DIRECTORY.get().getUsableSpace();
        _downloadFreeSpace = "" + SharingSettings.getSaveDirectory().getUsableSpace();

        // Store information about the bug and the current thread.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        bug.printStackTrace(pw);
        pw.flush();
        _bug = sw.toString();
        _currentThread = threadName;

        _bugName = bug.getClass().getName();

        _fatalError = "" + fatal;

        // Store the properties.
        sw = new StringWriter();
        pw = new PrintWriter(sw);
        Properties props = new Properties();
        // Load the properties from SettingsFactory, excluding
        // FileSettings and FileArraySettings.
        SettingsFactory sf = LimeProps.instance().getFactory();
        synchronized (sf) {
            for (Setting set : sf) {
                if (!set.isPrivate() && !set.isDefault())
                    props.put(set.getKey(), set.getValueAsString());
            }
        }
        sf = MojitoProps.instance().getFactory();
        synchronized (sf) {
            for (Setting set : sf) {
                if (!set.isPrivate() && !set.isDefault())
                    props.put(set.getKey(), set.getValueAsString());

            }
        }
        // list the properties in the PrintWriter.
        props.list(pw);
        pw.flush();
        _props = sw.toString();

        // Store extra debugging information. We removed an old call to
        // AppFrame.isStarted() because it is no longer useful.
        if (sessionInfo.isLifecycleLoaded()) {
            _upTime = CommonUtils.seconds2time(sessionInfo.getCurrentUptime() / 1000);
            _connected = "" + sessionInfo.isConnected();
            _upToUp = "" + sessionInfo.getNumUltrapeerToUltrapeerConnections();
            _upToLeaf = "" + sessionInfo.getNumUltrapeerToLeafConnections();
            _leafToUp = "" + sessionInfo.getNumLeafToUltrapeerConnections();
            _oldConnections = "" + sessionInfo.getNumOldConnections();
            _ultrapeer = "" + sessionInfo.isSupernode();
            _leaf = "" + sessionInfo.isShieldedLeaf();
            _activeUploads = "" + sessionInfo.getNumActiveUploads();
            _queuedUploads = "" + sessionInfo.getNumQueuedUploads();
            _activeDownloads = "" + sessionInfo.getNumActiveDownloads();
            _httpDownloaders = "" + sessionInfo.getNumIndividualDownloaders();
            _waitingDownloaders = "" + sessionInfo.getNumWaitingDownloads();
            _acceptedIncoming = "" + sessionInfo.acceptedIncomingConnection();
            _sharedFiles = "" + sessionInfo.getSharedFileListSize();
            _managedFiles = "" + sessionInfo.getManagedFileListSize();
            _friendFiles = "" + sessionInfo.getAllFriendsFileListSize();
            _guessCapable = "" + sessionInfo.isGUESSCapable();
            _solicitedCapable = "" + sessionInfo.canReceiveSolicited();
            _portStable = "" + sessionInfo.isUdpPortStable();
            _canDoFWT = "" + sessionInfo.canDoFWT();
            _lastReportedPort = "" + sessionInfo.lastReportedUdpPort();
            _externalPort = "" + sessionInfo.getPort();
            _receivedIpPong = "" + sessionInfo.receivedIpPong();
            _responseSize = "";
            _creationCacheSize = "" + sessionInfo.getCreationCacheSize();
            _vfByteSize = "" + sessionInfo.getDiskControllerByteCacheSize();
            _vfVerifyingSize = "" + sessionInfo.getDiskControllerVerifyingCacheSize();
            _bbSize = "" + sessionInfo.getByteBufferCacheSize();
            _vfQueueSize = "" + sessionInfo.getDiskControllerQueueSize();
            _waitingSockets = "" + sessionInfo.getNumberOfWaitingSockets();
            _pendingTimeouts = "" + sessionInfo.getNumberOfPendingTimeouts();
            _sp2Workarounds = "" + sessionInfo.getNumConnectionCheckerWorkarounds();
            _slotManager = "" + sessionInfo.getUploadSlotManagerInfo();
            long[] selectStats = sessionInfo.getSelectStats();
            if (selectStats != null) {
                _numSelects = "" + selectStats[0];
                _numImmediateSelects = "" + selectStats[1];
                _avgSelectTime = "" + selectStats[2];
            }
        }

        // Store the detail, thread counts, and other information.
        _detail = detail;

        Thread[] allThreads = new Thread[Thread.activeCount()];
        int copied = Thread.enumerate(allThreads);
        _threadCount = "" + copied;
        Map<String, Integer> threads = new HashMap<String, Integer>();
        for (int i = 0; i < copied; i++) {
            String name = allThreads[i].getName();
            Integer val = threads.get(name);
            if (val == null)
                threads.put(name, new Integer(1));
            else {
                int num = val.intValue() + 1;
                threads.put(name, new Integer(num));
            }
        }
        sw = new StringWriter();
        pw = new PrintWriter(sw);
        for (Map.Entry<String, Integer> info : threads.entrySet())
            pw.println(info.getKey() + ": " + info.getValue());
        pw.flush();
        _otherThreads = sw.toString();
    }

    /**
     * Returns an array of map entries in this info.
     */
    public final Map.Entry[] getPostRequestParams() {
        List<Map.Entry> params = new LinkedList<Map.Entry>();

        append(params, LIMEWIRE_VERSION, _limewireVersion);
        append(params, JAVA_VERSION, _javaVersion);
        append(params, OS, _os);
        append(params, OS_VERSION, _osVersion);
        append(params, ARCHITECTURE, _architecture);
        append(params, FREE_MEMORY, _freeMemory);
        append(params, TOTAL_MEMORY, _totalMemory);
        append(params, BUG, _bug);
        append(params, CURRENT_THREAD, _currentThread);
        append(params, PROPS, _props);
        append(params, UPTIME, _upTime);
        append(params, CONNECTED, _connected);
        append(params, UP_TO_UP, _upToUp);
        append(params, UP_TO_LEAF, _upToLeaf);
        append(params, LEAF_TO_UP, _leafToUp);
        append(params, OLD_CONNECTIONS, _oldConnections);
        append(params, ULTRAPEER, _ultrapeer);
        append(params, LEAF, _leaf);
        append(params, ACTIVE_UPLOADS, _activeUploads);
        append(params, QUEUED_UPLOADS, _queuedUploads);
        append(params, ACTIVE_DOWNLOADS, _activeDownloads);
        append(params, HTTP_DOWNLOADERS, _httpDownloaders);
        append(params, WAITING_DOWNLOADERS, _waitingDownloaders);
        append(params, ACCEPTED_INCOMING, _acceptedIncoming);
        append(params, SHARED_FILES, _sharedFiles);
        append(params, OTHER_THREADS, _otherThreads);
        append(params, DETAIL, _detail);
        append(params, OTHER_BUG, _otherBug);
        append(params, JAVA_VENDOR, _javaVendor);
        append(params, THREAD_COUNT, _threadCount);
        append(params, BUG_NAME, _bugName);
        append(params, GUESS_CAPABLE, _guessCapable);
        append(params, SOLICITED_CAPABLE, _solicitedCapable);
        append(params, PORT_STABLE, _portStable);
        append(params, CAN_DO_FWT, _canDoFWT);
        append(params, LAST_REPORTED_PORT, _lastReportedPort);
        append(params, EXTERNAL_PORT, _externalPort);
        append(params, RECEIVED_IP_PONG, _receivedIpPong);
        append(params, FATAL_ERROR, _fatalError);
        append(params, RESPONSE_SIZE, _responseSize);
        append(params, CT_SIZE, _creationCacheSize);
        append(params, VF_BYTE_SIZE, _vfByteSize);
        append(params, VF_VERIFY_SIZE, _vfVerifyingSize);
        append(params, BB_BYTE_SIZE, _bbSize);
        append(params, VF_QUEUE_SIZE, _vfQueueSize);
        append(params, WAITING_SOCKETS, _waitingSockets);
        append(params, PENDING_TIMEOUTS, _pendingTimeouts);
        append(params, PEAK_THREADS, _peakThreads);
        append(params, SP2_WORKAROUNDS, _sp2Workarounds);
        append(params, LOAD_AVERAGE, _loadAverage);
        append(params, PENDING_GCOBJ, _pendingObjects);
        append(params, SETTINGS_FREE_SPACE, _settingsFreeSpace);
        append(params, INCOMPLETES_FREE_SPACE, _incompleteFreeSpace);
        append(params, DOWNLOAD_FREE_SPACE, _downloadFreeSpace);
        append(params, HEAP_USAGE, _heapUsage);
        append(params, NON_HEAP_USAGE, _nonHeapUsage);
        append(params, SLOT_MANAGER, _slotManager);
        append(params, NUM_SELECTS, _numSelects);
        append(params, NUM_IMMEDIATE_SELECTS, _numImmediateSelects);
        append(params, AVG_SELECT_TIME, _avgSelectTime);
        if (!_userComments.equals("")) {
            append(params, USER_COMMENTS, _userComments);
        }
        append(params, MANAGED_FILES, _managedFiles);
        append(params, FRIEND_FILES, _friendFiles);
        // APPEND OTHER PARAMETERS HERE.

        return params.toArray(new Map.Entry[params.size()]);
    }

    /**
     * @return compact printout of the list of parameters
     */
    public String getShortParamList() {
        StringBuilder sb = new StringBuilder(2000);
        for (Map.Entry entry : getPostRequestParams()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Appends a map entry to the specified list using the specified key and
     * value. If the value is null, then the entry is not added.
     */
    private final void append(List<Map.Entry> list, String key, String value) {
        if (value != null) {
            list.add(new AbstractMap.SimpleEntry<String, String>(key, value));
        }
    }
}

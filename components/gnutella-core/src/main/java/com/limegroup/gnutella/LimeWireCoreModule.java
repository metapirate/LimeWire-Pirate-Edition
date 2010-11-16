package com.limegroup.gnutella;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.bittorrent.TorrentScrapeScheduler;
import org.limewire.bittorrent.TorrentScrapeSchedulerImpl;
import org.limewire.bittorrent.TorrentTrackerScraper;
import org.limewire.common.LimeWireCommonModule;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.LimeScheduledThreadPoolExecutor;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.core.api.connection.FirewallStatusEvent;
import org.limewire.core.api.connection.FirewallTransferStatusEvent;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.network.BandwidthCollector;
import org.limewire.core.settings.LimeWireCoreSettingsModule;
import org.limewire.http.LimeWireHttpModule;
import org.limewire.inject.AbstractModule;
import org.limewire.inject.EagerSingleton;
import org.limewire.inject.LazySingleton;
import org.limewire.io.LimeWireIOModule;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.jmx.ExecutorBean;
import org.limewire.listener.AsynchronousCachingEventMulticasterImpl;
import org.limewire.listener.AsynchronousEventBroadcaster;
import org.limewire.listener.AsynchronousMulticasterImpl;
import org.limewire.listener.BroadcastPolicy;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.ListenerSupport;
import org.limewire.mojito.LimeWireMojitoModule;
import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.ConnectionDispatcherImpl;
import org.limewire.net.ExternalIP;
import org.limewire.net.LimeWireNetModule;
import org.limewire.net.TLSManager;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.FirewalledAddressSerializer;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.security.SecureMessageVerifierImpl;
import org.limewire.security.SecurityToken;
import org.limewire.security.SettingsProvider;
import org.limewire.security.certificate.LimeWireSecurityCertificateModule;
import org.limewire.security.id.SecureIdStore;
import org.limewire.statistic.LimeWireStatisticsModule;
import org.limewire.util.JmxUtils;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.limegroup.bittorrent.BTUploaderFactory;
import com.limegroup.bittorrent.LimeWireBittorrentModule;
import com.limegroup.bittorrent.TorrentTrackerScraperImpl;
import com.limegroup.bittorrent.TorrentUploadManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.AlternateLocationFactoryImpl;
import com.limegroup.gnutella.bootstrap.Bootstrapper;
import com.limegroup.gnutella.bootstrap.LimeWireBootstrapModule;
import com.limegroup.gnutella.browser.LocalAcceptor;
import com.limegroup.gnutella.connection.LimeWireCoreConnectionModule;
import com.limegroup.gnutella.daap.DaapManager;
import com.limegroup.gnutella.dht.DHTBootstrapperFactory;
import com.limegroup.gnutella.dht.DHTBootstrapperFactoryImpl;
import com.limegroup.gnutella.dht.DHTControllerFacade;
import com.limegroup.gnutella.dht.DHTControllerFacadeImpl;
import com.limegroup.gnutella.dht.DHTControllerFactory;
import com.limegroup.gnutella.dht.DHTControllerFactoryImpl;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManagerImpl;
import com.limegroup.gnutella.dht.DHTNodeFetcherFactory;
import com.limegroup.gnutella.dht.DHTNodeFetcherFactoryImpl;
import com.limegroup.gnutella.dht.LimeWireDHTModule;
import com.limegroup.gnutella.dht.db.AltLocValueFactory;
import com.limegroup.gnutella.dht.db.AltLocValueFactoryImpl;
import com.limegroup.gnutella.dht.db.PushEndpointService;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactory;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactoryImpl;
import com.limegroup.gnutella.dht.io.LimeMessageDispatcherFactoryImpl;
import com.limegroup.gnutella.downloader.LimeWireDownloadModule;
import com.limegroup.gnutella.downloader.serial.conversion.DownloadUpgradeTaskService;
import com.limegroup.gnutella.filters.LimeWireFiltersModule;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactoryImpl;
import com.limegroup.gnutella.handshaking.HandshakeServices;
import com.limegroup.gnutella.handshaking.HandshakeServicesImpl;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.handshaking.HeadersFactoryImpl;
import com.limegroup.gnutella.http.DefaultHttpExecutor;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.library.LimeWireLibraryModule;
import com.limegroup.gnutella.library.RareFileStrategy;
import com.limegroup.gnutella.library.RareFileStrategyImpl;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.licenses.LicenseFactoryImpl;
import com.limegroup.gnutella.malware.LimeWireMalwareModule;
import com.limegroup.gnutella.messagehandlers.MessageHandlerBinderImpl;
import com.limegroup.gnutella.messages.LocalPongInfo;
import com.limegroup.gnutella.messages.LocalPongInfoImpl;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.MessageFactoryImpl;
import com.limegroup.gnutella.messages.MessageParserBinder;
import com.limegroup.gnutella.messages.MessageParserBinderImpl;
import com.limegroup.gnutella.messages.OutgoingQueryReplyFactory;
import com.limegroup.gnutella.messages.OutgoingQueryReplyFactoryImpl;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingReplyFactoryImpl;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.PingRequestFactoryImpl;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryReplyFactoryImpl;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.QueryRequestFactoryImpl;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactoryImpl;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.HeadPongFactoryImpl;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactoryImpl;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPongFactory;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPongFactoryImpl;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactory;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactoryImpl;
import com.limegroup.gnutella.messages.vendor.VendorMessageParserBinder;
import com.limegroup.gnutella.messages.vendor.VendorMessageParserBinderImpl;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.metadata.MetaDataFactoryImpl;
import com.limegroup.gnutella.net.address.ConnectableConnector;
import com.limegroup.gnutella.net.address.SameNATAddressResolver;
import com.limegroup.gnutella.routing.QRPUpdater;
import com.limegroup.gnutella.rudp.LimeWireGnutellaRudpModule;
import com.limegroup.gnutella.search.LimeWireSearchModule;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryDispatcherImpl;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.QueryHandlerFactoryImpl;
import com.limegroup.gnutella.security.LimeWireSecurityModule;
import com.limegroup.gnutella.settings.LimeWireSettingsModule;
import com.limegroup.gnutella.settings.SettingsBackedProxySettings;
import com.limegroup.gnutella.settings.SettingsBackedSocketBindingSettings;
import com.limegroup.gnutella.spam.LimeWireSpamModule;
import com.limegroup.gnutella.statistics.LimeWireGnutellaStatisticsModule;
import com.limegroup.gnutella.tigertree.LimeWireHashTreeModule;
import com.limegroup.gnutella.uploader.FileResponseEntityFactory;
import com.limegroup.gnutella.uploader.FileResponseEntityFactoryImpl;
import com.limegroup.gnutella.uploader.HTTPUploadSessionManager;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactoryImpl;
import com.limegroup.gnutella.uploader.LimeWireUploaderModule;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.uploader.UploadSlotManagerImpl;
import com.limegroup.gnutella.util.FECUtils;
import com.limegroup.gnutella.util.FECUtilsImpl;
import com.limegroup.gnutella.xml.LimeWireXmlModule;

/**
 * The module that defines what implementations are used within
 * LimeWire's core.  This class can be constructed with or without
 * an ActivitiyCallback class.  If it is without, then another module
 * must explicitly identify which class is going to define the
 * ActivityCallback.
 */
public class LimeWireCoreModule extends AbstractModule {
 
	private final Class<? extends ActivityCallback> activityCallbackClass;
    
    public LimeWireCoreModule() {
        this(null);
    }
    
    public LimeWireCoreModule(Class<? extends ActivityCallback> activityCallbackClass) {
        this.activityCallbackClass = activityCallbackClass;
    }
    
    @Override
    protected void configure() {
        binder().install(new LimeWireCommonModule());
        binder().install(new LimeWireCoreSettingsModule());
        binder().install(new LimeWireSettingsModule());
        binder().install(new LimeWireNetModule(SettingsBackedProxySettings.class, SettingsBackedSocketBindingSettings.class));
        binder().install(new LimeWireDownloadModule());
        binder().install(new LimeWireHashTreeModule());        
        binder().install(new LimeWireDHTModule());
        
        binder().install(new LimeWireHttpModule());
        binder().install(new LimeWireSearchModule());
        binder().install(new LimeWireStatisticsModule());
        binder().install(new LimeWireGnutellaStatisticsModule());
        binder().install(new LimeWireGnutellaRudpModule());
        binder().install(new LimeWireIOModule());
        binder().install(new LimeWireMojitoModule());
        binder().install(new LimeWireSecurityCertificateModule());
        binder().install(new LimeWireSecurityModule());
        binder().install(new LimeWireBittorrentModule());
        binder().install(new LimeWireLibraryModule());
        binder().install(new LimeWireUploaderModule());
        binder().install(new LimeWireFiltersModule());
        binder().install(new LimeWireSpamModule());
        binder().install(new LimeWireCoreConnectionModule());
        binder().install(new LimeWireBootstrapModule());
        binder().install(new LimeWireMalwareModule());
        binder().install(new LimeWireXmlModule());
        
        if(activityCallbackClass != null) {
            bind(ActivityCallback.class).to(activityCallbackClass);
        }        

        bind(DownloadCallback.class).to(ActivityCallback.class);
        bind(NetworkManager.class).to(NetworkManagerImpl.class);
        bind(TLSManager.class).to(NetworkManagerImpl.class);
        bind(new TypeLiteral<ListenerSupport<AddressEvent>>(){}).to(NetworkManagerImpl.class);
        bind(DHTManager.class).to(DHTManagerImpl.class);
        bind(DHTControllerFactory.class).to(DHTControllerFactoryImpl.class);
        bind(PingReplyFactory.class).to(PingReplyFactoryImpl.class);
        bind(PushProxiesValueFactory.class).to(PushProxiesValueFactoryImpl.class);
        bind(HandshakeResponderFactory.class).to(HandshakeResponderFactoryImpl.class);
        bind(HeadersFactory.class).to(HeadersFactoryImpl.class);
        bind(PushEndpointFactory.class).to(PushEndpointFactoryImpl.class);
        bind(HeadPongFactory.class).to(HeadPongFactoryImpl.class);
        bind(UploadManager.class).to(HTTPUploadManager.class);
        bind(HTTPUploadSessionManager.class).to(HTTPUploadManager.class);
        bind(QueryHandlerFactory.class).to(QueryHandlerFactoryImpl.class);
        bind(QueryRequestFactory.class).to(QueryRequestFactoryImpl.class);
        bind(AltLocValueFactory.class).to(AltLocValueFactoryImpl.class);
        bind(AlternateLocationFactory.class).to(AlternateLocationFactoryImpl.class);
        bind(HttpExecutor.class).to(DefaultHttpExecutor.class);
        bind(HttpRequestHandlerFactory.class).to(HttpRequestHandlerFactoryImpl.class);
        bind(ResponseFactory.class).to(ResponseFactoryImpl.class);
        bind(QueryReplyFactory.class).to(QueryReplyFactoryImpl.class);
        bind(MessageDispatcherFactory.class).to(LimeMessageDispatcherFactoryImpl.class);
        bind(CapabilitiesVMFactory.class).to(CapabilitiesVMFactoryImpl.class);
        bind(LifecycleManager.class).to(LifecycleManagerImpl.class);
        bind(LocalPongInfo.class).to(LocalPongInfoImpl.class);
        bind(ConnectionServices.class).to(ConnectionServicesImpl.class);
        bind(SearchServices.class).to(SearchServicesImpl.class);
        bind(DownloadServices.class).to(DownloadServicesImpl.class);
        bind(UploadServices.class).to(UploadServicesImpl.class);
        bind(ApplicationServices.class).to(ApplicationServicesImpl.class);
        bind(SpamServices.class).to(SpamServicesImpl.class);
        bind(DHTControllerFacade.class).to(DHTControllerFacadeImpl.class);
        bind(DHTBootstrapperFactory.class).to(DHTBootstrapperFactoryImpl.class);
        bind(DHTNodeFetcherFactory.class).to(DHTNodeFetcherFactoryImpl.class);
        bind(UDPReplyHandlerFactory.class).to(UDPReplyHandlerFactoryImpl.class);
        bind(UDPReplyHandlerCache.class).to(UDPReplyHandlerCacheImpl.class);
        bind(SocketProcessor.class).to(Acceptor.class);
        bind(DownloadManager.class).to(DownloadManagerImpl.class).asEagerSingleton();
        bind(BrowseHostHandlerManagerImpl.class).asEagerSingleton();
        bind(ReplyNumberVendorMessageFactory.class).to(ReplyNumberVendorMessageFactoryImpl.class);
        bind(GuidMapManager.class).to(GuidMapManagerImpl.class);
        bind(BrowseHostHandlerManager.class).to(BrowseHostHandlerManagerImpl.class);
        bind(PushEndpointCache.class).to(PushEndpointCacheImpl.class);
        bind(PushEndpointService.class).annotatedWith(Names.named("pushEndpointCache")).to(PushEndpointCacheImpl.class);
        bind(FileResponseEntityFactory.class).to(FileResponseEntityFactoryImpl.class);
        bind(MessageFactory.class).to(MessageFactoryImpl.class);
        bind(MessageParserBinder.class).to(MessageParserBinderImpl.class);
        bind(VendorMessageFactory.class).to(VendorMessageFactoryImpl.class);
        bind(VendorMessageParserBinder.class).to(VendorMessageParserBinderImpl.class);
        bind(UDPCrawlerPongFactory.class).to(UDPCrawlerPongFactoryImpl.class);
        bind(LicenseFactory.class).to(LicenseFactoryImpl.class);
        bind(MetaDataFactory.class).to(MetaDataFactoryImpl.class);
        bind(SaveLocationManager.class).to(DownloadManager.class);
        bind(BTUploaderFactory.class).to(TorrentUploadManager.class);
        bind(PingRequestFactory.class).to(PingRequestFactoryImpl.class);
        bind(ConnectionDispatcher.class).annotatedWith(Names.named("global")).to(ConnectionDispatcherImpl.class).in(Scopes.SINGLETON);
        bind(ConnectionDispatcher.class).annotatedWith(Names.named("local")).to(ConnectionDispatcherImpl.class).in(Scopes.SINGLETON);
        bind(UDPPinger.class).to(UDPPingerImpl.class);
        bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderImpl.class);
        bind(SettingsProvider.class).to(MacCalculatorSettingsProviderImpl.class);
        bind(ReplyHandler.class).annotatedWith(Names.named("forMeReplyHandler")).to(ForMeReplyHandler.class);
        bind(MessageRouter.class).to(StandardMessageRouter.class);
        bind(UploadSlotManager.class).to(UploadSlotManagerImpl.class);
        bind(BandwidthManager.class).to(BandwidthManagerImpl.class);
        bind(PongCacher.class).to(PongCacherImpl.class);        
        bind(BandwidthTracker.class).annotatedWith(Names.named("uploadTracker")).to(UploadManager.class);     // For NodeAssigner.
        bind(BandwidthTracker.class).annotatedWith(Names.named("downloadTracker")).to(DownloadManager.class); // For NodeAssigner.
        bind(ResponseVerifier.class).to(ResponseVerifierImpl.class);
        bind(HandshakeServices.class).to(HandshakeServicesImpl.class);
        bind(ConnectionManager.class).to(ConnectionManagerImpl.class);
        bind(MessageHandlerBinder.class).to(MessageHandlerBinderImpl.class);
        bind(QueryDispatcher.class).to(QueryDispatcherImpl.class);
        bind(Acceptor.class).to(AcceptorImpl.class);        
        bind(SecurityToken.TokenProvider.class).to(SecurityToken.AddressSecurityTokenProvider.class);
        bind(FECUtils.class).to(FECUtilsImpl.class);
        bind(BandwidthCollector.class).to(BandwidthCollectorImpl.class);
        bind(BandwidthCollectorDriver.class).to(BandwidthCollectorImpl.class);
        bind(NodeAssigner.class).to(NodeAssignerImpl.class);
        bind(OutgoingQueryReplyFactory.class).to(OutgoingQueryReplyFactoryImpl.class);
        bind(UPnPManagerConfiguration.class).to(UPnPManagerConfigurationImpl.class);
        bind(Bootstrapper.Listener.class).to(HostCatcher.class);
        bind(RareFileStrategy.class).to(RareFileStrategyImpl.class);
        bind(MulticastService.class).to(MulticastServiceImpl.class);
        bind(CategoryManager.class).to(CategoryManagerImpl.class);
        bind(TorrentScrapeScheduler.class).to(TorrentScrapeSchedulerImpl.class);
        bind(TorrentTrackerScraper.class).to(TorrentTrackerScraperImpl.class);
        
        Executor fwtEventExecutor = ExecutorsHelper.newProcessingQueue("FirewallEventThread");        
        AsynchronousCachingEventMulticasterImpl<FirewallTransferStatusEvent> asyncTransferMulticaster 
            = new AsynchronousCachingEventMulticasterImpl<FirewallTransferStatusEvent>(fwtEventExecutor, BroadcastPolicy.IF_NOT_EQUALS);
        bind(new TypeLiteral<EventBean<FirewallTransferStatusEvent>>(){}).toInstance(asyncTransferMulticaster);
        bind(new TypeLiteral<AsynchronousEventBroadcaster<FirewallTransferStatusEvent>>(){}).toInstance(asyncTransferMulticaster);
        bind(new TypeLiteral<ListenerSupport<FirewallTransferStatusEvent>>(){}).toInstance(asyncTransferMulticaster);
        
        AsynchronousCachingEventMulticasterImpl<FirewallStatusEvent> asyncStatusMulticaster =
            new AsynchronousCachingEventMulticasterImpl<FirewallStatusEvent>(fwtEventExecutor, BroadcastPolicy.IF_NOT_EQUALS);
        bind(new TypeLiteral<EventBean<FirewallStatusEvent>>(){}).toInstance(asyncStatusMulticaster);
        bind(new TypeLiteral<AsynchronousEventBroadcaster<FirewallStatusEvent>>(){}).toInstance(asyncStatusMulticaster);
        bind(new TypeLiteral<ListenerSupport<FirewallStatusEvent>>(){}).toInstance(asyncStatusMulticaster);
        
        // These are bound because they are Singletons & Services, and must be started.
        bind(Statistics.class);
        bind(CoreRandomGlue.class);
        bind(ConnectionAcceptorGlue.class);
        bind(DownloadUpgradeTaskService.class);
        bind(LocalAcceptor.class);
        bind(Pinger.class);
        bind(ConnectionWatchdog.class);
        bind(OutOfBandThroughputMeasurer.class);
        bind(HostCatcher.class);
        bind(LimeCoreGlue.class);
        bind(QRPUpdater.class);
        bind(DaapManager.class);
        bind(FirewalledAddressSerializer.class).asEagerSingleton();
        bind(SameNATAddressResolver.class).asEagerSingleton();
        bind(ConnectableConnector.class).asEagerSingleton();
        bind(PushEndpointSerializer.class).asEagerSingleton();        
        bind(SecureIdStore.class).to(SecureIdDatabaseStore.class);
    }
    
    @Provides @Singleton SecureMessageVerifier smv() {        
        return new SecureMessageVerifierImpl("GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMAHR2O6ZOZA4SFMDNGGUC7PDA7W7HMUGEA32R7SCKAANQXFWMOD6KJE43YM53HIPVADVKFL5FA6MKL5GHTBHIURAWGGQTXPEGPLXB7KYTMC6TAPUPFYGNWB4THDQVN4PDARIU3UGXQKFHNAQFL6TUJBA6KXTBLAJBSXD54J6NUVIECRUOA7R57AH6GWGO7VOBDRTIYBXPSY7FTI",
                    null);    
    };
    
    @Provides @Singleton @Named("inspection") SecureMessageVerifier imv() {
        return new SecureMessageVerifierImpl("GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMAEYRDUD6O2YID3ORGATJV7UQLUEJORGPY4ETQUH3SKDGITTQENVN6IRZBDJOUZLD6UKX2APFEEA6IJVMCURT4VWBICX5L7GKAUYU325AMMNR7PW6GWGXHR24D5HVTIO6JZ2VRMTOIE7GIZPINPOJXWYDUZQG57ZVBII6XHW2KGITQKQLODJTZGRJHELY6BRXL7VHHQDGCIBWYU",
                    null);
    }
    
    private static final String UNLIMITED = "unlimitedExecutor";
    @Provides @LazySingleton @Named(UNLIMITED) ListeningExecutorService unlimitedLES() {
        return ExecutorsHelper.newThreadPool(ExecutorsHelper.daemonThreadFactory("IdleThread"));
    }
    @Provides @LazySingleton @Named(UNLIMITED) ExecutorService unlimitedES(@Named(UNLIMITED) ListeningExecutorService les) {
        return les;
    }
    @Provides @LazySingleton @Named(UNLIMITED) Executor unlimitedE(@Named(UNLIMITED) ListeningExecutorService les) {
        return les;
    }
    
    private static final String FAST = "fastExecutor";
    @Provides @LazySingleton @Named(FAST) ScheduledExecutorService fastSES() {
        LimeScheduledThreadPoolExecutor executor 
            = new LimeScheduledThreadPoolExecutor(1, 
                    ExecutorsHelper.daemonThreadFactory("FastExecutor")); 
        
        JmxUtils.add(LimeWireCoreModule.class, FAST, new ExecutorBean.Impl(executor));
        
        return executor;
    }
    @Provides @LazySingleton @Named(FAST) ExecutorService fastES(@Named(FAST) ScheduledExecutorService ses) {
        return ses;
    }
    @Provides @LazySingleton @Named(FAST) Executor fastE(@Named(FAST) ScheduledExecutorService ses) {
        return ses;
    }
    
    private static final String BACKGROUND = "backgroundExecutor";
    @Provides @LazySingleton @Named(BACKGROUND) ScheduledListeningExecutorService backgroundSLES() {
        return new SimpleTimer("backgroundExecutor", true);
    }    
    @Provides @LazySingleton @Named(BACKGROUND) ScheduledExecutorService backgroundSES(@Named(BACKGROUND) ScheduledListeningExecutorService lses) {
        return lses;
    }
    @Provides @LazySingleton @Named(BACKGROUND) ExecutorService backgroundES(@Named(BACKGROUND) ScheduledListeningExecutorService lsel) {
        return lsel;
    }
    @Provides @LazySingleton @Named(BACKGROUND) Executor backgroundE(@Named(BACKGROUND) ScheduledListeningExecutorService lsel) {
        return lsel;
    }
    
    private static final String MESSAGE = "messageExecutor";
    @Provides @LazySingleton @Named(MESSAGE) ListeningExecutorService messageLES() {
        return ExecutorsHelper.newProcessingQueue("Message-Executor");
    }
    @Provides @LazySingleton @Named(MESSAGE) ExecutorService messageES(@Named(MESSAGE) ListeningExecutorService les) {
        return les;
    }
    @Provides @LazySingleton @Named(MESSAGE) Executor messageE(@Named(MESSAGE) ListeningExecutorService les) {
        return les;
    }
    
    @Provides @Singleton EventMulticaster<MessageSentEvent> messageSentEM(@Named("messageExecutor") Executor executor) { 
        return new AsynchronousMulticasterImpl<MessageSentEvent>(executor); 
    } 
    
    @Provides @Singleton EventBroadcaster<MessageSentEvent> messageSentEB(EventMulticaster<MessageSentEvent> multicaster) { 
        return multicaster; 
    } 
    
    @Provides @Singleton ListenerSupport<MessageSentEvent> messageSentLS(EventMulticaster<MessageSentEvent> multicaster) { 
        return multicaster; 
    }
    
    private static final String DHT = "dhtExecutor";
    @Provides @LazySingleton @Named(DHT) ListeningExecutorService dhtLES() {
        return ExecutorsHelper.newProcessingQueue("DHT-Executor");
    }
    @Provides @LazySingleton @Named(DHT) ExecutorService dhtES(@Named(DHT) ListeningExecutorService les) {
        return les;
    }
    @Provides @LazySingleton @Named(DHT) Executor dhtE(@Named(DHT) ListeningExecutorService les) {
        return les;
    }
    
    @Provides @ExternalIP byte[] externalAddr(NetworkManager networkManager) {
        return networkManager.getExternalAddress();
    }
    
    ///////////////////////////////////////////////////////////////////////////
    /// BELOW ARE ALL HACK PROVIDERS THAT NEED TO BE UPDATED TO CONSTRUCT OBJECTS!
    // (This needs to wait till components are injected and stop using singletons too.)
    
    @Provides @EagerSingleton NIODispatcher nd() {
        return NIODispatcher.instance();
    }
    
    @Provides @LazySingleton ByteBufferCache bbc(NIODispatcher nd) {
        return nd.getBufferCache();
    }
    
    private static final String NIO = "nioExecutor";
    @Provides @LazySingleton @Named(NIO) ScheduledExecutorService nioSES(NIODispatcher nd) {
        return nd.getScheduledExecutorService(); 
    }
    @Provides @LazySingleton @Named(NIO) ExecutorService nioES(NIODispatcher nd) {
        return nd.getScheduledExecutorService();
    }
    @Provides @LazySingleton @Named(NIO) Executor nioE(NIODispatcher nd) {
        return nd.getScheduledExecutorService();
    }
        
    ///////////////////////////////////////////////////////////////
    // !!! DO NOT ADD THINGS BELOW HERE !!!  PUT THEM ABOVE THE HACKS!
}

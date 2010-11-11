package org.limewire.net;

import java.util.Map;
import java.util.HashMap;

import org.limewire.net.SocketsManager;
import org.limewire.net.WhoIsRequest;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Allows you to create a new WhoIsRequest instance.
 */
@Singleton
public class WhoIsRequestFactoryImpl implements WhoIsRequestFactory {

    /**
     * Sockets manager. Keeps sockets within system limits.
     */
    private final Provider<SocketsManager> socketsManager;
    
    /**
     * List of default whois servers.
     */
    private final Map<String,String> defaultServers;
       
    /**
     * Factory constructor. Uses default server list.
     * 
     */
    @Inject
    public WhoIsRequestFactoryImpl (Provider<SocketsManager> socketsManager) {
        this.socketsManager = socketsManager;
        this.defaultServers = new HashMap<String,String>();
        
        // Found this list at: http://www.math.utah.edu/whois.html
        this.defaultServers.put("0", "whois.arin.net");
        this.defaultServers.put("iq", "vrx.net");
        this.defaultServers.put("128", "whois.arin.net");
        this.defaultServers.put("192", "whois.arin.net");
        this.defaultServers.put("64", "whois.arin.net");
        this.defaultServers.put("800", "whois.vrx.net");
        this.defaultServers.put("888", "whois.vrx.net");
        this.defaultServers.put("ac", "whois.nic.ac");
        this.defaultServers.put("af", "whois.nic.af");
        this.defaultServers.put("al", "whois.ripe.net");
        this.defaultServers.put("am", "whois.amnic.net");
        this.defaultServers.put("apotheke", "whois.apo-nic.de");
        this.defaultServers.put("art", "whois.skyscape.net");
        this.defaultServers.put("arts", "whois.skyscape.net");
        this.defaultServers.put("as", "whois.nic.as");
        this.defaultServers.put("at", "whois.ripe.net");
        this.defaultServers.put("au", "whois.aunic.net");
        this.defaultServers.put("ave", "whois.quasar.net");
        this.defaultServers.put("ave", "whois.quasar.net");
        this.defaultServers.put("az", "whois.ripe.net");
        this.defaultServers.put("ba", "whois.ripe.net");
        this.defaultServers.put("bank", "whois.skyscape.net");
        this.defaultServers.put("be", "whois.ripe.net");
        this.defaultServers.put("bg", "whois.ripe.net");
        this.defaultServers.put("biz", "whois.neulevel.biz");
        this.defaultServers.put("blues", "whois.quasar.net");
        this.defaultServers.put("blues", "whois.quasar.net");
        this.defaultServers.put("bot", "whois.quasar.net");
        this.defaultServers.put("br", "whois.registro.br");
        this.defaultServers.put("bt", "whois.netnames.net");
        this.defaultServers.put("by", "whois.ripe.net");
        this.defaultServers.put("ca", "whois.cira.ca");
        this.defaultServers.put("cc", "whois.nic.cc");
        this.defaultServers.put("cdn", "whois.vrx.net");
        this.defaultServers.put("cgi", "whois.quasar.net");
        this.defaultServers.put("ch", "whois.nic.ch");
        this.defaultServers.put("ck", "whois.nic.ck");
        this.defaultServers.put("cl", "nic.cl");
        this.defaultServers.put("cn", "whois.apnic.net");
        this.defaultServers.put("com", "whois.verisign-grs.com");
        this.defaultServers.put("corp", "rs.mcs.net");
        this.defaultServers.put("cx", "whois.nic.cx");
        this.defaultServers.put("cy", "whois.ripe.net");
        this.defaultServers.put("cz", "whois.ripe.net");
        this.defaultServers.put("dds", "whois.vrx.net");
        this.defaultServers.put("de", "whois.ripe.net");
        this.defaultServers.put("depot", "whois.vrx.net");
        this.defaultServers.put("dev", "whois.quasar.net");
        this.defaultServers.put("dir", "whois.skyscape.net");
        this.defaultServers.put("dk", "whois.dk");
        this.defaultServers.put("dz", "whois.ripe.net");
        this.defaultServers.put("earth", "whois.adns.net");
        this.defaultServers.put("edu", "whois.educause.net");
        this.defaultServers.put("ee", "whois.ripe.net");
        this.defaultServers.put("eg", "whois.ripe.net");
        this.defaultServers.put("es", "whois.ripe.net");
        this.defaultServers.put("event", "whois.quasar.net");
        this.defaultServers.put("faq", "whois.vrx.net");
        this.defaultServers.put("fi", "whois.ripe.net");
        this.defaultServers.put("film", "whois.skyscape.net");
        this.defaultServers.put("florida", "whois.quasar.net");
        this.defaultServers.put("fo", "whois.ripe.net");
        this.defaultServers.put("fr", "whois.ripe.net");
        this.defaultServers.put("fund", "whois.skyscape.net");
        this.defaultServers.put("gallery", "whois.vrx.net");
        this.defaultServers.put("gb", "whois.ripe.net");
        this.defaultServers.put("ge", "whois.ripe.net");
        this.defaultServers.put("gm", "whois.ripe.net");
        this.defaultServers.put("gmbh", "whois.vrx.net");
        this.defaultServers.put("gov", "whois.nic.gov");
        this.defaultServers.put("gr", "whois.ripe.net");
        this.defaultServers.put("gs", "whois.adamsnames.tc");
        this.defaultServers.put("heck", "whois.quasar.net");
        this.defaultServers.put("help", "whois.skyscape.net");
        this.defaultServers.put("here", "whois.quasar.net");
        this.defaultServers.put("here", "whois.quasar.net");
        this.defaultServers.put("hk", "whois.hknic.net.hk");
        this.defaultServers.put("hosts", "whois.quasar.net");
        this.defaultServers.put("hotel", "whois.skyscape.net");
        this.defaultServers.put("hr", "whois.ripe.net");
        this.defaultServers.put("hu", "whois.ripe.net");
        this.defaultServers.put("humanrights", "whois.vrx.net");
        this.defaultServers.put("hwy", "whois.quasar.net");
        this.defaultServers.put("ie", "whois.ripe.net");
        this.defaultServers.put("il", "whois.ripe.net");
        this.defaultServers.put("info", "whois.afilias.info");
        this.defaultServers.put("ins", "whois.quasar.net");
        this.defaultServers.put("int", "whois.isi.edu");
        this.defaultServers.put("is", "whois.ripe.net");
        this.defaultServers.put("isp", "whois.skyscape.net");
        this.defaultServers.put("it", "whois.nic.it");
        this.defaultServers.put("java", "whois.quasar.net");
        this.defaultServers.put("jp", "whois.nic.ad.jp");
        this.defaultServers.put("kg", "whois.domain.kg");
        this.defaultServers.put("kr", "whois.nic.or.kr");
        this.defaultServers.put("li", "whois.nic.li");
        this.defaultServers.put("life", "whois.quasar.net");
        this.defaultServers.put("list", "whois.vrx.net");
        this.defaultServers.put("llb", "whois.vrx.net");
        this.defaultServers.put("lol", "whois.quasar.net");
        this.defaultServers.put("lt", "whois.ripe.net");
        this.defaultServers.put("lu", "whois.restena.lu");
        this.defaultServers.put("luv", "whois.quasar.net");
        this.defaultServers.put("lv", "whois.ripe.net");
        this.defaultServers.put("ma", "whois.ripe.net");
        this.defaultServers.put("mart", "whois.quasar.net");
        this.defaultServers.put("mart", "whois.quasar.net");
        this.defaultServers.put("mbx", "whois.quasar.net");
        this.defaultServers.put("md", "whois.ripe.net");
        this.defaultServers.put("med", "whois.skyscape.net");
        this.defaultServers.put("mil", "whois.nic.mil");
        this.defaultServers.put("mk", "whois.ripe.net");
        this.defaultServers.put("moi", "whois.quasar.net");
        this.defaultServers.put("ms", "whois.adamsnames.tc");
        this.defaultServers.put("mt", "whois.ripe.net");
        this.defaultServers.put("music", "whois.skyscape.net");
        this.defaultServers.put("mx", "nic.mx");
        this.defaultServers.put("net", "whois.verisign-grs.com");
        this.defaultServers.put("nic", "whois.vrx.net");
        this.defaultServers.put("nl", "whois.nic.nl");
        this.defaultServers.put("no", "whois.norid.no");
        this.defaultServers.put("npo", "rs.mcs.net");
        this.defaultServers.put("nu", "whois.nic.nu");
        this.defaultServers.put("org", "whois.publicinterestregistry.net");
        this.defaultServers.put("php", "whois.quasar.net");
        this.defaultServers.put("pics", "whois.quasar.net");
        this.defaultServers.put("pka", "whois.nic.de");
        this.defaultServers.put("pl", "whois.ripe.net");
        this.defaultServers.put("plan", "whois.quasar.net");
        this.defaultServers.put("prices", "whois.vrx.net");
        this.defaultServers.put("pt", "whois.ripe.net");
        this.defaultServers.put("pta", "whois.nic.de");
        this.defaultServers.put("radio", "whois.skyscape.net");
        this.defaultServers.put("ro", "whois.ripe.net");
        this.defaultServers.put("rofl", "whois.quasar.net");
        this.defaultServers.put("ru", "whois.ripn.ru");
        this.defaultServers.put("safe", "whois2.vrx.net");
        this.defaultServers.put("script", "whois.quasar.net");
        this.defaultServers.put("se", "whois.nic.se");
        this.defaultServers.put("sex", "ns2.dotsexmachine.com");
        this.defaultServers.put("sg", "whois.nic.net.sg");
        this.defaultServers.put("sh", "whois.nic.sh");
        this.defaultServers.put("si", "whois.ripe.net");
        this.defaultServers.put("sk", "whois.ripe.net");
        this.defaultServers.put("sky", "whois.skyscape.net");
        this.defaultServers.put("sm", "whois.ripe.net");
        this.defaultServers.put("speed", "whois.quasar.net");
        this.defaultServers.put("sql", "whois.vrx.net");
        this.defaultServers.put("st", "whois.nic.st");
        this.defaultServers.put("su", "whois.ripn.net");
        this.defaultServers.put("swap", "whois.vrx.net");
        this.defaultServers.put("tc", "whois.adamsnames.tc");
        this.defaultServers.put("texas", "whois.quasar.net");
        this.defaultServers.put("tf", "whois.adamsnames.tc");
        this.defaultServers.put("th", "whois.thnic.net");
        this.defaultServers.put("this", "whois.quasar.net");
        this.defaultServers.put("this", "whois.quasar.net");
        this.defaultServers.put("tj", "whois.nic.tj");
        this.defaultServers.put("tm", "whois.nic.tm");
        this.defaultServers.put("tn", "whois.ripe.net");
        this.defaultServers.put("to", "whois.tonic.to");
        this.defaultServers.put("tr", "whois.ripe.net");
        this.defaultServers.put("tux", "whois.quasar.net");
        this.defaultServers.put("tw", "whois.apnic.net");
        this.defaultServers.put("ua", "whois.ripe.net");
        this.defaultServers.put("uk", "whois.nic.uk");
        this.defaultServers.put("us", "whois.nic.us");
        this.defaultServers.put("usa", "whois.adns.net");
        this.defaultServers.put("va", "whois.ripe.net");
        this.defaultServers.put("ve", "whois.reacciun.ve");
        this.defaultServers.put("vg", "whois.adamsnames.tc");
        this.defaultServers.put("video", "whois.skyscape.net");
        this.defaultServers.put("whois", "whois.pccf.net");
        this.defaultServers.put("xxx", "whois.skyscape.net");
        this.defaultServers.put("y3k", "whois.quasar.net");
        this.defaultServers.put("yu", "whois.ripe.net");
        this.defaultServers.put("z", "whois.adns.net");
        this.defaultServers.put("zine", "whois.skyscape.net");
        this.defaultServers.put("zoo", "whois.vrx.net");
    }
    
    /**
     * Specify the request name (ie, "google.com", "64.233.187.99", etc.).
     */
    public WhoIsRequest createWhoIsRequest (String name) {
        return new WhoIsRequestImpl(name, this.socketsManager.get(), this.defaultServers);
    }
    
    /**
     * Specify the request name and a server list.
     */
    public WhoIsRequest createWhoIsRequest (String name, Map<String,String> servers) {
        return new WhoIsRequestImpl(name, this.socketsManager.get(), servers);
    }
    
}

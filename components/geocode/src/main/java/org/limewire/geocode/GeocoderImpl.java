package org.limewire.geocode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

final class GeocoderImpl implements Geocoder {
    
    private static final Log LOG = LogFactory.getLog(GeocoderImpl.class);

    private final Provider<String> geoCodeURL;
    private final Provider<LimeHttpClient> httpClient;

    @Inject
    public GeocoderImpl(@GeocodeUrl Provider<String> geoCodeURL,
                        Provider<LimeHttpClient> client) {
        this.geoCodeURL = geoCodeURL;
        this.httpClient = client;
    }

    public GeocodeInformation getGeocodeInformation() {
        String url = geoCodeURL.get();
        if (url.isEmpty()) {
            return new GeocodeInformation();
        }
        
        HttpGet get = new HttpGet(url);        
        LimeHttpClient client = httpClient.get();
        HttpResponse response = null;
        try {
            // TODO: The following call seems to hang on some systems.
            response = client.execute(get);
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if(entity != null) {
                    String charset = EntityUtils.getContentCharSet(entity);
                    return parseGeoInfo(entity.getContent(), charset != null ? charset : HTTP.DEFAULT_CONTENT_CHARSET);
                }
            }            
        } catch (IOException e) {
            LOG.debug("error parsing", e);
        } finally {
            client.releaseConnection(response);
        }
        return GeocodeInformation.EMPTY_GEO_INFO;
    }

    /**
     * Read the lines and set the fields appropriately. The fields will be
     * name/value pairs separated by tabs (i.e. <code>\t</code>). The name
     * correspond to the set method on {@link GeocodeInformationImpl}.
     * <p>
     * For example: <code>countryName   United States</code> would cause a call
     * to <code>g.setCountryName("United States").
     * 
     * @param is input lines of the form
     * <blockquote>
     * &lt;first line ignored&gt;<br>
     * ( String[<em>Name</em>] &lt;tab&gt; String[<em>Value</em>] &lt;newline&gt; )<br>
     * [ repeat name/value pairs ]
     * </blockquote>
     * @param charset
     * @throws java.io.IOException 
     */
    private GeocodeInformation parseGeoInfo(InputStream is, String charset) throws IOException {

        GeocodeInformation res = new GeocodeInformation();

        String separator = "\t";

        BufferedReader in = new BufferedReader(new InputStreamReader(is, charset));
        
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(separator);
            if (parts.length < 2) {
                continue;
            }
            String name = parts[0];
            String value = parts[1];
            res.setProperty(name, value);
        }
        return res;
    }
}

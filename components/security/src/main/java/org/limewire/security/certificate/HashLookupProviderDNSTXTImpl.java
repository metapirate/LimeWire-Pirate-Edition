package org.limewire.security.certificate;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import com.google.inject.Singleton;

/**
 * DNS lookup-backed provider, takes given keys and does a DNS lookup for a TXT
 * record matching the key. We'd expect the key to be something like
 * 'something.auth.limewire.com'.
 */
@Singleton
public class HashLookupProviderDNSTXTImpl implements HashLookupProvider {
    private static final Log LOG = LogFactory.getLog(HashLookupProviderDNSTXTImpl.class);

    public String lookup(String key) {
        try {
            Lookup lookup = new Lookup(key, Type.TXT);
            lookup.run();

            int result = lookup.getResult();
            if (result != Lookup.SUCCESSFUL)
                throw new IOException("Error during lookup: " + lookup.getErrorString());

            Record[] answers = lookup.getAnswers();
            if (answers == null || answers.length != 1)
                throw new IOException("Incorrect number of answers, expected 1.");
            return stripLeadingTrailingQuotes(answers[0].rdataToString());
        } catch (IOException ex) {
            LOG.error("Failed lookup for key '" + key + "'", ex);
            return null;
        }
    }

    String stripLeadingTrailingQuotes(String incoming) {
        if (incoming == null || incoming.length() == 0)
            return incoming;
        boolean start = incoming.charAt(0) == '\"';
        boolean end = incoming.charAt(incoming.length() - 1) == '\"';
        return incoming.substring(start ? 1 : 0, incoming.length() - (end ? 1 : 0));
    }
}

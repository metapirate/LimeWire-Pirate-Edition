package org.limewire.ui.swing.browser;

import java.util.Locale;

import org.limewire.util.Objects;

/** An action taken when an href with a target is clicked in the browser. */
public interface UriAction {

    public static class TargetedUri {
        private final String target;

        private final String url;

        private final String protocol;

        public TargetedUri(String target, String uri) {
            this.target = Objects.nonNull(target, "target");
            this.url = uri;
            this.protocol = extractProtocol(uri);
        }

        /**
         * @return null if there is no protocol.
         */
        private static String extractProtocol(String url) {
            int colon = url.indexOf(':');
            if (colon != -1) {
                String protocol = url.substring(0, colon);
                return protocol.toLowerCase(Locale.US);
            }
            return null;
        }

        /**
         * @return empty string if uri does not contain target
         */
        public String getTarget() {
            return target;
        }

        public String getUri() {
            return url;
        }

        /**
         * @return null if URI didn't have one
         */
        public String getProtocol() {
            return protocol;
        }
    }

    /**
     * Notification that a targeted URL has been clicked.
     * 
     * @return indicator of whether or not this UriAction handled the href
     */
    public boolean uriClicked(TargetedUri targetedUri);

}

package org.limewire.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.protocol.HttpContext;

/**
 * Iterates over all headers of a {@link HttpMessage} and invokes interested
 * {@link HeaderInterceptor}s for processing.
 */
public class BasicHeaderProcessor {

    private List<HeaderInterceptor> headerInterceptors = null;

    /**
     * Adds <code>headerInterceptor</code> to list of interceptors. The
     * <code>interceptor</code> is notified when
     * {@link #process(HttpMessage, HttpContext)} is invoked.
     * 
     * @see #removeInterceptor(HeaderInterceptor)
     */
    public void addInterceptor(final HeaderInterceptor headerInterceptor) {
        if (headerInterceptor == null) {
            throw new IllegalArgumentException();
        }

        if (this.headerInterceptors == null) {
            this.headerInterceptors = new ArrayList<HeaderInterceptor>();
        }
        this.headerInterceptors.add(headerInterceptor);
    }

    /**
     * Removes <code>headerInterceptor</code> from the list of interceptors.
     *
     * @see #addInterceptor(HeaderInterceptor)
     */
    public void removeInterceptor(final HeaderInterceptor headerInterceptor) {
        if (headerInterceptor == null) {
            throw new IllegalArgumentException();
        }

        if (this.headerInterceptors != null) {
            this.headerInterceptors.remove(headerInterceptor);
        }
    }

    /**
     * Removes all interceptors.
     */
    public void clearInterceptors() {
        this.headerInterceptors = null;
    }

    /**
     * Returns all interceptors.
     * 
     * @return the returned array is a new instance, modifications of the array
     *         will not be reflected in the list of interceptors
     */
    public HeaderInterceptor[] getInterceptors() {
        return (headerInterceptors != null) ? headerInterceptors.toArray(new HeaderInterceptor[0]) : new HeaderInterceptor[0];
    }

    /**
     * Iterates of the headers of <code>message</code> and invokes
     * {@link HeaderInterceptor#process(Header, HttpContext)} for all
     * interceptors on each header.
     * 
     * @param message the message that provides the headers
     * @param context the context that is passed to
     *        {@link HeaderInterceptor#process(Header, HttpContext)}
     * @throws IOException thrown when a processing error occurs
     * @throws HttpException thrown when a processing error occurs
     */
    public void process(final HttpMessage message, final HttpContext context)
            throws IOException, HttpException {
        if (this.headerInterceptors != null) {
            Header[] headers = message.getAllHeaders();
            for (Header header : headers) {
                for (HeaderInterceptor interceptor : headerInterceptors) {
                    interceptor.process(header, context);
                }
            }
        }
    }

}

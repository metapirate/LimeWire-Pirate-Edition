package org.limewire.http.protocol;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;

/**
 * A synchronized thread-safe of {@link HttpProcessor}. It is safe to add and
 * remove interceptors on a different threads than the thread that invokes
 * <code>process()</code>.
 */
public class SynchronizedHttpProcessor implements HttpProcessor {

    private final CopyOnWriteArrayList<HttpRequestInterceptor> requestInterceptors = new CopyOnWriteArrayList<HttpRequestInterceptor>();

    private final CopyOnWriteArrayList<HttpResponseInterceptor> responseInterceptors = new CopyOnWriteArrayList<HttpResponseInterceptor>();

    public SynchronizedHttpProcessor() {
    }
    
    public void addInterceptor(final HttpRequestInterceptor interceptor) {
        this.requestInterceptors.add(interceptor);
    }

    public void addInterceptor(final HttpResponseInterceptor interceptor) {
        this.responseInterceptors.add(interceptor);
    }

    public void process(HttpRequest request, HttpContext context)
            throws HttpException, IOException {
        for (HttpRequestInterceptor interceptor : requestInterceptors) {
            interceptor.process(request, context);
        }
    }

    public void process(HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        for (HttpResponseInterceptor interceptor : responseInterceptors) {
            interceptor.process(response, context);
        }
    }

    public void removeInterceptor(final HttpRequestInterceptor interceptor) {
        this.requestInterceptors.remove(interceptor);
    }

    public void removeInterceptor(final HttpResponseInterceptor interceptor) {
        this.responseInterceptors.remove(interceptor);
    }

}

package org.skywalking.apm.plugin.httpClient.v4;

import org.apache.http.*;
import org.skywalking.apm.agent.core.context.ContextCarrier;
import org.skywalking.apm.agent.core.context.ContextManager;
import org.skywalking.apm.agent.core.plugin.interceptor.EnhancedClassInstanceContext;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodInvokeContext;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.skywalking.apm.trace.Span;
import org.skywalking.apm.trace.tag.Tags;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * {@link HttpClientExecuteInterceptor} transport the trace context by call {@link HttpRequest#setHeader(Header)},
 * The current span tag the {@link Tags#ERROR} if {@link StatusLine#getStatusCode()} is not equals 200.
 *
 * @author zhangxin
 */
public class HttpClientExecuteInterceptor implements InstanceMethodsAroundInterceptor {
    public static final String HEADER_NAME_OF_CONTEXT_DATA = "SWTraceContext";
    private static final String COMPONENT_NAME = "HttpClient";

    @Override
    public void beforeMethod(EnhancedClassInstanceContext context,
                             InstanceMethodInvokeContext interceptorContext, MethodInterceptResult result) {
        Object[] allArguments = interceptorContext.allArguments();
        if (allArguments[0] == null || allArguments[1] == null) {
            // illegal args, can't trace. ignore.
            return;
        }
        HttpHost httpHost = (HttpHost) allArguments[0];
        HttpRequest httpRequest = (HttpRequest) allArguments[1];
        Span span = createSpan(httpRequest);
        Tags.PEER_PORT.set(span, httpHost.getPort());
        Tags.PEER_HOST.set(span, httpHost.getHostName());
        Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_CLIENT);
        Tags.COMPONENT.set(span, COMPONENT_NAME);
        Tags.URL.set(span, generateURL(httpRequest));
        Tags.SPAN_LAYER.asHttp(span);

        ContextCarrier contextCarrier = new ContextCarrier();
        ContextManager.inject(contextCarrier);
        httpRequest.setHeader(HEADER_NAME_OF_CONTEXT_DATA, contextCarrier.serialize());
    }

    /**
     * Format request URL.
     *
     * @return request URL
     */
    private String generateURL(HttpRequest httpRequest) {
        return httpRequest.getRequestLine().getUri();
    }

    /**
     * Create span.
     */
    private Span createSpan(HttpRequest httpRequest) {
        Span span;
        try {
            URL url = new URL(httpRequest.getRequestLine().getUri());
            span = ContextManager.createSpan(url.getPath());
        } catch (MalformedURLException e) {
            span = ContextManager.createSpan(httpRequest.getRequestLine().getUri());
        }
        return span;
    }

    @Override
    public Object afterMethod(EnhancedClassInstanceContext context,
                              InstanceMethodInvokeContext interceptorContext, Object ret) {
        Object[] allArguments = interceptorContext.allArguments();
        if (allArguments[0] == null || allArguments[1] == null) {
            return ret;
        }

        HttpResponse response = (HttpResponse) ret;
        int statusCode = response.getStatusLine().getStatusCode();
        Span span = ContextManager.activeSpan();
        if (statusCode != 200) {
            Tags.ERROR.set(span, true);
        }

        Tags.STATUS_CODE.set(span, statusCode);
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(Throwable t, EnhancedClassInstanceContext context,
                                      InstanceMethodInvokeContext interceptorContext) {
        Tags.ERROR.set(ContextManager.activeSpan(), true);
        ContextManager.activeSpan().log(t);
    }

}

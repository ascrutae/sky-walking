package org.skywalking.apm.plugin.resin.v4;

import com.caucho.server.http.CauchoRequest;
import com.caucho.server.http.HttpRequest;
import javax.servlet.http.HttpServletResponse;
import org.skywalking.apm.agent.core.context.ContextCarrier;
import org.skywalking.apm.agent.core.context.ContextManager;
import org.skywalking.apm.agent.core.plugin.interceptor.EnhancedClassInstanceContext;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodInvokeContext;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.skywalking.apm.trace.Span;
import org.skywalking.apm.trace.tag.Tags;
import org.skywalking.apm.util.StringUtil;

/**
 * Created by Baiyang on 2017/5/2.
 */
public class ResinV4Interceptor implements InstanceMethodsAroundInterceptor {
    /**
     * Header name that the serialized context data stored in
     * {@link HttpRequest#getHeader(String)}.
     */
    public static final String HEADER_NAME_OF_CONTEXT_DATA = "SWTraceContext";
    /**
     * Resin component.
     */
    public static final String RESIN_COMPONENT = "Resin";

    @Override
    public void beforeMethod(EnhancedClassInstanceContext context, InstanceMethodInvokeContext interceptorContext,
        MethodInterceptResult result) {
        Object[] args = interceptorContext.allArguments();
        CauchoRequest request = (CauchoRequest)args[0];
        Span span = ContextManager.createSpan(request.getPageURI());
        Tags.COMPONENT.set(span, RESIN_COMPONENT);
        Tags.PEER_HOST.set(span, request.getServerName());
        Tags.PEER_PORT.set(span, request.getServerPort());
        Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_SERVER);
        Tags.URL.set(span, appendRequestURL(request));
        Tags.SPAN_LAYER.asHttp(span);

        String tracingHeaderValue = request.getHeader(HEADER_NAME_OF_CONTEXT_DATA);
        if (!StringUtil.isEmpty(tracingHeaderValue)) {
            ContextManager.extract(new ContextCarrier().deserialize(tracingHeaderValue));
        }
    }

    /**
     * Append request URL.
     *
     * @param request
     * @return
     */
    private String appendRequestURL(CauchoRequest request) {
        StringBuffer sb = new StringBuffer();
        sb.append(request.getScheme());
        sb.append("://");
        sb.append(request.getServerName());
        sb.append(":");
        sb.append(request.getServerPort());
        sb.append(request.getPageURI());
        return sb.toString();
    }

    @Override
    public Object afterMethod(EnhancedClassInstanceContext context, InstanceMethodInvokeContext interceptorContext,
        Object ret) {
        HttpServletResponse response = (HttpServletResponse)interceptorContext.allArguments()[1];
        Span span = ContextManager.activeSpan();
        Tags.STATUS_CODE.set(span, response.getStatus());

        if (response.getStatus() != 200) {
            Tags.ERROR.set(span, true);
        }
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(Throwable t, EnhancedClassInstanceContext context,
        InstanceMethodInvokeContext interceptorContext) {
        Span span = ContextManager.activeSpan();
        span.log(t);
        Tags.ERROR.set(span, true);
    }
}

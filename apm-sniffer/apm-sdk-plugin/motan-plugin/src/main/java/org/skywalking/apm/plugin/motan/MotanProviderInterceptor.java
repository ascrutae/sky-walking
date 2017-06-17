package org.skywalking.apm.plugin.motan;

import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import org.skywalking.apm.agent.core.context.ContextCarrier;
import org.skywalking.apm.agent.core.context.ContextManager;
import org.skywalking.apm.agent.core.plugin.interceptor.EnhancedClassInstanceContext;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodInvokeContext;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.skywalking.apm.util.StringUtil;
import org.skywalking.apm.trace.Span;
import org.skywalking.apm.trace.tag.Tags;

/**
 * Current trace segment will ref the trace segment if the serialized trace context that fetch from {@link
 * Request#getAttachments()} is not null.
 * <p>
 * {@link MotanConsumerInterceptor} intercept all constructor of {@link com.weibo.api.motan.rpc.AbstractProvider} for
 * record the request url from consumer side.
 *
 * @author zhangxin
 */
public class MotanProviderInterceptor implements InstanceMethodsAroundInterceptor {

    /**
     * Attachment key of the serialized context data.
     */
    private static final String ATTACHMENT_KEY_OF_CONTEXT_DATA = "SWTraceContext";

    /**
     * Motan component
     */
    private static final String MOTAN_COMPONENT = "Motan";

    @Override
    public void beforeMethod(EnhancedClassInstanceContext context, InstanceMethodInvokeContext interceptorContext,
                             MethodInterceptResult result) {
        Request request = (Request) interceptorContext.allArguments()[0];
        Span span = ContextManager.createSpan(generateViewPoint(request));
        Tags.COMPONENT.set(span, MOTAN_COMPONENT);
        Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_SERVER);
        Tags.SPAN_LAYER.asRPCFramework(span);

        String serializedContextData = request.getAttachments().get(ATTACHMENT_KEY_OF_CONTEXT_DATA);
        if (!StringUtil.isEmpty(serializedContextData)) {
            ContextManager.extract(new ContextCarrier().deserialize(serializedContextData));
        }
    }

    @Override
    public Object afterMethod(EnhancedClassInstanceContext context, InstanceMethodInvokeContext interceptorContext,
                              Object ret) {
        Response response = (Response) ret;
        if (response != null && response.getException() != null) {
            Span span = ContextManager.activeSpan();
            span.log(response.getException());
            Tags.ERROR.set(span, true);
        }

        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(Throwable t, EnhancedClassInstanceContext context,
                                      InstanceMethodInvokeContext interceptorContext) {
        ContextManager.activeSpan().log(t);
    }

    private static String generateViewPoint(Request request) {
        StringBuilder viewPoint = new StringBuilder(request.getInterfaceName());
        viewPoint.append("." + request.getMethodName());
        viewPoint.append("(" + request.getParamtersDesc() + ")");
        return viewPoint.toString();
    }
}

package org.skywalking.apm.plugin.tomcat78x;

import java.lang.reflect.Method;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.skywalking.apm.agent.core.conf.Config;
import org.skywalking.apm.agent.core.context.ContextCarrier;
import org.skywalking.apm.agent.core.context.ContextManager;
import org.skywalking.apm.agent.core.context.tag.Tags;
import org.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.skywalking.apm.logging.ILog;
import org.skywalking.apm.logging.LogManager;
import org.skywalking.apm.network.trace.component.ComponentsDefine;

/**
 * {@link TomcatInterceptor} fetch the serialized context data by using {@link HttpServletRequest#getHeader(String)}.
 * The {@link TraceSegment#refs} of current trace segment will reference to the trace segment id of the previous level
 * if the serialized context is not null.
 */
public class TomcatInterceptor implements InstanceMethodsAroundInterceptor {

    private ILog logger = LogManager.getLogger(TomcatInterceptor.class);

    /**
     * * The {@link TraceSegment#refs} of current trace segment will reference to the trace segment id of the previous
     * level if the serialized context is not null.
     *
     * @param objInst
     * @param method
     * @param allArguments
     * @param argumentsTypes
     * @param result change this result, if you want to truncate the method.
     * @throws Throwable
     */
    @Override public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        HttpServletRequest request = (HttpServletRequest)allArguments[0];
        String tracingHeaderValue = request.getHeader(Config.Plugin.Propagation.HEADER_NAME);
        ContextCarrier contextCarrier = new ContextCarrier().deserialize(tracingHeaderValue);
        AbstractSpan span = ContextManager.createEntrySpan(request.getRequestURI(), contextCarrier);
        Tags.URL.set(span, request.getRequestURL().toString());
        Tags.HTTP.METHOD.set(span, request.getMethod());
        span.setComponent(ComponentsDefine.TOMCAT);
        SpanLayer.asHttp(span);

    }

    @Override public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Object ret) throws Throwable {
        List<AbstractTracingSpan> activeSpans = ContextManager.activeSpans();
        if (activeSpans.size() != 1){
            StringBuilder logInfo = new StringBuilder("[\n");
            for (AbstractTracingSpan span : activeSpans) {
                logInfo.append("<" + span.getParentSpanId() + "," + span.getSpanId() + ">\t" + span.getOperationId() + "\n");
            }
            logger.info(logInfo + "]");
        }
        HttpServletResponse response = (HttpServletResponse)allArguments[1];

        AbstractSpan span = ContextManager.activeSpan();
        if (response.getStatus() >= 400) {
            span.errorOccurred();
            Tags.STATUS_CODE.set(span, Integer.toString(response.getStatus()));
        }
        ContextManager.stopSpan();
        return ret;
    }

    @Override public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan span = ContextManager.activeSpan();
        span.log(t);
        span.errorOccurred();
    }
}

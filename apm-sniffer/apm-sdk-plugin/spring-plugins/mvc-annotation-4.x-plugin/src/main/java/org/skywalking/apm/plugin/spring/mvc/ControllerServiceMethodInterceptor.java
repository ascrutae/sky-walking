package org.skywalking.apm.plugin.spring.mvc;

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
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.skywalking.apm.logging.ILog;
import org.skywalking.apm.logging.LogManager;
import org.skywalking.apm.network.trace.component.ComponentsDefine;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * The <code>ControllerServiceMethodInterceptor</code> only use the first mapping value.
 *
 * @See {@link ControllerConstructorInterceptor} to explain why we are doing this.
 */
public class ControllerServiceMethodInterceptor implements InstanceMethodsAroundInterceptor {
    private ILog logger = LogManager.getLogger(ControllerServiceMethodInterceptor.class);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        MethodInterceptResult result) throws Throwable {
        PathMappingCache pathMappingCache = (PathMappingCache)objInst.getSkyWalkingDynamicField();
        String requestURL = pathMappingCache.findPathMapping(method);
        if (requestURL == null) {
            RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
            if (methodRequestMapping.value().length > 0) {
                requestURL = methodRequestMapping.value()[0];
            } else {
                requestURL = "";
            }
            pathMappingCache.addPathMapping(method, requestURL);
        }

        HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
        String tracingHeaderValue = request.getHeader(Config.Plugin.Propagation.HEADER_NAME);
        ContextCarrier contextCarrier = new ContextCarrier().deserialize(tracingHeaderValue);
        AbstractSpan span = ContextManager.createEntrySpan(requestURL, contextCarrier);
        Tags.URL.set(span, request.getRequestURL().toString());
        Tags.HTTP.METHOD.set(span, request.getMethod());
        span.setComponent(ComponentsDefine.SPRING_MVC_ANNOTATION);
        SpanLayer.asHttp(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret) throws Throwable {
        List<AbstractTracingSpan> activeSpans = ContextManager.activeSpans();
        if (activeSpans.size() != 1){
            StringBuilder logInfo = new StringBuilder("[\n");
            for (AbstractTracingSpan span : activeSpans) {
                logInfo.append("<" + span.getParentSpanId() + "," + span.getSpanId() + ">\t" + span.getOperationId() + "\n");
            }
            logger.info(logInfo + "]");
        }

        HttpServletResponse response = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getResponse();

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
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}

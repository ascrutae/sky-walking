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
        List<AbstractTracingSpan> activeSpans = ContextManager.activeSpans();
        if (activeSpans.size() != 1) {
            StringBuilder logInfo = new StringBuilder("ControllerServiceMethodInterceptor.beforeMethod [\n");
            for (AbstractTracingSpan activeSpan : activeSpans) {
                logInfo.append("<" + activeSpan.getParentSpanId() + "," + activeSpan.getSpanId() + ">\t" + activeSpan.getOperationId() + "\n");
            }
            logger.info(logInfo + "]");
        }

        HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
        String tracingHeaderValue = request.getHeader(Config.Plugin.Propagation.HEADER_NAME);
        ContextCarrier contextCarrier = new ContextCarrier().deserialize(tracingHeaderValue);
        AbstractSpan span = ContextManager.createEntrySpan(requestURL, contextCarrier);
        Tags.URL.set(span, request.getRequestURL().toString());
        Tags.HTTP.METHOD.set(span, request.getMethod());
        span.setComponent(ComponentsDefine.SPRING_MVC_ANNOTATION);
        SpanLayer.asHttp(span);

        activeSpans = ContextManager.activeSpans();
        if (activeSpans.size() != 1) {
            StringBuilder logInfo = new StringBuilder("ControllerServiceMethodInterceptor.beforeMethod-createSpan [\n");
            for (AbstractTracingSpan activeSpan : activeSpans) {
                logInfo.append("<" + activeSpan.getParentSpanId() + "," + activeSpan.getSpanId() + ">\t" + activeSpan.getOperationId() + "\n");
            }
            logger.info(logInfo + "]");
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret) throws Throwable {
        List<AbstractTracingSpan> activeSpans = ContextManager.activeSpans();
        if (activeSpans.size() != 1) {
            StringBuilder logInfo = new StringBuilder("ControllerServiceMethodInterceptor.afterMethod [\n");
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
        if (activeSpans.size() != 1) {
            StringBuilder logInfo = new StringBuilder("ControllerServiceMethodInterceptor.afterMethod-stopSpan [\n");
            for (AbstractTracingSpan activeSpan : activeSpans) {
                logInfo.append("<" + activeSpan.getParentSpanId() + "," + activeSpan.getSpanId() + ">\t" + activeSpan.getOperationId() + "\n");
            }
            logger.info(logInfo + "]");
        }
        return ret;
    }

    @Override public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        logger.info("JedisMethodInterceptor occur exception.");
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}

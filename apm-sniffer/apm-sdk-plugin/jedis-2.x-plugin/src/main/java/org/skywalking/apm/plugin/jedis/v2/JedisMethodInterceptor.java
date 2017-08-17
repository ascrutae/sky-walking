package org.skywalking.apm.plugin.jedis.v2;

import java.lang.reflect.Method;
import java.util.List;
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

public class JedisMethodInterceptor implements InstanceMethodsAroundInterceptor {

    private ILog logger = LogManager.getLogger(JedisMethodInterceptor.class);

    @Override public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        String peer = String.valueOf(objInst.getSkyWalkingDynamicField());
        List<AbstractTracingSpan> activeSpans = ContextManager.activeSpans();
        if (activeSpans.size() != 1) {
            StringBuilder logInfo = new StringBuilder("JedisMethodInterceptor.beforeMethod[\n");
            for (AbstractTracingSpan span : activeSpans) {
                logInfo.append("<" + span.getParentSpanId() + "," + span.getSpanId() + ">\t" + span.getOperationId() + "\n");
            }
            logger.info(logInfo + "]");
        }
        AbstractSpan span = ContextManager.createExitSpan("Jedis/" + method.getName(), peer);
        span.setComponent(ComponentsDefine.REDIS);
        Tags.DB_TYPE.set(span, "Redis");
        SpanLayer.asDB(span);

        if (allArguments.length > 0 && allArguments[0] instanceof String) {
            Tags.DB_STATEMENT.set(span, method.getName() + " " + allArguments[0]);
        }

        activeSpans = ContextManager.activeSpans();
        if (activeSpans.size() != 2) {
            StringBuilder logInfo = new StringBuilder("JedisMethodInterceptor.beforeMethod-createSpan[\n");
            for (AbstractTracingSpan activeSpan : activeSpans) {
                logInfo.append("<" + activeSpan.getParentSpanId() + "," + activeSpan.getSpanId() + ">\t" + activeSpan.getOperationId() + "\n");
            }
            logger.info(logInfo + "]");
        }
    }

    @Override public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Object ret) throws Throwable {
        List<AbstractTracingSpan> activeSpans = ContextManager.activeSpans();
        if (activeSpans.size() != 2) {
            StringBuilder logInfo = new StringBuilder("JedisMethodInterceptor.afterMethod[\n");
            for (AbstractTracingSpan span : activeSpans) {
                logInfo.append("<" + span.getParentSpanId() + "," + span.getSpanId() + ">\t" + span.getOperationId() + "\n");
            }
            logger.info(logInfo + "]");
        }

        ContextManager.stopSpan();

        activeSpans = ContextManager.activeSpans();
        if (activeSpans.size() != 1) {
            StringBuilder logInfo = new StringBuilder("JedisMethodInterceptor.afterMethod-stop[\n");
            for (AbstractTracingSpan span : activeSpans) {
                logInfo.append("<" + span.getParentSpanId() + "," + span.getSpanId() + ">\t" + span.getOperationId() + "\n");
            }
            logger.info(logInfo + "]");
        }
        return ret;
    }

    @Override public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        logger.info("JedisMethodInterceptor occur exception.");
        AbstractSpan span = ContextManager.activeSpan();
        span.errorOccurred();
        span.log(t);
    }
}

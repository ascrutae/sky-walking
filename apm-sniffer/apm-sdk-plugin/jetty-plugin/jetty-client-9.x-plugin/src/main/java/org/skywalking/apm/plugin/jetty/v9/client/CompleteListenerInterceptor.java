package org.skywalking.apm.plugin.jetty.v9.client;

import java.lang.reflect.Method;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpFields;
import org.skywalking.apm.agent.core.context.CarrierItem;
import org.skywalking.apm.agent.core.context.ContextCarrier;
import org.skywalking.apm.agent.core.context.ContextManager;
import org.skywalking.apm.agent.core.context.ContextSnapshot;
import org.skywalking.apm.agent.core.context.tag.Tags;
import org.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.skywalking.apm.network.trace.component.ComponentsDefine;

public class CompleteListenerInterceptor implements InstanceMethodsAroundInterceptor {
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        MethodInterceptResult result) throws Throwable {
        ContextSnapshot contextSnapshot = (ContextSnapshot)objInst.getSkyWalkingDynamicField();
        if (contextSnapshot != null) {
            Result callBackResult = (Result)allArguments[0];
            HttpFields httpFields = callBackResult.getResponse().getHeaders();

            ContextCarrier contextCarrier = new ContextCarrier();
            CarrierItem next = contextCarrier.items();
            boolean findContextInHeader = false;
            while (next.hasNext()) {
                next = next.next();
                String headValue = httpFields.get(next.getHeadKey());
                if (headValue != null) {
                    findContextInHeader = true;
                    next.setHeadValue(headValue);
                }
            }

            AbstractSpan abstractSpan;
            if (findContextInHeader) {
                abstractSpan = ContextManager.createEntrySpan("CallBack/" + callBackResult.getRequest().getURI().getPath(), contextCarrier);
            } else {
                abstractSpan = ContextManager.createLocalSpan("CallBack/" + callBackResult.getRequest().getURI().getPath());
                ContextManager.continued(contextSnapshot);
            }

            if (callBackResult.isFailed()) {
                abstractSpan.errorOccurred().log(callBackResult.getFailure());
                Tags.STATUS_CODE.set(abstractSpan, Integer.toString(callBackResult.getResponse().getStatus()));
            }
            abstractSpan.setComponent(ComponentsDefine.JETTY_CLIENT);
            abstractSpan.setLayer(SpanLayer.HTTP);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret) throws Throwable {
        ContextSnapshot contextSnapshot = (ContextSnapshot)objInst.getSkyWalkingDynamicField();
        if (contextSnapshot != null) {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}

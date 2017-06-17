package org.skywalking.apm.agent.core.plugin.interceptor.enhance;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldProxy;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.skywalking.apm.agent.core.plugin.interceptor.EnhancedClassInstanceContext;
import org.skywalking.apm.agent.core.plugin.interceptor.loader.InterceptorInstanceLoader;
import org.skywalking.apm.logging.ILog;
import org.skywalking.apm.logging.LogManager;

/**
 * The actual byte-buddy's interceptor to intercept constructor methods.
 * In this class, it provide a bridge between byte-buddy and sky-walking plugin.
 *
 * @author wusheng
 */
public class ClassConstructorInterceptor {
    private static final ILog logger = LogManager.getLogger(ClassConstructorInterceptor.class);

    /**
     * A class full name, and instanceof {@link InstanceConstructorInterceptor}
     * This name should only stay in {@link String}, the real {@link Class} type will trigger classloader failure.
     * If you want to know more, please check on books about Classloader or Classloader appointment mechanism.
     */
    private String constructorInterceptorClassName;

    /**
     * Set the name of {@link ClassConstructorInterceptor#constructorInterceptorClassName}
     *
     * @param constructorInterceptorClassName class full name.
     */
    public ClassConstructorInterceptor(String constructorInterceptorClassName) {
        this.constructorInterceptorClassName = constructorInterceptorClassName;
    }

    /**
     * Intercept the target constructor.
     *
     * @param obj          target class instance.
     * @param accessor     setter to the new added field of the target enhanced class.
     * @param allArguments all constructor arguments
     */
    @RuntimeType
    public void intercept(@This Object obj, @FieldProxy(ClassEnhancePluginDefine.CONTEXT_ATTR_NAME) FieldSetter accessor,
                          @AllArguments Object[] allArguments) {
        try {
            InstanceConstructorInterceptor interceptor = InterceptorInstanceLoader.load(constructorInterceptorClassName, obj.getClass().getClassLoader());

            EnhancedClassInstanceContext context = new EnhancedClassInstanceContext();
            accessor.setValue(context);
            ConstructorInvokeContext interceptorContext = new ConstructorInvokeContext(obj, allArguments);
            interceptor.onConstruct(context, interceptorContext);
        } catch (Throwable t) {
            logger.error("ClassConstructorInterceptor failure.", t);
        }

    }
}

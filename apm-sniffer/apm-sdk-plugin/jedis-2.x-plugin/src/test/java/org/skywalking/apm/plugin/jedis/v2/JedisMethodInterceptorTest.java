package org.skywalking.apm.plugin.jedis.v2;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.skywalking.apm.agent.core.boot.ServiceManager;
import org.skywalking.apm.agent.core.context.TracerContext;
import org.skywalking.apm.agent.core.plugin.interceptor.EnhancedClassInstanceContext;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodInvokeContext;
import org.skywalking.apm.sniffer.mock.context.MockTracerContextListener;
import org.skywalking.apm.sniffer.mock.context.SegmentAssert;
import org.skywalking.apm.trace.LogData;
import org.skywalking.apm.trace.Span;
import org.skywalking.apm.trace.TraceSegment;
import org.skywalking.apm.trace.tag.Tags;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.skywalking.apm.plugin.jedis.v2.JedisMethodInterceptor.*;

@RunWith(MockitoJUnitRunner.class)
public class JedisMethodInterceptorTest {

    private JedisMethodInterceptor interceptor;

    private MockTracerContextListener mockTracerContextListener;

    @Mock
    private EnhancedClassInstanceContext classInstanceContext;
    @Mock
    private InstanceMethodInvokeContext methodInvokeContext;

    @Before
    public void setUp() throws Exception {
        ServiceManager.INSTANCE.boot();

        interceptor = new JedisMethodInterceptor();
        mockTracerContextListener = new MockTracerContextListener();

        TracerContext.ListenerManager.add(mockTracerContextListener);

        when(classInstanceContext.get(KEY_OF_REDIS_HOST, String.class)).thenReturn("127.0.0.1");
        when(classInstanceContext.get(KEY_OF_REDIS_PORT)).thenReturn(6379);
        when(methodInvokeContext.methodName()).thenReturn("set");
        when(methodInvokeContext.allArguments()).thenReturn(new Object[] {"OperationKey"});
        when(classInstanceContext.isContain("__$invokeCounterKey")).thenReturn(true);
    }

    @Test
    public void testIntercept() {
        when(classInstanceContext.get("__$invokeCounterKey", Integer.class)).thenReturn(0);
        interceptor.beforeMethod(classInstanceContext, methodInvokeContext, null);
        when(classInstanceContext.get("__$invokeCounterKey", Integer.class)).thenReturn(1);
        interceptor.afterMethod(classInstanceContext, methodInvokeContext, null);

        mockTracerContextListener.assertSize(1);
        mockTracerContextListener.assertTraceSegment(0, new SegmentAssert() {
            @Override
            public void call(TraceSegment traceSegment) {
                assertThat(traceSegment.getSpans().size(), is(1));
                Span span = traceSegment.getSpans().get(0);
                assertRedisSpan(span);
            }
        });
    }

    @Test
    public void testInterceptWithMultiHost() {
        when(classInstanceContext.get("__$invokeCounterKey", Integer.class)).thenReturn(0);
        when(classInstanceContext.get(KEY_OF_REDIS_HOST, String.class)).thenReturn(null);
        when(classInstanceContext.get(KEY_OF_REDIS_HOSTS)).thenReturn("127.0.0.1:6379;127.0.0.1:16379;");

        interceptor.beforeMethod(classInstanceContext, methodInvokeContext, null);
        when(classInstanceContext.get("__$invokeCounterKey", Integer.class)).thenReturn(1);
        interceptor.afterMethod(classInstanceContext, methodInvokeContext, null);

        mockTracerContextListener.assertSize(1);
        mockTracerContextListener.assertTraceSegment(0, new SegmentAssert() {
            @Override
            public void call(TraceSegment traceSegment) {
                assertThat(traceSegment.getSpans().size(), is(1));
                Span span = traceSegment.getSpans().get(0);
                assertRedisSpan(span, "127.0.0.1:6379;127.0.0.1:16379;");
            }
        });
    }

    @Test
    public void testInterceptWithException() {
        when(classInstanceContext.get("__$invokeCounterKey", Integer.class)).thenReturn(0);
        interceptor.beforeMethod(classInstanceContext, methodInvokeContext, null);
        interceptor.handleMethodException(new RuntimeException(), classInstanceContext, methodInvokeContext);
        when(classInstanceContext.get("__$invokeCounterKey", Integer.class)).thenReturn(1);
        interceptor.afterMethod(classInstanceContext, methodInvokeContext, null);

        mockTracerContextListener.assertSize(1);
        mockTracerContextListener.assertTraceSegment(0, new SegmentAssert() {
            @Override
            public void call(TraceSegment traceSegment) {
                assertThat(traceSegment.getSpans().size(), is(1));
                Span span = traceSegment.getSpans().get(0);
                assertRedisSpan(span);
                assertThat(span.getLogs().size(), is(1));
                assertLogData(span.getLogs().get(0));
            }
        });
    }

    private void assertLogData(LogData logData) {
        MatcherAssert.assertThat(logData.getFields().size(), is(4));
        MatcherAssert.assertThat(logData.getFields().get("event"), CoreMatchers.<Object>is("error"));
        assertEquals(logData.getFields().get("error.kind"), RuntimeException.class.getName());
        assertNull(logData.getFields().get("message"));
    }

    private void assertRedisSpan(Span span) {
        assertThat(span.getOperationName(), is("Jedis/set"));
        assertThat(Tags.PEER_HOST.get(span), is("127.0.0.1"));
        assertThat(Tags.PEER_PORT.get(span), is(6379));
        assertThat(Tags.COMPONENT.get(span), is("Redis"));
        assertThat(Tags.DB_STATEMENT.get(span), is("set OperationKey"));
        assertThat(Tags.DB_TYPE.get(span), is("Redis"));
        assertTrue(Tags.SPAN_LAYER.isDB(span));
    }

    private void assertRedisSpan(Span span, String exceptedPeerHosts) {
        assertThat(span.getOperationName(), is("Jedis/set"));
        assertThat(Tags.PEERS.get(span), is(exceptedPeerHosts));
        assertThat(Tags.COMPONENT.get(span), is("Redis"));
        assertThat(Tags.DB_STATEMENT.get(span), is("set OperationKey"));
        assertThat(Tags.DB_TYPE.get(span), is("Redis"));
        assertTrue(Tags.SPAN_LAYER.isDB(span));
    }

    @After
    public void tearDown() throws Exception {
        TracerContext.ListenerManager.remove(mockTracerContextListener);
    }

}

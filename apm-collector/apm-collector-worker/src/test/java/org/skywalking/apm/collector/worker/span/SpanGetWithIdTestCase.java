package org.skywalking.apm.collector.worker.span;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.skywalking.apm.collector.actor.ClusterWorkerContext;
import org.skywalking.apm.collector.actor.LocalWorkerContext;
import org.skywalking.apm.collector.actor.ProviderNotFoundException;
import org.skywalking.apm.collector.actor.WorkerRefs;
import org.skywalking.apm.collector.actor.selector.RollingSelector;
import org.skywalking.apm.collector.worker.httpserver.ArgumentsParseException;
import org.skywalking.apm.collector.worker.span.persistence.SpanSearchWithId;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author pengys5
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ClusterWorkerContext.class})
@PowerMockIgnore({"javax.management.*"})
public class SpanGetWithIdTestCase {

    private SpanGetWithId getObj;
    private SpanGetAnswerGet answer;
    private ClusterWorkerContext clusterWorkerContext;

    @Before
    public void init() throws Exception {
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        clusterWorkerContext = PowerMockito.mock(ClusterWorkerContext.class);

        LocalWorkerContext localWorkerContext = PowerMockito.mock(LocalWorkerContext.class);
        WorkerRefs workerRefs = mock(WorkerRefs.class);

        answer = new SpanGetAnswerGet();
        doAnswer(answer).when(workerRefs).ask(Mockito.any(SpanSearchWithId.RequestEntity.class), Mockito.any(JsonObject.class));

        when(localWorkerContext.lookup(SpanSearchWithId.WorkerRole.INSTANCE)).thenReturn(workerRefs);
        getObj = new SpanGetWithId(SpanGetWithId.WorkerRole.INSTANCE, clusterWorkerContext, localWorkerContext);
    }

    @Test
    public void testRole() {
        Assert.assertEquals(SpanGetWithId.class.getSimpleName(), SpanGetWithId.WorkerRole.INSTANCE.roleName());
        Assert.assertEquals(RollingSelector.class.getSimpleName(), SpanGetWithId.WorkerRole.INSTANCE.workerSelector().getClass().getSimpleName());
    }

    @Test
    public void testFactory() {
        SpanGetWithId.Factory factory = new SpanGetWithId.Factory();
        Assert.assertEquals(SpanGetWithId.class.getSimpleName(), factory.role().roleName());
        Assert.assertEquals(SpanGetWithId.class.getSimpleName(), factory.workerInstance(null).getClass().getSimpleName());
        Assert.assertEquals("/span/spanId", factory.servletPath());
    }

    @Test
    public void testPreStart() throws ProviderNotFoundException {
        when(clusterWorkerContext.findProvider(SpanSearchWithId.WorkerRole.INSTANCE)).thenReturn(new SpanSearchWithId.Factory());

        ArgumentCaptor<SpanSearchWithId.WorkerRole> argumentCaptor = ArgumentCaptor.forClass(SpanSearchWithId.WorkerRole.class);
        getObj.preStart();
        verify(clusterWorkerContext).findProvider(argumentCaptor.capture());
    }

    @Test
    public void testOnSearch() throws Exception {
        Map<String, String[]> request = new HashMap<>();
        String[] segId = {"10"};
        request.put("segId", segId);
        String[] spanId = {"20"};
        request.put("spanId", spanId);
        JsonObject response = new JsonObject();
        getObj.onReceive(request, response);
    }

    @Test(expected = ArgumentsParseException.class)
    public void testOnSearchError() throws Exception {
        Map<String, String[]> request = new HashMap<>();
        JsonObject response = new JsonObject();
        getObj.onReceive(request, response);
    }

    class SpanGetAnswerGet implements Answer {

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            SpanSearchWithId.RequestEntity requestEntity = (SpanSearchWithId.RequestEntity)invocation.getArguments()[0];
            Assert.assertEquals("10", requestEntity.getSegId());
            Assert.assertEquals("20", requestEntity.getSpanId());
            return null;
        }
    }
}

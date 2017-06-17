package org.skywalking.apm.collector.worker.globaltrace.analysis;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.skywalking.apm.collector.actor.ClusterWorkerContext;
import org.skywalking.apm.collector.actor.LocalWorkerContext;
import org.skywalking.apm.collector.actor.WorkerRefs;
import org.skywalking.apm.collector.actor.selector.RollingSelector;
import org.skywalking.apm.collector.worker.config.WorkerConfig;
import org.skywalking.apm.collector.worker.globaltrace.persistence.GlobalTraceAgg;
import org.skywalking.apm.collector.worker.mock.MergeDataAnswer;
import org.skywalking.apm.collector.worker.segment.mock.SegmentMock;
import org.skywalking.apm.collector.worker.storage.JoinAndSplitData;

import java.util.TimeZone;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * @author pengys5
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {ClusterWorkerContext.class})
@PowerMockIgnore( {"javax.management.*"})
public class GlobalTraceAnalysisTestCase {

    private GlobalTraceAnalysis analysis;
    private SegmentMock segmentMock = new SegmentMock();
    private MergeDataAnswer answer;
    private ClusterWorkerContext clusterWorkerContext;

    @Before
    public void init() throws Exception {
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        clusterWorkerContext = PowerMockito.mock(ClusterWorkerContext.class);
        WorkerRefs workerRefs = mock(WorkerRefs.class);
        answer = new MergeDataAnswer();
        doAnswer(answer).when(workerRefs).tell(Mockito.any(JoinAndSplitData.class));

        when(clusterWorkerContext.lookup(GlobalTraceAgg.Role.INSTANCE)).thenReturn(workerRefs);

        LocalWorkerContext localWorkerContext = new LocalWorkerContext();
        analysis = new GlobalTraceAnalysis(GlobalTraceAnalysis.Role.INSTANCE, clusterWorkerContext, localWorkerContext);
    }

    @Test
    public void testRole() {
        Assert.assertEquals(GlobalTraceAnalysis.class.getSimpleName(), GlobalTraceAnalysis.Role.INSTANCE.roleName());
        Assert.assertEquals(RollingSelector.class.getSimpleName(), GlobalTraceAnalysis.Role.INSTANCE.workerSelector().getClass().getSimpleName());
    }

    @Test
    public void testFactory() {
        GlobalTraceAnalysis.Factory factory = new GlobalTraceAnalysis.Factory();
        Assert.assertEquals(GlobalTraceAnalysis.class.getSimpleName(), factory.role().roleName());
        Assert.assertEquals(GlobalTraceAnalysis.class.getSimpleName(), factory.workerInstance(null).getClass().getSimpleName());

        int testSize = 10;
        WorkerConfig.Queue.GlobalTrace.GlobalTraceAnalysis.SIZE = testSize;
        Assert.assertEquals(testSize, factory.queueSize());
    }

    @Test
    public void testAnalyse() throws Exception {
        segmentMock.executeAnalysis(analysis);

        Assert.assertEquals(1, answer.getJoinAndSplitDataList().size());
        JoinAndSplitData joinAndSplitData = answer.getJoinAndSplitDataList().get(0);
        Assert.assertEquals(id, joinAndSplitData.getId());
        String subSegIds = joinAndSplitData.asMap().get("subSegIds").toString();
        Assert.assertEquals(cacheServiceSubSegIds, subSegIds);
    }

    private String id = "Trace.1490922929254.1797892356.6003.69.2";
    private String cacheServiceSubSegIds = "Segment.1490922929298.927784221.5991.28.1,Segment.1490922929274.1382198130.5997.47.1,Segment.1490922929258.927784221.5991.27.1,Segment.1490922929254.1797892356.6003.69.1";
}

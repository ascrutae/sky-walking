package org.skywalking.apm.collector.worker.node.analysis;

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
import org.skywalking.apm.collector.worker.datamerge.RecordDataMergeJson;
import org.skywalking.apm.collector.worker.mock.RecordDataAnswer;
import org.skywalking.apm.collector.worker.node.persistence.NodeMappingHourAgg;
import org.skywalking.apm.collector.worker.segment.mock.SegmentMock;
import org.skywalking.apm.collector.worker.storage.RecordData;

import java.util.List;
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
public class NodeMappingHourAnalysisTestCase {

    private NodeMappingHourAnalysis analysis;
    private SegmentMock segmentMock = new SegmentMock();
    private RecordDataAnswer answer;

    @Before
    public void init() throws Exception {
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        ClusterWorkerContext clusterWorkerContext = PowerMockito.mock(ClusterWorkerContext.class);
        WorkerRefs workerRefs = mock(WorkerRefs.class);
        answer = new RecordDataAnswer();
        doAnswer(answer).when(workerRefs).tell(Mockito.any(RecordData.class));

        when(clusterWorkerContext.lookup(NodeMappingHourAgg.Role.INSTANCE)).thenReturn(workerRefs);

        LocalWorkerContext localWorkerContext = new LocalWorkerContext();
        analysis = new NodeMappingHourAnalysis(NodeMappingHourAnalysis.Role.INSTANCE, clusterWorkerContext, localWorkerContext);
    }

    @Test
    public void testRole() {
        Assert.assertEquals(NodeMappingHourAnalysis.class.getSimpleName(), NodeMappingHourAnalysis.Role.INSTANCE.roleName());
        Assert.assertEquals(RollingSelector.class.getSimpleName(), NodeMappingHourAnalysis.Role.INSTANCE.workerSelector().getClass().getSimpleName());
    }

    @Test
    public void testFactory() {
        NodeMappingHourAnalysis.Factory factory = new NodeMappingHourAnalysis.Factory();
        Assert.assertEquals(NodeMappingHourAnalysis.class.getSimpleName(), factory.role().roleName());
        Assert.assertEquals(NodeMappingHourAnalysis.class.getSimpleName(), factory.workerInstance(null).getClass().getSimpleName());

        int testSize = 10;
        WorkerConfig.Queue.Node.NodeMappingHourAnalysis.SIZE = testSize;
        Assert.assertEquals(testSize, factory.queueSize());
    }

    String jsonFile = "/json/node/analysis/node_mapping_hour_analysis.json";

    @Test
    public void testAnalyse() throws Exception {
        segmentMock.executeAnalysis(analysis);

        List<RecordData> recordDataList = answer.getRecordDataList();
        RecordDataMergeJson.INSTANCE.merge(jsonFile, recordDataList);
    }
}

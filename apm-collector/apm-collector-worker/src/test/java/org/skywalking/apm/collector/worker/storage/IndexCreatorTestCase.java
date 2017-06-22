package org.skywalking.apm.collector.worker.storage;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.skywalking.apm.collector.worker.config.EsConfig;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author pengys5
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {IndexCreator.class, IndexCreatorTestCase.TestIndex.class})
@PowerMockIgnore( {"javax.management.*"})
public class IndexCreatorTestCase {

    private IndexCreator indexCreator;
    private TestIndex testIndex;

    @Before
    public void init() throws Exception {
        testIndex = mock(TestIndex.class);

        indexCreator = mock(IndexCreator.class);
        doCallRealMethod().when(indexCreator).create();

        Set<AbstractIndex> indexSet = new HashSet<>();
        indexSet.add(testIndex);
        when(indexCreator, "loadIndex").thenReturn(indexSet);
    }

    @Test
    public void testLoadIndex() throws Exception {
        IndexCreator indexCreator = spy(IndexCreator.INSTANCE);
        Set<AbstractIndex> indexSet = Whitebox.invokeMethod(indexCreator, "loadIndex");
        Assert.assertEquals(10, indexSet.size());

        Set<String> indexName = new HashSet<>();
        for (AbstractIndex index : indexSet) {
            indexName.add(index.index());
        }

        Assert.assertEquals(true, indexName.contains("node_ref_idx"));
        Assert.assertEquals(true, indexName.contains("node_ref_res_sum_idx"));
        Assert.assertEquals(true, indexName.contains("global_trace_idx"));
        Assert.assertEquals(true, indexName.contains("segment_cost_idx"));
        Assert.assertEquals(true, indexName.contains("node_mapping_idx"));
        Assert.assertEquals(true, indexName.contains("segment_idx"));
        Assert.assertEquals(true, indexName.contains("segment_exp_idx"));
        Assert.assertEquals(true, indexName.contains("node_comp_idx"));
    }

    @Test
    public void testCreateOptionManual() throws Exception {
        EsConfig.Es.Index.Initialize.MODE = EsConfig.IndexInitMode.MANUAL;
        indexCreator.create();
        Mockito.verify(testIndex, Mockito.never()).createIndex();
        Mockito.verify(testIndex, Mockito.never()).deleteIndex();
    }

    @Test
    public void testCreateOptionForcedIndexIsExists() throws Exception {
        EsConfig.Es.Index.Initialize.MODE = EsConfig.IndexInitMode.FORCED;
        when(testIndex.isExists()).thenReturn(true);
        indexCreator.create();
        Mockito.verify(testIndex).createIndex();
        Mockito.verify(testIndex).deleteIndex();
    }

    @Test
    public void testCreateOptionForcedIndexNotExists() throws Exception {
        EsConfig.Es.Index.Initialize.MODE = EsConfig.IndexInitMode.FORCED;
        when(testIndex.isExists()).thenReturn(false);
        indexCreator.create();
        Mockito.verify(testIndex).createIndex();
        Mockito.verify(testIndex, Mockito.never()).deleteIndex();
    }

    @Test
    public void testCreateOptionAutoIndexNotExists() throws Exception {
        EsConfig.Es.Index.Initialize.MODE = EsConfig.IndexInitMode.AUTO;
        when(testIndex.isExists()).thenReturn(false);
        indexCreator.create();
        Mockito.verify(testIndex).createIndex();
        Mockito.verify(testIndex, Mockito.never()).deleteIndex();
    }

    @Test
    public void testCreateOptionAutoIndexExists() throws Exception {
        EsConfig.Es.Index.Initialize.MODE = EsConfig.IndexInitMode.AUTO;
        when(testIndex.isExists()).thenReturn(true);
        indexCreator.create();
        Mockito.verify(testIndex, Mockito.never()).createIndex();
        Mockito.verify(testIndex, Mockito.never()).deleteIndex();
    }

    class TestIndex extends AbstractIndex {

        @Override
        public String index() {
            return "TestIndex";
        }

        @Override
        public boolean isRecord() {
            return false;
        }

        @Override
        public int refreshInterval() {
            return 0;
        }

        @Override
        public XContentBuilder createMappingBuilder() throws IOException {
            return XContentFactory.jsonBuilder();
        }
    }
}

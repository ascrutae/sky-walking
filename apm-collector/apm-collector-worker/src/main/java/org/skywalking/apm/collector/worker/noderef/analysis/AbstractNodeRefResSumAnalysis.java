package org.skywalking.apm.collector.worker.noderef.analysis;

import org.skywalking.apm.collector.actor.ClusterWorkerContext;
import org.skywalking.apm.collector.actor.LocalWorkerContext;
import org.skywalking.apm.collector.actor.Role;
import org.skywalking.apm.collector.worker.MetricAnalysisMember;
import org.skywalking.apm.collector.worker.noderef.NodeRefResSumIndex;
import org.skywalking.apm.collector.worker.storage.AbstractTimeSlice;

/**
 * @author pengys5
 */
abstract class AbstractNodeRefResSumAnalysis extends MetricAnalysisMember {

    AbstractNodeRefResSumAnalysis(Role role, ClusterWorkerContext clusterContext,
        LocalWorkerContext selfContext) {
        super(role, clusterContext, selfContext);
    }

    final void analyseResSum(NodeRefResRecord nodeRefRes) {
        long startTime = nodeRefRes.startTime;
        long endTime = nodeRefRes.endTime;
        boolean isError = nodeRefRes.isError;
        long cost = endTime - startTime;

        set(nodeRefRes.nodeRefId, NodeRefResSumIndex.ONE_SECOND_LESS, 0L);
        set(nodeRefRes.nodeRefId, NodeRefResSumIndex.THREE_SECOND_LESS, 0L);
        set(nodeRefRes.nodeRefId, NodeRefResSumIndex.FIVE_SECOND_LESS, 0L);
        set(nodeRefRes.nodeRefId, NodeRefResSumIndex.FIVE_SECOND_GREATER, 0L);
        set(nodeRefRes.nodeRefId, NodeRefResSumIndex.ERROR, 0L);
        if (cost <= 1000 && !isError) {
            set(nodeRefRes.nodeRefId, NodeRefResSumIndex.ONE_SECOND_LESS, 1L);
        } else if (1000 < cost && cost <= 3000 && !isError) {
            set(nodeRefRes.nodeRefId, NodeRefResSumIndex.THREE_SECOND_LESS, 1L);
        } else if (3000 < cost && cost <= 5000 && !isError) {
            set(nodeRefRes.nodeRefId, NodeRefResSumIndex.FIVE_SECOND_LESS, 1L);
        } else if (5000 < cost && !isError) {
            set(nodeRefRes.nodeRefId, NodeRefResSumIndex.FIVE_SECOND_GREATER, 1L);
        } else {
            set(nodeRefRes.nodeRefId, NodeRefResSumIndex.ERROR, 1L);
        }
        set(nodeRefRes.nodeRefId, NodeRefResSumIndex.SUMMARY, 1L);
    }

    public static class NodeRefResRecord extends AbstractTimeSlice {
        private String nodeRefId;
        private long startTime;
        private long endTime;
        private Boolean isError;

        NodeRefResRecord(long minute, long hour, long day, int second) {
            super(minute, hour, day, second);
        }

        void setNodeRefId(String nodeRefId) {
            this.nodeRefId = nodeRefId;
        }

        void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        void setError(Boolean error) {
            isError = error;
        }

        String getNodeRefId() {
            return nodeRefId;
        }
    }
}

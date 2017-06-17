package org.skywalking.apm.collector.worker.noderef.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.skywalking.apm.collector.actor.AbstractLocalAsyncWorkerProvider;
import org.skywalking.apm.collector.actor.ClusterWorkerContext;
import org.skywalking.apm.collector.actor.LocalWorkerContext;
import org.skywalking.apm.collector.actor.ProviderNotFoundException;
import org.skywalking.apm.collector.actor.WorkerInvokeException;
import org.skywalking.apm.collector.actor.WorkerNotFoundException;
import org.skywalking.apm.collector.actor.WorkerRefs;
import org.skywalking.apm.collector.actor.selector.RollingSelector;
import org.skywalking.apm.collector.actor.selector.WorkerSelector;
import org.skywalking.apm.collector.worker.config.WorkerConfig;
import org.skywalking.apm.collector.worker.noderef.persistence.NodeRefMinuteAgg;
import org.skywalking.apm.collector.worker.segment.SegmentPost;
import org.skywalking.apm.collector.worker.segment.entity.Segment;

/**
 * @author pengys5
 */
public class NodeRefMinuteAnalysis extends AbstractNodeRefAnalysis {

    private Logger logger = LogManager.getFormatterLogger(NodeRefMinuteAnalysis.class);

    protected NodeRefMinuteAnalysis(org.skywalking.apm.collector.actor.Role role, ClusterWorkerContext clusterContext,
        LocalWorkerContext selfContext) {
        super(role, clusterContext, selfContext);
    }

    @Override
    public void preStart() throws ProviderNotFoundException {
        super.preStart();
        getClusterContext().findProvider(NodeRefResSumMinuteAnalysis.Role.INSTANCE).create(this);
    }

    @Override
    public void analyse(Object message) {
        if (message instanceof SegmentPost.SegmentWithTimeSlice) {
            SegmentPost.SegmentWithTimeSlice segmentWithTimeSlice = (SegmentPost.SegmentWithTimeSlice)message;
            Segment segment = segmentWithTimeSlice.getSegment();
            long minute = segmentWithTimeSlice.getMinute();
            long hour = segmentWithTimeSlice.getHour();
            long day = segmentWithTimeSlice.getDay();
            int second = segmentWithTimeSlice.getSecond();
            analyseNodeRef(segment, segmentWithTimeSlice.getMinute(), minute, hour, day, second);
        } else {
            logger.error("unhandled message, message instance must SegmentPost.SegmentWithTimeSlice, but is %s", message.getClass().toString());
        }
    }

    @Override
    protected void sendToResSumAnalysis(AbstractNodeRefResSumAnalysis.NodeRefResRecord refResRecord) {
        try {
            getSelfContext().lookup(NodeRefResSumMinuteAnalysis.Role.INSTANCE).tell(refResRecord);
        } catch (WorkerInvokeException | WorkerNotFoundException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    protected WorkerRefs aggWorkRefs() {
        try {
            return getClusterContext().lookup(NodeRefMinuteAgg.Role.INSTANCE);
        } catch (WorkerNotFoundException e) {
            logger.error("The role of %s worker not found", NodeRefMinuteAgg.Role.INSTANCE.roleName());
        }
        return null;
    }

    public static class Factory extends AbstractLocalAsyncWorkerProvider<NodeRefMinuteAnalysis> {
        @Override
        public Role role() {
            return Role.INSTANCE;
        }

        @Override
        public NodeRefMinuteAnalysis workerInstance(ClusterWorkerContext clusterContext) {
            return new NodeRefMinuteAnalysis(role(), clusterContext, new LocalWorkerContext());
        }

        @Override
        public int queueSize() {
            return WorkerConfig.Queue.NodeRef.NodeRefMinuteAnalysis.SIZE;
        }
    }

    public enum Role implements org.skywalking.apm.collector.actor.Role {
        INSTANCE;

        @Override
        public String roleName() {
            return NodeRefMinuteAnalysis.class.getSimpleName();
        }

        @Override
        public WorkerSelector workerSelector() {
            return new RollingSelector();
        }
    }
}

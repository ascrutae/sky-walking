package org.skywalking.apm.collector.worker.node.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.skywalking.apm.collector.actor.AbstractClusterWorker;
import org.skywalking.apm.collector.actor.AbstractClusterWorkerProvider;
import org.skywalking.apm.collector.actor.ClusterWorkerContext;
import org.skywalking.apm.collector.actor.LocalWorkerContext;
import org.skywalking.apm.collector.actor.ProviderNotFoundException;
import org.skywalking.apm.collector.actor.WorkerException;
import org.skywalking.apm.collector.actor.selector.HashCodeSelector;
import org.skywalking.apm.collector.actor.selector.WorkerSelector;
import org.skywalking.apm.collector.worker.config.WorkerConfig;
import org.skywalking.apm.collector.worker.storage.RecordData;

/**
 * @author pengys5
 */
public class NodeCompAgg extends AbstractClusterWorker {

    private Logger logger = LogManager.getFormatterLogger(NodeCompAgg.class);

    NodeCompAgg(org.skywalking.apm.collector.actor.Role role, ClusterWorkerContext clusterContext,
        LocalWorkerContext selfContext) {
        super(role, clusterContext, selfContext);
    }

    @Override
    public void preStart() throws ProviderNotFoundException {
        getClusterContext().findProvider(NodeCompSave.Role.INSTANCE).create(this);
    }

    @Override
    protected void onWork(Object message) throws WorkerException {
        if (message instanceof RecordData) {
            getSelfContext().lookup(NodeCompSave.Role.INSTANCE).tell(message);
        } else {
            logger.error("unhandled message, message instance must RecordData, but is %s", message.getClass().toString());
        }
    }

    public static class Factory extends AbstractClusterWorkerProvider<NodeCompAgg> {
        @Override
        public Role role() {
            return Role.INSTANCE;
        }

        @Override
        public NodeCompAgg workerInstance(ClusterWorkerContext clusterContext) {
            return new NodeCompAgg(role(), clusterContext, new LocalWorkerContext());
        }

        @Override
        public int workerNum() {
            return WorkerConfig.WorkerNum.Node.NodeCompAgg.VALUE;
        }
    }

    public enum Role implements org.skywalking.apm.collector.actor.Role {
        INSTANCE;

        @Override
        public String roleName() {
            return NodeCompAgg.class.getSimpleName();
        }

        @Override
        public WorkerSelector workerSelector() {
            return new HashCodeSelector();
        }
    }
}

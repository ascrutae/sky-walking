package org.skywalking.apm.collector.worker.instance.persistence;

import org.skywalking.apm.collector.actor.AbstractLocalSyncWorkerProvider;
import org.skywalking.apm.collector.actor.ClusterWorkerContext;
import org.skywalking.apm.collector.actor.LocalWorkerContext;
import org.skywalking.apm.collector.actor.selector.HashCodeSelector;
import org.skywalking.apm.collector.actor.selector.WorkerSelector;
import org.skywalking.apm.collector.worker.RecordPersistenceMember;
import org.skywalking.apm.collector.worker.instance.InstanceIndex;
import org.skywalking.apm.collector.worker.storage.PersistenceWorkerListener;

public class ActivationSaver extends RecordPersistenceMember {

    public ActivationSaver(Role role, ClusterWorkerContext clusterContext, LocalWorkerContext selfContext) {
        super(role, clusterContext, selfContext);
    }

    @Override
    public String esIndex() {
        return InstanceIndex.INDEX;
    }

    @Override
    public String esType() {
        return InstanceIndex.TYPE_REGISTRY;
    }

    public static class Factory extends AbstractLocalSyncWorkerProvider<ActivationSaver> {
        @Override
        public ActivationSaver.Role role() {
            return ActivationSaver.Role.INSTANCE;
        }

        @Override
        public ActivationSaver workerInstance(ClusterWorkerContext clusterContext) {
            ActivationSaver worker = new ActivationSaver(role(), clusterContext, new LocalWorkerContext());
            PersistenceWorkerListener.INSTANCE.register(worker);
            return worker;
        }
    }

    public enum Role implements org.skywalking.apm.collector.actor.Role {
        INSTANCE;

        @Override
        public String roleName() {
            return ActivationSaver.class.getSimpleName();
        }

        @Override
        public WorkerSelector workerSelector() {
            return new HashCodeSelector();
        }
    }
}

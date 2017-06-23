package org.skywalking.apm.collector.worker.instance;

import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import org.skywalking.apm.collector.actor.ClusterWorkerContext;
import org.skywalking.apm.collector.actor.LocalWorkerContext;
import org.skywalking.apm.collector.actor.ProviderNotFoundException;
import org.skywalking.apm.collector.actor.Role;
import org.skywalking.apm.collector.actor.WorkerInvokeException;
import org.skywalking.apm.collector.actor.WorkerNotFoundException;
import org.skywalking.apm.collector.actor.selector.RollingSelector;
import org.skywalking.apm.collector.actor.selector.WorkerSelector;
import org.skywalking.apm.collector.worker.httpserver.AbstractStreamPost;
import org.skywalking.apm.collector.worker.httpserver.AbstractStreamPostProvider;
import org.skywalking.apm.collector.worker.httpserver.ArgumentsParseException;
import org.skywalking.apm.collector.worker.instance.analysis.ActivationAnalysis;
import org.skywalking.apm.collector.worker.instance.analysis.PingTimeAnalysis;
import org.skywalking.apm.collector.worker.instance.entity.Activation;
import org.skywalking.apm.collector.worker.instance.entity.InstanceDeserialize;
import org.skywalking.apm.collector.worker.tools.DateTools;

public class ActivatePost extends AbstractStreamPost {
    public ActivatePost(Role role, ClusterWorkerContext clusterContext, LocalWorkerContext selfContext) {
        super(role, clusterContext, selfContext);
    }

    @Override public void preStart() throws ProviderNotFoundException {
        getClusterContext().findProvider(ActivationAnalysis.Role.INSTANCE).create(this);
        getClusterContext().findProvider(PingTimeAnalysis.Role.INSTANCE).create(this);
    }

    @Override
    protected void onReceive(BufferedReader reader,
        JsonObject response) throws ArgumentsParseException, WorkerInvokeException, WorkerNotFoundException {
        try {
            Activation activation = InstanceDeserialize.INSTANCE.deserializeActivation(reader.readLine());

            if (activation.getInstanceId() == 0) {
                throw new ArgumentsParseException("Instance Id required.");
            }

            if (activation.getApplicationCode() == null || activation.getApplicationCode().length() == 0) {
                throw new ArgumentsParseException("Application code required.");
            }

            if (activation.getRegistryTime() == 0) {
                throw new ArgumentsParseException("Registry time required.");
            }

            ActivationAnalysis.Instance instance = new ActivationAnalysis.Instance(activation.getApplicationCode(), activation.getInstanceId(), activation.getRegistryTime());
            getSelfContext().lookup(ActivationAnalysis.Role.INSTANCE).tell(instance);
            getSelfContext().lookup(PingTimeAnalysis.Role.INSTANCE).tell(new PingTimeAnalysis.Ping(activation.getInstanceId(),
                DateTools.getMinuteSlice(System.currentTimeMillis())));
        } catch (IOException e) {
            throw new ArgumentsParseException(e.getMessage(), e);
        }
    }

    public static class Factory extends AbstractStreamPostProvider<ActivatePost> {
        @Override
        public Role role() {
            return ActivatePost.WorkerRole.INSTANCE;
        }

        @Override
        public ActivatePost workerInstance(ClusterWorkerContext clusterContext) {
            return new ActivatePost(role(), clusterContext, new LocalWorkerContext());
        }

        @Override
        public String servletPath() {
            return "/instance/activate";
        }
    }

    public enum WorkerRole implements Role {
        INSTANCE;

        @Override
        public String roleName() {
            return ActivatePost.class.getSimpleName();
        }

        @Override
        public WorkerSelector workerSelector() {
            return new RollingSelector();
        }
    }
}

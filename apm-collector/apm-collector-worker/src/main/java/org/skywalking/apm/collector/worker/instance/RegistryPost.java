package org.skywalking.apm.collector.worker.instance;

import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.skywalking.apm.collector.worker.instance.analysis.InstanceAnalysis;
import org.skywalking.apm.collector.worker.instance.analysis.PingTimeAnalysis;
import org.skywalking.apm.collector.worker.instance.entity.InstanceDeserialize;
import org.skywalking.apm.collector.worker.instance.entity.Registry;
import org.skywalking.apm.collector.worker.instance.util.IDSequenceCache;
import org.skywalking.apm.collector.worker.tools.DateTools;
import org.skywalking.apm.util.StringUtil;

public class RegistryPost extends AbstractStreamPost {

    private Logger logger = LogManager.getFormatterLogger(RegistryPost.class);

    public RegistryPost(Role role, ClusterWorkerContext clusterContext, LocalWorkerContext selfContext) {
        super(role, clusterContext, selfContext);
    }

    @Override
    protected void onReceive(BufferedReader reader,
        JsonObject response) throws ArgumentsParseException, WorkerInvokeException, WorkerNotFoundException {
        try {
            long instanceId = IDSequenceCache.INSTANCE.fetchInstanceId();
            Registry registryInfo = InstanceDeserialize.INSTANCE.deserializeRegistryInfo(reader.readLine());

            if (StringUtil.isEmpty(registryInfo.getApplicationCode())) {
                throw new ArgumentsParseException("application code required.");
            }

            long registryTime = DateTools.getMinuteSlice(System.currentTimeMillis());

            getSelfContext().lookup(InstanceAnalysis.Role.INSTANCE).tell(new InstanceAnalysis.Instance(registryInfo.getApplicationCode(),
                instanceId, registryTime));
            getSelfContext().lookup(PingTimeAnalysis.Role.INSTANCE).tell(new PingTimeAnalysis.Ping(instanceId, registryTime));

            response.addProperty("ii", instanceId);
            response.addProperty("pt", registryTime);

            logger.debug("Application[%s] registry an instance. instance id is %d", registryInfo.getApplicationCode(), instanceId);
        } catch (IOException e) {
            throw new ArgumentsParseException(e.getMessage(), e);
        }
    }

    @Override
    public void preStart() throws ProviderNotFoundException {
        getClusterContext().findProvider(InstanceAnalysis.Role.INSTANCE).create(this);
        getClusterContext().findProvider(PingTimeAnalysis.Role.INSTANCE).create(this);
    }

    public static class Factory extends AbstractStreamPostProvider<RegistryPost> {
        @Override
        public Role role() {
            return RegistryPost.WorkerRole.INSTANCE;
        }

        @Override
        public RegistryPost workerInstance(ClusterWorkerContext clusterContext) {
            return new RegistryPost(role(), clusterContext, new LocalWorkerContext());
        }

        @Override
        public String servletPath() {
            return "/instance/register";
        }
    }

    public enum WorkerRole implements Role {
        INSTANCE;

        @Override
        public String roleName() {
            return RegistryPost.class.getSimpleName();
        }

        @Override
        public WorkerSelector workerSelector() {
            return new RollingSelector();
        }
    }
}

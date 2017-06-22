package org.skywalking.apm.collector.worker.tracedag;

import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.Map;
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
import org.skywalking.apm.collector.worker.Const;
import org.skywalking.apm.collector.worker.httpserver.AbstractGet;
import org.skywalking.apm.collector.worker.httpserver.AbstractGetProvider;
import org.skywalking.apm.collector.worker.httpserver.ArgumentsParseException;
import org.skywalking.apm.collector.worker.instance.persistence.CountInstancesWithTimeSlice;
import org.skywalking.apm.collector.worker.node.persistence.NodeCompLoad;
import org.skywalking.apm.collector.worker.node.persistence.NodeMappingSearchWithTimeSlice;
import org.skywalking.apm.collector.worker.noderef.persistence.NodeRefResSumSearchWithTimeSlice;
import org.skywalking.apm.collector.worker.noderef.persistence.NodeRefSearchWithTimeSlice;
import org.skywalking.apm.collector.worker.tools.ParameterTools;

/**
 * @author pengys5
 */
public class TraceDagGetWithTimeSlice extends AbstractGet {

    private Logger logger = LogManager.getFormatterLogger(TraceDagGetWithTimeSlice.class);

    TraceDagGetWithTimeSlice(Role role, ClusterWorkerContext clusterContext, LocalWorkerContext selfContext) {
        super(role, clusterContext, selfContext);
    }

    @Override
    public void preStart() throws ProviderNotFoundException {
        getClusterContext().findProvider(NodeCompLoad.WorkerRole.INSTANCE).create(this);
        getClusterContext().findProvider(NodeMappingSearchWithTimeSlice.WorkerRole.INSTANCE).create(this);
        getClusterContext().findProvider(NodeRefSearchWithTimeSlice.WorkerRole.INSTANCE).create(this);
        getClusterContext().findProvider(NodeRefResSumSearchWithTimeSlice.WorkerRole.INSTANCE).create(this);
        getClusterContext().findProvider(CountInstancesWithTimeSlice.WorkerRole.INSTANCE).create(this);
    }

    @Override protected void onReceive(Map<String, String[]> parameter,
        JsonObject response) throws ArgumentsParseException, WorkerInvokeException, WorkerNotFoundException {
        if (!parameter.containsKey("startTime") || !parameter.containsKey("endTime") || !parameter.containsKey("timeSliceType")) {
            throw new ArgumentsParseException("the request parameter must contains startTime,endTime,timeSliceType");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("startTime: %s, endTime: %s, timeSliceType: %s", Arrays.toString(parameter.get("startTime")),
                Arrays.toString(parameter.get("endTime")), Arrays.toString(parameter.get("timeSliceType")));
        }

        long startTime;
        try {
            startTime = Long.valueOf(ParameterTools.INSTANCE.toString(parameter, "startTime"));
        } catch (NumberFormatException e) {
            throw new ArgumentsParseException("the request parameter startTime must be a long");
        }

        long endTime;
        try {
            endTime = Long.valueOf(ParameterTools.INSTANCE.toString(parameter, "endTime"));
        } catch (NumberFormatException e) {
            throw new ArgumentsParseException("the request parameter endTime must be a long");
        }

        String timeSliceType = ParameterTools.INSTANCE.toString(parameter, "timeSliceType");

        JsonObject compResponse = getNewResponse();
        getSelfContext().lookup(NodeCompLoad.WorkerRole.INSTANCE).ask(null, compResponse);

        JsonObject nodeMappingResponse = getNewResponse();
        NodeMappingSearchWithTimeSlice.RequestEntity nodeMappingEntity = new NodeMappingSearchWithTimeSlice.RequestEntity(timeSliceType, startTime, endTime);
        getSelfContext().lookup(NodeMappingSearchWithTimeSlice.WorkerRole.INSTANCE).ask(nodeMappingEntity, nodeMappingResponse);

        JsonObject nodeCountResponse = getNewResponse();
        CountInstancesWithTimeSlice.RequestEntity nodeCountEntity = new CountInstancesWithTimeSlice.RequestEntity(startTime, endTime);
        getSelfContext().lookup(CountInstancesWithTimeSlice.WorkerRole.INSTANCE).ask(nodeCountEntity, nodeCountResponse);

        JsonObject nodeRefResponse = getNewResponse();
        NodeRefSearchWithTimeSlice.RequestEntity nodeReftEntity = new NodeRefSearchWithTimeSlice.RequestEntity(timeSliceType, startTime, endTime);
        getSelfContext().lookup(NodeRefSearchWithTimeSlice.WorkerRole.INSTANCE).ask(nodeReftEntity, nodeRefResponse);

        JsonObject resSumResponse = getNewResponse();
        NodeRefResSumSearchWithTimeSlice.RequestEntity resSumEntity = new NodeRefResSumSearchWithTimeSlice.RequestEntity(timeSliceType, startTime, endTime);
        getSelfContext().lookup(NodeRefResSumSearchWithTimeSlice.WorkerRole.INSTANCE).ask(resSumEntity, resSumResponse);

        JsonObject result = getBuilder().build(compResponse.get(Const.RESULT).getAsJsonArray(), nodeMappingResponse.get(Const.RESULT).getAsJsonArray(),
            nodeRefResponse.get(Const.RESULT).getAsJsonArray(), resSumResponse.get(Const.RESULT).getAsJsonArray(), nodeCountResponse.get(Const.RESULT).getAsJsonArray());

        response.add(Const.RESULT, result);
    }

    private JsonObject getNewResponse() {
        JsonObject response = new JsonObject();
        return response;
    }

    private TraceDagDataBuilder getBuilder() {
        TraceDagDataBuilder builder = new TraceDagDataBuilder();
        return builder;
    }

    public static class Factory extends AbstractGetProvider<TraceDagGetWithTimeSlice> {

        public static Factory INSTANCE = new Factory();

        @Override
        public Role role() {
            return WorkerRole.INSTANCE;
        }

        @Override
        public TraceDagGetWithTimeSlice workerInstance(ClusterWorkerContext clusterContext) {
            return new TraceDagGetWithTimeSlice(role(), clusterContext, new LocalWorkerContext());
        }

        @Override
        public String servletPath() {
            return "/traceDag/timeSlice";
        }
    }

    public enum WorkerRole implements Role {
        INSTANCE;

        @Override
        public String roleName() {
            return TraceDagGetWithTimeSlice.class.getSimpleName();
        }

        @Override
        public WorkerSelector workerSelector() {
            return new RollingSelector();
        }
    }
}

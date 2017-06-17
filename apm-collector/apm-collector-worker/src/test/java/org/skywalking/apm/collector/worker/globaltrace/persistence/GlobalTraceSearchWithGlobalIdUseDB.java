package org.skywalking.apm.collector.worker.globaltrace.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.skywalking.apm.collector.actor.ClusterWorkerContext;
import org.skywalking.apm.collector.actor.LocalWorkerContext;
import org.skywalking.apm.collector.worker.storage.EsClient;

/**
 * @author pengys5
 */
public class GlobalTraceSearchWithGlobalIdUseDB {

    public static void main(String[] args) throws Exception {
        EsClient.INSTANCE.boot();

        ClusterWorkerContext clusterWorkerContext = new ClusterWorkerContext(null);
        LocalWorkerContext localWorkerContext = new LocalWorkerContext();
        GlobalTraceSearchWithGlobalId globalTraceSearchWithGlobalId =
            new GlobalTraceSearchWithGlobalId(GlobalTraceSearchWithGlobalId.WorkerRole.INSTANCE, clusterWorkerContext, localWorkerContext);

        JsonObject response = new JsonObject();
        globalTraceSearchWithGlobalId.onWork("Trace.1491277147443.-1562443425.70539.65.2", response);

        JsonArray nodeArray = response.get("result").getAsJsonArray();
        for (int i = 0; i < nodeArray.size(); i++) {
            JsonObject nodeJsonObj = nodeArray.get(i).getAsJsonObject();
        }
    }
}

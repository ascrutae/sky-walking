package org.skywalking.apm.collector.worker;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.skywalking.apm.collector.actor.AbstractLocalSyncWorker;
import org.skywalking.apm.collector.actor.ClusterWorkerContext;
import org.skywalking.apm.collector.actor.LocalWorkerContext;
import org.skywalking.apm.collector.actor.ProviderNotFoundException;
import org.skywalking.apm.collector.actor.Role;
import org.skywalking.apm.collector.actor.WorkerException;
import org.skywalking.apm.collector.worker.storage.Data;
import org.skywalking.apm.collector.worker.storage.EsClient;
import org.skywalking.apm.collector.worker.storage.FlushAndSwitch;
import org.skywalking.apm.collector.worker.storage.PersistenceData;
import org.skywalking.apm.collector.worker.storage.Window;

/**
 * @author pengys5
 */
public abstract class PersistenceMember<T extends Window & PersistenceData, D extends Data> extends AbstractLocalSyncWorker {

    private Logger logger = LogManager.getFormatterLogger(PersistenceMember.class);

    public PersistenceMember(Role role, ClusterWorkerContext clusterContext, LocalWorkerContext selfContext) {
        super(role, clusterContext, selfContext);
        persistenceData = initializeData();
    }

    private T persistenceData;

    public abstract T initializeData();

    protected T getPersistenceData() {
        return persistenceData;
    }

    public abstract String esIndex();

    public abstract String esType();

    public abstract void analyse(Object message);

    @Override final public void preStart() throws ProviderNotFoundException {

    }

    @Override
    protected void onWork(Object request, Object response) throws WorkerException {
        if (request instanceof FlushAndSwitch) {
            persistenceData.switchPointer();
            while (persistenceData.getLast().isHolding()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new WorkerException(e.getMessage(), e);
                }
            }

            if (response instanceof LinkedList) {
                prepareIndex((LinkedList)response);
            } else {
                logger.error("unhandled response, response instance must LinkedList, but is %s", response.getClass().toString());
            }

        } else {
            analyse(request);
        }
    }

    private MultiGetResponse searchFromEs(Map<String, D> dataMap) {
        Client client = EsClient.INSTANCE.getClient();
        MultiGetRequestBuilder multiGetRequestBuilder = client.prepareMultiGet();

        HasDataFlag flag = new HasDataFlag();
        dataMap.forEach((key, value) -> {
            multiGetRequestBuilder.add(esIndex(), esType(), value.getId());
            flag.doTagHasData();
        });

        if (flag.isHasData()) {
            return multiGetRequestBuilder.get();
        } else {
            return null;
        }
    }

    final void extractData(Map<String, D> dataMap) {
        MultiGetResponse multiGetResponse = searchFromEs(dataMap);
        if (multiGetResponse != null) {
            for (MultiGetItemResponse itemResponse : multiGetResponse) {
                GetResponse response = itemResponse.getResponse();
                if (response != null && response.isExists()) {
                    if (dataMap.containsKey(response.getId())) {
                        dataMap.get(response.getId()).merge(response.getSource());
                    }
                }
            }
        }
    }

    protected abstract void prepareIndex(List<IndexRequestBuilder> builderList);

    class HasDataFlag {
        private boolean hasData;

        HasDataFlag() {
            hasData = false;
        }

        boolean isHasData() {
            return hasData;
        }

        void doTagHasData() {
            this.hasData = true;
        }
    }
}

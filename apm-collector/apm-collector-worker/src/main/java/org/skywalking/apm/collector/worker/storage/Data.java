package org.skywalking.apm.collector.worker.storage;

import java.util.Map;

/**
 * @author pengys5
 */
public interface Data {
    String getId();

    void merge(Map<String, ?> dbData);
}

package org.skywalking.apm.collector.worker.datamerge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.skywalking.apm.collector.worker.Const;
import org.skywalking.apm.collector.worker.storage.MetricData;
import org.skywalking.apm.collector.worker.tools.DateTools;
import org.skywalking.apm.collector.worker.tools.JsonFileReader;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author pengys5
 */
public enum MetricDataMergeJson {
    INSTANCE;

    private String path = this.getClass().getResource("/").getPath();

    public void merge(String expectJsonFile, List<MetricData> metricDataList) throws FileNotFoundException {
        Gson gson = new Gson();
        String jsonStrData = JsonFileReader.INSTANCE.read(path + expectJsonFile);
        JsonArray expectJsonArray = gson.fromJson(jsonStrData, JsonArray.class);
        Assert.assertEquals(expectJsonArray.size(), metricDataList.size());

        Map<String, JsonObject> recordDataMap = recordData2Map(metricDataList);

        for (int i = 0; i < expectJsonArray.size(); i++) {
            JsonObject rowData = expectJsonArray.get(i).getAsJsonObject();
            String id = rowData.get("id").getAsString();
            id = id2UTCSlice(id);

            JsonObject data = rowData.get("data").getAsJsonObject();
            if (recordDataMap.containsKey(id)) {
                mergeData(data, recordDataMap.get(id));
            } else {
                Assert.fail();
            }
        }
    }

    private void mergeData(JsonObject expectData, JsonObject actualData) {
        Iterator<Map.Entry<String, JsonElement>> iterator = expectData.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> entry = iterator.next();
            String key = entry.getKey();
            String value = null;
            if (!entry.getValue().isJsonNull()) {
                value = entry.getValue().getAsString();
            }

            if (entry.getValue().isJsonNull()) {
                Assert.assertEquals(true, actualData.get(key).isJsonNull());
            } else {
                String actualValue = actualData.get(key).getAsString();
                if (key.equals("timeSlice")) {
                    value = String.valueOf(DateTools.changeToUTCSlice(Long.valueOf(value)));
                    actualValue = String.valueOf(DateTools.changeToUTCSlice(Long.valueOf(actualValue)));
                }
                Assert.assertEquals(value, actualValue);
            }
        }
    }

    private Map<String, JsonObject> recordData2Map(List<MetricData> recordDataList) {
        Map<String, JsonObject> recordDataMap = new HashMap<>();
        Gson gson = new Gson();
        for (MetricData metricData : recordDataList) {
            JsonObject jsonObject = gson.fromJson(gson.toJson(metricData.asMap()), JsonObject.class);
            recordDataMap.put(id2UTCSlice(metricData.getId()), jsonObject);
        }
        return recordDataMap;
    }

    private String id2UTCSlice(String id) {
        String[] ids = id.split(Const.IDS_SPLIT);
        long changedSlice = DateTools.changeToUTCSlice(Long.valueOf(ids[0]));

        String changedId = "";

        for (int i = 1; i < ids.length; i++) {
            changedId = changedId + Const.ID_SPLIT + ids[i];
        }
        changedId = String.valueOf(changedSlice) + changedId;
        return changedId;
    }
}

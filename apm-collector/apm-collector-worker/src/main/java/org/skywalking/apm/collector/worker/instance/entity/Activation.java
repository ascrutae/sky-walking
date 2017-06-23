package org.skywalking.apm.collector.worker.instance.entity;

import com.google.gson.annotations.SerializedName;

public class Activation {
    @SerializedName("ac")
    private String applicationCode;
    @SerializedName("ii")
    private long instanceId;
    @SerializedName("rt")
    private long registryTime;

    public String getApplicationCode() {
        return applicationCode;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public long getRegistryTime() {
        return registryTime;
    }
}

package org.skywalking.apm.agent.core.collector.instance.registry;

import java.util.concurrent.ScheduledFuture;

public class Register {

    private ScheduledFuture task;

    public void registry(String applicationCode, Listener listener) {

    }

    public interface Listener {

        void success(RegistryInfo info);

        void failed();
    }

}

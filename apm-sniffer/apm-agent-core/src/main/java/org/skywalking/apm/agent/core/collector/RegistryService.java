package org.skywalking.apm.agent.core.collector;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.skywalking.apm.agent.core.boot.BootService;
import org.skywalking.apm.agent.core.collector.instance.registry.Register;
import org.skywalking.apm.agent.core.collector.instance.registry.RegistryInfo;
import org.skywalking.apm.agent.core.conf.Config;

public class RegistryService implements BootService {

    private ScheduledFuture task;

    @Override
    public void bootUp() throws Throwable {
        final Register register = new Register();
        register.registry(Config.Agent.APPLICATION_CODE, new Register.Listener() {
            @Override
            public void success(RegistryInfo info) {
                Config.Agent.INSTANCE_ID = info.getInstanceId();
            }

            @Override
            public void failed() {
                registryInBackGround();
            }
        });
    }

    private void registryInBackGround() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        final Register register = new Register();
        task = service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                register.registry(Config.Agent.APPLICATION_CODE, new Register.Listener() {
                    @Override
                    public void success(RegistryInfo info) {
                        Config.Agent.INSTANCE_ID = info.getInstanceId();
                        task.cancel(false);
                    }

                    @Override
                    public void failed() {
                    }
                });
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}

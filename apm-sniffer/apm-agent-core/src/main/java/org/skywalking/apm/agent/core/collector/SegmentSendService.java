package org.skywalking.apm.agent.core.collector;

import org.skywalking.apm.agent.core.boot.BootService;
import org.skywalking.apm.agent.core.collector.segment.SegmentSendThread;

public class SegmentSendService implements BootService {

    @Override
    public void bootUp() throws Throwable {
        SegmentSendThread sendThread = new SegmentSendThread();
    }
}

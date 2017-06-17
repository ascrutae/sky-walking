package org.skywalking.apm.sniffer.mock.trace.builders.trace;

import org.skywalking.apm.sniffer.mock.context.MockTracerContextListener;
import org.skywalking.apm.sniffer.mock.trace.TraceSegmentBuilder;
import org.skywalking.apm.sniffer.mock.trace.builders.span.TomcatSpanGenerator;
import org.skywalking.apm.trace.TraceSegment;

/**
 * A Trace contains only one span, which represent a tomcat server side span.
 * <p>
 * Created by wusheng on 2017/2/20.
 */
public enum SingleTomcat500TraceBuilder implements TraceSegmentBuilder {
    INSTANCE;

    @Override
    public TraceSegment build(MockTracerContextListener listener) {
        TomcatSpanGenerator.ON500.INSTANCE.generate();
        return listener.getFinished(0);
    }
}

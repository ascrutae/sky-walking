package org.skywalking.apm.collector.worker.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.skywalking.apm.collector.worker.segment.entity.Span;
import org.skywalking.apm.collector.worker.segment.entity.tag.Tags;

import java.util.List;

/**
 * @author pengys5
 */
public class ClientSpanIsLeafTools {
    private static final Logger logger = LogManager.getFormatterLogger(ClientSpanIsLeafTools.class);

    public static boolean isLeaf(int spanId, List<Span> spanList) {
        boolean isLeaf = true;
        for (Span span : spanList) {
            if (span.getParentSpanId() == spanId && Tags.SPAN_KIND_CLIENT.equals(Tags.SPAN_KIND.get(span))) {
                logger.debug("current spanId=%s, merge spanId=%s, span kind=%s", spanId, span.getSpanId(), Tags.SPAN_KIND.get(span));
                isLeaf = false;
            }
        }

        return isLeaf;
    }
}

package org.skywalking.apm.collector.actor.selector;

import org.skywalking.apm.collector.actor.AbstractWorker;
import org.skywalking.apm.collector.actor.WorkerRef;

import java.util.List;

/**
 * The <code>WorkerSelector</code> should be implemented by any class whose instances
 * are intended to provide select a {@link WorkerRef} from a {@link WorkerRef} list.
 * <p>
 * Actually, the <code>WorkerRef</code> is designed to provide a routing ability in the collector cluster
 *
 * @author pengys5
 * @since v3.0-2017
 */
public interface WorkerSelector<T extends WorkerRef> {

    /**
     * select a {@link WorkerRef} from a {@link WorkerRef} list.
     *
     * @param members given {@link WorkerRef} list, which size is greater than 0;
     * @param message the {@link AbstractWorker} is going to send.
     * @return the selected {@link WorkerRef}
     */
    T select(List<T> members, Object message);
}

package org.skywalking.apm.collector.actor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.skywalking.apm.collector.queue.DaemonThreadFactory;
import org.skywalking.apm.collector.queue.MessageHolder;
import org.skywalking.apm.collector.queue.MessageHolderFactory;

/**
 * @author pengys5
 */
public abstract class AbstractLocalAsyncWorkerProvider<T extends AbstractLocalAsyncWorker> extends AbstractLocalWorkerProvider<T> {

    public abstract int queueSize();

    @Override final public WorkerRef onCreate(
        LocalWorkerContext localContext) throws ProviderNotFoundException {
        T localAsyncWorker = (T)workerInstance(getClusterContext());
        localAsyncWorker.preStart();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = queueSize();
        if (!((((bufferSize - 1) & bufferSize) == 0) && bufferSize != 0)) {
            throw new IllegalArgumentException("queue size must be power of 2");
        }

        // Construct the Disruptor
        Disruptor<MessageHolder> disruptor = new Disruptor<MessageHolder>(MessageHolderFactory.INSTANCE, bufferSize, DaemonThreadFactory.INSTANCE);

        RingBuffer<MessageHolder> ringBuffer = disruptor.getRingBuffer();
        T.WorkerWithDisruptor disruptorWorker = new T.WorkerWithDisruptor(ringBuffer, localAsyncWorker);

        // Connect the handler
        disruptor.handleEventsWith(disruptorWorker);

        // Start the Disruptor, starts all threads running
        disruptor.start();

        LocalAsyncWorkerRef workerRef = new LocalAsyncWorkerRef(role(), disruptorWorker);

        if (localContext != null) {
            localContext.put(workerRef);
        }

        return workerRef;
    }
}

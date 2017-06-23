package org.skywalking.apm.agent.core.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.skywalking.apm.agent.core.boot.ServiceManager;
import org.skywalking.apm.agent.core.conf.Config;
import org.skywalking.apm.agent.core.context.trace.SegmentsMessage;
import org.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.skywalking.apm.agent.core.queue.TraceSegmentProcessQueue;
import org.skywalking.apm.logging.ILog;
import org.skywalking.apm.logging.LogManager;

/**
 * The <code>CollectorClient</code> runs as an independency thread.
 * It retrieves cached {@link TraceSegment} from {@link TraceSegmentProcessQueue},
 * and send to collector by HTTP-RESTFUL-SERVICE: POST /skywalking/trace/segment
 *
 * @author wusheng
 */
public class CollectorClient implements Runnable {
    private static final ILog logger = LogManager.getLogger(CollectorClient.class);
    private static long SLEEP_TIME_MILLIS = 500;
    private String[] serverList;
    private volatile int selectedServer = -1;
    private Gson serializer;
    private long lastSendSegmentTime;

    public CollectorClient() {
        serverList = Config.Collector.SERVERS.split(",");
        Random r = new Random();
        if (serverList.length > 0) {
            selectedServer = r.nextInt(serverList.length);
        }
        serializer = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();
    }

    @Override
    public void run() {
        while (true) {
            try {
                long sleepTime = -1;
                TraceSegmentProcessQueue segmentProcessQueue = ServiceManager.INSTANCE.findService(TraceSegmentProcessQueue.class);
                List<TraceSegment> cachedTraceSegments = segmentProcessQueue.getCachedTraceSegments();
                if (cachedTraceSegments.size() > 0) {
                    SegmentsMessage message = null;
                    int count = 0;
                    int segmentWithoutInstanceIdCount = 0;
                    for (TraceSegment segment : cachedTraceSegments) {
                        if (segment.getInstanceId() == 0) {
                            segmentWithoutInstanceIdCount++;
                            continue;
                        }

                        if (message == null) {
                            message = new SegmentsMessage();
                        }
                        message.append(segment);
                        if (count++ == Config.Collector.BATCH_SIZE) {
                            sendToCollector(message);
                            message = null;
                        }
                    }
                    sendToCollector(message);
                    lastSendSegmentTime = System.currentTimeMillis();

                    logger.debug("Send segment count: {}, segment without instance id count: {}.", cachedTraceSegments.size(),
                        segmentWithoutInstanceIdCount);
                } else {
                    sleepTime = SLEEP_TIME_MILLIS;
                    sendPingTimeIfNecessary();
                }

                if (sleepTime > 0) {
                    try2Sleep(sleepTime);
                }
            } catch (Throwable t) {
                logger.error(t, "Send trace segments to collector failure.");
            }
        }
    }

    /**
     * Send the given {@link SegmentsMessage} to collector.
     *
     * @param message to be send.
     */
    private void sendToCollector(SegmentsMessage message) throws RESTResponseStatusError, IOException {
        if (message == null) {
            return;
        }
        sendMessage(message.serialize(serializer), Config.Collector.Service.SEGMENTS);
    }

    private void sendToCollector(PingTime time) throws RESTResponseStatusError, IOException {
        if (time == null) {
            return;
        }
        sendMessage(time.serialize(serializer), Config.Collector.Service.PING_TIME);
    }

    private void sendMessage(String messageJson, String requestURL) throws RESTResponseStatusError, IOException {
        CloseableHttpClient httpClient = HttpClients.custom().build();
        try {
            HttpPost httpPost = ready2Send(messageJson, requestURL);
            if (httpPost != null) {
                CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (200 != statusCode) {
                    findBackupServer();
                    throw new RESTResponseStatusError(statusCode);
                }
            }
        } catch (IOException e) {
            findBackupServer();
            throw e;
        } finally {
            httpClient.close();
        }
    }

    private void sendPingTimeIfNecessary() throws RESTResponseStatusError, IOException {
        boolean sendPingTime = System.currentTimeMillis() - lastSendSegmentTime <= TimeUnit.MINUTES.toMillis(1);
        if (Config.Agent.INSTANCE_ID != -1 && sendPingTime) {
            sendToCollector(new PingTime(Config.Agent.INSTANCE_ID));
        }
    }

    /**
     * Prepare the given message for HTTP Post service.
     *
     * @param messageJson to send
     * @param requestURL
     * @return {@link HttpPost}, when is ready to send. otherwise, null.
     */
    private HttpPost ready2Send(String messageJson, String requestURL) {
        if (selectedServer == -1) {
            //no available server
            return null;
        }
        HttpPost post = new HttpPost("http://" + serverList[selectedServer] + requestURL);
        StringEntity entity = new StringEntity(messageJson, ContentType.APPLICATION_JSON);
        post.setEntity(entity);

        return post;
    }

    /**
     * Choose the next server in {@link #serverList}, by moving {@link #selectedServer}.
     */
    private void findBackupServer() {
        selectedServer++;
        if (selectedServer == serverList.length) {
            selectedServer = 0;
        }
    }

    /**
     * Try to sleep, and ignore the {@link InterruptedException}
     *
     * @param millis the length of time to sleep in milliseconds
     */
    private void try2Sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {

        }
    }

    private class PingTime {
        @Expose
        @SerializedName("ii")
        private Long instanceId;

        public PingTime(Long instanceId) {
            this.instanceId = instanceId;
        }

        public String serialize(Gson serializer) {
            return serializer.toJson(this);
        }
    }
}

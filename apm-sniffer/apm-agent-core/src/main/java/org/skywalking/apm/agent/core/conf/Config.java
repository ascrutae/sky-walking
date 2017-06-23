package org.skywalking.apm.agent.core.conf;

import java.util.LinkedList;
import java.util.List;
import org.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.skywalking.apm.agent.core.logging.LogLevel;
import org.skywalking.apm.agent.core.logging.WriterFactory;

/**
 * This is the core config in sniffer agent.
 *
 * @author wusheng
 */
public class Config {

    public static class Agent {
        /**
         * Application code is showed in sky-walking-ui.
         * Suggestion: set a unique name for each application, one application's nodes share the same code.
         */
        public static String APPLICATION_CODE = "";
        /**
         * Negative or zero means off, by default.
         * {@link #SAMPLE_N_PER_10_SECS} means sampling N {@link TraceSegment} in 10 seconds tops.
         */
        public static int SAMPLE_N_PER_10_SECS = -1;

        /**
         * If the operation name of the first span is included in this set,
         * this segment should be ignored.
         */
        public static String IGNORE_SUFFIX = ".jpg,.jpeg,.js,.css,.png,.bmp,.gif,.ico,.mp3,.mp4,.html";

        public static long INSTANCE_ID = 0;
    }

    public static class Collector {
        /**
         * Collector REST-Service address.
         * e.g.
         * SERVERS="127.0.0.1:8080"  for single collector node.
         * SERVERS="10.2.45.126:8080,10.2.45.127:7600"  for multi collector nodes.
         */
        public static String SERVERS = "";

        /**
         * The max size to send traces per rest-service call.
         */
        public static int BATCH_SIZE = 50;

        public static class Service {

            /**
             * Collector receive segments REST-Service name.
             */
            public static String SEGMENTS = "/segments";

            /**
             * Collector receive ping time REST-Service name.
             */
            public static String PING_TIME = "/instances/ping";

            /**
             * Collector receive registry info REST-Service name.
             */
            public static String REGISTRY_INSTANCE = "/instances/registry";

            /**
             * Collector receive activation info REST-Service name.
             */
            public static String ACTIVATE_INSTANCE = "/instances/active";

        }
    }

    public static class Buffer {
        /**
         * The in-memory buffer size. Based on Disruptor, this value must be 2^n.
         *
         * @see {https://github.com/LMAX-Exchange/disruptor}
         */
        public static int SIZE = 512;
    }

    public static class Logging {
        /**
         * Log file name.
         */
        public static String FILE_NAME = "skywalking-api.log";

        /**
         * Log files directory.
         * Default is blank string, means, use "system.out" to output logs.
         *
         * @see {@link WriterFactory#getLogWriter()}
         */
        public static String DIR = "";

        /**
         * The max size of log file.
         * If the size is bigger than this, archive the current file, and write into a new file.
         */
        public static int MAX_FILE_SIZE = 300 * 1024 * 1024;

        /**
         * The log level. Default is debug.
         *
         * @see {@link LogLevel}
         */
        public static LogLevel LEVEL = LogLevel.DEBUG;
    }

    public static class Plugin {

        /**
         * Name of disabled plugin, The value spilt by <code>,</code>
         * if you have multiple plugins need to disable.
         *
         * Here are the plugin names :
         * tomcat-7.x/8.x, dubbo, jedis-2.x, motan, httpclient-4.x, jdbc, mongodb-3.x.
         */
        public static List DISABLED_PLUGINS = new LinkedList();

        public static class MongoDB {
            /**
             * If true, trace all the parameters, default is false.
             * Only trace the operation, not include parameters.
             */
            public static boolean TRACE_PARAM = false;
        }

        public static class Propagation {

            /**
             * The header name of cross process propagation data.
             */
            public static String HEADER_NAME = "sw3";
        }
    }
}

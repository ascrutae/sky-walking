package org.skywalking.apm.plugin.jdbc;

import java.sql.SQLException;
import java.util.List;
import org.skywalking.apm.agent.core.context.ContextManager;
import org.skywalking.apm.agent.core.context.tag.Tags;
import org.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.skywalking.apm.logging.ILog;
import org.skywalking.apm.logging.LogManager;
import org.skywalking.apm.util.StringUtil;

public class PreparedStatementTracing {

    private static ILog LOGGER = LogManager.getLogger(PreparedStatementTracing.class);

    public static <R> R execute(java.sql.PreparedStatement realStatement,
        ConnectionInfo connectInfo, String method, String sql, Executable<R> exec)
        throws SQLException {
        try {
            String remotePeer;
            if (!StringUtil.isEmpty(connectInfo.getHosts())) {
                remotePeer = connectInfo.getHosts();
            } else {
                remotePeer = connectInfo.getHost() + ":" + connectInfo.getPort();
            }
            List<AbstractTracingSpan> activeSpans = ContextManager.activeSpans();
            if (activeSpans.size() != 1) {
                StringBuilder logInfo = new StringBuilder("PreparedStatementTracing.beforeMethod [\n");
                for (AbstractTracingSpan span : activeSpans) {
                    logInfo.append("<" + span.getParentSpanId() + "," + span.getSpanId() + ">\t" + span.getOperationId() + "\n");
                }
                LOGGER.info(logInfo + "]");
            }

            AbstractSpan span = ContextManager.createExitSpan(connectInfo.getDBType() + "/JDBI/PreparedStatement/" + method, remotePeer);
            Tags.DB_TYPE.set(span, "sql");
            Tags.DB_INSTANCE.set(span, connectInfo.getDatabaseName());
            Tags.DB_STATEMENT.set(span, sql);
            span.setComponent(connectInfo.getComponent());
            activeSpans = ContextManager.activeSpans();
            if (activeSpans.size() != 2) {
                StringBuilder logInfo = new StringBuilder("PreparedStatementTracing.beforeMethod-createSpan [\n");
                for (AbstractTracingSpan activeSpan : activeSpans) {
                    logInfo.append("<" + activeSpan.getParentSpanId() + "," + activeSpan.getSpanId() + ">\t" + activeSpan.getOperationId() + "\n");
                }
                LOGGER.info(logInfo + "]");
            }
            SpanLayer.asDB(span);
            return exec.exe(realStatement, sql);
        } catch (SQLException e) {
            LOGGER.info("JedisMethodInterceptor occur exception.");
            AbstractSpan span = ContextManager.activeSpan();
            span.errorOccurred();
            span.log(e);
            throw e;
        } finally {
            List<AbstractTracingSpan> activeSpans = ContextManager.activeSpans();
            if (activeSpans.size() != 2) {
                StringBuilder logInfo = new StringBuilder("PreparedStatementTracing.afterMethod [\n");
                for (AbstractTracingSpan span : activeSpans) {
                    logInfo.append("<" + span.getParentSpanId() + "," + span.getSpanId() + ">\t" + span.getOperationId() + "\n");
                }
                LOGGER.info(logInfo + "]");
            }
            ContextManager.stopSpan();

            activeSpans = ContextManager.activeSpans();
            if (activeSpans.size() != 1) {
                StringBuilder logInfo = new StringBuilder("PreparedStatementTracing.afterMethod-stopSpan [\n");
                for (AbstractTracingSpan span : activeSpans) {
                    logInfo.append("<" + span.getParentSpanId() + "," + span.getSpanId() + ">\t" + span.getOperationId() + "\n");
                }
                LOGGER.info(logInfo + "]");
            }
        }
    }

    public interface Executable<R> {
        R exe(java.sql.PreparedStatement realConnection, String sql)
            throws SQLException;
    }
}

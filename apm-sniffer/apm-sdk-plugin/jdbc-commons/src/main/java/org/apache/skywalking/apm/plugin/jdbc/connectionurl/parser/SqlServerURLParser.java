package org.apache.skywalking.apm.plugin.jdbc.connectionurl.parser;

import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

import static org.apache.skywalking.apm.network.trace.component.ComponentsDefine.SQLSERVER_JDBC_DRIVER;

/**
 * @author Zhang Xin
 */
public class SqlServerURLParser extends AbstractURLParser {
    private static final int DEFAULT_PORT = 1433;
    private static final String DB_TYPE = "SQLServer";
    private static final String DEFAULT_DATABASE_NAME = "(default)";

    public SqlServerURLParser(String url) {
        super(url);
    }

    @Override protected URLLocation fetchDatabaseHostsIndexRange() {
        int hostLabelStartIndex = url.indexOf("//");
        int hostLabelEndIndex = url.indexOf(";", hostLabelStartIndex + 2);
        if (hostLabelEndIndex == -1) {
            hostLabelEndIndex = url.length();
        }
        return new URLLocation(hostLabelStartIndex + 2, hostLabelEndIndex);
    }

    @Override protected URLLocation fetchDatabaseNameIndexRange() {
        int databaseNameStartIndex = url.toLowerCase().indexOf("databasename");
        if (databaseNameStartIndex == -1) {
            return null;
        }

        int equalIndex = url.indexOf("=", databaseNameStartIndex + 12);
        int databaseNameEndIndex = url.indexOf(";", databaseNameStartIndex + 12);
        if (databaseNameEndIndex == -1) {
            databaseNameEndIndex = url.length();
        }

        return new URLLocation(equalIndex + 1, databaseNameEndIndex);
    }

    @Override public ConnectionInfo parse() {
        URLLocation databaseNameLocation = fetchDatabaseNameIndexRange();
        String databaseName = DEFAULT_DATABASE_NAME;
        if (databaseNameLocation != null) {
            databaseName = fetchDatabaseNameFromURL();
        }

        URLLocation hostName = fetchDatabaseHostsIndexRange();
        String hosts = url.substring(hostName.startIndex(), hostName.endIndex());
        String[] hostAndPort = hosts.split(":");
        if (hostAndPort.length != 1) {
            return new ConnectionInfo(SQLSERVER_JDBC_DRIVER, DB_TYPE, hostAndPort[0], Integer.valueOf(hostAndPort[1]), databaseName);
        } else {
            return new ConnectionInfo(SQLSERVER_JDBC_DRIVER, DB_TYPE, hostAndPort[0], DEFAULT_PORT, databaseName);
        }
    }
}

/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * 
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL license a copy of which has
 * been included with this distribution in the LICENSE.txt file.
 */

package com.mirth.connect.connectors.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.DispatcherConnectorPropertiesInterface;
import com.mirth.connect.donkey.model.channel.QueueConnectorProperties;
import com.mirth.connect.donkey.util.DonkeyElement;

public class DatabaseDispatcherProperties extends ConnectorProperties implements DispatcherConnectorPropertiesInterface {
    public static final String NAME = "Database Writer";

    private QueueConnectorProperties queueConnectorProperties;

    private String driver;
    private String url;
    private String username;
    private String password;
    private String query;
    private Object[] parameters;
    private boolean useScript;

    public static final String DRIVER_DEFAULT = "Please Select One";

    public DatabaseDispatcherProperties() {
        queueConnectorProperties = new QueueConnectorProperties();

        this.driver = DRIVER_DEFAULT;
        this.url = "";
        this.username = "";
        this.password = "";
        this.query = "";
        this.useScript = false;
    }

    public DatabaseDispatcherProperties(DatabaseDispatcherProperties props) {
        super(props);
        queueConnectorProperties = new QueueConnectorProperties(props.getQueueConnectorProperties());

        this.driver = props.getDriver();
        this.url = props.getUrl();
        this.username = props.getUsername();
        this.password = props.getPassword();
        this.query = props.getQuery();
        this.useScript = props.isUseScript();
    }

    @Override
    public String getProtocol() {
        return "jdbc";
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String toFormattedString() {
        StringBuilder builder = new StringBuilder();
        String newLine = "\n";
        builder.append("URL: ");
        builder.append(url);
        builder.append(newLine);

        builder.append("USERNAME: ");
        builder.append(username);
        builder.append(newLine);

        builder.append(newLine);
        builder.append(useScript ? "[SCRIPT]" : "[QUERY]");
        builder.append(newLine);
        builder.append(StringUtils.trim(query));

        for (int i = 0; i < parameters.length; i++) {
            builder.append(newLine);
            builder.append(newLine);
            builder.append("[PARAMETER ");
            builder.append(String.valueOf(i + 1));
            builder.append("]");
            builder.append(newLine);
            builder.append(parameters[i]);
        }

        return builder.toString();
    }

    @Override
    public QueueConnectorProperties getQueueConnectorProperties() {
        return queueConnectorProperties;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public boolean isUseScript() {
        return useScript;
    }

    public void setUseScript(boolean useScript) {
        this.useScript = useScript;
    }

    @Override
    public ConnectorProperties clone() {
        return new DatabaseDispatcherProperties(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public void migrate3_0_1(DonkeyElement element) {}

    @Override
    public void migrate3_0_2(DonkeyElement element) {}
}
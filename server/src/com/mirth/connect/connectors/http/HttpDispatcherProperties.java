/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * 
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL license a copy of which has
 * been included with this distribution in the LICENSE.txt file.
 */

package com.mirth.connect.connectors.http;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.DispatcherConnectorPropertiesInterface;
import com.mirth.connect.donkey.model.channel.QueueConnectorProperties;
import com.mirth.connect.donkey.util.DonkeyElement;

public class HttpDispatcherProperties extends ConnectorProperties implements DispatcherConnectorPropertiesInterface {

    private QueueConnectorProperties queueConnectorProperties;

    private String host;
    private boolean useProxyServer;
    private String proxyAddress;
    private String proxyPort;
    private String method;
    private Map<String, String> headers;
    private Map<String, String> parameters;
    private boolean responseXmlBody;
    private boolean responseParseMultipart;
    private boolean responseIncludeMetadata;
    private boolean multipart;
    private boolean useAuthentication;
    private String authenticationType;
    private String username;
    private String password;
    private String content;
    private String contentType;
    private String charset;
    private String socketTimeout;

    public HttpDispatcherProperties() {
        queueConnectorProperties = new QueueConnectorProperties();

        this.host = "";
        this.useProxyServer = false;
        this.proxyAddress = "";
        this.proxyPort = "";
        this.method = "post";
        this.headers = new LinkedHashMap<String, String>();
        this.parameters = new LinkedHashMap<String, String>();
        this.responseXmlBody = false;
        this.responseParseMultipart = true;
        this.responseIncludeMetadata = false;
        this.multipart = false;
        this.useAuthentication = false;
        this.authenticationType = "Basic";
        this.username = "";
        this.password = "";
        this.content = "";
        this.contentType = "text/plain";
        this.charset = "UTF-8";
        this.socketTimeout = "30000";
    }

    public HttpDispatcherProperties(HttpDispatcherProperties props) {
        super(props);
        queueConnectorProperties = new QueueConnectorProperties(props.getQueueConnectorProperties());

        host = props.getHost();
        useProxyServer = props.isUseProxyServer();
        proxyAddress = props.getProxyAddress();
        proxyPort = props.getProxyPort();
        method = props.getMethod();
        headers = new LinkedHashMap<String, String>(props.getHeaders());
        parameters = new LinkedHashMap<String, String>(props.getParameters());
        responseXmlBody = props.isResponseXmlBody();
        responseParseMultipart = props.isResponseParseMultipart();
        responseIncludeMetadata = props.isResponseIncludeMetadata();
        multipart = props.isMultipart();
        useAuthentication = props.isUseAuthentication();
        authenticationType = props.getAuthenticationType();
        username = props.getUsername();
        password = props.getPassword();
        content = props.getContent();
        contentType = props.getContentType();
        charset = props.getCharset();
        socketTimeout = props.getSocketTimeout();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isUseProxyServer() {
        return useProxyServer;
    }

    public void setUseProxyServer(boolean useProxyServer) {
        this.useProxyServer = useProxyServer;
    }

    public String getProxyAddress() {
        return proxyAddress;
    }

    public void setProxyAddress(String proxyAddress) {
        this.proxyAddress = proxyAddress;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public boolean isResponseXmlBody() {
        return responseXmlBody;
    }

    public void setResponseXmlBody(boolean responseXmlBody) {
        this.responseXmlBody = responseXmlBody;
    }

    public boolean isResponseParseMultipart() {
        return responseParseMultipart;
    }

    public void setResponseParseMultipart(boolean responseParseMultipart) {
        this.responseParseMultipart = responseParseMultipart;
    }

    public boolean isResponseIncludeMetadata() {
        return responseIncludeMetadata;
    }

    public void setResponseIncludeMetadata(boolean responseIncludeMetadata) {
        this.responseIncludeMetadata = responseIncludeMetadata;
    }

    public boolean isMultipart() {
        return multipart;
    }

    public void setMultipart(boolean multipart) {
        this.multipart = multipart;
    }

    public boolean isUseAuthentication() {
        return useAuthentication;
    }

    public void setUseAuthentication(boolean useAuthentication) {
        this.useAuthentication = useAuthentication;
    }

    public String getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(String socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    @Override
    public String getProtocol() {
        return "HTTP";
    }

    @Override
    public String getName() {
        return "HTTP Sender";
    }

    @Override
    public String toFormattedString() {
        StringBuilder builder = new StringBuilder();
        String newLine = "\n";

        builder.append("URL: ");
        builder.append(host);
        builder.append(newLine);

        builder.append("METHOD: ");
        builder.append(method.toUpperCase());
        builder.append(newLine);

        if (StringUtils.isNotBlank(username)) {
            builder.append("USERNAME: ");
            builder.append(username);
            builder.append(newLine);
        }

        builder.append(newLine);
        builder.append("[HEADERS]");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.append(newLine);
            builder.append(header.getKey() + ": " + header.getValue());
        }
        builder.append(newLine);

        builder.append(newLine);
        builder.append("[PARAMETERS]");
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            builder.append(newLine);
            builder.append(parameter.getKey() + ": " + parameter.getValue());
        }
        builder.append(newLine);

        builder.append(newLine);
        builder.append("[CONTENT]");
        builder.append(newLine);
        builder.append(content);
        return builder.toString();
    }

    @Override
    public QueueConnectorProperties getQueueConnectorProperties() {
        return queueConnectorProperties;
    }

    @Override
    public ConnectorProperties clone() {
        return new HttpDispatcherProperties(this);
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
/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * 
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL license a copy of which has
 * been included with this distribution in the LICENSE.txt file.
 */

package com.mirth.connect.connectors.ws;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.ws.Binding;
import javax.xml.ws.Endpoint;
import javax.xml.ws.handler.Handler;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

import com.mirth.connect.donkey.model.event.ConnectionStatusEventType;
import com.mirth.connect.donkey.model.message.RawMessage;
import com.mirth.connect.donkey.server.DeployException;
import com.mirth.connect.donkey.server.HaltException;
import com.mirth.connect.donkey.server.StartException;
import com.mirth.connect.donkey.server.StopException;
import com.mirth.connect.donkey.server.UndeployException;
import com.mirth.connect.donkey.server.channel.ChannelException;
import com.mirth.connect.donkey.server.channel.DispatchResult;
import com.mirth.connect.donkey.server.channel.SourceConnector;
import com.mirth.connect.donkey.server.event.ConnectionStatusEvent;
import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import com.mirth.connect.server.util.TemplateValueReplacer;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

public class WebServiceReceiver extends SourceConnector {
    private Logger logger = Logger.getLogger(this.getClass());
    private EventController eventController = ControllerFactory.getFactory().createEventController();
    private ConfigurationController configurationController = ControllerFactory.getFactory().createConfigurationController();
    private ExecutorService executor;
    private Endpoint webServiceEndpoint;
    private TemplateValueReplacer replacer = new TemplateValueReplacer();
    private WebServiceConfiguration configuration;
    private HttpServer server;
    private WebServiceReceiverProperties connectorProperties;

    @Override
    public void onDeploy() throws DeployException {
        this.connectorProperties = (WebServiceReceiverProperties) getConnectorProperties();

        // load the default configuration
        String configurationClass = configurationController.getProperty(connectorProperties.getProtocol(), "wsConfigurationClass");

        try {
            configuration = (WebServiceConfiguration) Class.forName(configurationClass).newInstance();
        } catch (Exception e) {
            logger.trace("could not find custom configuration class, using default");
            configuration = new DefaultWebServiceConfiguration();
        }

        try {
            configuration.configureConnectorDeploy(this);
        } catch (Exception e) {
            throw new DeployException(e);
        }
    }

    @Override
    public void onUndeploy() throws UndeployException {
        configuration.configureConnectorUndeploy(this);
    }

    @Override
    public void onStart() throws StartException {
        String host = replacer.replaceValues(connectorProperties.getListenerConnectorProperties().getHost(), getChannelId());
        int port = NumberUtils.toInt(replacer.replaceValues(connectorProperties.getListenerConnectorProperties().getPort(), getChannelId()));

        logger.debug("starting Web Service HTTP server on port: " + port);

        java.util.logging.Logger.getLogger("javax.enterprise.resource.webservices.jaxws.server").setLevel(java.util.logging.Level.OFF);

        try {
            configuration.configureReceiver(this);
            server.bind(new InetSocketAddress(host, port), 5);
        } catch (Exception e) {
            throw new StartException("Error creating HTTP Server.", e);
        }

        executor = Executors.newFixedThreadPool(5);
        server.setExecutor(executor);
        server.start();

        AcceptMessage acceptMessageWebService = null;

        try {
            Class<?> clazz = Class.forName(replacer.replaceValues(connectorProperties.getClassName(), getChannelId()));

            if (clazz.getSuperclass().equals(AcceptMessage.class)) {
                Constructor<?>[] constructors = clazz.getDeclaredConstructors();
                for (int i = 0; i < constructors.length; i++) {
                    Class<?>[] parameters = constructors[i].getParameterTypes();
                    if ((parameters.length == 1) && parameters[0].equals(this.getClass())) {
                        acceptMessageWebService = (AcceptMessage) constructors[i].newInstance(new Object[] { this });
                    }
                }

                if (acceptMessageWebService == null) {
                    logger.error("Custom web service class must implement the constructor: public AcceptMessage(WebServiceReceiver webServiceReceiver)");
                }
            } else {
                logger.error("Custom web service class must extend com.mirth.connect.connectors.ws.AcceptMessage");
            }
        } catch (Exception e) {
            logger.error("Custom web service class initialization failed", e);
        }

        if (acceptMessageWebService == null) {
            logger.error("Custom web service class initialization failed, using DefaultAcceptMessage");
            acceptMessageWebService = new DefaultAcceptMessage(this);
        }

        webServiceEndpoint = Endpoint.create(acceptMessageWebService);
        Binding binding = webServiceEndpoint.getBinding();
        List<Handler> handlerChain = new LinkedList<Handler>();
        handlerChain.add(new LoggingSOAPHandler(this));
        binding.setHandlerChain(handlerChain);

        String serviceName = replacer.replaceValues(connectorProperties.getServiceName(), getChannelId());
        HttpContext context = server.createContext("/services/" + serviceName);

        if (connectorProperties.getUsernames().size() > 0) {
            context.setAuthenticator(new BasicAuthenticator("/services/" + serviceName) {
                @Override
                public boolean checkCredentials(String username, String password) {
                    List<String> usernames = connectorProperties.getUsernames();
                    List<String> passwords = connectorProperties.getPasswords();
                    if (usernames.contains(username) && passwords.get(usernames.indexOf(username)).equals(password)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }

        webServiceEndpoint.publish(context);

        eventController.dispatchEvent(new ConnectionStatusEvent(getChannelId(), getMetaDataId(), getSourceName(), ConnectionStatusEventType.IDLE));
    }

    @Override
    public void onStop() throws StopException {
        try {
            logger.debug("stopping Web Service HTTP server");

            if (webServiceEndpoint != null) {
                webServiceEndpoint.stop();
            }

            if (server != null) {
                server.stop(1);
            }

            if (executor != null) {
                executor.shutdown();
            }
        } catch (Exception e) {
            throw new StopException("Failed to stop Web Service Listener", e);
        }
    }

    @Override
    public void onHalt() throws HaltException {
        try {
            onStop();
        } catch (StopException e) {
            throw new HaltException(e);
        }
    }

    @Override
    public void handleRecoveredResponse(DispatchResult dispatchResult) {
        finishDispatch(dispatchResult);
    }

    public String processData(String message) {
        eventController.dispatchEvent(new ConnectionStatusEvent(getChannelId(), getMetaDataId(), getSourceName(), ConnectionStatusEventType.RECEIVING));

        RawMessage rawMessage = new RawMessage(message);
        DispatchResult dispatchResult = null;
        String response = null;

        try {
            dispatchResult = dispatchRawMessage(rawMessage);

            if (dispatchResult.getSelectedResponse() != null) {
                response = dispatchResult.getSelectedResponse().getMessage();
            }
        } catch (ChannelException e) {
            // TODO auto-generate an error response?
        } finally {
            // TODO: response should be returned before it is marked as finished
            // TODO: figure out how to get the error message if an error occurred in sending the response back
            finishDispatch(dispatchResult, true, null);
        }

        // TODO find a way to call this after the response was sent
        eventController.dispatchEvent(new ConnectionStatusEvent(getChannelId(), getMetaDataId(), getSourceName(), ConnectionStatusEventType.IDLE));
        return response;
    }

    public void setServer(HttpServer server) {
        this.server = server;
    }
}
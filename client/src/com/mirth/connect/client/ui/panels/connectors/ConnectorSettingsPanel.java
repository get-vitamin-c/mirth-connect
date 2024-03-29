/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * 
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL license a copy of which has
 * been included with this distribution in the LICENSE.txt file.
 */

package com.mirth.connect.client.ui.panels.connectors;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.mirth.connect.client.ui.ConnectorTypeDecoration;
import com.mirth.connect.client.ui.VariableListHandler.TransferMode;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.model.CodeTemplate;

public abstract class ConnectorSettingsPanel extends JPanel {

    protected ConnectorPanel connectorPanel;

    /**
     * Gets the name of the connector
     */
    public abstract String getConnectorName();

    /**
     * Gets all of the current data in the connector's form.
     */
    public abstract ConnectorProperties getProperties();

    /**
     * Sets all of the current data in the connector's form to the data in the properties object
     * parameter
     */
    public abstract void setProperties(ConnectorProperties properties);

    /**
     * Gets all of the default connector properties
     */
    public abstract ConnectorProperties getDefaults();

    /**
     * Checks to see if the properties in the connector are all valid. Highlights fields that are
     * not valid if highlight=true.
     * 
     * @param props
     * @param highlight
     * @return
     */
    public abstract boolean checkProperties(ConnectorProperties properties, boolean highlight);

    /**
     * Resets the highlighting on fields that could be highlighted.
     */
    public abstract void resetInvalidProperties();

    /**
     * Runs any custom validation that has been created on the connector. Returns null if
     * successful, and a String error message otherwise. Also validates the form and highlights
     * invalid fields if highlight=true.
     * 
     * @param properties
     * @return
     */
    public String doValidate(ConnectorProperties properties, boolean highlight) {
        return null;
    }

    public TransferMode getTransferMode() {
        return TransferMode.VELOCITY;
    }

    public ArrayList<CodeTemplate> getReferenceItems() {
        return new ArrayList<CodeTemplate>();
    }

    public boolean requiresXmlDataType() {
        return false;
    }

    public List<String> getScripts(ConnectorProperties properties) {
        return new ArrayList<String>();
    }

    /**
     * An update notification that a specific field was updated. Only some fields are implemented to
     * call this method.
     * 
     * @param field
     */
    public void updatedField(String field) {}

    /**
     * Get the this connector's filled properties. This contains all of the properties this
     * connector inherits.
     * 
     * @return
     */
    public ConnectorProperties getFilledProperties() {
        return ((ConnectorPanel) getParent().getParent()).getProperties();
    }

    /**
     * Sets the ConnectorPanel associated with this connector-specific panel.
     * 
     * @param connectorPanel
     */
    public final void setConnectorPanel(ConnectorPanel connectorPanel) {
        this.connectorPanel = connectorPanel;
    }

    /**
     * Returns any special highlighting/etc. that should be done for the connector type in its
     * associated table. Returns null if no decoration should be done, or if not applicable.
     */
    public ConnectorTypeDecoration getConnectorTypeDecoration() {
        return null;
    }

    /**
     * Notifies the overall connector panel that connector type decoration updates should take
     * place.
     */
    protected final void decorateConnectorType() {
        if (connectorPanel != null) {
            connectorPanel.decorateConnectorType();
        }
    }

    /**
     * Using the decoration object parameter, performs any special highlighting/etc. that should be
     * done.
     * 
     * @param connectorTypeDecoration
     */
    public void doLocalDecoration(ConnectorTypeDecoration connectorTypeDecoration) {}

    /**
     * Allows the overall connector panel to take care of actions after a connector service has been
     * invoked.
     * 
     * @param method
     * @param response
     */
    protected final void handlePluginConnectorServiceResponse(String method, Object response) {
        connectorPanel.handlePluginConnectorServiceResponse(method, response);
    }

    /**
     * Allows the connector-specific settings panel to take actions after a connector service has
     * been invoked. This may be called when handlePluginConnectorServiceResponse is called, unless
     * the event is intercepted by a connector service plugin.
     * 
     * @param method
     * @param response
     */
    public void handleConnectorServiceResponse(String method, Object response) {}
}

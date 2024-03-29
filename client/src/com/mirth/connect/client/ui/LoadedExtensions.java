/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * 
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL license a copy of which has
 * been included with this distribution in the LICENSE.txt file.
 */

package com.mirth.connect.client.ui;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.model.ConnectorMetaData;
import com.mirth.connect.model.PluginClass;
import com.mirth.connect.model.PluginMetaData;
import com.mirth.connect.plugins.AttachmentViewer;
import com.mirth.connect.plugins.ChannelColumnPlugin;
import com.mirth.connect.plugins.ChannelPanelPlugin;
import com.mirth.connect.plugins.ChannelWizardPlugin;
import com.mirth.connect.plugins.ClientPlugin;
import com.mirth.connect.plugins.CodeTemplatePlugin;
import com.mirth.connect.plugins.ConnectorPropertiesPlugin;
import com.mirth.connect.plugins.DashboardColumnPlugin;
import com.mirth.connect.plugins.DashboardPanelPlugin;
import com.mirth.connect.plugins.DataTypeClientPlugin;
import com.mirth.connect.plugins.FilterRulePlugin;
import com.mirth.connect.plugins.SettingsPanelPlugin;
import com.mirth.connect.plugins.TransformerStepPlugin;
import com.mirth.connect.plugins.TransmissionModePlugin;

public class LoadedExtensions {

    private List<ClientPlugin> clientPlugins = new ArrayList<ClientPlugin>();
    private Map<String, SettingsPanelPlugin> settingsPanelPlugins = new LinkedHashMap<String, SettingsPanelPlugin>();
    private Map<String, ChannelPanelPlugin> channelPanelPlugins = new LinkedHashMap<String, ChannelPanelPlugin>();
    private Map<String, DashboardPanelPlugin> dashboardPanelPlugins = new LinkedHashMap<String, DashboardPanelPlugin>();
    private Map<String, ChannelWizardPlugin> channelWizardPlugins = new LinkedHashMap<String, ChannelWizardPlugin>();
    private Map<String, ChannelColumnPlugin> channelColumnPlugins = new LinkedHashMap<String, ChannelColumnPlugin>();
    private Map<String, DashboardColumnPlugin> dashboardColumnPlugins = new LinkedHashMap<String, DashboardColumnPlugin>();
    private Map<String, AttachmentViewer> attachmentViewerPlugins = new LinkedHashMap<String, AttachmentViewer>();
    private Map<String, FilterRulePlugin> filterRulePlugins = new LinkedHashMap<String, FilterRulePlugin>();
    private Map<String, TransformerStepPlugin> transformerStepPlugins = new LinkedHashMap<String, TransformerStepPlugin>();
    private Map<String, CodeTemplatePlugin> codeTemplatePlugins = new LinkedHashMap<String, CodeTemplatePlugin>();
    private Map<String, DataTypeClientPlugin> dataTypePlugins = new TreeMap<String, DataTypeClientPlugin>();
    private Map<String, TransmissionModePlugin> transmissionModePlugins = new TreeMap<String, TransmissionModePlugin>();
    private Map<String, ConnectorPropertiesPlugin> connectorPropertiesPlugins = new LinkedHashMap<String, ConnectorPropertiesPlugin>();
    private Map<String, ConnectorSettingsPanel> connectors = new TreeMap<String, ConnectorSettingsPanel>();
    private Map<String, ConnectorSettingsPanel> sourceConnectors = new TreeMap<String, ConnectorSettingsPanel>();
    private Map<String, ConnectorSettingsPanel> destinationConnectors = new TreeMap<String, ConnectorSettingsPanel>();
    private static LoadedExtensions instance = null;

    private LoadedExtensions() {
        // private
    }

    public static LoadedExtensions getInstance() {
        synchronized (LoadedExtensions.class) {
            if (instance == null) {
                instance = new LoadedExtensions();
            }

            return instance;
        }
    }

    public void initialize() {
        // Remove all existing extensions from the maps in case they are being
        // initialized again
        clearExtensionMaps();

        // Order all the plugins by their weight before loading any of them.
        Map<String, String> pluginNameMap = new HashMap<String, String>();
        NavigableMap<Integer, List<String>> weightedPlugins = new TreeMap<Integer, List<String>>();
        for (PluginMetaData metaData : PlatformUI.MIRTH_FRAME.getPluginMetaData().values()) {
            try {
                if (PlatformUI.MIRTH_FRAME.mirthClient.isExtensionEnabled(metaData.getName()) && (metaData.getClientClasses() != null)) {
                    for (PluginClass pluginClass : metaData.getClientClasses()) {
                        String clazzName = pluginClass.getName();
                        int weight = pluginClass.getWeight();
                        pluginNameMap.put(clazzName, metaData.getName());

                        List<String> classList = weightedPlugins.get(weight);
                        if (classList == null) {
                            classList = new ArrayList<String>();
                            weightedPlugins.put(weight, classList);
                        }

                        classList.add(clazzName);
                    }
                }
            } catch (ClientException e) {
                PlatformUI.MIRTH_FRAME.alertException(PlatformUI.MIRTH_FRAME, e.getStackTrace(), e.getMessage());
            }
        }

        // Load the plugins in order of their weight
        for (List<String> classList : weightedPlugins.descendingMap().values()) {
            for (String clazzName : classList) {
                try {
                    String pluginName = pluginNameMap.get(clazzName);
                    Class<?> clazz = Class.forName(clazzName);
                    Constructor<?>[] constructors = clazz.getDeclaredConstructors();

                    for (int i = 0; i < constructors.length; i++) {
                        Class<?> parameters[];
                        parameters = constructors[i].getParameterTypes();
                        // load plugin if the number of parameters
                        // in the constructor is 1.
                        if (parameters.length == 1) {
                            ClientPlugin clientPlugin = (ClientPlugin) constructors[i].newInstance(new Object[] { pluginName });
                            addPluginPoints(clientPlugin);
                            i = constructors.length;
                        }
                    }
                } catch (Exception e) {
                    PlatformUI.MIRTH_FRAME.alertException(PlatformUI.MIRTH_FRAME, e.getStackTrace(), e.getMessage());
                }
            }
        }

        for (ConnectorMetaData metaData : PlatformUI.MIRTH_FRAME.getConnectorMetaData().values()) {
            try {
                if (PlatformUI.MIRTH_FRAME.mirthClient.isExtensionEnabled(metaData.getName())) {

                    String connectorName = metaData.getName();
                    ConnectorSettingsPanel connectorSettingsPanel = (ConnectorSettingsPanel) Class.forName(metaData.getClientClassName()).newInstance();

                    if (metaData.getType() == ConnectorMetaData.Type.SOURCE) {
                        connectors.put(connectorName, connectorSettingsPanel);
                        sourceConnectors.put(connectorName, connectorSettingsPanel);
                    } else if (metaData.getType() == ConnectorMetaData.Type.DESTINATION) {
                        connectors.put(connectorName, connectorSettingsPanel);
                        destinationConnectors.put(connectorName, connectorSettingsPanel);
                    } else {
                        // type must be SOURCE or DESTINATION
                        throw new Exception();
                    }
                }
            } catch (Exception e) {
                PlatformUI.MIRTH_FRAME.alertException(PlatformUI.MIRTH_FRAME, e.getStackTrace(), "Could not load connector class: " + metaData.getClientClassName());
            }
        }
    }

    public void startPlugins() {
        for (ClientPlugin clientPlugin : clientPlugins) {
            clientPlugin.start();
        }
    }

    public void stopPlugins() {
        for (ClientPlugin clientPlugin : clientPlugins) {
            clientPlugin.stop();
        }
    }

    public void resetPlugins() {
        for (ClientPlugin clientPlugin : clientPlugins) {
            clientPlugin.reset();
        }
    }

    /**
     * Add all plugin points in the given ClientPlugin class. A single class could implement
     * multiple plugin points.
     * 
     * @param plugin
     */
    private void addPluginPoints(ClientPlugin plugin) {
        clientPlugins.add(plugin);

        if (plugin instanceof SettingsPanelPlugin) {
            settingsPanelPlugins.put(plugin.getPluginPointName(), (SettingsPanelPlugin) plugin);
        }

        if (plugin instanceof DashboardPanelPlugin) {
            dashboardPanelPlugins.put(plugin.getPluginPointName(), (DashboardPanelPlugin) plugin);
        }

        if (plugin instanceof ChannelPanelPlugin) {
            channelPanelPlugins.put(plugin.getPluginPointName(), (ChannelPanelPlugin) plugin);
        }

        if (plugin instanceof ChannelWizardPlugin) {
            channelWizardPlugins.put(plugin.getPluginPointName(), (ChannelWizardPlugin) plugin);
        }

        if (plugin instanceof DashboardColumnPlugin) {
            dashboardColumnPlugins.put(plugin.getPluginPointName(), (DashboardColumnPlugin) plugin);
        }

        if (plugin instanceof ChannelColumnPlugin) {
            channelColumnPlugins.put(plugin.getPluginPointName(), (ChannelColumnPlugin) plugin);
        }

        if (plugin instanceof AttachmentViewer) {
            attachmentViewerPlugins.put(plugin.getPluginPointName(), (AttachmentViewer) plugin);
        }

        if (plugin instanceof FilterRulePlugin) {
            filterRulePlugins.put(plugin.getPluginPointName(), (FilterRulePlugin) plugin);
        }

        if (plugin instanceof TransformerStepPlugin) {
            transformerStepPlugins.put(plugin.getPluginPointName(), (TransformerStepPlugin) plugin);
        }

        if (plugin instanceof CodeTemplatePlugin) {
            codeTemplatePlugins.put(plugin.getPluginPointName(), (CodeTemplatePlugin) plugin);
        }

        if (plugin instanceof DataTypeClientPlugin) {
            dataTypePlugins.put(plugin.getPluginPointName(), (DataTypeClientPlugin) plugin);
        }

        if (plugin instanceof TransmissionModePlugin) {
            transmissionModePlugins.put(plugin.getPluginPointName(), (TransmissionModePlugin) plugin);
        }

        if (plugin instanceof ConnectorPropertiesPlugin) {
            connectorPropertiesPlugins.put(plugin.getPluginPointName(), (ConnectorPropertiesPlugin) plugin);
        }
    }

    private void clearExtensionMaps() {
        clientPlugins.clear();

        settingsPanelPlugins.clear();
        dashboardPanelPlugins.clear();
        channelPanelPlugins.clear();
        channelWizardPlugins.clear();
        dashboardColumnPlugins.clear();
        channelColumnPlugins.clear();
        attachmentViewerPlugins.clear();
        filterRulePlugins.clear();
        transformerStepPlugins.clear();
        codeTemplatePlugins.clear();
        dataTypePlugins.clear();
        transmissionModePlugins.clear();
        connectorPropertiesPlugins.clear();

        connectors.clear();
        sourceConnectors.clear();
        destinationConnectors.clear();
    }

    public List<ClientPlugin> getClientPlugins() {
        return clientPlugins;
    }

    public Map<String, SettingsPanelPlugin> getSettingsPanelPlugins() {
        return settingsPanelPlugins;
    }

    public Map<String, DashboardPanelPlugin> getDashboardPanelPlugins() {
        return dashboardPanelPlugins;
    }

    public Map<String, ChannelPanelPlugin> getChannelPanelPlugins() {
        return channelPanelPlugins;
    }

    public Map<String, ChannelWizardPlugin> getChannelWizardPlugins() {
        return channelWizardPlugins;
    }

    public Map<String, DashboardColumnPlugin> getDashboardColumnPlugins() {
        return dashboardColumnPlugins;
    }

    public Map<String, ChannelColumnPlugin> getChannelColumnPlugins() {
        return channelColumnPlugins;
    }

    public Map<String, AttachmentViewer> getAttachmentViewerPlugins() {
        return attachmentViewerPlugins;
    }

    public Map<String, FilterRulePlugin> getFilterRulePlugins() {
        return filterRulePlugins;
    }

    public Map<String, TransformerStepPlugin> getTransformerStepPlugins() {
        return transformerStepPlugins;
    }

    public Map<String, CodeTemplatePlugin> getCodeTemplatePlugins() {
        return codeTemplatePlugins;
    }

    public Map<String, DataTypeClientPlugin> getDataTypePlugins() {
        return dataTypePlugins;
    }

    public Map<String, TransmissionModePlugin> getTransmissionModePlugins() {
        return transmissionModePlugins;
    }

    public Map<String, ConnectorPropertiesPlugin> getConnectorPropertiesPlugins() {
        return connectorPropertiesPlugins;
    }

    public Map<String, ConnectorSettingsPanel> getConnectors() {
        return connectors;
    }

    public Map<String, ConnectorSettingsPanel> getSourceConnectors() {
        return sourceConnectors;
    }

    public Map<String, ConnectorSettingsPanel> getDestinationConnectors() {
        return destinationConnectors;
    }
}

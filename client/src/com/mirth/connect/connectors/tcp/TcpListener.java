/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * 
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL license a copy of which has
 * been included with this distribution in the LICENSE.txt file.
 */

package com.mirth.connect.connectors.tcp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.LoadedExtensions;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthFieldConstraints;
import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.mirth.connect.plugins.BasicModeClientProvider;
import com.mirth.connect.plugins.TransmissionModeClientProvider;
import com.mirth.connect.plugins.TransmissionModePlugin;

public class TcpListener extends ConnectorSettingsPanel implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass());
    private Frame parent;
    private TransmissionModeClientProvider defaultProvider;
    private TransmissionModeClientProvider transmissionModeProvider;
    private JComponent settingsPlaceHolder;
    private String selectedMode;
    private boolean modeLock = false;

    /** Creates new form TcpListener */
    public TcpListener() {
        this.parent = PlatformUI.MIRTH_FRAME;
        initComponents();
        reconnectIntervalField.setDocument(new MirthFieldConstraints(0, false, false, true));
        receiveTimeoutField.setDocument(new MirthFieldConstraints(0, false, false, true));
        bufferSizeField.setDocument(new MirthFieldConstraints(0, false, false, true));
        maxConnectionsField.setDocument(new MirthFieldConstraints(0, false, false, true));

        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.addElement("Basic TCP");
        selectedMode = "Basic TCP";

        for (String pluginPointName : LoadedExtensions.getInstance().getTransmissionModePlugins().keySet()) {
            model.addElement(pluginPointName);
            if (pluginPointName.equals("MLLP")) {
                defaultProvider = LoadedExtensions.getInstance().getTransmissionModePlugins().get(pluginPointName).createProvider();
            }
        }

        transmissionModeComboBox.setModel(model);

        settingsPlaceHolder = settingsPlaceHolderLabel;

        parent.setupCharsetEncodingForConnector(charsetEncodingCombobox);
    }

    @Override
    public String getConnectorName() {
        return new TcpReceiverProperties().getName();
    }

    @Override
    public ConnectorProperties getProperties() {
        TcpReceiverProperties properties = new TcpReceiverProperties();

        if (transmissionModeProvider != null) {
            properties.setTransmissionModeProperties((TransmissionModeProperties) transmissionModeProvider.getProperties());
        }
        properties.setServerMode(modeServerRadio.isSelected());
        properties.setRemoteAddress(remoteAddressField.getText());
        properties.setRemotePort(remotePortField.getText());
        properties.setOverrideLocalBinding(overrideLocalBindingYesRadio.isSelected());
        properties.setReconnectInterval(reconnectIntervalField.getText());
        properties.setReceiveTimeout(receiveTimeoutField.getText());
        properties.setBufferSize(bufferSizeField.getText());
        properties.setMaxConnections(maxConnectionsField.getText());
        properties.setKeepConnectionOpen(keepConnectionOpenYesRadio.isSelected());
        properties.setProcessBatch(processBatchYesRadio.isSelected());
        properties.setCharsetEncoding(parent.getSelectedEncodingForConnector(charsetEncodingCombobox));
        properties.setDataTypeBinary(dataTypeBinaryRadio.isSelected());

        if (respondOnNewConnectionYesRadio.isSelected()) {
            properties.setRespondOnNewConnection(TcpReceiverProperties.NEW_CONNECTION);
        } else if (respondOnNewConnectionNoRadio.isSelected()) {
            properties.setRespondOnNewConnection(TcpReceiverProperties.SAME_CONNECTION);
        } else if (respondOnNewConnectionRecoveryRadio.isSelected()) {
            properties.setRespondOnNewConnection(TcpReceiverProperties.NEW_CONNECTION_ON_RECOVERY);
        }
        properties.setResponseAddress(responseAddressField.getText());
        properties.setResponsePort(responsePortField.getText());

        return properties;
    }

    @Override
    public void setProperties(ConnectorProperties properties) {
        TcpReceiverProperties props = (TcpReceiverProperties) properties;

        TransmissionModeProperties modeProps = props.getTransmissionModeProperties();
        String name = "Basic TCP";
        if (modeProps != null && LoadedExtensions.getInstance().getTransmissionModePlugins().containsKey(modeProps.getPluginPointName())) {
            name = modeProps.getPluginPointName();
        }

        modeLock = true;
        transmissionModeComboBox.setSelectedItem(name);
        transmissionModeComboBoxActionPerformed(null);
        modeLock = false;
        selectedMode = name;
        if (transmissionModeProvider != null) {
            transmissionModeProvider.setProperties(modeProps);
        }

        if (props.isServerMode()) {
            modeServerRadio.setSelected(true);
            modeServerRadioActionPerformed(null);
        } else {
            modeClientRadio.setSelected(true);
            modeClientRadioActionPerformed(null);
        }

        remoteAddressField.setText(props.getRemoteAddress());
        remotePortField.setText(props.getRemotePort());

        if (props.isOverrideLocalBinding()) {
            overrideLocalBindingYesRadio.setSelected(true);
        } else {
            overrideLocalBindingNoRadio.setSelected(true);
        }

        reconnectIntervalField.setText(props.getReconnectInterval());
        receiveTimeoutField.setText(props.getReceiveTimeout());
        bufferSizeField.setText(props.getBufferSize());
        maxConnectionsField.setText(props.getMaxConnections());

        if (props.isKeepConnectionOpen()) {
            keepConnectionOpenYesRadio.setSelected(true);
        } else {
            keepConnectionOpenNoRadio.setSelected(true);
        }

        if (props.isProcessBatch()) {
            processBatchYesRadio.setSelected(true);
        } else {
            processBatchNoRadio.setSelected(true);
        }

        if (props.isDataTypeBinary()) {
            dataTypeBinaryRadio.setSelected(true);
            dataTypeBinaryRadioActionPerformed(null);
        } else {
            dataTypeASCIIRadio.setSelected(true);
            dataTypeASCIIRadioActionPerformed(null);
        }

        parent.setPreviousSelectedEncodingForConnector(charsetEncodingCombobox, props.getCharsetEncoding());

        switch (props.getRespondOnNewConnection()) {
            case TcpReceiverProperties.NEW_CONNECTION:
                respondOnNewConnectionYesRadio.setSelected(true);
                respondOnNewConnectionYesRadioActionPerformed(null);
                break;
            case TcpReceiverProperties.SAME_CONNECTION:
                respondOnNewConnectionNoRadio.setSelected(true);
                respondOnNewConnectionNoRadioActionPerformed(null);
                break;
            case TcpReceiverProperties.NEW_CONNECTION_ON_RECOVERY:
                respondOnNewConnectionRecoveryRadio.setSelected(true);
                respondOnNewConnectionRecoveryRadioActionPerformed(null);
                break;
        }

        responseAddressField.setText(props.getResponseAddress());
        responsePortField.setText(props.getResponsePort());
    }

    @Override
    public ConnectorProperties getDefaults() {
        TcpReceiverProperties props = new TcpReceiverProperties();
        if (defaultProvider != null) {
            props.setTransmissionModeProperties(defaultProvider.getDefaultProperties());
        }
        return props;
    }

    @Override
    public boolean checkProperties(ConnectorProperties properties, boolean highlight) {
        TcpReceiverProperties props = (TcpReceiverProperties) properties;

        boolean valid = true;

        if (transmissionModeProvider != null) {
            if (!transmissionModeProvider.checkProperties(transmissionModeProvider.getProperties(), highlight)) {
                valid = false;
            }
        }
        if (!props.isServerMode()) {
            if (props.getRemoteAddress().length() == 0) {
                valid = false;
                if (highlight) {
                    remoteAddressField.setBackground(UIConstants.INVALID_COLOR);
                }
            }

            if (props.getRemotePort().length() == 0) {
                valid = false;
                if (highlight) {
                    remotePortField.setBackground(UIConstants.INVALID_COLOR);
                }
            }

            if (props.getReconnectInterval().length() == 0) {
                valid = false;
                if (highlight) {
                    reconnectIntervalField.setBackground(UIConstants.INVALID_COLOR);
                }
            }
        }
        if (props.getReceiveTimeout().length() == 0) {
            valid = false;
            if (highlight) {
                receiveTimeoutField.setBackground(UIConstants.INVALID_COLOR);
            }
        }
        if (props.getBufferSize().length() == 0) {
            valid = false;
            if (highlight) {
                bufferSizeField.setBackground(UIConstants.INVALID_COLOR);
            }
        }
        if (props.getMaxConnections().length() == 0 || NumberUtils.toInt(props.getMaxConnections()) <= 0) {
            valid = false;
            if (highlight) {
                maxConnectionsField.setBackground(UIConstants.INVALID_COLOR);
            }
        }
        if (props.getRespondOnNewConnection() == TcpReceiverProperties.NEW_CONNECTION || props.getRespondOnNewConnection() == TcpReceiverProperties.NEW_CONNECTION_ON_RECOVERY) {
            if (props.getResponseAddress().length() <= 3) {
                valid = false;
                if (highlight) {
                    responseAddressField.setBackground(UIConstants.INVALID_COLOR);
                }
            }
            if (props.getResponsePort().length() == 0) {
                valid = false;
                if (highlight) {
                    responsePortField.setBackground(UIConstants.INVALID_COLOR);
                }
            }
        }

        return valid;
    }

    @Override
    public void resetInvalidProperties() {
        if (transmissionModeProvider != null) {
            transmissionModeProvider.resetInvalidProperties();
        }
        remoteAddressField.setBackground(null);
        remotePortField.setBackground(null);
        reconnectIntervalField.setBackground(null);
        receiveTimeoutField.setBackground(null);
        bufferSizeField.setBackground(null);
        maxConnectionsField.setBackground(null);
        responseAddressField.setBackground(null);
        responsePortField.setBackground(null);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource().equals(transmissionModeProvider)) {
            if (evt.getActionCommand().equals(TransmissionModeClientProvider.CHANGE_SAMPLE_LABEL_COMMAND)) {
                sampleLabel.setText(transmissionModeProvider.getSampleLabel());
            } else if (evt.getActionCommand().equals(TransmissionModeClientProvider.CHANGE_SAMPLE_VALUE_COMMAND)) {
                sampleValue.setText(transmissionModeProvider.getSampleValue());
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        keepConnectionOpenGroup = new javax.swing.ButtonGroup();
        dataTypeButtonGroup = new javax.swing.ButtonGroup();
        respondOnNewConnectionButtonGroup = new javax.swing.ButtonGroup();
        modeButtonGroup = new javax.swing.ButtonGroup();
        processBatchButtonGroup = new javax.swing.ButtonGroup();
        overrideLocalBindingButtonGroup = new javax.swing.ButtonGroup();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        bufferSizeField = new com.mirth.connect.client.ui.components.MirthTextField();
        receiveTimeoutField = new com.mirth.connect.client.ui.components.MirthTextField();
        charsetEncodingCombobox = new com.mirth.connect.client.ui.components.MirthComboBox();
        encodingLabel = new javax.swing.JLabel();
        ackOnNewConnectionLabel = new javax.swing.JLabel();
        respondOnNewConnectionYesRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        respondOnNewConnectionNoRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        respondOnNewConnectionRecoveryRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        ackIPLabel = new javax.swing.JLabel();
        ackPortLabel = new javax.swing.JLabel();
        responsePortField = new com.mirth.connect.client.ui.components.MirthTextField();
        responseAddressField = new com.mirth.connect.client.ui.components.MirthTextField();
        dataTypeASCIIRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        dataTypeBinaryRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        dataTypeLabel = new javax.swing.JLabel();
        keepConnectionOpenLabel = new javax.swing.JLabel();
        keepConnectionOpenYesRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        keepConnectionOpenNoRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        jLabel5 = new javax.swing.JLabel();
        modeServerRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        modeClientRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        reconnectIntervalLabel = new javax.swing.JLabel();
        reconnectIntervalField = new com.mirth.connect.client.ui.components.MirthTextField();
        maxConnectionsLabel = new javax.swing.JLabel();
        maxConnectionsField = new com.mirth.connect.client.ui.components.MirthTextField();
        processBatchLabel = new javax.swing.JLabel();
        processBatchYesRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        processBatchNoRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        transmissionModeComboBox = new com.mirth.connect.client.ui.components.MirthComboBox();
        transmissionModeLabel = new javax.swing.JLabel();
        sampleLabel = new javax.swing.JLabel();
        sampleValue = new javax.swing.JLabel();
        settingsPlaceHolderLabel = new javax.swing.JLabel();
        remoteAddressLabel = new javax.swing.JLabel();
        remoteAddressField = new com.mirth.connect.client.ui.components.MirthTextField();
        remotePortLabel = new javax.swing.JLabel();
        remotePortField = new com.mirth.connect.client.ui.components.MirthTextField();
        overrideLocalBindingLabel = new javax.swing.JLabel();
        overrideLocalBindingYesRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        overrideLocalBindingNoRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();

        setBackground(new java.awt.Color(255, 255, 255));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        jLabel3.setText("Receive Timeout (ms):");

        jLabel4.setText("Buffer Size (bytes):");

        bufferSizeField.setToolTipText("<html>Use larger values for larger messages, and smaller values <br>for smaller messages. Generally, the default value is fine.</html>");

        receiveTimeoutField.setToolTipText("The amount of time, in milliseconds, to wait without receiving a message before closing a connection.");

        charsetEncodingCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Default", "utf-8", "iso-8859-1", "utf-16 (le)", "utf-16 (be)", "utf-16 (bom)", "us-ascii" }));
        charsetEncodingCombobox.setToolTipText("<html>Select the character set encoding used by the message sender,<br/>or Select Default to use the default character set encoding for the JVM running Mirth.</html>");

        encodingLabel.setText("Encoding:");

        ackOnNewConnectionLabel.setText("Respond on New Connection:");

        respondOnNewConnectionYesRadio.setBackground(new java.awt.Color(255, 255, 255));
        respondOnNewConnectionYesRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        respondOnNewConnectionButtonGroup.add(respondOnNewConnectionYesRadio);
        respondOnNewConnectionYesRadio.setText("Yes");
        respondOnNewConnectionYesRadio.setToolTipText("<html>Select No to send responses only via the same connection the inbound message was received on.<br/>Select Yes to always send responses on a new connection (during normal processing as well as recovery).<br/>Select Message Recovery to only send responses on a new connection during message recovery.<br/>Connections will be bound locally on the same interface chosen in the Listener Settings with an ephemeral port.</html>");
        respondOnNewConnectionYesRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        respondOnNewConnectionYesRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                respondOnNewConnectionYesRadioActionPerformed(evt);
            }
        });

        respondOnNewConnectionNoRadio.setBackground(new java.awt.Color(255, 255, 255));
        respondOnNewConnectionNoRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        respondOnNewConnectionButtonGroup.add(respondOnNewConnectionNoRadio);
        respondOnNewConnectionNoRadio.setText("No");
        respondOnNewConnectionNoRadio.setToolTipText("<html>Select No to send responses only via the same connection the inbound message was received on.<br/>Select Yes to always send responses on a new connection (during normal processing as well as recovery).<br/>Select Message Recovery to only send responses on a new connection during message recovery.<br/>Connections will be bound locally on the same interface chosen in the Listener Settings with an ephemeral port.</html>");
        respondOnNewConnectionNoRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        respondOnNewConnectionNoRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                respondOnNewConnectionNoRadioActionPerformed(evt);
            }
        });

        respondOnNewConnectionRecoveryRadio.setBackground(new java.awt.Color(255, 255, 255));
        respondOnNewConnectionRecoveryRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        respondOnNewConnectionButtonGroup.add(respondOnNewConnectionRecoveryRadio);
        respondOnNewConnectionRecoveryRadio.setText("Message Recovery");
        respondOnNewConnectionRecoveryRadio.setToolTipText("<html>Select No to send responses only via the same connection the inbound message was received on.<br/>Select Yes to always send responses on a new connection (during normal processing as well as recovery).<br/>Select Message Recovery to only send responses on a new connection during message recovery.<br/>Connections will be bound locally on the same interface chosen in the Listener Settings with an ephemeral port.</html>");
        respondOnNewConnectionRecoveryRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        respondOnNewConnectionRecoveryRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                respondOnNewConnectionRecoveryRadioActionPerformed(evt);
            }
        });

        ackIPLabel.setText("Response Address:");

        ackPortLabel.setText("Response Port:");

        responsePortField.setToolTipText("<html>Enter the port to send message responses to.</html>");

        responseAddressField.setToolTipText("<html>Enter the DNS domain name or IP address to send message responses to.</html>");

        dataTypeASCIIRadio.setBackground(new java.awt.Color(255, 255, 255));
        dataTypeASCIIRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        dataTypeButtonGroup.add(dataTypeASCIIRadio);
        dataTypeASCIIRadio.setSelected(true);
        dataTypeASCIIRadio.setText("Text");
        dataTypeASCIIRadio.setToolTipText("<html>Select Binary if the inbound messages are raw byte streams; the payload will be Base64 encoded.<br>Select Text if the inbound messages are text streams; the payload will be encoded with the specified character set encoding.</html>");
        dataTypeASCIIRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        dataTypeASCIIRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dataTypeASCIIRadioActionPerformed(evt);
            }
        });

        dataTypeBinaryRadio.setBackground(new java.awt.Color(255, 255, 255));
        dataTypeBinaryRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        dataTypeButtonGroup.add(dataTypeBinaryRadio);
        dataTypeBinaryRadio.setText("Binary");
        dataTypeBinaryRadio.setToolTipText("<html>Select Binary if the inbound messages are raw byte streams; the payload will be Base64 encoded.<br>Select Text if the inbound messages are text streams; the payload will be encoded with the specified character set encoding.</html>");
        dataTypeBinaryRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        dataTypeBinaryRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dataTypeBinaryRadioActionPerformed(evt);
            }
        });

        dataTypeLabel.setText("Data Type:");

        keepConnectionOpenLabel.setText("Keep Connection Open:");

        keepConnectionOpenYesRadio.setBackground(new java.awt.Color(255, 255, 255));
        keepConnectionOpenYesRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        keepConnectionOpenGroup.add(keepConnectionOpenYesRadio);
        keepConnectionOpenYesRadio.setText("Yes");
        keepConnectionOpenYesRadio.setToolTipText("<html>Select No to close the listening socket after a received message has finished processing.<br/>Otherwise the socket will remain open until the sending system closes it. In that case,<br/>messages will only be processed if data is received and either the receive timeout is reached,<br/>the client closes the socket, or an end of message byte sequence has been detected.</html>");
        keepConnectionOpenYesRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));

        keepConnectionOpenNoRadio.setBackground(new java.awt.Color(255, 255, 255));
        keepConnectionOpenNoRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        keepConnectionOpenGroup.add(keepConnectionOpenNoRadio);
        keepConnectionOpenNoRadio.setSelected(true);
        keepConnectionOpenNoRadio.setText("No");
        keepConnectionOpenNoRadio.setToolTipText("<html>Select No to close the listening socket after a received message has finished processing.<br/>Otherwise the socket will remain open until the sending system closes it. In that case,<br/>messages will only be processed if data is received and either the receive timeout is reached,<br/>the client closes the socket, or an end of message byte sequence has been detected.</html>");
        keepConnectionOpenNoRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));

        jLabel5.setText("Mode:");

        modeServerRadio.setBackground(new java.awt.Color(255, 255, 255));
        modeServerRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        modeButtonGroup.add(modeServerRadio);
        modeServerRadio.setText("Server");
        modeServerRadio.setToolTipText("<html>Select Server to listen for connections from clients, or Client to connect to a TCP Server.<br/>In Client mode, the listener settings will only be used if Override Local Binding is enabled.</html>");
        modeServerRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        modeServerRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modeServerRadioActionPerformed(evt);
            }
        });

        modeClientRadio.setBackground(new java.awt.Color(255, 255, 255));
        modeClientRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        modeButtonGroup.add(modeClientRadio);
        modeClientRadio.setText("Client");
        modeClientRadio.setToolTipText("<html>Select Server to listen for connections from clients, or Client to connect to a TCP Server.<br/>In Client mode, the listener settings will only be used if Override Local Binding is enabled.</html>");
        modeClientRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        modeClientRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modeClientRadioActionPerformed(evt);
            }
        });

        reconnectIntervalLabel.setText("Reconnect Interval (ms):");

        reconnectIntervalField.setToolTipText("<html>If Client mode is selected, enter the time (in milliseconds) to wait<br/>between disconnecting from the TCP server and connecting to it again.</html>");

        maxConnectionsLabel.setText("Max Connections:");

        maxConnectionsField.setToolTipText("<html>The maximum number of client connections to accept.<br/>After this number has been reached, subsequent socket requests will result in a rejection.</html>");

        processBatchLabel.setText("Process Batch:");

        processBatchYesRadio.setBackground(new java.awt.Color(255, 255, 255));
        processBatchYesRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        processBatchButtonGroup.add(processBatchYesRadio);
        processBatchYesRadio.setText("Yes");
        processBatchYesRadio.setToolTipText("<html>Select Yes to allow Mirth to automatically split batched HL7 v2.x messages into discrete messages.<br/>This can be used with batch files containing FHS/BHS/BTS/FTS segments, or it can be used on batch files that just have multiple MSH segments.<br/>The location of each MSH segment signifies the start of a new message to be processed.</html>");
        processBatchYesRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));

        processBatchNoRadio.setBackground(new java.awt.Color(255, 255, 255));
        processBatchNoRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        processBatchButtonGroup.add(processBatchNoRadio);
        processBatchNoRadio.setSelected(true);
        processBatchNoRadio.setText("No");
        processBatchNoRadio.setToolTipText("<html>Select Yes to allow Mirth to automatically split batched HL7 v2.x messages into discrete messages.<br/>This can be used with batch files containing FHS/BHS/BTS/FTS segments, or it can be used on batch files that just have multiple MSH segments.<br/>The location of each MSH segment signifies the start of a new message to be processed.</html>");
        processBatchNoRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        processBatchNoRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                processBatchNoRadioActionPerformed(evt);
            }
        });

        transmissionModeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "MLLP Frame Encoded", "Custom Frame Encoded" }));
        transmissionModeComboBox.setToolTipText("<html>Select the transmission mode to use for sending and receiving data.<br/></html>");
        transmissionModeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transmissionModeComboBoxActionPerformed(evt);
            }
        });

        transmissionModeLabel.setText("Transmission Mode:");
        transmissionModeLabel.setToolTipText("<html>Select the transmission mode to use for sending and receiving data.<br/></html>");

        sampleLabel.setText("Sample Frame:");

        sampleValue.setForeground(new java.awt.Color(153, 153, 153));
        sampleValue.setText("<html><b>&lt;VT&gt;</b> <i>&lt;Message Data&gt;</i> <b>&lt;FS&gt;&lt;CR&gt;</b></html>");
        sampleValue.setEnabled(false);

        settingsPlaceHolderLabel.setText("     ");

        remoteAddressLabel.setText("Remote Address:");

        remoteAddressField.setToolTipText("<html>The DNS domain name or IP address on which to connect.</html>");

        remotePortLabel.setText("Remote Port:");

        remotePortField.setToolTipText("<html>The port on which to connect.</html>");

        overrideLocalBindingLabel.setText("Override Local Binding:");

        overrideLocalBindingYesRadio.setBackground(new java.awt.Color(255, 255, 255));
        overrideLocalBindingYesRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        overrideLocalBindingButtonGroup.add(overrideLocalBindingYesRadio);
        overrideLocalBindingYesRadio.setText("Yes");
        overrideLocalBindingYesRadio.setToolTipText("<html>Select Yes to override the local address and port that the client socket will be bound to.<br/>Select No to use the default values of 0.0.0.0:0.<br/>A local port of zero (0) indicates that the OS should assign an ephemeral port automatically.<br/><br/>Note that if a specific (non-zero) local port is chosen, then after a socket is closed it's up to the<br/>underlying OS to release the port before the next socket creation, otherwise the bind attempt will fail.<br/></html>");
        overrideLocalBindingYesRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));

        overrideLocalBindingNoRadio.setBackground(new java.awt.Color(255, 255, 255));
        overrideLocalBindingNoRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        overrideLocalBindingButtonGroup.add(overrideLocalBindingNoRadio);
        overrideLocalBindingNoRadio.setSelected(true);
        overrideLocalBindingNoRadio.setText("No");
        overrideLocalBindingNoRadio.setToolTipText("<html>Select Yes to override the local address and port that the client socket will be bound to.<br/>Select No to use the default values of 0.0.0.0:0.<br/>A local port of zero (0) indicates that the OS should assign an ephemeral port automatically.<br/><br/>Note that if a specific (non-zero) local port is chosen, then after a socket is closed it's up to the<br/>underlying OS to release the port before the next socket creation, otherwise the bind attempt will fail.<br/></html>");
        overrideLocalBindingNoRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(sampleLabel)
                    .addComponent(jLabel5)
                    .addComponent(transmissionModeLabel)
                    .addComponent(remoteAddressLabel)
                    .addComponent(remotePortLabel)
                    .addComponent(overrideLocalBindingLabel)
                    .addComponent(reconnectIntervalLabel)
                    .addComponent(maxConnectionsLabel)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(keepConnectionOpenLabel)
                    .addComponent(processBatchLabel)
                    .addComponent(dataTypeLabel)
                    .addComponent(encodingLabel)
                    .addComponent(ackOnNewConnectionLabel)
                    .addComponent(ackIPLabel)
                    .addComponent(ackPortLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleValue)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(transmissionModeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(settingsPlaceHolderLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 70, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(modeServerRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(modeClientRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(remoteAddressField, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(remotePortField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(overrideLocalBindingYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(overrideLocalBindingNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(reconnectIntervalField, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(maxConnectionsField, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(receiveTimeoutField, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(bufferSizeField, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(keepConnectionOpenYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(keepConnectionOpenNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(processBatchYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(processBatchNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(dataTypeBinaryRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(dataTypeASCIIRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(charsetEncodingCombobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(respondOnNewConnectionYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(respondOnNewConnectionNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(respondOnNewConnectionRecoveryRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(responseAddressField, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(responsePortField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(transmissionModeLabel)
                    .addComponent(transmissionModeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(settingsPlaceHolderLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sampleLabel)
                    .addComponent(sampleValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(modeServerRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(modeClientRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(remoteAddressLabel)
                    .addComponent(remoteAddressField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(remotePortLabel)
                    .addComponent(remotePortField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(overrideLocalBindingLabel)
                    .addComponent(overrideLocalBindingYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(overrideLocalBindingNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(reconnectIntervalLabel)
                    .addComponent(reconnectIntervalField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(maxConnectionsLabel)
                    .addComponent(maxConnectionsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(receiveTimeoutField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(bufferSizeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(keepConnectionOpenLabel)
                    .addComponent(keepConnectionOpenYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(keepConnectionOpenNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(processBatchLabel)
                    .addComponent(processBatchYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(processBatchNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dataTypeLabel)
                    .addComponent(dataTypeBinaryRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dataTypeASCIIRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(encodingLabel)
                    .addComponent(charsetEncodingCombobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ackOnNewConnectionLabel)
                    .addComponent(respondOnNewConnectionYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(respondOnNewConnectionNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(respondOnNewConnectionRecoveryRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ackIPLabel)
                    .addComponent(responseAddressField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ackPortLabel)
                    .addComponent(responsePortField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {settingsPlaceHolderLabel, transmissionModeComboBox});

    }// </editor-fold>//GEN-END:initComponents

    private void dataTypeBinaryRadioActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_dataTypeBinaryRadioActionPerformed
    {//GEN-HEADEREND:event_dataTypeBinaryRadioActionPerformed
        encodingLabel.setEnabled(false);
        charsetEncodingCombobox.setEnabled(false);
        charsetEncodingCombobox.setSelectedIndex(0);
    }//GEN-LAST:event_dataTypeBinaryRadioActionPerformed

    private void dataTypeASCIIRadioActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_dataTypeASCIIRadioActionPerformed
    {//GEN-HEADEREND:event_dataTypeASCIIRadioActionPerformed
        encodingLabel.setEnabled(true);
        charsetEncodingCombobox.setEnabled(true);
    }//GEN-LAST:event_dataTypeASCIIRadioActionPerformed

    private void modeClientRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modeClientRadioActionPerformed
        remoteAddressLabel.setEnabled(true);
        remoteAddressField.setEnabled(true);
        remotePortLabel.setEnabled(true);
        remotePortField.setEnabled(true);
        overrideLocalBindingLabel.setEnabled(true);
        overrideLocalBindingYesRadio.setEnabled(true);
        overrideLocalBindingNoRadio.setEnabled(true);
        reconnectIntervalLabel.setEnabled(true);
        reconnectIntervalField.setEnabled(true);
        maxConnectionsLabel.setEnabled(false);
        maxConnectionsField.setEnabled(false);
    }//GEN-LAST:event_modeClientRadioActionPerformed

    private void processBatchNoRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_processBatchNoRadioActionPerformed
    }//GEN-LAST:event_processBatchNoRadioActionPerformed

    private void modeServerRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modeServerRadioActionPerformed
        remoteAddressLabel.setEnabled(false);
        remoteAddressField.setEnabled(false);
        remotePortLabel.setEnabled(false);
        remotePortField.setEnabled(false);
        overrideLocalBindingLabel.setEnabled(false);
        overrideLocalBindingYesRadio.setEnabled(false);
        overrideLocalBindingNoRadio.setEnabled(false);
        reconnectIntervalLabel.setEnabled(false);
        reconnectIntervalField.setEnabled(false);
        maxConnectionsLabel.setEnabled(true);
        maxConnectionsField.setEnabled(true);
    }//GEN-LAST:event_modeServerRadioActionPerformed

    private void transmissionModeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transmissionModeComboBoxActionPerformed
        String name = (String) transmissionModeComboBox.getSelectedItem();

        if (!modeLock && transmissionModeProvider != null) {
            if (!transmissionModeProvider.getDefaultProperties().equals(transmissionModeProvider.getProperties())) {
                if (JOptionPane.showConfirmDialog(parent, "Are you sure you would like to change the transmission mode and lose all of the current transmission properties?", "Select an Option", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    modeLock = true;
                    transmissionModeComboBox.setSelectedItem(selectedMode);
                    modeLock = false;
                    return;
                }
            }
        }

        selectedMode = name;
        if (name.equals("Basic TCP")) {
            transmissionModeProvider = new BasicModeClientProvider();
        } else {
            for (TransmissionModePlugin plugin : LoadedExtensions.getInstance().getTransmissionModePlugins().values()) {
                if (plugin.getPluginPointName().equals(name)) {
                    transmissionModeProvider = plugin.createProvider();
                }
            }
        }

        if (transmissionModeProvider != null) {
            transmissionModeProvider.initialize(this);
            ((GroupLayout) getLayout()).replace(settingsPlaceHolder, transmissionModeProvider.getSettingsComponent());
            settingsPlaceHolder = transmissionModeProvider.getSettingsComponent();
        }
    }//GEN-LAST:event_transmissionModeComboBoxActionPerformed

    private void respondOnNewConnectionRecoveryRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_respondOnNewConnectionRecoveryRadioActionPerformed
        responseAddressField.setEnabled(true);
        responsePortField.setEnabled(true);
        ackIPLabel.setEnabled(true);
        ackPortLabel.setEnabled(true);
    }//GEN-LAST:event_respondOnNewConnectionRecoveryRadioActionPerformed

    private void respondOnNewConnectionNoRadioActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_ackOnNewConnectionNoActionPerformed
    {// GEN-HEADEREND:event_ackOnNewConnectionNoActionPerformed
        responseAddressField.setEnabled(false);
        responsePortField.setEnabled(false);
        ackIPLabel.setEnabled(false);
        ackPortLabel.setEnabled(false);
    }// GEN-LAST:event_ackOnNewConnectionNoActionPerformed

    private void respondOnNewConnectionYesRadioActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_ackOnNewConnectionYesActionPerformed
    {// GEN-HEADEREND:event_ackOnNewConnectionYesActionPerformed
        responseAddressField.setEnabled(true);
        responsePortField.setEnabled(true);
        ackIPLabel.setEnabled(true);
        ackPortLabel.setEnabled(true);
    }// GEN-LAST:event_ackOnNewConnectionYesActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel ackIPLabel;
    private javax.swing.JLabel ackOnNewConnectionLabel;
    private javax.swing.JLabel ackPortLabel;
    private com.mirth.connect.client.ui.components.MirthTextField bufferSizeField;
    private com.mirth.connect.client.ui.components.MirthComboBox charsetEncodingCombobox;
    private com.mirth.connect.client.ui.components.MirthRadioButton dataTypeASCIIRadio;
    private com.mirth.connect.client.ui.components.MirthRadioButton dataTypeBinaryRadio;
    private javax.swing.ButtonGroup dataTypeButtonGroup;
    private javax.swing.JLabel dataTypeLabel;
    private javax.swing.JLabel encodingLabel;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.ButtonGroup keepConnectionOpenGroup;
    private javax.swing.JLabel keepConnectionOpenLabel;
    private com.mirth.connect.client.ui.components.MirthRadioButton keepConnectionOpenNoRadio;
    private com.mirth.connect.client.ui.components.MirthRadioButton keepConnectionOpenYesRadio;
    private com.mirth.connect.client.ui.components.MirthTextField maxConnectionsField;
    private javax.swing.JLabel maxConnectionsLabel;
    private javax.swing.ButtonGroup modeButtonGroup;
    private com.mirth.connect.client.ui.components.MirthRadioButton modeClientRadio;
    private com.mirth.connect.client.ui.components.MirthRadioButton modeServerRadio;
    private javax.swing.ButtonGroup overrideLocalBindingButtonGroup;
    private javax.swing.JLabel overrideLocalBindingLabel;
    private com.mirth.connect.client.ui.components.MirthRadioButton overrideLocalBindingNoRadio;
    private com.mirth.connect.client.ui.components.MirthRadioButton overrideLocalBindingYesRadio;
    private javax.swing.ButtonGroup processBatchButtonGroup;
    private javax.swing.JLabel processBatchLabel;
    private com.mirth.connect.client.ui.components.MirthRadioButton processBatchNoRadio;
    private com.mirth.connect.client.ui.components.MirthRadioButton processBatchYesRadio;
    private com.mirth.connect.client.ui.components.MirthTextField receiveTimeoutField;
    private com.mirth.connect.client.ui.components.MirthTextField reconnectIntervalField;
    private javax.swing.JLabel reconnectIntervalLabel;
    private com.mirth.connect.client.ui.components.MirthTextField remoteAddressField;
    private javax.swing.JLabel remoteAddressLabel;
    private com.mirth.connect.client.ui.components.MirthTextField remotePortField;
    private javax.swing.JLabel remotePortLabel;
    private javax.swing.ButtonGroup respondOnNewConnectionButtonGroup;
    private com.mirth.connect.client.ui.components.MirthRadioButton respondOnNewConnectionNoRadio;
    private com.mirth.connect.client.ui.components.MirthRadioButton respondOnNewConnectionRecoveryRadio;
    private com.mirth.connect.client.ui.components.MirthRadioButton respondOnNewConnectionYesRadio;
    private com.mirth.connect.client.ui.components.MirthTextField responseAddressField;
    private com.mirth.connect.client.ui.components.MirthTextField responsePortField;
    private javax.swing.JLabel sampleLabel;
    private javax.swing.JLabel sampleValue;
    private javax.swing.JLabel settingsPlaceHolderLabel;
    private com.mirth.connect.client.ui.components.MirthComboBox transmissionModeComboBox;
    private javax.swing.JLabel transmissionModeLabel;
    // End of variables declaration//GEN-END:variables
}

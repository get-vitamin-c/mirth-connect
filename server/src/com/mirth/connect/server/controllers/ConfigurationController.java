/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * 
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL license a copy of which has
 * been included with this distribution in the LICENSE.txt file.
 */

package com.mirth.connect.server.controllers;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import com.mirth.commons.encryption.Digester;
import com.mirth.commons.encryption.Encryptor;
import com.mirth.connect.donkey.server.StartException;
import com.mirth.connect.donkey.server.StopException;
import com.mirth.connect.model.DatabaseSettings;
import com.mirth.connect.model.DriverInfo;
import com.mirth.connect.model.EncryptionSettings;
import com.mirth.connect.model.PasswordRequirements;
import com.mirth.connect.model.ServerConfiguration;
import com.mirth.connect.model.ServerSettings;
import com.mirth.connect.model.UpdateSettings;
import com.mirth.connect.util.ConfigurationProperty;

/**
 * The ConfigurationController provides access to the Mirth configuration.
 * 
 */
public abstract class ConfigurationController extends Controller {
    // status codes

    public static final int STATUS_OK = 0;
    public static final int STATUS_UNAVAILABLE = 1;
    public static final int STATUS_ENGINE_STARTING = 2;
    public static final int STATUS_INITIAL_DEPLOY = 3;

    public static ConfigurationController getInstance() {
        return ControllerFactory.getFactory().createConfigurationController();
    }

    /**
     * Initializes several items relates to security. Specifically:
     * 
     * <ol>
     * <li>Instantiates the default encryptor and digester</li>
     * <li>Loads or generates the default keystore and certificate</li>
     * <li>Loads or generates the default truststore</li>
     * </ol>
     * 
     */
    public abstract void initializeSecuritySettings();

    /**
     * Initializes the DatabaseSettings from the properties configuration.
     */
    public abstract void initializeDatabaseSettings();

    /**
     * Migrates the encryption key from the database to a new JCEKS keystore. This should only be
     * run once during the migration from pre-2.2 to 2.2.
     */
    public abstract void migrateKeystore();

    /**
     * Returns the default encryptor.
     * 
     * @return the default encryptor
     */
    public abstract Encryptor getEncryptor();

    /**
     * Returns the default digester.
     * 
     * @return the default digester
     */
    public abstract Digester getDigester();

    /**
     * Returns the database type (ex. derby)
     * 
     * @return the database type
     */
    public abstract String getDatabaseType();

    /**
     * Returns the server's unique ID
     * 
     * @return the server's unique ID
     */
    public abstract String getServerId();

    public abstract String getServerTimezone(Locale locale);

    /**
     * Returns all of the charset encodings available on the server.
     * 
     * @return a list of charset encoding names
     * @throws ControllerException
     */
    public abstract List<String> getAvaiableCharsetEncodings() throws ControllerException;

    /**
     * Returns the base directory for the server.
     * 
     * @return the base directory for the server.
     */
    public abstract String getBaseDir();

    /**
     * Returns the conf directory for the server. This is where configuration files and database
     * mapping scripts are stored.
     * 
     * @return the conf directory for the server.
     */
    public abstract String getConfigurationDir();

    /**
     * Returns the app data directory for the server. This is where files generated by the server
     * are stored.
     * 
     * @return the app data directory for the server.
     */
    public abstract String getApplicationDataDir();

    /**
     * Returns all server settings.
     * 
     * @return server settings
     * @throws ControllerException
     */
    public abstract ServerSettings getServerSettings() throws ControllerException;

    /**
     * Returns all encryption settings.
     * 
     * @return encryption settings
     * @throws ControllerException
     */
    public abstract EncryptionSettings getEncryptionSettings() throws ControllerException;

    /**
     * Returns all database settings.
     * 
     * @return encryption settings
     * @throws ControllerException
     */
    public abstract DatabaseSettings getDatabaseSettings() throws ControllerException;

    /**
     * Sets all server settings.
     * 
     * @param server
     *            settings
     * @throws ControllerException
     */
    public abstract void setServerSettings(ServerSettings settings) throws ControllerException;

    /**
     * Returns all update settings.
     * 
     * @return update settings
     * @throws ControllerException
     */
    public abstract UpdateSettings getUpdateSettings() throws ControllerException;

    /**
     * Sets all update settings.
     * 
     * @param update
     *            settings
     * @throws ControllerException
     */
    public abstract void setUpdateSettings(UpdateSettings settings) throws ControllerException;

    /**
     * Generates a new GUID.
     * 
     * @return a new GUID
     */
    public abstract String generateGuid();

    /**
     * A list of database driver metadata specified in the dbdrivers.xml file.
     * 
     * @return a list of database driver metadata
     * @throws ControllerException
     *             if the list could not be retrieved or parsed
     */
    public abstract List<DriverInfo> getDatabaseDrivers() throws ControllerException;

    /**
     * Returns the server version (ex. 1.8.2).
     * 
     * @return the server version
     */
    public abstract String getServerVersion();

    /**
     * Returns the server build date.
     * 
     * @return the server build date.
     */
    public abstract String getBuildDate();

    /**
     * Returns the server configuration, which contains:
     * <ul>
     * <li>Channels</li>
     * <li>Users</li>
     * <li>Alerts</li>
     * <li>Code templates</li>
     * <li>Server properties</li>
     * <li>Scripts</li>
     * </ul>
     * 
     * @return the server configuration
     * @throws ControllerException
     */
    public abstract ServerConfiguration getServerConfiguration() throws ControllerException;

    /**
     * Restores the server configuration.
     * 
     * @param serverConfiguration
     *            the server configuration to restore
     * @throws ControllerException
     *             if the server configuration could not be restored
     * @throws InterruptedException
     */
    public abstract void setServerConfiguration(ServerConfiguration serverConfiguration) throws StartException, StopException, ControllerException, InterruptedException;

    /**
     * Returns the password requirements specified in the mirth.properties file (ex. min length).
     * 
     * @return the password requriements
     */
    public abstract PasswordRequirements getPasswordRequirements();

    // status

    /**
     * Returns the current status of the server. See status constants in ConfigurationController.
     */
    public abstract int getStatus();

    /**
     * Sets the current status of the server. See status constants in ConfigurationController.
     */
    public abstract void setStatus(int status);

    /**
     * Returns the configuration map
     */
    public abstract Map<String, String> getConfigurationMap();

    /**
     * Returns the configuration map properties containing the values and comments for each key
     */
    public abstract Map<String, ConfigurationProperty> getConfigurationProperties() throws ControllerException;

    /**
     * Sets the configuration map properties
     */
    public abstract void setConfigurationProperties(Map<String, ConfigurationProperty> map, boolean persist) throws ControllerException;

    // properties

    public abstract Properties getPropertiesForGroup(String group);

    public abstract void removePropertiesForGroup(String group);

    public abstract String getProperty(String group, String name);

    public abstract void saveProperty(String group, String name, String property);

    public abstract void removeProperty(String group, String name);
}

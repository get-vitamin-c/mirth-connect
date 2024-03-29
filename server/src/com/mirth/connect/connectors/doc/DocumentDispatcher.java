/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * 
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL license a copy of which has
 * been included with this distribution in the LICENSE.txt file.
 */

package com.mirth.connect.connectors.doc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.resource.FSEntityResolver;
import org.xml.sax.InputSource;

import com.lowagie.text.html.HtmlParser;
import com.lowagie.text.rtf.RtfWriter2;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.event.ConnectionStatusEventType;
import com.mirth.connect.donkey.model.event.ErrorEventType;
import com.mirth.connect.donkey.model.message.ConnectorMessage;
import com.mirth.connect.donkey.model.message.Response;
import com.mirth.connect.donkey.model.message.Status;
import com.mirth.connect.donkey.server.DeployException;
import com.mirth.connect.donkey.server.HaltException;
import com.mirth.connect.donkey.server.StartException;
import com.mirth.connect.donkey.server.StopException;
import com.mirth.connect.donkey.server.UndeployException;
import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.donkey.server.event.ConnectionStatusEvent;
import com.mirth.connect.donkey.server.event.ErrorEvent;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import com.mirth.connect.server.util.TemplateValueReplacer;
import com.mirth.connect.util.ErrorMessageBuilder;

public class DocumentDispatcher extends DestinationConnector {
    private Logger logger = Logger.getLogger(this.getClass());
    private DocumentDispatcherProperties connectorProperties;
    private EventController eventController = ControllerFactory.getFactory().createEventController();
    private TemplateValueReplacer replacer = new TemplateValueReplacer();

    private static long ownerPasswordSeq = System.currentTimeMillis();

    @Override
    public void onDeploy() throws DeployException {
        this.connectorProperties = (DocumentDispatcherProperties) getConnectorProperties();

        eventController.dispatchEvent(new ConnectionStatusEvent(getChannelId(), getMetaDataId(), getDestinationName(), ConnectionStatusEventType.IDLE));
    }

    @Override
    public void onUndeploy() throws UndeployException {}

    @Override
    public void onStart() throws StartException {}

    @Override
    public void onStop() throws StopException {}

    @Override
    public void onHalt() throws HaltException {}

    @Override
    public void replaceConnectorProperties(ConnectorProperties connectorProperties, ConnectorMessage connectorMessage) {
        DocumentDispatcherProperties documentDispatcherProperties = (DocumentDispatcherProperties) connectorProperties;

        documentDispatcherProperties.setHost(replacer.replaceValues(documentDispatcherProperties.getHost(), connectorMessage));
        documentDispatcherProperties.setOutputPattern(replacer.replaceValues(documentDispatcherProperties.getOutputPattern(), connectorMessage));
        documentDispatcherProperties.setPassword(replacer.replaceValues(documentDispatcherProperties.getPassword(), connectorMessage));
        documentDispatcherProperties.setTemplate(replacer.replaceValues(documentDispatcherProperties.getTemplate(), connectorMessage));
    }

    @Override
    public Response send(ConnectorProperties connectorProperties, ConnectorMessage connectorMessage) {
        DocumentDispatcherProperties documentDispatcherProperties = (DocumentDispatcherProperties) connectorProperties;
        String responseData = null;
        String responseError = null;
        String responseStatusMessage = null;
        Status responseStatus = Status.QUEUED;

        String info = "";
        if (documentDispatcherProperties.isEncrypt()) {
            info = "Encrypted ";
        }
        info += documentDispatcherProperties.getDocumentType() + " Document Type Result Written To: " + documentDispatcherProperties.getHost() + "/" + documentDispatcherProperties.getOutputPattern();

        eventController.dispatchEvent(new ConnectionStatusEvent(getChannelId(), getMetaDataId(), getDestinationName(), ConnectionStatusEventType.WRITING, info));

        try {
            File file = createFile(documentDispatcherProperties.getHost() + "/" + documentDispatcherProperties.getOutputPattern());
            logger.info("Writing document to: " + file.getAbsolutePath());
            writeDocument(documentDispatcherProperties.getTemplate(), file, documentDispatcherProperties, connectorMessage);

            responseStatusMessage = "Document successfully written: " + documentDispatcherProperties.toURIString();
            responseStatus = Status.SENT;
        } catch (Exception e) {
            eventController.dispatchEvent(new ErrorEvent(getChannelId(), getMetaDataId(), ErrorEventType.DESTINATION_CONNECTOR, getDestinationName(), connectorProperties.getName(), "Error writing document", e));
            responseStatusMessage = ErrorMessageBuilder.buildErrorResponse("Error writing document", e);
            responseError = ErrorMessageBuilder.buildErrorMessage(connectorProperties.getName(), "Error writing document", e);

            // TODO: Handle exception
//            connector.handleException(e);
        } finally {
            eventController.dispatchEvent(new ConnectionStatusEvent(getChannelId(), getMetaDataId(), getDestinationName(), ConnectionStatusEventType.IDLE));
        }

        return new Response(responseStatus, responseData, responseStatusMessage, responseError);
    }

    private void writeDocument(String template, File file, DocumentDispatcherProperties documentDispatcherProperties, ConnectorMessage connectorMessage) throws Exception {
        // add tags to the template to create a valid HTML document
        StringBuilder contents = new StringBuilder();
        if (template.lastIndexOf("<html") < 0) {
            contents.append("<html>");
            if (template.lastIndexOf("<body") < 0) {
                contents.append("<body>");
                contents.append(template);
                contents.append("</body>");
            } else {
                contents.append(template);
            }
            contents.append("</html>");
        } else {
            contents.append(template);
        }

        String stringContents = getAttachmentHandler().reAttachMessage(contents.toString(), connectorMessage);

        if (documentDispatcherProperties.getDocumentType().toLowerCase().equals("pdf")) {
            FileOutputStream renderFos = null;

            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                builder.setEntityResolver(FSEntityResolver.instance());
                org.w3c.dom.Document document = builder.parse(new InputSource(new StringReader(stringContents)));

                ITextRenderer renderer = new ITextRenderer();
                renderer.setDocument(document, null);
                renderFos = new FileOutputStream(file);
                renderer.layout();
                renderer.createPDF(renderFos, true);
            } catch (Throwable e) {
                throw new Exception(e);
            } finally {
                if (renderFos != null) {
                    renderFos.close();
                }
            }

            if (documentDispatcherProperties.isEncrypt() && (documentDispatcherProperties.getPassword() != null)) {
                FileInputStream encryptFis = null;
                FileOutputStream encryptFos = null;
                PDDocument document = null;

                try {
                    encryptFis = new FileInputStream(file);

                    document = PDDocument.load(encryptFis);

                    AccessPermission accessPermission = new AccessPermission();
                    accessPermission.setCanAssembleDocument(false);
                    accessPermission.setCanExtractContent(true);
                    accessPermission.setCanExtractForAccessibility(false);
                    accessPermission.setCanFillInForm(false);
                    accessPermission.setCanModify(false);
                    accessPermission.setCanModifyAnnotations(false);
                    accessPermission.setCanPrint(true);
                    accessPermission.setCanPrintDegraded(true);

                    /*
                     * In 2.x when using iText for encryption, a "random" owner password was
                     * actually being generated even when null was passed in. However that doesn't
                     * happen by default with PDFBox, so we need to create one here instead. This
                     * method of concatenating the current time with the current free memory bytes
                     * is the same as was used in 2.x.
                     */
                    String ownerPassword = System.currentTimeMillis() + "+" + Runtime.getRuntime().freeMemory() + "+" + (ownerPasswordSeq++);
                    StandardProtectionPolicy policy = new StandardProtectionPolicy(ownerPassword, documentDispatcherProperties.getPassword(), accessPermission);
                    policy.setEncryptionKeyLength(128);

                    encryptFos = new FileOutputStream(file);
                    document.protect(policy);
                    document.save(encryptFos);
                } catch (Exception e) {
                    throw e;
                } finally {
                    if (document != null) {
                        document.close();
                    }

                    if (encryptFis != null) {
                        encryptFis.close();
                    }

                    if (encryptFos != null) {
                        encryptFos.close();
                    }
                }
            }
        } else if (documentDispatcherProperties.getDocumentType().toLowerCase().equals("rtf")) {
            com.lowagie.text.Document document = null;

            try {
                document = new com.lowagie.text.Document();
                //TODO verify the character encoding
                ByteArrayInputStream bais = new ByteArrayInputStream(stringContents.getBytes());
                RtfWriter2.getInstance(document, new FileOutputStream(file));
                document.open();
                HtmlParser parser = new HtmlParser();
                parser.go(document, bais);
            } finally {
                if (document != null) {
                    document.close();
                }
            }
        }
    }

    private File createFile(String filename) throws IOException {
        File file = new File(filename);
        if (!file.canWrite()) {
            String dirName = file.getPath();
            int i = dirName.lastIndexOf(File.separator);
            if (i > -1) {
                dirName = dirName.substring(0, i);
                File dir = new File(dirName);
                dir.mkdirs();
            }
            file.createNewFile();
        }
        return file;
    }

}

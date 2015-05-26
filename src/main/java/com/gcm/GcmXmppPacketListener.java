package com.gcm;

import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 4/23/15
 * Time: 2:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class GcmXmppPacketListener implements PacketListener {

    private static final Logger log = LoggerFactory.getLogger(GcmXmppPacketListener.class);
    private static final String GCM_NAMESPACE = "google:mobile:data";
    private DefaultLumberjackGcmServiceImpl gcmXmppServer;

    @Override
    public void processPacket(Packet packet) throws SmackException.NotConnectedException {
        log.debug( "< processPacket");
        Message incomingMessage = (Message) packet;
        GcmPacketExtension gcmPacket = (GcmPacketExtension) incomingMessage.getExtension(GCM_NAMESPACE);
        String json = gcmPacket.getJson();
        try {
            log.debug( "Received: " + packet.toXML());

            Map<String, Object> jsonObject = (Map<String, Object>) JSONValue.parseWithException(json);

            Object messageType = jsonObject.get("message_type");

            if ( StringUtils.equalsIgnoreCase("ack", messageType.toString()) ) {
                handleAckReceipt(jsonObject);
            } else if ( StringUtils.equalsIgnoreCase("nack", messageType.toString()) ) {
                handleNackReceipt(jsonObject);
            } else if ( StringUtils.equalsIgnoreCase("control", messageType.toString()) ) {
                handleControlMessage(jsonObject);
            } else {
                log.info("Unrecognized message type (%s)", messageType.toString());
            }
        } catch (ParseException e) {
            log.error( "Error parsing JSON " + json, e);
        } catch (Exception e) {
            log.error( "Failed to process packet", e);
        } finally {
            log.debug( "> processPacket");
        }
    }

    /**
     * Handles a NACK.
     *
     * <p>Logs a INFO message, but subclasses could override it to
     * properly handle NACKs.
     */
    protected void handleNackReceipt(Map<String, Object> jsonObject) {
        String messageId = (String) jsonObject.get("message_id");
        String from = (String) jsonObject.get("from");
        MDC.put("first", "Dorothy");
        log.info( "handleNackReceipt() from: " + from + ", messageId: " + messageId);
        MDC.clear();
        System.out.println( "handleNackReceipt() from: " + from + ", messageId: " + messageId);
    }

    protected void handleControlMessage(Map<String, Object> jsonObject) {
        log.info( "handleControlMessage(): " + jsonObject);
        String controlType = (String) jsonObject.get("control_type");
        if ("CONNECTION_DRAINING".equals(controlType)) {
           getGcmXmppServer().setConnectionDraining(true);
        } else {
            log.info( "Unrecognized control type: {}. This could happen if new features are added to the CCS protocol.", controlType);
        }
    }

    /**
     * Handles an ACK.
     *
     * <p>Logs a INFO message, but subclasses could override it to
     * properly handle ACKs.
     */
    protected void handleAckReceipt(Map<String, Object> jsonObject) {
        String messageId = (String) jsonObject.get("message_id");
        String from = (String) jsonObject.get("from");
        log.info( "handleAckReceipt() from: " + from + " messageId: " + messageId);
        System.out.println( "handleAckReceipt() from: " + from + " messageId: " + messageId);
    }

    public DefaultLumberjackGcmServiceImpl getGcmXmppServer() {
        return gcmXmppServer;
    }

    public void setGcmXmppServer(DefaultLumberjackGcmServiceImpl gcmXmppServer) {
        this.gcmXmppServer = gcmXmppServer;
    }
}

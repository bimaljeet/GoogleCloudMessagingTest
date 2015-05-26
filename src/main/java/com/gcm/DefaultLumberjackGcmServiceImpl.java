package com.gcm;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.StringUtils;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DefaultLumberjackGcmServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(DefaultLumberjackGcmServiceImpl.class);
    private GcmXmppPacketListener gcmXmppPacketListener;
    private static String GCM_SERVER = "gcm.googleapis.com";
    private static int GCM_PORT = 5235;
    private static final String GCM_ELEMENT_NAME = "gcm";
    private static final String GCM_NAMESPACE = "google:mobile:data";
    private Long gcmSenderId;
    private String gcmApiKey;
    private XMPPConnection connection;
    private String collapseKey;
    private Long timeToLive;
    private Boolean delayWhileIdle;

    /**
     * Indicates whether the connection is in draining state, which means that it
     * will not accept any new downstream messages.
     */
    protected volatile boolean connectionDraining = false;

    public void setGcmSenderId(Long gcmSenderId) {
        this.gcmSenderId = gcmSenderId;
    }
    public void setGcmApiKey(String gcmApiKey) {
        this.gcmApiKey = gcmApiKey;
    }

    public void start() throws Exception {
        log.debug("< start");
        try {
            ProviderManager.addExtensionProvider(GCM_ELEMENT_NAME, GCM_NAMESPACE,
                    new PacketExtensionProvider() {
                        @Override
                        public PacketExtension parseExtension(XmlPullParser parser) throws
                                Exception {
                            String json = parser.nextText();
                            return new GcmPacketExtension(json);
                        }
                    });
            connect(this.gcmSenderId, this.gcmApiKey);
        } catch(Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw e;
        } finally {
            log.debug("> start");
        }
    }

    public void sendGcmNotification(String destinationKey ,String messageContent) throws Exception {
        log.debug("< sendGcmNotification");
        try {
            String messageId = nextMessageId();
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("message", messageContent);
            String jsonMessage = createJsonMessage(destinationKey, messageId, payload, getCollapseKey(), getTimeToLive(), getDelayWhileIdle());
            log.debug("About to push payload:" + jsonMessage);
            sendDownstreamMessage(jsonMessage);
            log.debug("DONE.....");
        } catch (Exception ex) {
            log.error("Exception in sendGcmNotification : " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        } finally {
            log.debug("> sendGcmNotification");
        }
    }

    private boolean sendDownstreamMessage(String jsonRequest) throws Exception {
        log.debug("< sendDownstreamMessage");
        try {
            if ( !isConnectionDraining() ) {
                send(jsonRequest);
                return true;
            }
            log.info("Dropping downstream message since the connection is draining");
            return false;
        } catch(NotConnectedException e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw e;
        } catch(Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw e;
        } finally {
            log.debug("> sendDownstreamMessage");
        }
    }

    public String nextMessageId() {
        return "m-" + UUID.randomUUID().toString();
    }

    /**
     * Sends a packet with contents provided.
     */
    private void send(String jsonRequest) throws NotConnectedException {
        Packet request = new GcmPacketExtension(jsonRequest).toPacket();
        connection.sendPacket(request);
    }

    private static String createJsonMessage(String to, String messageId,
                                           Map<String, String> payload, String collapseKey, Long timeToLive,
                                           Boolean delayWhileIdle) {
        Map<String, Object> message = new HashMap<String, Object>();
        log.debug("> createJsonMessage");
        try{
            message.put("to", to);
            if (collapseKey != null) {
                message.put("collapse_key", collapseKey);
            }
            if (timeToLive != null) {
                message.put("time_to_live", timeToLive);
            }
            if (delayWhileIdle != null && delayWhileIdle) {
                message.put("delay_while_idle", true);
            }
            message.put("message_id", messageId);
            message.put("data", payload);

            return JSONValue.toJSONString(message);
        } finally {
            log.debug("< createJsonMessage");
        }
    }

    private ConnectionConfiguration getConnectionConfiguration(){
        log.debug("< getConnectionConfiguration");
        try{
            ConnectionConfiguration config = new ConnectionConfiguration(GCM_SERVER, GCM_PORT);
            config.setSecurityMode(SecurityMode.enabled);
            config.setReconnectionAllowed(true);
            config.setRosterLoadedAtLogin(false);
            config.setSendPresence(false);
            config.setSocketFactory(SSLSocketFactory.getDefault());
            return config;
        } finally {
            log.debug("< getConnectionConfiguration");
        }
    }

    private XMPPConnection getXMPPConnection() throws Exception {
        log.debug("< getXMPPConnection");
        try{
            connection = new XMPPTCPConnection(getConnectionConfiguration());
            connection.connect();
            connection.addConnectionListener(new LoggingConnectionListener());
            // Handle incoming packets
            connection.addPacketListener(getGcmXmppPacketListener() , new PacketTypeFilter(Message.class));
            // Log all outgoing packets
            connection.addPacketInterceptor(new PacketInterceptor() {
                @Override
                public void interceptPacket(Packet packet) {
                    log.info( "Sent: {0}", packet.toXML());
                }
            }, new PacketTypeFilter(Message.class));
            return connection;
        } catch (XMPPException xMPPException){
            log.error("XMPPException");
            xMPPException.printStackTrace();
            throw xMPPException;
        } catch (IOException iOException){
            log.error("IOException");
            iOException.printStackTrace();
            throw iOException;
        } catch (SmackException smackException){
            log.error("SmackException");
            smackException.printStackTrace();
            throw smackException;
        } catch (Exception exception){
            log.error("exception");
            exception.printStackTrace();
            throw exception;
        } finally {
            log.debug("< getXMPPConnection");
        }
    }

    private void connect(long senderId, String apiKey ) throws Exception {
        connection = getXMPPConnection();
        connection.login(senderId + "@gcm.googleapis.com", apiKey);
    }

    private static final class LoggingConnectionListener implements ConnectionListener {

        @Override
        public void connected(XMPPConnection xmppConnection) {
            log.info("Connected.");
        }

        @Override
        public void authenticated(XMPPConnection xmppConnection) {
            log.info("Authenticated.");
        }

        @Override
        public void reconnectionSuccessful() {
            log.info("Reconnecting..");
        }

        @Override
        public void reconnectionFailed(Exception e) {
            log.info( "Reconnection failed.. ", e);
        }

        @Override
        public void reconnectingIn(int seconds) {
            log.info( "Reconnecting in %d secs", seconds);
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            log.info("Connection closed on error.");
        }

        @Override
        public void connectionClosed() {
            log.info("Connection closed.");
        }
    }

    public boolean isConnectionDraining() {
        return connectionDraining;
    }

    public void setConnectionDraining(boolean connectionDraining) {
        this.connectionDraining = connectionDraining;
    }

    public GcmXmppPacketListener getGcmXmppPacketListener() {
        return gcmXmppPacketListener;
    }

    public void setGcmXmppPacketListener(GcmXmppPacketListener gcmXmppPacketListener) {
        this.gcmXmppPacketListener = gcmXmppPacketListener;
    }

    public String getCollapseKey() {
        return collapseKey;
    }

    public void setCollapseKey(String collapseKey) {
        this.collapseKey = collapseKey;
    }

    public Long getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(Long timeToLive) {
        this.timeToLive = timeToLive;
    }

    public Boolean getDelayWhileIdle() {
        return delayWhileIdle;
    }

    public void setDelayWhileIdle(Boolean delayWhileIdle) {
        this.delayWhileIdle = delayWhileIdle;
    }
}

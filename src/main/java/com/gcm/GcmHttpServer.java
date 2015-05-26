package com.gcm;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 4/23/15
 * Time: 9:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class GcmHttpServer {

    private Sender httpSender;
    private String serverKey;
    private final Logger log = LoggerFactory.getLogger(GcmHttpServer.class);

    public void init() throws Exception {
        httpSender = new Sender(getServerKey());
    }

    public Result sendGcmMessage(String payload, String registrationId)  throws Exception {
        Result result = null;
        log.debug("< sendGcmMessage");
        try {
            Message message = makeHttpMessage();
            log.debug("Message ------------  = "+message);
            System.out.println("Message ------------  = "+message);
            result = httpSender.sendNoRetry(message, registrationId);
            return result;
        } catch (Exception e){
            log.error("Exception in sendGcmMessage !! "+e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            log.debug("> sendGcmMessage");
        }
    }

    private Message makeHttpMessage() throws Exception {
        log.debug("< makeHttpMessage");
        try {
            Message message = new Message.Builder()
                    .collapseKey("demo")
                    .timeToLive(3)
                    .delayWhileIdle(true)
                    .addData("Nick", "Mario")
                    .addData("Text", "great match!")
                    .build();
            return message;
        } catch (Exception e){
            log.error("Exception in makeHttpMessage !! "+e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            log.debug("> makeHttpMessage");
        }
    }


    public Sender getHttpSender() {
        return httpSender;
    }

    public void setHttpSender(Sender httpSender) {
        this.httpSender = httpSender;
    }

    public String getServerKey() {
        return serverKey;
    }

    public void setServerKey(String serverKey) {
        this.serverKey = serverKey;
    }
}

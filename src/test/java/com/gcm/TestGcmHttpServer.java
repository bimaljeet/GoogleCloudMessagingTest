package com.gcm;


import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 4/23/15
 * Time: 10:29 AM
 * To change this template use File | Settings | File Templates.
 */
//@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/TestBeans.xml" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGcmHttpServer  implements ApplicationContextAware {

    private ApplicationContext ctx;
    final static Logger log = LoggerFactory.getLogger(TestGcmHttpServer.class);
    private GcmHttpServer gcmHttpServer;
    private DefaultLumberjackGcmServiceImpl vvmGcmXmppServer;
    private DefaultLumberjackGcmServiceImpl cpsGcmXmppServer;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.debug("SETTING ctx in the JUnit for Cassandra");
        this.ctx = applicationContext;
    }

    @Before
    public void setUp(){
        log.debug("< setUp");
        try {
            if ( null == gcmHttpServer ){
                gcmHttpServer = (GcmHttpServer) ctx.getBean("gcmHttpServer");
            }
            if ( null == vvmGcmXmppServer){
                vvmGcmXmppServer = (DefaultLumberjackGcmServiceImpl) ctx.getBean("vvmGcmXmppServer");
            }
            if ( null == cpsGcmXmppServer){
                cpsGcmXmppServer = (DefaultLumberjackGcmServiceImpl) ctx.getBean("cpsGcmXmppServer");
            }
        } catch (Exception e){
            log.error("Error in setUp "+e.getMessage());
            e.printStackTrace();
        } finally {
            log.debug("> setUp");
        }
    }

//    @Test
    public void a_testHttpServer() throws Exception {
        log.debug("< a_testHttpServer");
        try {
            String token = "RegistrationIdOfTheTargetDevice";
            gcmHttpServer.sendGcmMessage("hello", token);
        } catch (Exception e){
            log.error("Error in a_testHttpServer "+e.getMessage());
            e.printStackTrace();
        } finally {
            log.debug("> a_testHttpServer");
        }
    }

    @Test
    public void a_testVvmXmppServer() throws Exception {
        log.debug("< a_testVvmXmppServer");
        try {
//            String toRegId = "APA91bFfpyJLXBkumGaeL1b16ezMQWdFGuq_IoCi1uUVduOGzC51Wc3F6_EZ0Ltcpnxcr7UyWMpW5ZHFEDtVbXr-4dAN6nvdAjC6qzjKg6KgA01SHt4-03xG5nvxk07p-B1NQDqgLj-2lWykMs4XHgaPt9hbhe87ZQ";
//              test device
            String toRegId = "APA91bFjlaKsMIKTwbjrTOYyhV_3XrXxZ6v5pciYmoHW7WdV7RqmwEDhw1ro2ijYI3PraUh3mEJ7r08UUKXxhCLyHCmVzmXZHWQgJguXz6C5jhcGTgujc2U65UHkaGQCl8xJY1xWV9rbxKx1QD2jukhBaq8RPIuaAw";
            String messageId = vvmGcmXmppServer.nextMessageId();
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("message", makePayload());
//            String collapseKey = "sample";
//            Long timeToLive = 10000L;
//            String message = vvmGcmXmppServer.createJsonMessage(toRegId, messageId, payload, collapseKey, timeToLive, true);

            vvmGcmXmppServer.sendGcmNotification(toRegId, makePayload());//dDownstreamMessage(message);

            //Thread.sleep(30000);
        } catch (Exception e){
            log.error("Error in a_testXmppServer "+e.getMessage());
            e.printStackTrace();
        } finally {
            log.debug("> a_testXmppServer");
        }
    }

    @Test
    public void a_testCpsXmppServer() throws Exception {
        log.debug("< a_testCpsXmppServer");
        try {
            String toRegId = "APA91bGD2la_z6ciTn-WVnRYnHY7ym9IMKvIzxhoQcFc6JFcyTkI4wbPhKXkmN9G__K81hsTzik__dx0i9Kddh7UyWUw0ai8G7ayIQ4EoKwrCYB30D4knQIv6RISeOndzC1Y0XAlRlbEowgqCyHx2EKdpPL7vruFEA";
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("message", makePayload());
            cpsGcmXmppServer.sendGcmNotification(toRegId, makePayload());
        } catch (Exception e){
            log.error("Error in a_testXmppServer "+e.getMessage());
            e.printStackTrace();
        } finally {
            log.debug("> a_testCpsXmppServer");
        }
    }


    private String makePayload(){
        StringBuffer payload = new StringBuffer();
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        payload.append("Hi from Bimal, message send at [");
        payload.append(date);
        payload.append("]");
        return payload.toString();
    }
}

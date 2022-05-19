package org.jvmxray.collector.jms;

import jakarta.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.jvmxray.exception.JVMXRayException;
import org.jvmxray.util.HDFSUtil;
import org.jvmxray.util.JSONUtil;
import org.jvmxray.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * Worker thread for processing inbound Agent connections.
 * @author Milton Smith
 */
public class JVMXRayJMSServerThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.collector.jms.JVMXRayJMSServerThread");
    private volatile String conFactory;
    private volatile String topicQueue;
    private PropertyUtil pu;

    /**
     * CTOR
     */
    public JVMXRayJMSServerThread(PropertyUtil pu) {
        super("JVMXRay Worker");
        // Worker configuration properties.
        this.pu = pu;
        Properties props = pu.getProperties();
        conFactory = props.getProperty(PropertyUtil.SYS_PROP_SERVER_BROKER_URL, "tcp://localhost:61616");
        if( conFactory != null ) {
            conFactory = conFactory.trim();
        }
        topicQueue = props.getProperty(PropertyUtil.SYS_PROP_SERVER_TOPICNAME, "topic0");
        if( topicQueue != null ) {
            topicQueue = topicQueue.trim();
        }
    }

    /**
     * Thread entry point.
     */
    public void run() {
        jakarta.jms.Connection connection = null;
        HDFSUtil hdfsutil = null;
        try {
            String event = "";
            JSONUtil jsonutil = JSONUtil.getInstance();
            // Getting JMS connection from the server
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(conFactory);
            connection = connectionFactory.createConnection();
            connection.start();
            // Creating session for seding messages
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            // Getting the topic queue
            Destination destination = session.createQueue(topicQueue);
            // MessageConsumer is used for receiving (consuming) messages
            MessageConsumer consumer = session.createConsumer(destination);
            // Event persistance
            hdfsutil = HDFSUtil.getInstance(pu);
            hdfsutil.openActiveFile();
            // Keep pulling messages off the queue until there are no more messages and finish normally.
            while (true) {
                // Receive a new message or null if consumer closed.
                Message message = consumer.receive();
                if( message == null ) {
                    logger.info("JMS consumer closed. msg=Unable to retrieve Agent message.");
                    break;
                }
                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    String msg = textMessage.getText();
                    if( msg == null ) {
                        logger.warn("Agent message received is null.");
                        break;
                    }
                    long len = msg.length();
                    if( msg.length()<1 ) {
                        logger.warn("Agent message received is zero length.");
                        break;
                    }
                    try {
                        event = jsonutil.fromJSON(msg);
                        logger.debug("Agent message received. msg[" + len + "b]=" + msg);
                    }catch(JVMXRayException e) {
                        logger.error("Agent message received. msg[" + len + "b]=" + msg);
                        logger.error("Agent event JSON deserialization error.", e);
                        break;
                    }
                   try {
                       hdfsutil.writeEvent(event);
                    }catch(IOException e) {
                        logger.error("Event persistence error.  msg="+e.getMessage(), e);
                        break;
                    }
                }
            }
        } catch( Throwable t ) {
            logger.error("Unhandled server connection exception. msg="+t.getMessage(),t);
        } finally {
            try {
                hdfsutil.closeActiveFile();
            } catch (IOException e) {
                logger.error("Error closing HDFSUtil.",e);
            }
            try {
                if( connection != null ) {
                    connection.close();
                }
            } catch (JMSException e) {}
            logger.debug("Agent connection closed.");
        }
    }

}


package org.jvmxray.task;

import jakarta.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.jvmxray.exception.JVMXRayRuntimeException;
import org.jvmxray.util.PropertyUtil;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class NativeJMSTransportTask extends BaseTask {

    private Vector queue = new Vector();
    private Properties p;

    public NativeJMSTransportTask(Properties p) {
        this.p = p;
    }

    @Override
    protected void queueMessage(String message) {
        queue.add(message);
    }

    public void sendEvent(String message) {
        queueMessage(message);
    }

    @Override
    public void execute() {
        try {
            // Thread safe shallow clone.
            Vector local = null;
            synchronized (queue) {
                local = (Vector) queue.clone();
                queue.clear();
            }

            // Retrieve configuration.
            String topic = p.getProperty(PropertyUtil.SYS_PROP_AGENT_TOPICNAME);
            String burl = p.getProperty(PropertyUtil.SYS_PROP_AGENT_BROKER_URL);
            if( topic!=null || burl!=null ) {
                topic = topic.trim();
                burl = burl.trim();
            } else {
                throw new JVMXRayRuntimeException("NativeJMSTransportTask.execute(): Bad JMS configuration.  topic="+topic+" broker url="+burl);
            }

            // Getting JMS connection from the server and starting it
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(burl);
            Connection connection = connectionFactory.createConnection();
            connection.start();

            //Creating a non transactional session to send/receive JMS message.
            Session session = connection.createSession(false,
                    Session.AUTO_ACKNOWLEDGE);

            //Destination represents here our queue 'JCG_QUEUE' on the JMS server.
            //The queue will be created automatically on the server.
            Destination destination = session.createQueue(topic);

            // MessageProducer is used for sending messages to the queue.
            MessageProducer producer = session.createProducer(destination);

            // Iterate over the event buffer
            Enumeration elements = local.elements();
            while( elements.hasMoreElements() ) {
                String event = (String)elements.nextElement();
                // assign message
                TextMessage message = session.createTextMessage();
                message.setText(event);
                // publish the messages
                producer.send(message);
            }
            // close the topic connection
            connection.close();
        }catch(Throwable t) {
            t.printStackTrace();
        }
    }
}

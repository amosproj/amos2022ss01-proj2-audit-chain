package ConsumerDummy.Client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

import ProducerDummy.Messages.Message;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;


/**
 * Consumerclient implementation
 */
public class Client extends Consumer {
    /**
     * Constructor for Client.AbstractClient. Initializes the filepath, the file reader and set information for the
     * connection factory. Call {@link #initFactory()} to initialize the connection factory.
     *
     * @throws IOException if the file cannot be read
     */
    public Client(String host, int port, String username, String password) throws IOException {
        super(host, port, username, password);
    }


    /***
     * Start receiving Messages from the RabbitMQ Server.
     * @throws IOException if an I/O error occurs
     * @throws TimeoutException if the timeout expires
     */
    public void start() throws IOException, TimeoutException {

        Channel channel = this.getChannel();
        channel.basicQos(100); // QoS must be specified
        channel.confirmSelect();
        channel.basicConsume(
                this.channel.getQueueName(),
                false,
                Collections.singletonMap("x-stream-offset", 0), // "first" offset specification
                (consumerTag, message) -> {
                    try {
                        ArrayList<Message> messages = (ArrayList<Message>) Consumer.deserialize(message.getBody());
                        for (int i = 0; i < messages.size(); i++) {
                            Message m = messages.get(i);
                            this.persistenceStrategy.StoreMessage(m);
                            System.out.println(String.format("Message received with event Number: %d",m.getSequence_number() ));
                        }

                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    channel.basicAck(message.getEnvelope().getDeliveryTag(), false); // ack is required
                },
                consumerTag -> {});
    }

}


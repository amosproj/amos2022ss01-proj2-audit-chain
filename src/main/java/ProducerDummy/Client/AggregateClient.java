package ProducerDummy.Client;


import ProducerDummy.DataGeneration.NullObjectDataReader;
import ProducerDummy.Messages.Hmac_Message_JsonMessage;
import ProducerDummy.Messages.Message;
import ProducerDummy.Persistence.NullObjectPersistenceStrategy;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AggregateClient extends Producer {

    private int SIZE = 1024;

    //TODO only for test purpose
    private static String KEY = "0123456";
    private static String ALGORITHM = "HmacSHA256";


    /**
     * Constructor for Client.Client.
     *
     * @param host
     * @param port
     * @param username
     * @param password
     * @param queue_name
     * @throws IOException if an I/O error occurs
     */
    public AggregateClient(String host, int port, String username, String password, String queue_name) throws IOException {
        super(host, port, username, password, queue_name);
        this.dataGenerator = new NullObjectDataReader();
        this.persistenceStrategy = new NullObjectPersistenceStrategy("no_path", "no_file");
    }

    public void start() throws IOException, TimeoutException {

        System.out.println("Starting to send Messages.Message to AMQP Host");
        try (Connection connection = this.factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.confirmSelect();
            int start_event = this.sequence_number;

            for (String line = this.dataGenerator.getData(); line != null; line = this.dataGenerator.getData()) {
                this.persistenceStrategy.StoreMessage(new Hmac_Message_JsonMessage(this.sequence_number, line, ALGORITHM, KEY));
                if (isReadyToSend()) {
                    this.getAcknowledgment(channel, this.persistenceStrategy.ReadLastMessage());
                    this.sequence_number += 1;
                    this.persistenceStrategy.cleanFile();
                    System.out.format(String.format("Message from event Number %d until %d were sent", start_event, this.sequence_number + "\n"));
                    //at least for now we just catch it ...
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException e) {
                        System.out.println("Thread was interrupted");
                    }
                    start_event = this.sequence_number;
                } else {
                    System.out.println("Append Message: " + line);
                    this.sequence_number += 1;
                }
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        return;
    }


    public void getAcknowledgment(Channel channel, Message message) {
        //we try for 5 times for acknowledgment and if we get it, we publish the message
        for (int i = 0; i <= 5; i++) {
            try {
                channel.basicPublish("", this.QUEUE_NAME, null, serialize(message));
                channel.waitForConfirmsOrDie(5_000);
                break;
            } catch (InterruptedException | TimeoutException | IOException e) {
                if (i == 5) {
                    System.out.println("Could not send message");
                }
            }
        }
    }


    public boolean isReadyToSend() throws IOException {
        long bytes = Files.size(this.persistenceStrategy.getFilePath());
        if (bytes <= this.SIZE) {
            return false;
        } else {
            return true;
        }
    }

    public int getSequence_number() {
        return this.sequence_number;
    }



}

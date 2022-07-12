package ConsumerDummy.Client;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import BlockchainImplementation.Blockchain.Blocks.SubBlock;
import ProducerDummy.Client.AbstractClient;
import ProducerDummy.Messages.Message;
import ProducerDummy.Persistence.NullObjectPersistenceStrategy;
import ProducerDummy.Persistence.PersistenceStrategy;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import BlockchainImplementation.Blockchain.BlockchainIntSequenceAPI;
import org.json.JSONObject;

public class Consumer extends AbstractClient {

    /**
     * Constructor for Client.AbstractClient. Initializes the filepath, the file reader and set information for the
     * connection factory. Call {@link #initFactory()} to initialize the connection factory.
     *
     * @param host
     * @param port
     * @param username
     * @param password
     * @throws IOException if the file cannot be read
     */

    public Consumer(String host, int port, String username, String password) {
        super(host, port, username, password);
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objStream = new ObjectInputStream(byteStream);

        return objStream.readObject();

    }

    public ConnectionFactory getFactory(){
        return this.factory;
    }

    public Channel getChannel() throws IOException, TimeoutException {
        return this.channel.createChannel(this.factory);
    }



    /***
     * Simple Consumer, receives Messages, prints them out and delivers an ACK
     * Start receiving Messages from the RabbitMQ Server.
     * @throws IOException if an I/O error occurs
     * @throws TimeoutException if the timeout expires
     */
    public void start() throws IOException, TimeoutException {
        // create Callback to receive Messages
        Channel channel = this.channel.createChannel(this.factory);
        channel.basicConsume(
                this.channel.getQueueName(), // set Queue Name
                false, // Autoack no
                (consumerTag, delivery) -> {
                    try {
                        ArrayList<Message> messages = (ArrayList<Message>) Consumer.deserialize(delivery.getBody());
                        messages.forEach(message ->
                                System.out.println(String.format(" [%d] Received %s'", message.getSequence_number(), message.getMessage())));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                },
                consumerTag -> { });


        this.listen();
    }

    public void listen() throws IOException {
        ServerSocket serverSocket = new ServerSocket( 6868);
        System.out.println("Local IP: " + serverSocket.getInetAddress().toString());
        System.out.println("Accepting Connections now");
        Socket socket = serverSocket.accept();
        System.out.println("Client connected");
        InputStream input = socket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        // Placeholder for the Blockchain
        BlockchainIntSequenceAPI blockchain = new BlockchainIntSequenceAPI<>("src/test/resources/testOutput/", 50);
        while(true){
            try {
                //converting the String into an JSON object
                JSONObject jsonObject = new JSONObject(reader.readLine());
                //switch case over 'command' field
                switch (jsonObject.get("command").toString()){
                    case "check_single_message":
                        final String check_single_message = blockchain.getTemperedMessageIfAnyAsString(Integer.parseInt(jsonObject.get("number").toString()));
                        jsonObject.append("check_single_message", check_single_message);
                        break;
                    case "check_message_interval":
                        final String check_message_interval = blockchain.getTemperedMessageIfAnyAsString(Integer.parseInt(jsonObject.get("start").toString()), Integer.parseInt(jsonObject.get("end").toString()));
                        jsonObject.append("check_message_interval", check_message_interval);
                        break;
                    case "get_statistics":
                        jsonObject.append("amountDataRecords", blockchain.getBytesSize());
                        jsonObject.append("amountFilesCreated", blockchain.getNumberOfFiles());
                        jsonObject.append("currentSize", blockchain.getSize());
                        break;
                }

                //send back JSOnObject
                DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
                dOut.writeUTF(jsonObject.toString());
                dOut.flush();
            }
            catch(SocketException e){
             System.out.println("Client disconnected");
            }
        }
    }

}

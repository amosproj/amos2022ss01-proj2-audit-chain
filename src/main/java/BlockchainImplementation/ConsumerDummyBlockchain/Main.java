package BlockchainImplementation.ConsumerDummyBlockchain;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import org.apache.commons.cli.*;

import ConsumerDummy.Client.StreamClient;
import ProducerDummy.ChannelSelection.QuorumQueues;
import ProducerDummy.ChannelSelection.RabbitMQChannel;
import ProducerDummy.ChannelSelection.StandardQueue;
import ProducerDummy.ChannelSelection.Stream;
import ProducerDummy.Client.AbstractClient;
import ProducerDummy.Persistence.AggregateMessageFilePersistence;
import ProducerDummy.Persistence.FilePersistenceStrategy;
import ProducerDummy.Persistence.NullObjectPersistenceStrategy;
import ProducerDummy.Persistence.PersistenceStrategy;

public class Main {

    public static void main(String[] args) throws IOException, TimeoutException {

        Options options = new Options();
        Option host = new Option( "h","host", false, "host ip");
        host.setRequired(false);
        options.addOption(host);
        Option port = new Option("p","port", false, "port");
        port.setRequired(false);
        options.addOption(port);
        Option username = new Option("u","username", true, "username of rabbitmq");
        username.setRequired(false);
        options.addOption(username);
        Option password = new Option("pw","password", true, "password of rabbitmq");
        password.setRequired(false);
        options.addOption(password);

        CommandLine cmd;
        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new DefaultParser();

        String HOST = null, PORT = null, USER = null, PASSWORD = null;

        try {
            cmd = parser.parse(options, args);
            HOST = cmd.getOptionValue("host");
            PORT = cmd.getOptionValue("port");
            USER = cmd.getOptionValue("username");
            PASSWORD = cmd.getOptionValue("password");
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar target/AuditChain-Blockchain.jar --host 127.0.0.1 --port 5672 --username admin --password admin", options);
            System.exit(1);
        }

        String filepath = Paths.get("src", "main", "resources","BlockchainImplementation").toString();
        String filename = "config.properties";

        Path config_path = Paths.get(System.getProperty("user.dir"), filepath, filename);
        Properties p = new Properties();
        FileReader reader = new FileReader(config_path.toString());
        p.load(reader);

        if (HOST == null) {
            HOST = p.getProperty("HOST");
        }
        if (PORT == null) {
            PORT = p.getProperty("PORT");
        }
        if (USER == null) {
            USER = p.getProperty("USERNAME");
        }
        if (PASSWORD == null) {
            PASSWORD = p.getProperty("PASSWORD");
        }
        String queue_name = p.getProperty("QUEUE_NAME");
        int gui_port = Integer.parseInt(p.getProperty("GUI_PORT"));
        String PATH = p.getProperty("PATH_BLOCKCHAIN_FILES");
        int MAX_BYTE = Integer.parseInt(p.getProperty("MAX_BYTE_PER_FILE"));

        AbstractClient client = new ConsumerClientBlockchain(HOST,Integer.parseInt(PORT),USER,PASSWORD, PATH, MAX_BYTE,gui_port);
        client.setChannel(new QuorumQueues(queue_name));
        try {
            client.start();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return;
    }

}

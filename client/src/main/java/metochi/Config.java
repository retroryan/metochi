package metochi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class Config {

    private static Logger logger = LoggerFactory.getLogger(MetochiClient.class.getName());

    int port;
    ArrayList<String> peerUrls;
    int startDelay;
    boolean isAuthorityNode = true;
    boolean enableRandomMessage = true;

    public Config(int port, ArrayList<String> peerUrls, int startDelay, boolean isAuthorityNode, boolean enableRandomMessage) {
        this.port = port;
        this.peerUrls = peerUrls;
        this.startDelay = startDelay;
        this.isAuthorityNode = isAuthorityNode;
        this.enableRandomMessage = enableRandomMessage;
    }

    @Override
    public String toString() {
        return "Config{" +
                "port=" + port +
                ", peerUrls=" + peerUrls +
                '}';
    }

    /**
     * Load the properties file for a given node
     * @param node
     * @return
     */
    static Config loadProperties(String node) {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            //load the properties file for the node
            String name = "conf/node_" + node + ".conf";
            logger.info("loading config file: " + name);
            input = new FileInputStream(name);
            prop.load(input);

            //load the individual properties for configuration
            int port = Integer.parseInt(prop.getProperty("port"));
            String peers = prop.getProperty("peers");
            String[] splitLine = peers.split(",");
            ArrayList<String> peersList = new ArrayList<>(Arrays.asList(splitLine));
            int startDelay = Integer.parseInt(prop.getProperty("startDelay"));
            boolean isAuthorityNode = Boolean.parseBoolean(prop.getProperty("isAuthorityNode"));
            boolean enableRandomMessage = Boolean.parseBoolean(prop.getProperty("enableRandomMessage"));

            //return the full configuration object
            return new Config(port, peersList, startDelay, isAuthorityNode, enableRandomMessage);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}

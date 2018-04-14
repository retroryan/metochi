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
    String hostname;
    ArrayList<String> peerUrls;
    int startDelay;
    boolean isAuthorityNode = false;
    boolean enableRandomMessage = false;
    boolean leadNode = false;

    public Config(int port, String hostname, ArrayList<String> peerUrls, int startDelay, boolean authorityNode, boolean enableRandomMessage, boolean leadNode) {
        this.port = port;
        this.hostname = hostname;
        this.peerUrls = peerUrls;
        this.startDelay = startDelay;
        this.isAuthorityNode = authorityNode;
        this.enableRandomMessage = enableRandomMessage;
        this.leadNode = leadNode;
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
            String hostname = prop.getProperty("hostname");
            String peers = prop.getProperty("peers");
            String[] splitLine = peers.split(",");
            ArrayList<String> peersList = new ArrayList<>(Arrays.asList(splitLine));
            int startDelay = Integer.parseInt(prop.getProperty("startDelay"));
            boolean isAuthorityNode = Boolean.parseBoolean(prop.getProperty("isAuthorityNode"));
            boolean enableRandomMessage = Boolean.parseBoolean(prop.getProperty("enableRandomMessage"));
            boolean leadNode = Boolean.parseBoolean(prop.getProperty("isLeadNode"));

            //return the full configuration object
            return new Config(port, hostname, peersList, startDelay, isAuthorityNode, enableRandomMessage, leadNode);

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

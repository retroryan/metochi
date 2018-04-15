package metochi;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * This encapsulates a connection to another node or peer in the decentralized cluster.
 * <p>
 * It sets up the gRPC client connection to the node and manages the communication.
 */
public class BroadcastPeer {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(MetochiClient.class.getName());
    private final String peerURL;

    private BroadcastServiceGrpc.BroadcastServiceBlockingStub broadcastService;

    BroadcastPeer(String peerURL, String jwtToken) {
        this.peerURL = peerURL;
        try {
            logger.info("setting peer connection to: " + peerURL);
            initBroadcastService(peerURL, jwtToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize a managed channel to connect to the broadcast service.
     * Set the broadcastChannel and broadcastService
     */
    void initBroadcastService(String peerURL, String jwtToken) {

        //TODO Initialize the Broadcast Channel and Broadcast Service here

        //TODO Get a new Blocking Stub

    }

    void broadcast(Block block, String senderURL) {
        //TODO Broadcast a block here
    }

    String getPeerURL() {
        return peerURL;
    }

    public Block queryLatest() {
        //TODO  Query for the latest blocks here
        return null;
    }

    Blockchain queryAll() {
        //TODO  Query for the entire blockchain here
        return null;
    }


    ProposeResponse propose(String nodeName) {
        return broadcastService.propose(ProposeRequest.newBuilder().setNodeName(nodeName).build());
    }

    void broadcastTransaction(Transaction transaction) {
        broadcastService.broadcastTransaction(transaction);
    }

}

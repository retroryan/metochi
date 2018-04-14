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

    BroadcastPeer(String peerURL) {
        this.peerURL = peerURL;
        try {
            logger.info("setting peer connection to: " + peerURL);
            initBroadcastService(peerURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize a managed channel to connect to the broadcast service.
     * Set the broadcastChannel and broadcastService
     */
    void initBroadcastService(String peerURL) {

        //TODO Initialize the Broadcast Channel and Broadcast Service here
        ManagedChannel broadcastChannel = ManagedChannelBuilder.forTarget(peerURL)
                .usePlaintext(true).build();

        //TODO Get a new Blocking Stub
        broadcastService = BroadcastServiceGrpc.newBlockingStub(broadcastChannel);

    }

    void broadcast(Block block, String senderURL) {
        //TODO Broadcast a block here
        broadcastService.broadcast(BroadcastMessage
                .newBuilder()
                .setBlock(block)
                .setSender(senderURL)
                .build());
    }

    String getPeerURL() {
        return peerURL;
    }

    public Block queryLatest() {
        //TODO  Query for the latest blocks here
        return broadcastService.queryLatest(Empty.newBuilder().build());
    }

    Blockchain queryAll() {
        //TODO  Query for the entire blockchain here
        return broadcastService.queryAll(Empty.newBuilder().build());
    }


    ProposeResponse propose(String nodeName) {
        return broadcastService.propose(ProposeRequest.newBuilder().setNodeName(nodeName).build());
    }

    void broadcastTransaction(Transaction transaction) {
        broadcastService.broadcastTransaction(transaction);
    }

}

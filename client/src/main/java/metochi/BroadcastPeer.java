package metochi;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.LoggerFactory;

import java.util.UUID;


/*

io.grpc.StatusRuntimeException: UNAVAILABLE: HTTP/2 error code: NO_ERROR
Received Goaway

io.grpc.StatusRuntimeException: UNAVAILABLE


 */
public class BroadcastPeer {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(MetochiClient.class.getName());
    private final String peerURL;

    private BroadcastServiceGrpc.BroadcastServiceBlockingStub broadcastService;

    public BroadcastPeer(String peerURL) {
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
    public void initBroadcastService(String peerURL) {

        //TODO Initialize the Broadcast Channel and Broadcast Service here

        //TODO Get a new Blocking Stub

    }

    public void broadcast(Block block) {

        //TODO Broadcast a block here
    }

    public String getPeerURL() {
        return peerURL;
    }

    public Block queryLatest() {
        return null;
    }

    public Blockchain queryAll() {
        //TODO  Query for the entire blockchain here
        return null;
    }


    public ProposeResponse propose(String nodeName) {
        return broadcastService.propose(ProposeRequest.newBuilder().setNodeName(nodeName).build());
    }

    void broadcastTransaction(Transaction transaction) {
        broadcastService.broadcastTransaction(transaction);
    }

}

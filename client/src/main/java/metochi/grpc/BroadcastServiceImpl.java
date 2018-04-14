package metochi.grpc;

import com.google.protobuf.Empty;
import metochi.*;
import org.slf4j.LoggerFactory;

import java.util.Optional;

// TODO Extend gRPC's BroadcastServiceImplBase
public class BroadcastServiceImpl extends BroadcastServiceGrpc.BroadcastServiceImplBase {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(MetochiClient.class.getName());

    private Optional<AuthorityNode> optAuthorityNode;

    private final BlockChainManager blockChainManager;

    public BroadcastServiceImpl(BlockChainManager manager, Optional<AuthorityNode> optAuthorityNode) {
        this.blockChainManager = manager;
        this.optAuthorityNode = optAuthorityNode;
    }

    //TODO - Override the broadcast, queryLatest and queryAll methods here

    /**
     *
     * This handles a call to broadcast a block to this node.  When the call is received this node add the block to it's blockchain.
     *
     * @param request
     * @param responseObserver
     */
    @Override
    public void broadcast(metochi.BroadcastMessage request,
                          io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.info("server broadcast received block index: " + request.getBlock().getIndex());
        blockChainManager.addLatestBlock(request.getBlock(), request.getSender());
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    /**
     *
     * This handles a call to query the latest block from this node and sends it back to the node that made the query.
     *
     * @param request
     * @param responseObserver
     */
    @Override
    public void queryLatest(com.google.protobuf.Empty request,
                            io.grpc.stub.StreamObserver<metochi.Block> responseObserver) {
        try {
            logger.info("server queryLatest");
            responseObserver.onNext(blockChainManager.getLatestBlock());
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     *
     * This handles a call to query the entire blockchain from this node and sends it back to the node that made the query.
     *
     * @param request
     * @param responseObserver
     */
    @Override
    public void queryAll(com.google.protobuf.Empty request,
                         io.grpc.stub.StreamObserver<metochi.Blockchain> responseObserver) {
        try {
            logger.info("server queryAll");
            responseObserver.onNext(blockChainManager.getBlockchain());
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //These methods are used when creating a Proof of Authority blockchain
    //@Override
    public void propose(metochi.ProposeRequest request,
                        io.grpc.stub.StreamObserver<metochi.ProposeResponse> responseObserver) {

        boolean accepted = false;

        if (optAuthorityNode.isPresent()) {
            boolean pendingVote = optAuthorityNode.get().isPendingVote();
            logger.info("authority node pending vote: " + pendingVote);
            if (!pendingVote) {
                accepted = true;
            }
        }
        else {
            //this is not a proper response - each authority node should connect to a quorum of authority nodes
            //and only authority nodes should be allowed to vote.
            //however to simplify things we just return true to make it easy for a vote proposal to be accepted.
            logger.info("not authority node, so what do I care");
            accepted = true;
        }

        logger.info("response to propose from " + request.getNodeName() + " is " + accepted);
        responseObserver.onNext(ProposeResponse.newBuilder().setAccepted(accepted).build());
        responseObserver.onCompleted();
    }

    //@Override
    public void broadcastTransaction(metochi.Transaction request,
                                     io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {

        logger.info("server received broadcast transaction - adding to chain");
        blockChainManager.addTransaction(request);
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}

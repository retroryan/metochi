package metochi.grpc;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import metochi.*;
import metochi.jwt.Constant;
import org.slf4j.LoggerFactory;

import java.util.Optional;

// TODO Extend gRPC's BroadcastServiceImplBase
public class BroadcastServiceImpl {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(MetochiClient.class.getName());

    private Optional<AuthorityNode> optAuthorityNode;

    private final BlockChainManager blockChainManager;

    public BroadcastServiceImpl(BlockChainManager manager, Optional<AuthorityNode> optAuthorityNode) {
        this.blockChainManager = manager;
        this.optAuthorityNode = optAuthorityNode;
    }

    //TODO - Override the broadcast, queryLatest and queryAll methods here

    //These methods are used when creating a Proof of Authority blockchain

    protected <T> boolean failBecauseNotAuthorityNode(StreamObserver<T> responseObserver) {
        // TODO Retrieve JWT from Constant.JWT_CTX_KEY
        DecodedJWT jwt = Constant.JWT_CTX_KEY.get();
        Claim claim = jwt.getClaim(Constant.IS_AUTHORITY);
        logger.info("failBecauseNotAuthorityNode claim: " + claim);
        if ((!claim.isNull()) && (!claim.asBoolean())) {
            logger.error("failing call because not authority node");
            StatusRuntimeException isNotAnAuthorityNode
                    = new StatusRuntimeException(Status.PERMISSION_DENIED.withDescription("Not an authority node!"));
            responseObserver.onError(isNotAnAuthorityNode);
            return true;
        } else {
            return false;
        }
    }

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

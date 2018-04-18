package metochi.grpc;

import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import metochi.*;
import metochi.jwt.Constant;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlockStreamServiceImpl extends BlockStreamServiceGrpc.BlockStreamServiceImplBase {

    private static final Logger logger = Logger.getLogger(BlockStreamServiceImpl.class.getName());

    //A mapping of room name to the set of stream observers for a room
    private Set<StreamObserver<Block>> blockObservers = new HashSet<>();

    private final PeersManager peersManager;
    private final BlockChainManager blockChainManager;

    public BlockStreamServiceImpl(PeersManager peersManager, BlockChainManager blockChainManager) {
        this.peersManager = peersManager;
        this.blockChainManager = blockChainManager;
    }

    public void streamBlock(Block block) {
        blockObservers.forEach(emptyStreamObserver -> {
            emptyStreamObserver.onNext(block);
        });

    }

    @Override
    public StreamObserver<NewMessage> streamBlock(StreamObserver<Block> responseObserver) {
        final String username = Constant.USER_ID_CTX_KEY.get();

        return new StreamObserver<NewMessage>() {
            @Override
            public void onNext(NewMessage message) {
                switch (message.getType()) {
                    case JOIN:
                        logger.info("joining block observers");
                        blockObservers.add(responseObserver);
                        return;
                    case LEAVE:
                        logger.info("leaving block observers");
                        blockObservers.remove(responseObserver);
                        return;
                    case MESSAGE:
                        //TODO put this on the pending transaction pool
                        logger.info("broadcasting transaction from lightclient");
                        Transaction transaction = Transaction.newBuilder()
                                .setUuid(UUID.randomUUID().toString())
                                .setMessage(message.getMessage())
                                .setSender("lightclient").build();

                        blockChainManager.addTransaction(transaction);
                        peersManager.broadcastMessage(transaction);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                logger.log(Level.SEVERE, "Error in StreamObserver", throwable);
                blockObservers.remove(responseObserver);
            }

            @Override
            public void onCompleted() {
                blockObservers.remove(responseObserver);
            }
        };
    }
}


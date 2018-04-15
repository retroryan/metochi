package metochi;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This manages a group of connections to peer nodes.
 * It stores a list of peers and sends them messages.
 */
public class PeersManager {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(PeersManager.class.getName());

    private ArrayList<BroadcastPeer> broadcastPeers = new ArrayList<>();

    private final String token;

    public PeersManager(String token) {
        this.token = token;
    }

    void addBroadcastPeer(String peerURL) {
        BroadcastPeer newPeer = new BroadcastPeer(peerURL, token);
        broadcastPeers.add(newPeer);
        logger.info("added peer: " + peerURL);
    }

    void queryAll(Consumer<List<Block>> chainConsumer, String nodeURL) {
        logger.info("Querying the chain from peer: " + nodeURL);
        for (BroadcastPeer peer : broadcastPeers) {
            if (peer.getPeerURL().equals(nodeURL)) {
                Blockchain blockchain = peer.queryAll();
                List<Block> chainList = blockchain.getChainList();
                chainConsumer.accept(chainList);
            }
        }
    }

    void broadcastBlock(Block latestBlock, String senderURL) {
        for (BroadcastPeer peer : broadcastPeers) {
            logger.info("Sending latest block to:" + peer.getPeerURL());
            peer.broadcast(latestBlock, senderURL);
        }
    }

    void getAllPeers() {
        for (BroadcastPeer peer : broadcastPeers) {
            logger.info(peer.getPeerURL());
        }
    }

    void broadcastMessage(Transaction transaction) {
        for (BroadcastPeer peer : broadcastPeers) {
            logger.info("sending message to:" + peer.getPeerURL());
            peer.broadcastTransaction(transaction);
        }
    }

    boolean proposeAuthorityRound(String nodeName) {
        logger.info("proposing to all nodes to lead authority round");

        boolean accepted = true;

        for (BroadcastPeer peer : broadcastPeers) {
            logger.info("proposing to:" + peer.getPeerURL());
            ProposeResponse proposeResponse = peer.propose(nodeName);
            if (!proposeResponse.getAccepted()) {
                logger.info(peer.getPeerURL() + " declined vote");
                accepted = false;
            } else {
                logger.info(peer.getPeerURL() + "  accepted vote");
            }
        }
        return accepted;
    }


}

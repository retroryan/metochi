package metochi;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;

class PeersManager {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(PeersManager.class.getName());

    private ArrayList<BroadcastPeer> broadcastPeers = new ArrayList<>();

    private static PeersManager peersManager;

    static PeersManager getInstance() {
        if (peersManager == null) {
            peersManager = new PeersManager();
        }
        return peersManager;
    }

    private PeersManager() {
    }

    void queryAll() {
        logger.info("We have to query the chain from our peer");
        for (BroadcastPeer peer : broadcastPeers) {
            logger.info("getting chain from:" + peer.getPeerURL());
            Blockchain blockchain = peer.queryAll();
            BasicChain.getInstance().replaceChain(blockchain.getChainList());
        }
    }

    void addBroadcastPeer(String peerURL) {
        BroadcastPeer newPeer = new BroadcastPeer(peerURL);
        broadcastPeers.add(newPeer);
        logger.info("added peer: " + peerURL);
    }

    void broadcastLatestBlock(Block latestBlock) {
        for (BroadcastPeer peer : broadcastPeers) {
            logger.info("Sending latest block to:" + peer.getPeerURL());
            peer.broadcast(latestBlock);
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

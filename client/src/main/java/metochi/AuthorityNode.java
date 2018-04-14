package metochi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An Authority Node runs a background thread that proposes every EPOC to run an authority round and generate a new block.
 * Each authority node proposes to run the authority round at approximately the EPOCH time.
 * A slight offset of time hopefully avoids to much overlap.
 */
public class AuthorityNode {

    private static Logger logger = LoggerFactory.getLogger(MetochiClient.class.getName());

    private final long EPOCH = 5000;

    private Semaphore semaphore = new Semaphore(1);

    private final String nodeName;

    private final BlockChainManager blockChainManager;
    private final PeersManager peersManager;

    private ConcurrentMap<Integer, Boolean> voteMap = new ConcurrentHashMap<>();

    public AuthorityNode(String nodeName, BlockChainManager blockChainManager, PeersManager peersManager) {
        this.nodeName = nodeName;
        this.blockChainManager = blockChainManager;
        this.peersManager = peersManager;
    }

    public void start() {
        try {
            Thread authorityThread = new Thread(() -> {
                try {
                    startAuthorityRound();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            authorityThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("authority thread started - returning to normal operation");
    }

    private void startAuthorityRound() throws InterruptedException {
        while (true) {
            long sleepTime = getSleepTime();
            logger.info("authority thread sleeping for " + sleepTime);
            Thread.sleep(sleepTime);
            if (blockChainManager.genesisBlockIsSet()) {
                proposeNextAuthorityRound();
            } else {
                logger.info("no genesis block - going back to sleep");
            }
        }
    }

    /**
     * If the time since the last block is greater than the EPOC then propose the next authority round
     *
     * @throws InterruptedException
     */
    private void proposeNextAuthorityRound() throws InterruptedException {
        long currentTime = BlockChainManager.getNowTimestamp().getSeconds();
        Block latestBlock = blockChainManager.getLatestBlock();
        long latestBlockTime = latestBlock.getTimestamp().getSeconds();
        long timeSinceLastBlock = (currentTime - latestBlockTime);

        if (timeSinceLastBlock > 9) {
            logger.info(nodeName + " proposing authority round");

            try {
                semaphore.acquire();
                boolean authorityRoundApproved = peersManager.proposeAuthorityRound(nodeName);
                if (authorityRoundApproved) {
                    logger.info("authority round was won, generating block");
                    blockChainManager.generateNextBlock("");
                } else {
                    logger.info("authority round was lost, going to sleep");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
            }

        } else {
            logger.info("too soon to create a block, going back to sleep");
        }
    }

    /**
     * Calculate the time to sleep before trying the next authority round.
     * Sleep time is randomized to be close to EPOC, with a small offset to avoid every node proposing an authority round at the same time.
     * Also the time that has elapsed since the last block is subtracted to get the next authority round close to the epoch time.
     *
     * @return
     */
    private long getSleepTime() {
        int randomNum = ThreadLocalRandom.current().nextInt(0, 6);
        long sleepTime = EPOCH + (randomNum * 1000);
        return sleepTime;
    }

    public boolean isPendingVote() {
        boolean acquired = semaphore.tryAcquire();
        if (!acquired) {
            logger.info("semaphore was not acquired, so pending vote");
            return true;
        } else {
            logger.info("semaphore was acquired, so no pending vote");
            semaphore.release();
            return false;
        }
    }

}

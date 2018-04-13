package metochi;

import com.google.protobuf.Timestamp;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * This creates a basic blockchain by creating an array list of blocks of data.
 *
 * It uses a basic consensus algorithm of longest chain to determine which chain from peer nodes is the valid chain.
 *
 * The internal representation is a mutable array list which is not ideal.
 * It should be an immutable append only type list to ensure that it is not accidentally modified.
 *
 */
public class BasicChain {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(BasicChain.class.getName());

    private List<Block> blockchain = new ArrayList<>();

    private static BasicChain basicChain;

    public static BasicChain getInstance() {
        if (basicChain == null) {
            basicChain = new BasicChain();
        }
        return basicChain;
    }

    private final String nodeName;

    private BasicChain() {
        this.nodeName = EnvVars.NODE_NAME;
        blockchain.add(genesisBlock());
    }


    public static Timestamp getNowTimestamp() {
        // uses the calculation from:
        // https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#google.protobuf.Timestamp

        long millis = System.currentTimeMillis();
        return Timestamp.newBuilder().setSeconds(millis / 1000)
                .setNanos((int) ((millis % 1000) * 1000000)).build();
    }

    public static Block genesisBlock() {

        Transaction genesis_block = Transaction.newBuilder()
                .setUuid(UUID.randomUUID().toString())
                .setMessage("Genesis Block")
                .setSender(EnvVars.NODE_NAME)
                .build();

        return Block.newBuilder()
                .setIndex(0)
                .setCreator(EnvVars.NODE_NAME)
                .setHash("816534932c2b7154836da6afc367695e6337db8a921823784c14378abed4f7d7")
                .setPreviousHash("0")
                .setTimestamp(getNowTimestamp())
                .setTxn(genesis_block)
                .build();
    }

    /**
     * This adds the latest block from a peer to the blockchain.
     *
     * It first looks if the new block's previous hash is equal to the latest hash in the existing chain.
     * If not then this chain is missing more than one block, so we need to query for the entire chain.
     *
     * @param peerLatestBlock
     */
    public void addLatestBlock(Block peerLatestBlock) {
        Block latestBlockHeld = getLatestBlock();
        if (peerLatestBlock.getIndex() > latestBlockHeld.getIndex()) {
            logger.info("blockchain is behind. We have latest index: "
                    + latestBlockHeld.getIndex() + " Peer has latest index: " + peerLatestBlock.getIndex());
            if (latestBlockHeld.getHash().equals(peerLatestBlock.getPreviousHash())) {
                if (blockchain.add(peerLatestBlock)) {
                    logger.info("added peer index: " + peerLatestBlock.getIndex() + "  to latest block to chain");
                    PeersManager.getInstance().broadcastLatestBlock(peerLatestBlock);
                } else {
                    logger.info("unexpected error - unable to add peer latest block");
                }
            } else {
                //Query the given peer url for the entire blockchain
            }
        } else {
            // we already have the block, so don't do anything
            // logger.info("received block is already in blockchain. Do nothing");
        }
    }

    public Block getLatestBlock() {
        return blockchain.get(blockchain.size() - 1);
    }

    public Blockchain getBlockchain() {
        return Blockchain.newBuilder().addAllChain(blockchain).build();
    }


    /**
     * This "mines" or generates the next block in the chain.
     *
     * It simply creates a new block with the passed in data and adds it to the chain.
     *
     * @param data
     * @return
     */
    Block generateNextBlock(String data) {
        logger.info("generating next block with data: " + data);

        Block previousBlock = getLatestBlock();
        int nextIndex = previousBlock.getIndex() + 1;
        Timestamp now = getNowTimestamp();

        Transaction nextTransaction = Transaction.newBuilder()
                .setMessage(data)
                .setSender(nodeName)
                .setUuid(UUID.randomUUID().toString())
                .build();

        String nextHash = calculateHash(nextIndex, previousBlock.getHash(), now, nextTransaction);

        Block block = Block.newBuilder()
                .setIndex(nextIndex)
                .setCreator(EnvVars.NODE_NAME)
                .setHash(nextHash)
                .setPreviousHash(previousBlock.getHash())
                .setTimestamp(now)
                .setTxn(nextTransaction)
                .build();

        //logger.info("adding block to: " + blockchain);
        blockchain.add(block);
        logger.info("block added: " + block);

        PeersManager.getInstance().broadcastLatestBlock(block);
        logger.info("generated next block");
        return block;
    }

    static String calculateHashForBlock(Block block) {
        return calculateHash(block.getIndex(), block.getPreviousHash(), block.getTimestamp(), block.getTxn());
    }

    static String calculateHash(Integer index, String prevHash, Timestamp timestamp, Transaction transaction) {
        String hex;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] numBytes = ByteBuffer.allocate(Integer.BYTES).putInt(index).array();
            byte[] prevHashBytes = prevHash.getBytes();
            byte[] timestampBytes = ByteBuffer.allocate(Long.BYTES).putLong(timestamp.getSeconds()).array();
            byte[] dataBytes = transaction.toByteArray();

            int size = numBytes.length + prevHashBytes.length + timestampBytes.length + dataBytes.length;

            ByteBuffer blockBytes = ByteBuffer.allocate(size)
                    .put(numBytes)
                    .put(prevHashBytes)
                    .put(timestampBytes)
                    .put(dataBytes);

            byte[] digest = sha.digest(blockBytes.array());

            // format the digest based on this SO question:
            // https://stackoverflow.com/questions/3103652/hash-string-via-sha-256-in-java
            hex = String.format("%064x", new BigInteger(1, digest));

        } catch (NoSuchAlgorithmException e) {
            hex = "";
            e.printStackTrace();
        }
        return hex;
    }

    static boolean isValidNewBlock(Block newBlock, Block prevBlock) {
        if (prevBlock.getIndex() + 1 != newBlock.getIndex()) {
            // logger.info("invalid index");
            return false;
        } else if (!prevBlock.getHash().equals(newBlock.getPreviousHash())) {
            //logger.info("invalid previous hash");
            return false;
        } else if (!calculateHashForBlock(newBlock).equals(newBlock.getHash())) {
            //logger.info("invalid hash: " + newBlock.getHash() + " expected hash: " + calculateHashForBlock(newBlock));
            return false;
        }
        //logger.info("block index: " + newBlock.getIndex() + " is valid");
        return true;
    }

    /**
     * The validates a chain by ensuring the hashes are valid for each block.
     *
     * @param chainToValidate
     * @return
     */
    static boolean isValidChain(List<Block> chainToValidate) {
        for (Block nextBlock : chainToValidate) {
            if (nextBlock.getIndex() == 0) {
                if (!nextBlock.getHash().equals(genesisBlock().getHash())) {
                    logger.info("invalid genesis block. hash: " + nextBlock.hashCode() + " genesis block hashCode: " + genesisBlock().hashCode());
                    return false;
                }
            } else if (!isValidNewBlock(nextBlock, chainToValidate.get(nextBlock.getIndex() - 1))) {
                logger.info("isValidChain block validation found invalid block");
                return false;
            }
        }
        logger.info("isValidChain validation is true");
        return true;
    }

    /**
     * The simplistic consensus algorithm used for determining which blockchain is the valid blockchain is simply the longest blockchain.
     * <p>
     * This validates if the new blockchain is valid and is longer than the existing one.  If it is then it replaces the old blockchain.
     *
     * @param newBlocks
     * @return
     */
    public boolean replaceChain(List<Block> newBlocks) {
        int oldBlockChainSize = blockchain.size();
        int newBlockChainSize = newBlocks.size();
        logger.info("size of new blockchain: " + newBlockChainSize + " my blockchain size: " + oldBlockChainSize);
        if (isValidChain(newBlocks)) {
            if (newBlocks.size() > oldBlockChainSize) {
                logger.info("Received blockchain is longer. Replacing current blockchain with received blockchain");
                //the new blocks chain is an immutable collection, so we need to copy it
                blockchain = new ArrayList<>(newBlocks);

                /**
                 We need to read and publich all transactions not yet seen on the new block chain
                 An example calculation:

                 old size = 3
                 new size = 5
                 start index = 5 - ((5-3)) => 3


                 old size = 1
                 new size = 2
                 start index = 2 - ((2-1)) => 1

                 */

                int startReadIndex = newBlockChainSize - ((newBlockChainSize - oldBlockChainSize));
                logger.info("dumping transactions starting at index: " + startReadIndex);


                for (int index = startReadIndex; index < newBlockChainSize; index++) {
                    Block nextBlock = blockchain.get(index);
                    outputBlockTransactions(nextBlock);
                }

                return true;
            } else {
                logger.info("Received blockchain is not longer, not replacing.");
                return false;
            }
        } else {
            logger.info("Received blockchain invalid");
            return false;
        }
    }

    private void outputBlockTransactions(Block nextBlock) {
        System.out.println("New Messages from block: " + nextBlock.getIndex());
/*
        nextBlock.getTxnList().forEach(transaction -> {
            if (!transaction.getSender().equals(nodeName))
                System.out.println(transaction);
        });
*/
    }

    private AtomicInteger saveCnt = new AtomicInteger();

    public String saveBlockchain(String nodeName) {
        String blockchainFileName = nodeName + saveCnt.getAndIncrement() + ".blocks";
        try {
            PrintWriter writer = new PrintWriter(blockchainFileName, "UTF-8");
            for (Block block : blockchain) {
                writer.println(block.toString());
            }
            writer.close();
        } catch (IOException io) {
            io.printStackTrace();
        }
        return blockchainFileName;
    }
}

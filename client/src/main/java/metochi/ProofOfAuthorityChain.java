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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * *  The golden genesis block should be created by the lead node.
 * *  This is done by checking if this is the lead node and broadcasting the genesis block.
 * *  All other nodes will generate a genesis block but they will be overwritten.
 * *  For the lead node, this will get the genesis block and broadcast it to other nodes.
 * <p>
 * The primary difference from the basic chain is that this maintains a list of transactions that are included when a new block is "mined" or created.
 */
public class ProofOfAuthorityChain implements BlockChainManager {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(ProofOfAuthorityChain.class.getName());

    private final PeersManager peersManager;
    private List<Block> blockchain = new ArrayList<>();
    private final String nodeURL;
    private final String nodeName;
    private AtomicBoolean genesisBlockSet = new AtomicBoolean();


    //  The list of transactions is included in each new block created when this node is elected to be the authority.
    //  The complication arises when the transaction list needs to be reset,
    //  ideally there would be a way to be sure we are not currently adding new transactions that might be lost.
    //  However adding a semaphore or lock to all transactions results in deadlock situations when used with the
    //  authority node voting process.
    private Map<UUID, Transaction> transactions = new HashMap<>();

    ProofOfAuthorityChain(PeersManager peersManager, String nodeName, String nodeURL) {
        this.peersManager = peersManager;
        this.nodeName = nodeName;
        this.nodeURL = nodeURL;
    }

    @Override
    public Block createGenesisBlock() {

        Transaction genesis_block = Transaction.newBuilder()
                .setMessage("Genesis Block")
                .setSender(nodeName).build();

        //TODO POA - Add Genesis Block
        Block block = Block.newBuilder()
                .setIndex(0)
                .setCreator(nodeName)
                .setHash("816534932c2b7154836da6afc367695e6337db8a921823784c14378abed4f7d7")
                .setPreviousHash("0")
                .setTimestamp(BlockChainManager.getNowTimestamp())
                .addTxn(genesis_block)
                .build();

        blockchain.add(block);
        genesisBlockSet.set(true);
        return block;
    }

    @Override
    public boolean genesisBlockIsSet() {
        return genesisBlockSet.get();
    }

    /**
     * This adds the latest block from a peer to the blockchain.
     * <p>
     * It first looks if the new block's previous hash is equal to the latest hash in the existing chain.
     * If not then this chain is missing more than one block, so we need to query for the entire chain.
     *
     * @param peerLatestBlock
     */
    @Override
    public void addLatestBlock(Block peerLatestBlock, String sender) {
        logger.info("add latest block");
        //if this is the genesis block then set this as our genesis block
        //we should validate this block before adding it --- eventually
        if ((!genesisBlockSet.get()) && (peerLatestBlock.getIndex() == 0)) {
            addGenesisBlock(peerLatestBlock);
        } else {
            addTailBlock(peerLatestBlock, sender);

        }
    }

    private void addGenesisBlock(Block peerLatestBlock) {
        logger.info("setting genesis block");
        blockchain.add(peerLatestBlock);
        genesisBlockSet.set(true);
        peersManager.broadcastBlock(peerLatestBlock, nodeURL);
    }

    /**
     * Add a tail block (everything after the genesis block
     *
     * @param peerLatestBlock
     * @param sender
     */
    private void addTailBlock(Block peerLatestBlock, String sender) {
        Block latestBlockHeld = getLatestBlock();
        //if this block is farther in the chain then the latest block we have then add it to this chain
        if (peerLatestBlock.getIndex() > latestBlockHeld.getIndex()) {
            logger.info("blockchain is behind. Current index: "
                    + latestBlockHeld.getIndex() + " Peer index: " + peerLatestBlock.getIndex());
            if (latestBlockHeld.getHash().equals(peerLatestBlock.getPreviousHash())) {
                if (blockchain.add(peerLatestBlock)) {
                    logger.info("added peer index: " + peerLatestBlock.getIndex() + "  to latest block to chain");
                    peersManager.broadcastBlock(peerLatestBlock, nodeURL);

                    //TODO POA - Print out new transactions where added by this block
                    if (peerLatestBlock.getTxnList().size() > 0) {
                        removeExistingTransactions(peerLatestBlock);
                        outputBlockTransactions(peerLatestBlock);
                    }

                } else {
                    logger.info("unexpected error - unable to add peer latest block");
                }
            } else {
                logger.info("replacing entire chain from: " + sender);
                peersManager.queryAll(newBlocks -> replaceChain(newBlocks), sender);
            }
        } else if (!peerLatestBlock.getHash().equals(latestBlockHeld.getHash())) {
            logger.info("peer latest block hash does not match - syncing chain from: " + sender);
            peersManager.queryAll(newBlocks -> replaceChain(newBlocks), sender);
        }
    }


    @Override
    public Block getLatestBlock() {
        return blockchain.get(blockchain.size() - 1);
    }

    @Override
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
    @Override
    public Block generateNextBlock(String data) {
        logger.info("generating next block");

        Block previousBlock = getLatestBlock();
        int nextIndex = previousBlock.getIndex() + 1;
        Timestamp now = BlockChainManager.getNowTimestamp();
        Collection<Transaction> latestTransactions = transactions.values();
        String nextHash = calculateHash(nextIndex, previousBlock.getHash(), now, latestTransactions);

        //TODO POA - Add all of the pending transactions to the new block
        Block block = Block.newBuilder()
                .setIndex(nextIndex)
                .setCreator(nodeURL)
                .setHash(nextHash)
                .setPreviousHash(previousBlock.getHash())
                .setTimestamp(now)
                .addAllTxn(latestTransactions)
                .build();

        blockchain.add(block);
        logger.info("new block added to chain: " + block);

        peersManager.broadcastBlock(block, nodeURL);

        if (latestTransactions.size() > 0) {
            System.out.println("Messages added to new block: " + block.getIndex());
            latestTransactions.forEach(transaction -> {
                if (!transaction.getSender().equals(nodeName)) {
                    System.out.println(transaction);
                }
            });
        }
        logger.info("reset pending transaction list");
        transactions = new HashMap<>();

        return block;
    }

    /**
     * The simplistic consensus algorithm used for determining which blockchain is the valid blockchain is simply the longest blockchain.
     *
     * This validates if the new blockchain is valid and is longer than the existing one.  If it is then it replaces the old blockchain.
     *
     * @param newBlockchain
     * @return
     */
    @Override
    public boolean replaceChain(List<Block> newBlockchain) {
        int oldBlockChainSize = blockchain.size();
        int newBlockChainSize = newBlockchain.size();
        logger.info("size of new blockchain: " + newBlockChainSize + " my blockchain size: " + oldBlockChainSize);
        if (isValidChain(newBlockchain)) {
            if (newBlockchain.size() > oldBlockChainSize) {
                logger.info("Received blockchain is longer. Replacing current blockchain with received blockchain");
                //the new blocks chain is an immutable collection, so we need to copy it
                blockchain = new ArrayList<>(newBlockchain);

                /**
                 We need to read and publich all transactions not yet seen on the new block chain
                 An example calculation:

                 old size = 3
                 new size = 5
                 start index = 5 - ((5-3)) => 3

                 old size = 1
                 new size = 2
                 start index = 2 - ((2-1)) => 1
                 **/

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

    public void addTransaction(Transaction txn) {
        logger.info("adding transaction to list of pending transactions");
        transactions.put(UUID.fromString(txn.getUuid()), txn);
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

    static String calculateHashForBlock(Block block) {
        // TODO POA - Calculate the hash for the block
        return calculateHash(block.getIndex(), block.getPreviousHash(), block.getTimestamp(), block.getTxnList());
    }

    static String calculateHash(Integer index, String prevHash, Timestamp timestamp, Collection<Transaction> txnList) {
        String hex;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] numBytes = ByteBuffer.allocate(Integer.BYTES).putInt(index).array();
            byte[] prevHashBytes = prevHash.getBytes();
            byte[] timestampBytes = ByteBuffer.allocate(Long.BYTES).putLong(timestamp.getSeconds()).array();

            //TODO - replace with the real byte array
            byte[] dataBytes = "TXN LIST".getBytes();

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
    boolean isValidChain(List<Block> chainToValidate) {
        for (Block nextBlock : chainToValidate) {
            if (nextBlock.getIndex() == 0) {
                Block genesisBlock = blockchain.get(0);
                if (!nextBlock.getHash().equals(genesisBlock.getHash())) {
                    logger.info("invalid genesis block. hash: " + nextBlock.hashCode() + " genesis block hashCode: " + genesisBlock.hashCode());
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

    private void outputBlockTransactions(Block nextBlock) {
        System.out.println("New Messages from block: " + nextBlock.getIndex());

        //TODO POA - Output the transactions from the given block
        //for simplicity just do a system println, which will show up in the command window
        nextBlock.getTxnList().forEach(transaction -> {
            if (!transaction.getSender().equals(nodeName))
                System.out.println(transaction);
        });
    }

    private void removeExistingTransactions(Block peerLatestBlock) {
        //TODO POA - Remove existing transactions from the given block.  That is transactions we already have seen.
        logger.info("removing transactions received in latest block ");
        peerLatestBlock.getTxnList().forEach(nxtPeerTxn -> {
            transactions.remove(UUID.fromString(nxtPeerTxn.getUuid()));
        });
    }






}

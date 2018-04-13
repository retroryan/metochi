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

public class ProofOfAuthorityChain {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(ProofOfAuthorityChain.class.getName());

    private List<Block> blockchain = new ArrayList<>();

    private static ProofOfAuthorityChain proofOfAuthorityChain;

    public static ProofOfAuthorityChain getInstance() {
        if (proofOfAuthorityChain == null) {
            proofOfAuthorityChain = new ProofOfAuthorityChain();
        }
        return proofOfAuthorityChain;
    }

    //  The list of transactions is included in each new block created when this node is elected to be the authority.
    //  The complication arises when the transaction list needs to be reset,
    //  ideally there would be a way to be sure we are not currently adding new transactions that might be lost.
    //  However adding a semaphore or lock to all transactions results in deadlock situations when used with the
    //  authority node voting process.
    private Map<UUID, Transaction> transactions = new HashMap<>();

    private final String nodeName;

    private ProofOfAuthorityChain() {
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


        Transaction genesis_block = Transaction.newBuilder().setMessage("Genesis Block").setSender(EnvVars.NODE_NAME).build();

        //TODO POA - Add Genesis Block
        return Block.newBuilder()
                .setIndex(0)
                .setCreator(EnvVars.NODE_NAME)
                .setHash("816534932c2b7154836da6afc367695e6337db8a921823784c14378abed4f7d7")
                .setPreviousHash("0")
                .setTimestamp(getNowTimestamp())
                // .addTxn(genesis_block)
                .build();
    }

    public void addLatestBlock(Block peerLatestBlock) {
        Block latestBlockHeld = getLatestBlock();
        if (peerLatestBlock.getIndex() > latestBlockHeld.getIndex()) {
            logger.info("blockchain is behind. We have latest index: "
                    + latestBlockHeld.getIndex() + " Peer has latest index: " + peerLatestBlock.getIndex());
            if (latestBlockHeld.getHash().equals(peerLatestBlock.getPreviousHash())) {
                if (blockchain.add(peerLatestBlock)) {
                    logger.info("added peer index: " + peerLatestBlock.getIndex() + "  to latest block to chain");
                    PeersManager.getInstance().broadcastLatestBlock(peerLatestBlock);

                    //TODO POA - Print out new transactions where added by this block
/*
                    if (peerLatestBlock.getTxnList().size() > 0) {
                        removeExistingTransactions(peerLatestBlock);
                        outputBlockTransactions(peerLatestBlock);
                    }
*/

                } else {
                    logger.info("unexpected error - unable to add peer latest block");
                }
            } else {
                PeersManager.getInstance().queryAll();
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
        Blockchain returnBlockchain = Blockchain.newBuilder().addAllChain(blockchain).build();
        return returnBlockchain;
    }


    Block generateNextBlock() {
        logger.info("generating next block");

        Block previousBlock = getLatestBlock();
        int nextIndex = previousBlock.getIndex() + 1;
        Timestamp now = getNowTimestamp();
        Collection<Transaction> latestTransactions = transactions.values();
        String nextHash = calculateHash(nextIndex, previousBlock.getHash(), now, latestTransactions);

        //TODO POA - Add all of the pending transactions to the new block
        Block block = Block.newBuilder()
                .setIndex(nextIndex)
                .setCreator(EnvVars.NODE_NAME)
                .setHash(nextHash)
                .setPreviousHash(previousBlock.getHash())
                .setTimestamp(now)
                //.addAllTxn(latestTransactions)
                .build();

        blockchain.add(block);
        logger.info("block added: " + block);

        PeersManager.getInstance().broadcastLatestBlock(block);

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

    static String calculateHashForBlock(Block block) {
        //TODO POA - Calculate the hash for the block
        //return calculateHash(block.getIndex(), block.getPreviousHash(), block.getTimestamp(), block.getTxnList());

        return "";
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

    private void outputBlockTransactions(Block nextBlock) {
        System.out.println("New Messages from block: " + nextBlock.getIndex());

        //TODO POA - Output the transactions from the given block

        //for simplicity just do a system println, which will show up in the command window
/*
        nextBlock.getTxnList().forEach(transaction -> {
            if (!transaction.getSender().equals(nodeName))
                System.out.println(transaction);
        });
*/
    }

    public void addTransaction(Transaction txn) {

        logger.info("adding transaction to list of pending transactions");
        transactions.put(UUID.fromString(txn.getUuid()), txn);
    }

    private void removeExistingTransactions(Block peerLatestBlock) {

        //TODO POA - Remove existing transactions from the given block.  That is transactions we already have seen.
/*
        logger.info("removing transactions received in latest block ");
        peerLatestBlock.getTxnList().forEach(transaction -> {
            transactions.remove(UUID.fromString(transaction.getUuid()));
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

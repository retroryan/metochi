package metochi;

import com.google.protobuf.Timestamp;

import java.util.List;
import java.util.Optional;

public interface BlockChainManager {
    static Timestamp getNowTimestamp() {
        // uses the calculation from:
        // https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#google.protobuf.Timestamp

        long millis = System.currentTimeMillis();

        return Timestamp.newBuilder().setSeconds(millis / 1000)
                .setNanos((int) ((millis % 1000) * 1000000)).build();
    }

    Block createGenesisBlock();
    boolean genesisBlockIsSet();
    void addLatestBlock(Block peerLatestBlock, String sender);
    Block getLatestBlock();
    Blockchain getBlockchain();
    Block generateNextBlock(String data);
    boolean replaceChain(List<Block> newBlocks);
    void addTransaction(Transaction txn);
    String saveBlockchain(String nodeName);
}

/*
 * Copyright 2017 Grand Cloud, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package metochi;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jline.console.ConsoleReader;
import metochi.grpc.BroadcastServiceImpl;

import java.io.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main Metochi Client that configures and runs the client, server and prompt.
 * <p>
 * Because Metochi is completely decentralized each node runs both a server and a client.
 */
public class MetochiClient {

    private static Logger logger = LoggerFactory.getLogger(MetochiClient.class.getName());
    private final ConsoleReader console = new ConsoleReader();
    private final String nodeName;
    private final String nodeURL;

    private Config config;

    private final BlockChainManager blockChainManager;
    private final PeersManager peersManager;

    public MetochiClient(BlockChainManager blockChainManager, String nodeName, String nodeURL, Config config, PeersManager peersManager) throws IOException {
        this.blockChainManager = blockChainManager;
        this.nodeName = nodeName;
        this.nodeURL = nodeURL;
        this.config = config;
        this.peersManager = peersManager;
    }

    public static void main(String[] args) throws Exception {

        String nodeName = EnvVars.NODE_NAME;
        Config config = Config.loadProperties(nodeName);
        logger.info("starting with config: " + config);

        //This manages the list of peer nodes that this node connects to
        PeersManager peersManager = new PeersManager();

        String nodeURL = config.hostname + ":" + config.port;
        logger.info("metochi node is starting on url: " + nodeURL);

        BlockChainManager blockChainManager = initBlockchainManager(nodeName, config, peersManager, nodeURL);

        MetochiClient client = new MetochiClient(blockChainManager, nodeName, nodeURL, config, peersManager);


        Optional<AuthorityNode> optionalAuthorityNode = Optional.empty();
        if (config.isAuthorityNode) {
            AuthorityNode authorityNode = new AuthorityNode(nodeName, blockChainManager, peersManager);
            authorityNode.start();
            optionalAuthorityNode = Optional.of(authorityNode);
        }

        client.initServer(blockChainManager, optionalAuthorityNode);

        // TODO - uncomment init peers to connect this node to other nodes in the network
        client.initPeers(config.leadNode);

        if (config.enableRandomMessage) {
            RandomMessageGenerator randomMessageGenerator = new RandomMessageGenerator(blockChainManager, peersManager);
            randomMessageGenerator.startGenerator(nodeName);
        }
        client.prompt();
    }

    /**
     *  The Blockchain Manager used will start as a basic chain for the first part of the exercises.
     *  When the exercises change to use Proof of Authority this needs to be changed to use ProofOfAuthorityChain
     *
     * @param nodeName
     * @param config
     * @param peersManager
     * @param nodeURL
     * @return
     */
    private static BlockChainManager initBlockchainManager(String nodeName, Config config, PeersManager peersManager, String nodeURL) {

        //BlockChainManager blockChainManager = new BasicChain(peersManager, nodeName, nodeURL);

        // TODO POA - switch to using the ProofOfAuthorityChain
        BlockChainManager blockChainManager = new ProofOfAuthorityChain(peersManager, nodeName, nodeURL);

        if (config.leadNode) {
            logger.info("lead node - creating genesis");
            blockChainManager.createGenesisBlock();
        }
        return blockChainManager;
    }

    /**
     * Start the server for this node.  This is in the client class because each node is a client and a server.
     *
     * @throws IOException
     */
    private void initServer(BlockChainManager blockChainManager, Optional<AuthorityNode> optionalAuthorityNode) throws IOException {

        // TODO Use ServerBuilder to create a new Server instance. Start it, and await termination.

        BroadcastServiceImpl broadcastService = new BroadcastServiceImpl(blockChainManager, optionalAuthorityNode);
        final Server server = ServerBuilder.forPort(config.port)
                .addService(broadcastService)
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.shutdownNow();
            }
        });

        server.start();
        logger.info("Server Started on port: " + config.port);
    }

    /**
     * Initialize the connection to the peer nodes after a delayed period.
     * The delay allows time for the peers to start before trying to connect.
     */
    private void initPeers(boolean leadNode) {
        //wait for configured time for peers to start before trying to connect
        try {
            logger.info("sleeping for " + config.startDelay + " seconds to wait for peers to start");
            Thread.sleep((config.startDelay * 1000));
            logger.info("connecting to peers");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (String peerURL : config.peerUrls) {
            peersManager.addBroadcastPeer(peerURL);
        }

        if (leadNode) {
            logger.info("lead node - broadcasting genesis");
            Block genesisBlock = blockChainManager.getBlockchain().getChain(0);
            peersManager.broadcastBlock(genesisBlock, nodeURL);
        }
    }

    private void prompt() throws Exception {
        console.println("Press Ctrl+D or Ctrl+C to quit");

        while (true) {
            try {
                readPrompt();
            } catch (Exception e) {
                e.printStackTrace();
                shutdown();
            }
        }
    }

    /**
     * Create a basic command line prompt to allow the user to interact with the client through the commandline
     *
     * @throws IOException
     */
    private void readPrompt() throws IOException {
        String prompt = "message | /blocks | /addpeer [data] | /getpeers | /quit\n-> ";

        String line = console.readLine(prompt).trim();
        if (line.startsWith("/")) {
            processCommand(line);
        } else if (!line.isEmpty()) {
            //in the first phase just send this message locally
            //later during Proof of Authority change this to broadcast the message
            // TODO POA - switch to using broadcastMsg
            broadcastMsg(line);
        }
    }

    /**
     * Process the command line prompt
     *
     * @param line
     */
    private void processCommand(String line) {
        String[] splitLine = line.split(" ");
        String command = splitLine[0];
        if (splitLine.length >= 2) {
            String data = splitLine[1];
            //process any commands that require data
        } else {
            if (command.equalsIgnoreCase("/blocks") || command.equalsIgnoreCase("/b")) {
                logger.info("getting blocks");
                String blockchainFileName = saveBlockchain(nodeName);
                System.out.println("blockchain written to file: " + blockchainFileName);
            } else if (command.equalsIgnoreCase("/getpeers") || command.equalsIgnoreCase("/g")) {
                peersManager.getAllPeers();
            } else if (command.equalsIgnoreCase("/quit") || command.equalsIgnoreCase("/q") || command.equalsIgnoreCase("q")) {
                shutdown();
            }
        }
    }

    /**
     * Mining the data generates the next block in the blockchain.
     * <p>
     * This is only used in the early stages of the sample when there is not an authority round.
     *
     * @param data
     */
    private void sendMsg(String data) {
        Block block = blockChainManager.generateNextBlock(data);
        System.out.println("Generated block: " + block);
    }

    /**
     * After the proof of authority rounds and transactions are added to the sample, then this method is used to send messages.
     * <p>
     * Send a message by adding it to the list of this nodes transactions and broadcasting it to other peers in the network.
     *
     * @param data
     */
    private void broadcastMsg(String data) {
        UUID uuid = UUID.randomUUID();
        Transaction transaction = Transaction.newBuilder()
                .setUuid(uuid.toString())
                .setMessage(data)
                .setSender(nodeName).build();
        blockChainManager.addTransaction(transaction);
        peersManager.broadcastMessage(transaction);
    }


    private String saveBlockchain(String nodeName) {
        return blockChainManager.saveBlockchain(nodeName);
    }

    private void shutdown() {
        logger.info("Exiting metochi client");
        System.exit(1);
    }
}

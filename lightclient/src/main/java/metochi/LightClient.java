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

import com.auth0.jwt.algorithms.Algorithm;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import jline.console.ConsoleReader;
import metochi.grpc.BlockStreamServiceImpl;
import metochi.grpc.BroadcastServiceImpl;

import java.io.*;
import java.util.*;

import metochi.jwt.Constant;
import metochi.jwt.JwtAuthService;
import metochi.jwt.JwtServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main Metochi Client that configures and runs the client, server and prompt.
 * <p>
 * Because Metochi is completely decentralized each node runs both a server and a client.
 */
public class LightClient {

    private static Logger logger = LoggerFactory.getLogger(LightClient.class.getName());
    private final ConsoleReader console = new ConsoleReader();

    private BlockStreamServiceGrpc.BlockStreamServiceStub blockStreamService;
    private StreamObserver<NewMessage> newMessageStreamObserver;

    public LightClient() throws IOException {

    }

    public static void main(String[] args) throws Exception {
        logger.info("starting lightclient");
        LightClient client = new LightClient();
        client.initBlockStreamService();
        client.prompt();
    }

    public void initBlockStreamService() {
        logger.info("init block stream");
        ManagedChannel blockStreamChannel = ManagedChannelBuilder.forTarget("chat-service:9002")
                .usePlaintext(true)
                .build();

        blockStreamService = BlockStreamServiceGrpc.newStub(blockStreamChannel);

        newMessageStreamObserver = blockStreamService.streamBlock(new StreamObserver<Block>() {
            @Override
            public void onNext(Block block) {

                System.out.println("received block index: " + block.getIndex());
                block.getTxnList().forEach(transaction -> {
                    System.out.println("transaction = " + transaction);
                });
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });

        logger.info("joining block stream");
        newMessageStreamObserver.onNext(NewMessage.newBuilder()
                .setType(MessageType.JOIN)
                .setMessage("")
                .build());

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

    private void broadcastMsg(String line) {
        logger.info("broadcasting message");
        newMessageStreamObserver.onNext(NewMessage.newBuilder()
                .setType(MessageType.MESSAGE)
                .setMessage(line)
                .build());
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
            if (command.equalsIgnoreCase("/leave") || command.equalsIgnoreCase("/l")) {
                logger.info("leaving stream");
            } else if (command.equalsIgnoreCase("/quit") || command.equalsIgnoreCase("/q") || command.equalsIgnoreCase("q")) {
                shutdown();
            }
        }
    }

    private void shutdown() {
        logger.info("Exiting metochi client");
        System.exit(1);
    }
}

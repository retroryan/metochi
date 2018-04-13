package metochi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

class RandomMessageGenerator {

    private static Logger logger = LoggerFactory.getLogger(MetochiClient.class.getName());

    public static void startGenerator(String nodeName) {

        Thread randoThread = new Thread(() -> {
            try {
                logger.info("starting random message generator");
                sendRandomMessages(nodeName);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        randoThread.start();

    }

    private static void sendRandomMessages(String nodeName) throws IOException, InterruptedException {
        ArrayList<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("data/all-shakespeare.txt"))) {
            logger.info("learning shakespear ...");
            String nxtLine = br.readLine().trim();
            while (nxtLine != null) {
                if (!nxtLine.isEmpty())
                    lines.add(nxtLine);
                nxtLine = br.readLine();
            }
            int max = lines.size() - 1;
            logger.info("read shakespear with " + max + " lines.");

            while (true) {
                int randLineNum = ThreadLocalRandom.current().nextInt(0, max);
                String nxtMsg = lines.get(randLineNum);
                sendMsg(nxtMsg, nodeName);
                int randSleep = ThreadLocalRandom.current().nextInt(5, 15);
                Thread.sleep(randSleep*1000);
            }
        }
    }

    private static void sendMsg(String data, String nodeName) {
        UUID uuid = UUID.randomUUID();
        Transaction transaction = Transaction.newBuilder()
                .setUuid(uuid.toString())
                .setMessage(data)
                .setSender(nodeName).build();

        //BasicChain.getInstance().addTransaction(transaction);
        //PeersManager.getInstance().broadcastMessage(transaction);
        
    }
}

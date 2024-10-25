package baccarat.server;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.Collections;

public class ServerMain {

    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.err.println("Incorrect number of arguments provided");
            System.exit(-1);
        }

        int port = Integer.parseInt(args[0]);
        int numDeck = Integer.parseInt(args[1]);

        saveDeck(numDeck);

        resetGameHistory();

        // create threadpool
        ExecutorService executor = Executors.newFixedThreadPool(5);

        // open server socket
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {

                // wait and accept connection
                System.out.printf("Awaiting connection on port %d\n", port);
                Socket sock = server.accept();
                System.out.println("Accepted a connection!");

                // give baccarat engine the socket to carry out server logic
                BaccaratEngine be = new BaccaratEngine(sock, numDeck);

                // send the baccarat engine (the thread) to work
                executor.submit(be);

            }
        } catch (IOException e) {
            System.out.println("Error starting server: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

    }

    public static void saveDeck(int numDeck) throws IOException {

        String fileName = CONSTANTS.CARDS_FILENAME;

        // open writer
        FileWriter fw = new FileWriter(fileName);
        BufferedWriter bw = new BufferedWriter(fw);

        // create deck of cards
        List<String> cards = new ArrayList<>();

        for (int i = 0; i < numDeck; i++) {
            for (int value = 1; value <= 13; value++) {
                for (int suit = 1; suit <= 4; suit++) {
                    String card = value + "." + suit;
                    cards.add(card);
                }
            }
        }

        // shuffle list
        Collections.shuffle(cards);

        // write cards to cards.db
        cards.forEach(
                c -> {
                    try {
                        bw.write(c + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        bw.flush();
        bw.close();
    }

    public static void resetGameHistory() {

        try (BufferedWriter br = new BufferedWriter(new FileWriter(CONSTANTS.GAME_HISTORY_FILENAME))) {
            br.write("");
            System.out.println("Game history has been reset");
            
        } catch (IOException ex) {
            System.out.println("Error resetting game history: " + ex.getMessage());
        }
    }
}

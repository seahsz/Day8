package baccarat.server;

import java.net.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class BaccaratEngine implements Runnable {

    private Socket sock;
    private int numDeck;
    private int bet;
    private String user;
    private int bal;
    private List<String> cards = new ArrayList<>();

    private String chose;
    private int player;
    private int banker;
    private int playerScore;
    private int bankerScore;

    private List<Integer> playerDrawn = new ArrayList<>();
    private List<Integer> bankerDrawn = new ArrayList<>();

    private static List<String> gameHistory = new ArrayList<>();

    private String winner;

    public BaccaratEngine(Socket sock, int numDeck) {
        this.sock = sock;
        this.numDeck = numDeck;
    }

    public void run() {

        try (
                BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));) {
            // wait for input from client
            String input;
            boolean end = false;

            while (!end) {
                input = br.readLine();

                if (input.startsWith("end"))
                    end = true;

                String[] terms = input.toLowerCase().trim().split(CONSTANTS.DELIMIT);

                if (terms[0].equalsIgnoreCase(CONSTANTS.LOGIN)) {

                    login(terms);
                } else if (terms[0].equalsIgnoreCase(CONSTANTS.BET)) {

                    int temp = Integer.parseInt(terms[1]);
                    if (temp > bal) {
                        bw.write("Insufficient amount to place bet");
                    } else {
                        this.bet = temp;
                        System.out.printf("bet updated for %s to %d\n", user, bet);
                    }
                } else if (terms[0].equalsIgnoreCase(CONSTANTS.DEAL)) {
                    if (terms.length == 2) {

                        // reset cards drawn list
                        playerDrawn = new ArrayList<>();
                        bankerDrawn = new ArrayList<>();

                        // determine who was chosen
                        chose = terms[1];

                        // banker, player total
                        this.banker = 0;
                        this.player = 0;

                        draw();

                        // send drawn cards to client
                        StringBuilder sb = new StringBuilder();
                        sb.append("P|");
                        for (int i = 0; i < this.playerDrawn.size(); i++) {
                            sb.append(this.playerDrawn.get(i));
                            if (i == this.playerDrawn.size() - 1)
                                sb.append(",");
                            else
                                sb.append("|");
                        }

                        sb.append("B|");
                        for (int i = 0; i < this.bankerDrawn.size(); i++) {
                            sb.append(this.bankerDrawn.get(i));
                            if (i != this.bankerDrawn.size() - 1)
                                sb.append("|");
                        }

                        bw.write(sb.toString() + "\n");
                        bw.flush();

                        // determine winner
                        determineWinner(player, banker);
                        updateBal(bal);

                        // update game history
                        synchronized (gameHistory) {
                            if (gameHistory.size() == 6) {
                                writeHistory();
                                gameHistory.clear();
                            }
                            gameHistory.add(this.winner);
                        }
                        System.out.println(gameHistory);
                    } else {
                        bw.write("deal invalid" + "\n");
                        bw.flush();
                    }

                }
            }

        } catch (IOException e) {
            e.getMessage();
            e.printStackTrace();
        } finally {
            try {
                sock.close();
            } catch (Exception e) {
                System.err.println("Unable to close sock");
            }
        }

    }

    public void login(String[] terms) throws IOException {

        user = terms[1];
        bal = Integer.parseInt(terms[2]);

        // creates user.db if file does not exist
        File dir = new File(CONSTANTS.USER_DB_DIRECTORY);
        dir.mkdir();

        File file = new File(CONSTANTS.USER_DB_DIRECTORY + "/" + user + ".db");
        file.createNewFile();

        // update bal
        updateBal(bal);
    }

    public int obtainValue(String card) {

        String[] c = card.split("\\.");
        int value;

        if (c[0].equals("11") || c[0].equals("12") || c[0].equals("13"))
            value = 10;
        else
            value = Integer.parseInt(c[0]);
        return value;
    }

    public void draw() {

        // check deck size
        readDeck();

        if (cards.size() < 6) {
            System.out.println("Deck has insufficient cards");
            newDeck();
            readDeck();
        }

        // initial deal (2 card player, 2 card banker) - drawing takes from start of
        // list
        for (int i = 0; i < 4; i++) {

            // draw and remove card from deck
            String card = cards.get(i);
            cards.remove(i);
            int cardValue = obtainValue(card);

            if (i == 0 || i == 2) {
                player += cardValue;
                this.playerDrawn.add(cardValue);
            } else if (i == 1 || i == 3) {
                banker += cardValue;
                this.bankerDrawn.add(cardValue);
            }
        }

        // determine if either needs to draw
        if (player <= 15) {
            String card = cards.get(0);
            cards.remove(0);
            int cardValue = obtainValue(card);
            player += cardValue;
            this.playerDrawn.add(cardValue);
        }

        if (banker <= 15) {
            String card = cards.get(0);
            cards.remove(0);
            int cardValue = obtainValue(card);
            banker += cardValue;
            this.bankerDrawn.add(cardValue);

        }

        writeDeck();

        System.out.println("Number of cards left in deck: " + cards.size());

    }

    public void determineWinner(int player, int banker) {
        playerScore = player % 10;
        bankerScore = banker % 10;

        if (playerScore > bankerScore & chose.equalsIgnoreCase("p")) {
            this.bal += bet;
            winner = "P";
        } else if (playerScore > bankerScore & chose.equalsIgnoreCase("b")) {
            this.bal -= bet;
            winner = "P";
        } else if (bankerScore > playerScore & chose.equalsIgnoreCase("b")) {
            winner = "B";
            if (bankerScore == 6)
                this.bal += bet / 2; // half pay out if banker wins on 6
            else
                this.bal += bet;
        } else if (bankerScore > playerScore & chose.equalsIgnoreCase("p")) {
            this.bal -= bet;
            winner = "B";
        } else
            winner = "T";

    }

    public void updateBal(int balance) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CONSTANTS.USER_DB_DIRECTORY + "/" + user + ".db"))) {
            bw.write(balance + "\n");
            bw.flush();

        } catch (Exception ex) {
            System.err.println("Unable to update balance" + ex.getMessage());
        }
    }

    public void readDeck() {

        // reset the List --> to repopulate
        this.cards = new ArrayList<>();

        // open file reader
        try (BufferedReader brFile = new BufferedReader(new FileReader(CONSTANTS.CARDS_FILENAME))) {
            // read file
            String line;

            while ((line = brFile.readLine()) != null) {
                cards.add(line);
            }
        } catch (IOException e) {
            System.err.println("Unable to read file" + e.getMessage());
        }

    }

    public void newDeck() {

        String fileName = CONSTANTS.CARDS_FILENAME;

        // open writer
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {

            // create deck of cards
            List<String> newCards = new ArrayList<>();

            for (int i = 0; i < numDeck; i++) {
                for (int value = 1; value <= 13; value++) {
                    for (int suit = 1; suit <= 4; suit++) {
                        String card = value + "." + suit;
                        newCards.add(card);
                    }
                }
            }

            // shuffle list
            Collections.shuffle(cards);

            // write cards to cards.db
            newCards.forEach(
                    c -> {
                        try {
                            bw.write(c + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            bw.flush();

            System.out.printf("%d new deck of cards shuffled\n", numDeck);

        } catch (IOException e) {
            System.err.println("Unable to write new deck" + e.getMessage());
        }

    }

    public void writeDeck() {

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CONSTANTS.CARDS_FILENAME))) {
            cards.forEach(
                    c -> {
                        try {
                            bw.write(c + "\n");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            System.err.println("Unable to update deck" + e.getMessage());
        }
    }

    public void writeHistory() {
        synchronized (gameHistory) {
            try (BufferedWriter csvWriter = new BufferedWriter(new FileWriter(CONSTANTS.GAME_HISTORY_FILENAME, true))) {
                csvWriter.write(String.join(",", gameHistory));
                csvWriter.newLine();

            } catch (IOException e) {
                System.err.println("Error when writing game history" + e.getMessage());
            }
        }
    }

}

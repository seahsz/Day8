package baccarat.client;

import java.net.*;

import baccarat.server.CONSTANTS;

import java.io.*;

public class ClientMain {

    public static void main(String[] args) throws UnknownHostException, IOException {

        if (args.length != 1) {
            System.err.println("Incorrect number of arguments provided");
            System.exit(-1);
        }

        String[] terms = args[0].split(":");
        String host = terms[0];
        int port = Integer.parseInt(terms[1]);

        // connect to server
        System.out.println("Waiting to connect");
        Socket sock = new Socket(host, port);
        System.out.println("Connected!");

        // open console
        Console cons = System.console();

        // send commands to server
        sendReadCommands(cons, sock);

        // close
        sock.close();

    }

    public static void sendReadCommands(Console cons, Socket sock) throws IOException {

        // obtain input from user
        String input;
        boolean end = false;

        OutputStream os = sock.getOutputStream();
        Writer writer = new OutputStreamWriter(os);
        BufferedWriter bw = new BufferedWriter(writer);

        InputStream is = sock.getInputStream();
        Reader reader = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(reader);

        while (!end) {
            input = cons.readLine("Enter command>>");

            input = input.trim().toLowerCase().replaceAll(" ", "|");

            if (input.startsWith("end"))
                end = true;

            bw.write(input + "\n");
            bw.flush();

            if (input.startsWith(CONSTANTS.DEAL)) {

                String line;
                line = br.readLine();
                System.out.println(line);

                if (!line.equals("deal invalid")) {
                    determineWinner(line);

                } else {
                    System.out.println("Please input only deal p or deal b");
                }

            }

        }

        bw.close();
        br.close();
    }

    public static void determineWinner(String line) {
        String[] terms = line.split(",");

        int playerTotal = 0;
        int bankerTotal = 0;

        String[] valueP = terms[0].split("\\|");
        for (int i = 0; i < valueP.length; i++) {
            if (i != 0)
                playerTotal += Integer.parseInt(valueP[i]);
        }

        String[] valueB = terms[1].split("\\|");
        for (int i = 0; i < valueB.length; i++) {
            if (i != 0)
                bankerTotal += Integer.parseInt(valueB[i]);
        }

        int playerScore = playerTotal % 10;
        int bankerScore = bankerTotal % 10;

        if (playerScore > bankerScore)
            System.out.printf("Player wins with %d points\n", playerScore);
        else if (bankerScore > playerScore)
            System.out.printf("Banker wins with %d points\n", bankerScore);
        else
            System.out.printf("Both player and banker has %d, bets refunded\n", bankerScore);

    }

}

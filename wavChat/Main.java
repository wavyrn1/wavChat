package wavChat;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
	    Server serverInstance = null;
	    int port = -1;
        // get and validate port to bind to
	    Scanner scanner = new Scanner(System.in);
        System.out.printf("Port: ");
        try {
            port = scanner.nextInt();
        }
        catch(Exception e) {
            System.err.printf("Invalid port number. Exiting.\n");
            return;
        }
        // establish server and run it
        try {
            serverInstance = new Server(port);
        }
        catch(IOException e) {
            System.err.printf("Error establishing a server on port %d. Exiting.\n", port);
            return;
        }

        serverInstance.start();

        // listen for server-side commands
        String line = "";
        while(!serverInstance.isClosed()) {
            line = scanner.nextLine();
            if("exit".equals(line.toLowerCase().replace("\n", ""))) {
                serverInstance.close();
            }
        }

    }
}

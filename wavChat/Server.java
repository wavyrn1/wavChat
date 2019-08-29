package wavChat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * The Server class connects to and manages clients
 */
public class Server extends Thread {
    private ServerSocket serv; // serv is the serversocket that the clients will connect to
    private ArrayList<Thread> handlers; // list of connected clients
    private boolean isClosed; // variable to keep track of server status

    /**
     *
     * @param port Port to host the server on
     * @throws IOException If unable to bind to the port
     */
    public Server(int port) throws IOException {
        this.serv = new ServerSocket(port);
        this.handlers = new ArrayList();
    }

    /**
     * This method is the thread's main body
     */
    @Override
    public void run() {
        Socket currentSocket; // socket for the newest client to join
        ClientHandler currentHandler; // the newest client to join
        // while the server isn't closed

        // accepts clients and starts their handlers, updating the clients after a new client joins
        while(!this.isClosed) {
            try {
                // accept any possible connection and convert it to a client handler
                currentSocket = this.serv.accept();
                currentHandler = new ClientHandler(currentSocket);
            }
            catch(IOException e) {
                if(this.isClosed()) {
                    break;
                }
                System.err.printf("Error connecting to a client. Continuing.\n");
                continue;
            }
            // add the handler to our list, start it, and update every client's peer list
            this.handlers.add(currentHandler);
            this.update();
            currentHandler.start();
        }
    }

    /**
     * Closes the server
     */
    public void close() {
        // set isClosed flag and closes the handlers and serversocket
        this.isClosed = true;
        for(Thread eachThread : this.handlers) {
            ((ClientHandler) eachThread).close();
        }
        try {
            this.serv.close();
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates this server's handler list and each handler's peer list
     */
    private void update() {
        // remove all closed handlers
        for(Thread eachThread : this.handlers) {
            if(((ClientHandler) eachThread).isClosed()) {
                this.handlers.remove(eachThread);
            }
        }
        // update all handlers that are left
        for(Thread eachThread : this.handlers) {
            ((ClientHandler) eachThread).update(this.handlers);
        }
    }

    /**
     * Checks if this handler is closed
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return this.isClosed;
    }
}

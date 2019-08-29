package wavChat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Represents each client's connection to the server, as well as the backend representation of this user
 */
public class ClientHandler extends Thread {
    private Socket sock; // Socket to the client
    private DataInputStream dis; // client io
    private DataOutputStream dos; // client io
    private ArrayList<ClientHandler> peers; // all the other handlers
    private ArrayList<String> inbox; // the messages we havent read
    private boolean isClosed; // if this handler is closed
    private String username; // user's name
    private boolean closedGracefully; // if this handler closed gracefully

    /**
     *
     * @param sock Socket connecting to the client
     * @throws IOException if there is an error establishing IO with the socket
     */
    public ClientHandler(Socket sock) throws IOException {
        this.sock = sock;
        this.dis = new DataInputStream(this.sock.getInputStream());
        this.dos = new DataOutputStream(this.sock.getOutputStream());
        this.peers = new ArrayList();
        this.inbox = new ArrayList();
        this.isClosed = false;
        this.username = this.readLine();
        this.closedGracefully = false;
    }

    /**
     * Reads one line from the client
     * @return one line from the client
     */
    private String readLine() {
        String message = "";
        char c;
        try {
            while((c = (char) this.dis.readByte()) != '\n') {
                message += c;
            }
        }
        catch(IOException e) {
            this.close();
        }
        return message;
    }

    /**
     * Sends a message to the client
     * @param message the message to send to the client
     */
    private void writeLine(String message) {
        message = message.replace("\n", "") + "\n";
        try {
            this.dos.writeBytes(message);
        }
        catch(IOException e) {
            this.close();
        }
    }

    /**
     * Closes this handler
     */
    public void close() {
        if(!this.closedGracefully) {
            for(ClientHandler eachPeer : this.peers) {
                if(eachPeer.equals(this)) {
                    continue;
                }
                this.closedGracefully = true;
                eachPeer.sendMessageTo(String.format("%s errored out of the channel!", this.username));
            }
        }
        this.isClosed = true;
        try {
            this.dis.close();
            this.dos.close();
            this.sock.close();
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @return true if the handler is closed, false otherwise
     */
    public boolean isClosed() {
        return this.isClosed;
    }

    /**
     * Handles client requests
     * @param line a request read from the client
     * @throws MalformedRequestException if the request is malformed
     */
    private void handleRequest(String line) throws MalformedRequestException {
        Request request = new Request(line);
        // instantiate a request object to parse the request
        switch(request.getRequestType()) {
            case GET: // sends a message from the user's inbox, or "empty" if none exists
                if(this.inbox.size() == 0) {
                    this.writeLine("empty");
                    break;
                }
                String message = this.inbox.get(0);
                this.inbox.remove(0);
                this.writeLine(message);
                break;
            case EXIT: // closes the handler
                for(ClientHandler eachPeer : this.peers) {
                    if(eachPeer.equals(this)) {
                        continue;
                    }
                    eachPeer.sendMessageTo(String.format("%s has left the channel!", this.username));
                    this.closedGracefully = true;
                }
                this.close();
                break;
            case SEND: // sends the string contained in args to all of our peers
                if("".equals(request.getArgs())) {
                    throw new MalformedRequestException();
                }
                for(ClientHandler eachHandler : this.peers) {
                    if(eachHandler.equals(this)) {
                        continue;
                    }
                    eachHandler.sendMessageTo(String.format("%s: %s\n", this.username, request.getArgs()));
                }
                break;
            case USERS: // sends a list of users in the channel to the client
                String users = "";
                for(ClientHandler eachHandler : this.peers) {
                    users += eachHandler.getUsername();
                    users += ", ";
                }
                users = users.substring(0, users.length() - 2);
                this.writeLine(users);
                break;
            case PM: // sends a message to the user specified, or an error message back to the client if the user doesnt exist
                if("".equals(request.getArgs())) {
                    throw new MalformedRequestException();
                }
                String[] args = request.getArgs().split(" ");
                if(args.length < 2) {
                    throw new MalformedRequestException();
                }
                ClientHandler destination = this.getPeerByName(args[0]);
                if(destination == null) {
                    this.writeLine(String.format("User %s does not exist.", args[0]));
                    break;
                }
                String pm = "";
                for(int i = 1; i < args.length; i++) {
                    pm += args[i];
                    pm += " ";
                }
                pm = pm.substring(0, pm.length() - 1);
                destination.sendMessageTo(String.format("~[%s]: %s\n", this.username, pm));
                break;

        }
    }

    /**
     * Sends a message to this user's inbox
     * @param message The message to send to the user
     */
    public void sendMessageTo(String message) {
        this.inbox.add(message);
    }

    /**
     * Gets the username of this client
     * @return the username of this client
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Main method of the thread, ensures that the client's peers list is kept updated, recieves and handles requests from the client, and then responds accordingly.
     */
    @Override
    public void run() {
        for(ClientHandler eachPeer : this.peers) {
            if(eachPeer.equals(this)) {
                continue;
            }
            eachPeer.sendMessageTo(String.format("%s has joined the channel!", this.username));
        }
        String line;
        while(!this.isClosed) {
            line = this.readLine();

            try {
                for (ClientHandler eachPeer : this.peers) {
                    if (eachPeer.isClosed()) {
                        try{eachPeer.stop();}catch(Exception e){};
                        this.peers.remove(eachPeer);
                    }
                }
            }
            catch(Exception e) {}

            try {
                this.handleRequest(line);
            }
            catch(MalformedRequestException e) {
                this.writeLine("Malformed Request.");
            }
        }

    }

    /**
     * Updates the peers list for this handler
     * @param peers new peers list
     */
    public void update(ArrayList<Thread> peers) {
        // converts the Thread arraylist to a ClientHandler one and saves it.
        ArrayList<ClientHandler> buffer = new ArrayList();
        for(Thread eachThread : peers) {
            buffer.add((ClientHandler) eachThread);
        }
        this.peers = buffer;
    }

    /**
     * Get a peer by their name, or null if none exists
     * @param name name of peer
     * @return either the named peer, or null
     */
    private ClientHandler getPeerByName(String name) {
        // checks each peer to see if their username equals name
        for(ClientHandler eachHandler : this.peers) {
            if(eachHandler.getUsername().equals(name)) {
                return eachHandler;
            }
        }
        return null;
    }
}

package wavChat;

/**
 * The Request class refers to requests sent from the client to the server.
 * format: "<command> <args>\n"
 */
public class Request {
    public enum Type {GET, SEND, USERS, EXIT, PM}; // different valid request types
    private Type requestType; // the type of this request
    private String args; // the arguments supplied to the request

    /**
     *
     * @param line The request recieved from the user
     * @throws MalformedRequestException if the request doesn't follow the right format
     */
    public Request(String line) throws MalformedRequestException {
        line = line.replace("\n", "");
        String[] fields = line.split(" ");
        String command = fields[0];
        this.args = "";

        // propagate args

        try {
            for (int i = 1; i < fields.length; i++) {
                this.args += fields[i];
                this.args += " ";
            }
        }
        catch(Exception e) {
            this.args = "";
        }

        // set request type

        if("get".equals(command)) {
            this.requestType = Type.GET;
        }
        else if("send".equals(command)) {
            this.requestType = Type.SEND;
        }
        else if("users".equals(command)) {
            this.requestType = Type.USERS;
        }
        else if("exit".equals(command)) {
            this.requestType = Type.EXIT;
        }
        else if("pm".equals(command)) {
            this.requestType = Type.PM;
        }
        else {
            throw new MalformedRequestException();
        }
    }

    /**
     *
     * @return the type of this request.
     */
    public Type getRequestType() {
        return this.requestType;
    }

    /**
     *
     * @return the args of this request.
     */
    public String getArgs() {
        return this.args;
    }
}

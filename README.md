# wavChat
  Multi-Threaded Chat Service in Java

# How to install
  Either run compile.sh, or, if that doesn't work for whatever reason, just compile wavChat/Main with javac.

# How to connect
  I haven't written a client yet, but that is the next project. However, you can connect with netcat <hostname> <port>. The first message sent is registered as your username.
  
  After you connect, only valid commands will be accepted as input.
  
# Commands

  get: sends either the next unread message, or "empty" if none is found, to the client
  send <message>: sends <message> to the server
  users: sends a list of online users to the client
  pm <user> <message>: sends <message> to <user> or sends an error message to the client
  exit: exits the server

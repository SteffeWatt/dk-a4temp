package no.ntnu.datakomm.chat;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TCPClient {
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private Socket connection;
    
    //Adding the different streams
    private InputStreamReader inputStreamReader;
    private OutputStreamWriter outputStreamWriter;
    
    //The List of valid commands
    private static final List<String> validCommands = Arrays.asList("login", "async", "sync", "msg", "privmsg", "inbox", "help", "users");
    //The different server's response commands
    private static final String cmdLoginOk = "loginok";
    private static final String cmdLoginError = "loginerr";
    private static final String cmdUsers = "users";
    private static final String cmdMsgOk = "msgok";
    private static final String cmdMsgError = "msgerr";
    private static final String cmdMsg = "msg";
    private static final String cmdMsgPrivate = "privmsg";
    private static final String cmdError = "cmderr";

    // Hint: if you want to store a message for the last error, store it here
    private String lastError = null;

    private final List<ChatListener> listeners = new LinkedList<>();

    /**
     * Connect to a chat server.
     *
     * @param host host name or IP address of the chat server
     * @param port TCP port of the chat server
     * @return True on success, false otherwise
     */
    public boolean connect(String host, int port) {
        // Step 1: implement this method
        // Hint: Remember to process all exceptions and return false on error
        // Hint: Remember to set up all the necessary input/output stream variables
        
        try {
            //Connecting to the socket of the server
            connection = new Socket(host, port);
            
            //retrieving the input stream from the socket
            inputStreamReader = new InputStreamReader(connection.getInputStream());
            //retrieving the output stream from the socket
            outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
            
            //Setting up the reader to the input stream
            fromServer = new BufferedReader(inputStreamReader);
            //Setting up the writer to the output stream
            toServer = new PrintWriter(outputStreamWriter);
            
            log("Connection Success");
            
        } catch (IOException ioException) {

            System.out.println("ERROR: Something went wrong when connecting to the server");
            ioException.printStackTrace();
            return false;
        }
        
        return true;
    }

    /**
     * Close the socket. This method must be synchronized, because several
     * threads may try to call it. For example: When "Disconnect" button is
     * pressed in the GUI thread, the connection will get closed. Meanwhile, the
     * background thread trying to read server's response will get error in the
     * input stream and may try to call this method when the socket is already
     * in the process of being closed. with "synchronized" keyword we make sure
     * that no two threads call this method in parallel.
     */
    public synchronized void disconnect() {
        // Step 4: implement this method
        // Hint: remember to check if connection is active
        
        try {
            
            //Closing the socket
            if (isConnectionActive()) {
                connection.close();
            }
            
            //Closing the Input StreamReader
            if (inputStreamReader != null) {
                inputStreamReader.close();
            }
            
            //Closing the Output StreamReader
            if (outputStreamWriter != null) {
                outputStreamWriter.close();
            }
            
            //Closing the buffReader
            if (fromServer != null) {
                fromServer.close();
            }
            
            //Closing the buffWriter
            if (toServer != null) {
                toServer.close();
            }
            
            //notify the listeners that connection is closed
            onDisconnect();
            
            log("Disconnect Success");
            
        } catch (IOException ioException) {
            System.out.println("ERROR: Something went wrong when disconnecting from the server");
            ioException.printStackTrace();
        }
        
    }

    /**
     * @return true if the connection is active (opened), false if not.
     */
    public boolean isConnectionActive() {
        
        //Checks if there is/has been a successful connection 
        // and check that the socket is not closed.
        if (connection != null) {
            if (connection.isConnected() && !connection.isClosed()) {
                return true;

            } else return false;
        } else return false;
    }

    /**
     * Send a command to server.
     *
     * @param cmd A command. It should include the command word and optional attributes, according to the protocol.
     * @return true on success, false otherwise
     */
    private boolean sendCommand(String cmd) {
        // Step 2: Implement this method
        // Hint: Remember to check if connection is active
        
        //A list of all valid commands
        //List<String> validCommands = Arrays.asList("login", "async", "sync", "msg", "privmsg", "inbox", "help", "users");
        
        
        //Check if the connection is active
        if (isConnectionActive()) {
            
            //Check if the command is valid
            if (validCommands.contains(cmd)) {
                
                //write the command to the writer
                toServer.write(cmd);
                //Return true since the command is valid and written down
                log("Command: " + cmd);
                return true;
                
            } else {
                
                log("ERROR: The command is not valid");
                return false;
            }
            
        } else {
            log("ERROR: The Connection is NOT active");
            return false;
        }

    }

    
    /**
     * Send a public message to all the recipients.
     *
     * @param message Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPublicMessage(String message) {
        // Step 2: implement this method
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        
        //Checks if the connection is active
        if (isConnectionActive()) {
            
            //Send the command "msg"
            if (sendCommand("msg")) {
                
                //Write the message to the server
                toServer.write(" " + message);

                //Prints/Sends the messages as a single line
                toServer.println();

                //flushes the printWriter
                toServer.flush();

                //Document the progress
                log("Sending to Server: " + "msg " + message);

                //Return true since the message is sent
                return true;
            } else {
                lastError = "Error: Command was not valid";
                return false;
            }
            
        } else {
            lastError = "Error: Failed to send a public messages";
            return false;
        }

    }

    /**
     * Send a login request to the chat server.
     *
     * @param username Username to use
     */
    public void tryLogin(String username) {
        // Step 3: implement this method
        // Hint: Reuse sendCommand() method
        
        if (isConnectionActive()) {

            //Send the command
            if (sendCommand("login")) {
                
                //writs the username to the server
                toServer.write(" " + username);
                //Sends the messages to the server
                toServer.println();
                //flush the writer
                toServer.flush();

                log("Sending to Server: login " + username);
            }

        } else {
            
            log("Error: The login command was not sent");
        }
    }

    /**
     * Send a request for latest user list to the server. To get the new users,
     * clear your current user list and use events in the listener.
     */
    public void refreshUserList() {
        // Step 5: implement this method
        // Hint: Use Wireshark and the provided chat client reference app to find out what commands the
        // client and server exchange for user listing.
        
        //The ip was 142.93.163.244
        //There is a command called "users"
        //the client sends "users" to the server and receives "users <list of users>"
        //<list of users> is a username followed by a new username, there is only a space between each username
        
        
        if (sendCommand("users")) {
            toServer.println();
            toServer.flush();
        }
        
        //Clearing the user lists
        onUsersList(new String[0]);
    }

    /**
     * Send a private message to a single recipient.
     *
     * @param recipient username of the chat user who should receive the message
     * @param message   Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPrivateMessage(String recipient, String message) {
        // Step 6: Implement this method
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.

        //how the message need to look: privmsg <recipient> <message>
        //Example: privmsg Harald Hello!

        String cmd = "privmsg";

        //Checks if the connection is active
        if (isConnectionActive()) {

            //Send the command "privmsg"
            if (sendCommand(cmd)) {

                //Send the <recipient> with a space in front
                toServer.write(" " + recipient);

                //Send the message with a space in front
                toServer.write(" " + message);

                //ends the line and sends the message
                toServer.println();

                //flushes the printWriter
                toServer.flush();

                //log
                log("Sending to Server: " + cmd + " " + recipient + " " + message);

                //Returning true since the message has been sent
                return true;

            } else {
                lastError = "Error: Command was not valid, when sending a private msg";
                return false;
            }
        } else {
            lastError = "Error: Failed to send private msg";
            return false;
        }
    }


    /**
     * Send a request for the list of commands that server supports.
     */
    public void askSupportedCommands() {
        // TODO Step 8: Implement this method
        // Hint: Reuse sendCommand() method
    }


    /**
     * Wait for chat server's response
     *
     * @return one line of text (one command) received from the server
     */
    private String waitServerResponse() {
        // Step 3: Implement this method
        // Step 4: If you get I/O Exception or null from the stream, it means that something has gone wrong
        // with the stream and hence the socket. Probably a good idea to close the socket in that case.
        
        //Is true when the client have received a response
        boolean response = false;
        
        //A String containing the msg from the server
        String msgFromServer = null;
        
        //Check if the connection to the server is open.
        if (isConnectionActive()) {
            
            //When there is no response then keep waiting and checking every second. 
            while (!response) {

                try {
                    //Waiting for the msg form the server
                    //The code will wait here for a response
                    
                    if (isConnectionActive()) msgFromServer = fromServer.readLine(); {
                        //msgFromServer = fromServer.readLine();

                        if (msgFromServer != null && !msgFromServer.isEmpty()) {

                            log("Server: " + msgFromServer);
                            //When you have a response stop the while loop
                            response = true;
                        }
                    }
                    
                } catch (IOException e) {
                    //closing the socket because something have gone wrong with the socket
                    if (isConnectionActive()) {
                        disconnect();
                    }
                }
            }
        } else {
            disconnect();
        }

        return msgFromServer;
    }

    /**
     * Get the last error message
     *
     * @return Error message or "" if there has been no error
     */
    public String getLastError() {
        if (lastError != null) {
            return lastError;
        } else {
            return "";
        }
    }

    /**
     * Start listening for incoming commands from the server in a new CPU thread.
     */
    public void startListenThread() {
        // Call parseIncomingCommands() in the new thread.
        Thread t = new Thread(() -> {
            parseIncomingCommands();
        });
        t.start();
    }

    /**
     * Read incoming messages one by one, generate events for the listeners. A loop that runs until
     * the connection is closed.
     */
    private void parseIncomingCommands() {
        
        //The different serve commands
        

        while (isConnectionActive()) {
            // Step 3: Implement this method
            // Hint: Reuse waitServerResponse() method
            // Hint: Have a switch-case (or other way) to check what type of response is received from the server
            // and act on it.
            // Hint: In Step 3 you need to handle only login-related responses.
            // Hint: In Step 3 reuse onLoginResult() method
            
            String msgFromServer = waitServerResponse();
            String serverCommand; //The command from the server ("loginok, loginerr ...")
            String[] msgFromServerTwoParts = null;
            
            //Retrieving the command
            if (msgFromServer.contains(" ")) {
                //Splitting the msg to view the command alone
                msgFromServerTwoParts = msgFromServer.split(" ", 2);
                serverCommand = msgFromServerTwoParts[0];
            } else {
                //If there is no space in the msg that it only contains the command
                serverCommand = msgFromServer;
            }
            
            log("Handling msg from server as command: " + serverCommand);
            
            //handles response "loginok"
            if (serverCommand.equals(cmdLoginOk)) {
                
                onLoginResult(true, null);
            }
            
            
            //handles response "loginerr <error message>"
            if (serverCommand.equals(cmdLoginError)) {
                
                //Checks if there is an error msg
                if (msgFromServerTwoParts != null) {
                    onLoginResult(false, msgFromServerTwoParts[1]);
                    
                    //if not then there is no error msg
                } else {
                    onLoginResult(false, null);
                }
            }
            

            // Step 5: update this method, handle user-list response from the server
            // Hint: In Step 5 reuse onUserList() method
            
            //handle response users <usernames>
            if (serverCommand.equals(cmdUsers)) {
                
                //Example: "users name1 name3 user4"
                
                //Split msg from the server
                String[] userListParts = new String[0];
                if (msgFromServerTwoParts != null) {
                    //retrieving the msg after the command and splitting it
                    userListParts = msgFromServerTwoParts[1].split(" ");
                }
                
                //Sends the list to onUsersList
                onUsersList(userListParts);
            }
            
            // Step 7: add support for incoming chat messages from other users (types: msg, privmsg)
            // Step 7: add support for incoming message errors (type: msgerr)
            // Step 7: add support for incoming command errors (type: cmderr)
            // Hint for Step 7: call corresponding onXXX() methods which will notify all the listeners
            
            //handles responses "msg"
            if (serverCommand.contains(cmdMsg)) {
                
                //String[] msgPartsArray = msgFromServer.split(" ", 2);       
                
                //msgCommand can be msgok, msgerr, msg or privmsg 
                //String msgCommand = msgPartsArray[0];
                
                //handles "msgok"
                if (serverCommand.equals(cmdMsgOk)) {
                    
                    //The message sent was approved by the server
                    //There is nothing that need to be done.
                }
                
                
                //handles "msgerr <error description>"
                if (serverCommand.equals(cmdMsgError)) {
                    
                    //Retrieving the error message and try to notify the listeners
                    onMsgError(msgFromServerTwoParts[1]);
                    lastError = "Something went wrong with the last private/Global message sent from this client";
                    
                }
                
                //handles "msg <sender> <message>" and
                //handles "privmsg <sender> <message>"
                if ((serverCommand.equals(cmdMsg) || serverCommand.equals(cmdMsgPrivate)) 
                        && msgFromServerTwoParts != null) {
                    
                    //Note: msgFromServerTwoParts[1] = <sender> <message>
                    String[] msgSmallerParts = msgFromServerTwoParts[1].split(" ", 2);
                    
                    //Note: msgSmallerParts[<sender>, <message>]
                    String msgSender = msgSmallerParts[0];
                    String msgMessage = msgSmallerParts[1];


                    //Updating the new message
                    if (serverCommand.equals(cmdMsg)) {
                        
                        //Global msg
                        onMsgReceived(false, msgSender, msgMessage);
                        
                    } else { //The only other option is that the command is "privmsg"
                        
                        //private msg
                        onMsgReceived(true, msgSender, msgMessage);
                    }
                }
            }

            //handles response "cmderr"
            if (serverCommand.equals(cmdError) && msgFromServerTwoParts != null) {
                
                //Retrieving the error message and
                //try to notify the listeners
                onCmdError(msgFromServerTwoParts[1]);
            }

            
            
            

            // TODO Step 8: add support for incoming supported command list (type: supported)

        }
    }

    /**
     * Register a new listener for events (login result, incoming message, etc)
     *
     * @param listener
     */
    public void addListener(ChatListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister an event listener
     *
     * @param listener
     */
    public void removeListener(ChatListener listener) {
        listeners.remove(listener);
    }

    /**
     * Log a message to the system console.
     *
     * @param message The message to be logged (printed).
     */
    private static void log(String message) {
        String threadId = "THREAD #" + Thread.currentThread().getId() + ": ";
        System.out.println(threadId + message);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The following methods are all event-notificators - notify all the listeners about a specific event.
    // By "event" here we mean "information received from the chat server".
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Notify listeners that login operation is complete (either with success or
     * failure)
     *
     * @param success When true, login successful. When false, it failed
     * @param errMsg  Error message if any
     */
    private void onLoginResult(boolean success, String errMsg) {
        for (ChatListener l : listeners) {
            l.onLoginResult(success, errMsg);
        }
    }

    /**
     * Notify listeners that socket was closed by the remote end (server or
     * Internet error)
     */
    private void onDisconnect() {
        // Step 4: Implement this method
        // Hint: all the onXXX() methods will be similar to onLoginResult()
        
        for (ChatListener chatListener : listeners) {
            chatListener.onDisconnect();
        }
    }

    /**
     * Notify listeners that server sent us a list of currently connected users
     *
     * @param users List with usernames
     */
    private void onUsersList(String[] users) {
        // Step 5: Implement this method
        
        for (ChatListener chatListener : listeners) {
            chatListener.onUserList(users);
        }
    }

    /**
     * Notify listeners that a message is received from the server
     *
     * @param priv   When true, this is a private message
     * @param sender Username of the sender
     * @param text   Message text
     */
    private void onMsgReceived(boolean priv, String sender, String text) {
        // Step 7: Implement this method
        
        //Creates the msg as its own class
        TextMessage textMessage = new TextMessage(sender, priv, text);
        
        log("Recived the msg: " + textMessage.toString());
        
        //Notify the listeners
        for (ChatListener chatListener : listeners) {
            
            chatListener.onMessageReceived(textMessage);
        }
        
    }

    /**
     * Notify listeners that our message was not delivered
     *
     * @param errMsg Error description returned by the server
     */
    private void onMsgError(String errMsg) {
        // Step 7: Implement this method

        //Notify the listeners
        for (ChatListener chatListener : listeners) {
            //tells the listeners the error msg
            chatListener.onMessageError(errMsg);
        }
    }

    /**
     * Notify listeners that command was not understood by the server.
     *
     * @param errMsg Error message
     */
    private void onCmdError(String errMsg) {
        // Step 7: Implement this method
        // Notify the listeners
        for (ChatListener chatListener : listeners) {
            //tells the listeners the error msg
            chatListener.onCommandError(errMsg);
        }
    }

    /**
     * Notify listeners that a help response (supported commands) was received
     * from the server
     *
     * @param commands Commands supported by the server
     */
    private void onSupported(String[] commands) {
        // TODO Step 8: Implement this method
    }
}

package org.example;
import java.io.*;
import java.net.*;
import org.ietf.jgss.*;

public class serverGSS {

    private static int portnumber = 11115;
    //what does this mean? service principle name
    //is this the right serviceName?
    private static String serviceName = "testuser@DC.SHADOWMOVE.COM";

    private static GSSName name         = null;
    //GSSCredential contains info for the context initiation or acceptance
    private static GSSCredential cred   = null;
    //instance of the default GSSManager subclass is created, which in turn implement the Name, Credential, and Context interfaces
    private static GSSManager mgr = GSSManager.getInstance();

    

    //removed String args from this method's params
    public void run() throws Exception 
    {
        System.out.println("Starting GSS-API Server Example"); 

        /* set up a shutdown hook to release GSSCredential storage
           when the user terminates the server with Ctrl+C */
        //when program exits normally, or in response to a user interrupt, the shutdown hook is started
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    cred.dispose(); //release system resources held by the credential when no longer needed
                } catch (GSSException e) {
                    System.out.println("Couldn't free GSSCredential storage"); // if it didn't work
                }
                System.out.println("Freed GSSCredential storage"); // successful disposal
            }
        });
        
        
        //actual client server interaction
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        OutputStream clientOut = null;
        InputStream clientIn = null;

        /* create server socket */
        try {
            serverSocket = new ServerSocket(portnumber);
            System.out.println("serverSocket created");
        } catch (IOException e) {
            System.out.println("Error on port: " + portnumber + ", " + e);
            System.exit(1); //exit if creating serverSocket doesn't work
        }

        /* set up GSS-API name, credential */
        //creating a name object for the entity
        //default = manager.createName("myusername", GSSName.NT_USER_NAME);
        //is the NT_USER_NAME correct?
        System.out.println("creating name");
        name = mgr.createName(serviceName, GSSName.NT_HOSTBASED_SERVICE);
        System.out.println("name created");
        //acquire credentials for the entity
        //default = manager.createCredential(name, GSSCredential.ACCEPT_ONLY);
        //added parameter GSSCredential.INDEFINITE_LIFETIME allows max permitted lifetime for credential to remain valid when initiating security context
        //added param (Oid)null = Object Identification used to store security mechanisms and name types
        //Oid krb5Oid = new Oid("1.2.840.113554.1.2.2"); // temp Oid to test
        System.out.println("creating credential");
        cred = mgr.createCredential(name, GSSCredential.INDEFINITE_LIFETIME,
                (Oid) null /*krb5Oid*/, GSSCredential.ACCEPT_ONLY);
        System.out.println("credential created");

        while(true)
        {
            byte[] inToken = null;
            byte[] outToken = null;
            byte[] buffer;
            int err = 0;

            GSSName peer;
            //constructor sets the desired privacy state
            MessageProp supplInfo = new MessageProp(true);

            try {
                System.out.println("waiting for client connection");
                clientSocket = serverSocket.accept(); //same as previous s = server.accept() method
                
                /* get input and output streams */
                clientOut = clientSocket.getOutputStream(); //returns output stream for the clientSocket
                clientIn = clientSocket.getInputStream();
               
                /* establish context with client */
                GSSContext context = mgr.createContext(cred);
                
                /* read message sent by the client */
                inToken = Util.ReadToken(clientIn);
                System.out.println("Received token from client...");

                /* unwrap the message */
                //Used by the peer application to process tokens generated with the wrap call.
                buffer = context.unwrap(inToken, 0, inToken.length, supplInfo);
                System.out.println("Message received from client: (new String(buffer))");

                
                supplInfo.setPrivacy(true);     // privacy requested
                supplInfo.setQOP(0);            // default QOP

                /* produce a signature block for the message */
                //Returns a token containing a cryptographic Message Integrity Code (MIC) for the supplied message
                buffer = context.getMIC(buffer, 0, buffer.length, supplInfo);

                /* send signature block to client */
                err = GssUtil.WriteToken(clientOut, buffer);
                if (err == 0) {
                    System.out.println("Sent sig block to client...");
                } else {
                    System.out.println("Error sending sig block to client...");
                }

                //closing
                context.dispose();

                //dealing with errors
            } catch (IOException e) {
                System.out.println("Server did not accept connection: " + e);
            } catch (GSSException e) {
                System.out.println("GSS-API Error: " + e.getMessage());
            }

            //closing down
            clientOut.close();
            clientIn.close();
            clientSocket.close();
        }
    }
}
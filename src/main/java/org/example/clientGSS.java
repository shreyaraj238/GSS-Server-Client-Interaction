package org.example;
import java.io.*;
import java.net.*;
import org.ietf.jgss.*;

public class clientGSS {

    /* set these to match your environment and principal names */
    private static int port = 11115;
    //what should these be set to?
    private static String server = "ssh.dc.shadowmove.com";
    private static String serviceName = "host/ssh.dc.shadowmove.com";
    private static String clientPrincipal = "testuser";

    private static GSSCredential clientCred = null;
    private static GSSContext context       = null;
    private static GSSManager mgr = GSSManager.getInstance();
   
    /* using null to request default mech, krb5 */ 
    private static Oid mech                 = null;
        
    private static Socket clientSocket      = null;
    private static OutputStream serverOut   = null;
    private static InputStream serverIn     = null;

    public void run() throws Exception  {
       
        System.out.println("Starting GSS-API Client Example\n"); 
       
        connectToServer();
        initializeGSS();
        establishContext(serverIn, serverOut);
        doCommunication(serverIn, serverOut);

        /* shutdown */
        context.dispose();
        clientCred.dispose();
        serverIn.close();
        serverOut.close();
        clientSocket.close();

        System.out.println("\nShut down GSS-API and closed connection to server");
    }

    /**
     * Connect to example GSS-API server, using specified port and 
     * service name.
     **/
    //creates socket and input output streams
    public void connectToServer() {
        
        try {
            clientSocket = new Socket(server, port);
            System.out.println("Connected to " + server + " at port " + port);

            /* get input and output streams */
            serverOut = clientSocket.getOutputStream();
            serverIn = clientSocket.getInputStream();
        
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + server);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("I/O error for the connection to " + server);
            e.printStackTrace();
        }

    }

    /**
     * Set up GSS-API in preparation for context establishment. Creates
     * GSSName and GSSCredential for client principal.
     **/
    //sets up the name and credential
    public void initializeGSS() {

        try {
        	//creating a name object for the entity
            //default = manager.createName("myusername", GSSName.NT_USER_NAME);
            //is the NT_USER_NAME correct?
            GSSName clientName = mgr.createName(clientPrincipal, GSSName.NT_USER_NAME);

            /* create cred with max lifetime */
            //acquire credentials for the entity
            clientCred = mgr.createCredential(clientName,
                    GSSCredential.INDEFINITE_LIFETIME, mech,
                    GSSCredential.INITIATE_ONLY);

            System.out.println("GSSCredential created");
            System.out.println("Credential lifetime: " + clientCred.getRemainingLifetime());

        } catch (GSSException e) {
            System.out.println("GSS-API error in credential acquisition: "
                    + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Establish a GSS-API context with example server, calling
     * initSecContext() until context.isEstablished() is true.
     *
     * This method also tests exporting and re-importing the security
     * context.
     **/
    public void establishContext(InputStream serverIn,
            OutputStream serverOut) {

        try {
        	//another name object and context created
            GSSName peer = mgr.createName(serviceName,
                    GSSName.NT_HOSTBASED_SERVICE);

            context = mgr.createContext(peer, mech, clientCred,
                    GSSContext.INDEFINITE_LIFETIME);

          // set desired context options prior to context establishment
            context.requestConf(true);
            context.requestReplayDet(true);
            context.requestMutualAuth(true);

     
        } catch (GSSException e) {
            System.out.println("GSS-API error during context establishment: "
                    + e.getMessage());
            System.exit(1);
        }

            }

    /**
     * Communicate with the server. First send a message that has been
     * wrapped with context.wrap(), then verify the signature block which
     * the server sends back.
     **/
    public void doCommunication(InputStream serverIn,
            OutputStream serverOut) {

    	//privacy/ confidentiality state = false
        MessageProp messagInfo = new MessageProp(false);
        byte[] inToken  = new byte[0];
        byte[] outToken = null;
        byte[] buffer;
        int err = 0;

        try {

            String msg = "Hello Server, this is the client!";
           // Encodes this String into a sequence of bytes using the platform's default char set, storing the result into byte[] buffer
            buffer = msg.getBytes();

            /* Set privacy to "true" and use the default QOP */
            //privacy state is true, QOP is 0
            messagInfo.setPrivacy(true);

            //Applies per-message security services over the established security context
            outToken = context.wrap(buffer, 0, buffer.length, messagInfo);
            err = GssUtil.WriteToken(serverOut, outToken);
            if (err == 0) {
                System.out.println("Sent message to server");

                /* Read signature block from the server */ 
                inToken = GssUtil.ReadToken(serverIn);
                System.out.println("Received sig block from server...");

                //Verifies the cryptographic MIC, contained in the token parameter, over the supplied message.
                /* Verify signature block */
                context.verifyMIC(inToken, 0, inToken.length, buffer, 0, 
                    buffer.length, messagInfo);
                System.out.println("Verified MIC from server");

            } else {
                System.out.println("Error sending message to server...");
            }

        } catch (GSSException e) {
            System.out.println("GSS-API error in per-message calls: " + 
                    e.getMessage());
        }
    }
}
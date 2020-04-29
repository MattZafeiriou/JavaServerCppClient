import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*
 * Created by Matt Zafeiriou (Ματθαιος Ζαφειριου)
 * 29/04/2020 @ 20:15
 */
public class JavaServerForCppClient
{

	private int port = 15000;
	private boolean running = true;

	private ServerSocket serverSocket = null;
	private Socket clientSocket = null;
	private OutputStream os = null;
	private PrintWriter out = null;
	private InputStream is = null;

	public static void main( String[] args )
	{
		new JavaServerForCppClient();
	}

	public JavaServerForCppClient()
	{
		createServer();
		createLoop( true );
	}

	private boolean createServer() // initializes the variables
	{
		System.out.println( "Creating server.." );
		try
		{
			serverSocket = new ServerSocket( port );
			System.out.println( "Server created!" );
			System.out.println( "Waiting for a client.." );
			clientSocket = serverSocket.accept();
			os = clientSocket.getOutputStream();
			out = new PrintWriter( os, true );
			is = clientSocket.getInputStream();
			System.out.println( "-------------------------" );
			System.out.println( "\nConnected to: "
					+ clientSocket.getRemoteSocketAddress().toString().replaceFirst( "/", "" ) + "\n" );
			return true;
		} catch( IOException e )
		{
			System.out.println( "-------------------------" );
			System.out.println( "An error occured:" );
			System.out.println(
					"Exception caught when trying to listen on port " + port + " or listening for a connection" );
			System.out.println( "-------------------------" );
			e.printStackTrace();
			return false;
		}
	}

	private void createLoop( boolean isRunning ) // creates the loop where the server will receive and send messages
	{
		running = isRunning;
		if( ! running )
		{
			System.out.println( "Server is closed: running == false" );
			return;
		}

		int i = 0;
		while( running )
		{
			i++;
			receiveMessage();
			if( i >= 10 ) // test example how to disconnect from the client and restart the loop so the next client can connect with no problems with the variables
			{
				restartServer();
			} else
			{
				if( ! sendMessage( "1" ) ) // Checks if the server couldn't send the status message: 1 (Problem with the connection) (Status messages: 0 = Close 1 = OK)
				{
					createLoop( running ); // re-create the loop so a new client can join
				}
			}
		}
		// retry if the client closed
		restartServer();
	}

	private String receiveMessage() // Call this function to wait for an input from the client
	{
		ByteBuffer bLength = ByteBuffer.allocate( 4 );
		bLength.order( ByteOrder.LITTLE_ENDIAN );
		try
		{
			is.read( bLength.array(), 0, 4 );
		} catch( IOException e )
		{
			System.out.println( "An error occured with the client connection. (closed?)" );
			restartServer();
		}
		// Convert the length
		int length = bLength.getInt();

		ByteBuffer bMessage = ByteBuffer.allocate( length );
		bMessage.order( ByteOrder.LITTLE_ENDIAN );
		try
		{
			is.read( bMessage.array(), 0, length );
		} catch( IOException e )
		{
			System.out.println( "An error occured with the client connection. (closed?)" );
			restartServer();
		}
		// Convert the message to string
		String msg = new String( bMessage.array() );
		System.out.println( "Received: " + msg );
		return msg;
	}

	private boolean sendMessage( String msg ) // Use this function to send a message to the client
	{
		ByteBuffer bLengthNew = ByteBuffer.allocate( 4 );
		bLengthNew.order( ByteOrder.LITTLE_ENDIAN );
		bLengthNew.putInt( msg.length() );
		try
		{
			os.write( bLengthNew.array() );
		} catch( IOException e )
		{
			return false;
		}
		out.print( msg );
		out.flush();
		return true;
	}

	private void closeServer() // Use this function to close the server and to stop the loop
	{
		System.out.println( "-------------------------" );
		System.out.println( "Server closing.." );
		disconnectClient(); // disconnects the client from the server
		running = false; // <-- closes the loop
		try
		{
			serverSocket.close(); // tries to close the server
		} catch( IOException e )
		{
			e.printStackTrace();
		}
		System.out.println( "Server closed!" );
		System.out.println( "-------------------------" );
	}

	private void disconnectClient() // Sends a status message 0 to the client (0 = CLOSE)
	{
		sendMessage( "0" );
		System.out.println( "Client disconnected!" );
	}

	private void restartServer() // Close the server, create a new one and create the loop again
	{
		System.out.println( "-------------------------" );
		System.out.println( "Restarting.." );
		closeServer();
		createServer();
		createLoop( true );
	}
}

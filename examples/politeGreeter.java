import java.net.*;
import java.io.*;
import psl.worklets.*;

/**
 * PoliteGreeter sends a message every 5 seconds
 *
 */
class politeGreeter extends Thread implements Greeter {
  private ServerSocket _serverSocket;
  private Socket _socket;

  private String _initialGreeting = "Bonjour, comment ca va?";
  private String _greeting = "Ca va bien, et tu, ca va?";
  private String _prompt = "ca va";

  String _initiateGreeting = "false";
  public boolean greet = true;

  private int _port;
  private String _rhostname;
  private int _rport;

  politeGreeter(int p, String h, int rp, String ig){
    if (p == rp){
      System.err.println("error, local port and remote port are equal");
      System.exit(-1);
    }

    _port = p;
    _rhostname = h;
    _rport = rp;
    _initiateGreeting = ig;

    WVM wvm = new WVM(this);

    try {
      _serverSocket = new ServerSocket(_port);

      // System.out.println("Ready");
      while(greet){
	if (_initiateGreeting.equals("true")){
	  sendGreeting(_initialGreeting);
	  _initiateGreeting = "false";
	}

	_socket = _serverSocket.accept();

	if (!receivedGreeting(new ObjectInputStream(_socket.getInputStream()))){
	  System.err.println("Merde, je ne peux pas comprendre cette fou!");
	  continue;
	}
	// 	  System.out.println("Received a greeting!");
	try {
	  sleep(5000);
	} catch (InterruptedException ie){
	  System.err.println("Damn, can't get a good night's sleep: " + ie);
	}
	sendGreeting(_greeting);
	_socket.close();
      }
    } catch (IOException e){
      System.err.println("Greeter IOException: " + e);
    }
  }

  private boolean receivedGreeting(ObjectInputStream ois) throws IOException{
    String request = ois.readUTF();
    ois.close();
    if (request.indexOf(_prompt) != -1)
      return true;
    return false;
  }

  private boolean sendGreeting(String message){
    System.out.println(message);
    ObjectOutputStream oos = null;

    try {
      _socket = new Socket(_rhostname, _rport);
      oos = new ObjectOutputStream(_socket.getOutputStream());
      oos.writeUTF(message);
      oos.close();
      _socket.close();
      return true;
    } catch (UnknownHostException uhe){
      System.err.println("Greeter UnknownHostException: " + uhe);
    } catch (IOException ioe){
      System.err.println("Greeter IOException: " + ioe);
    }
    return false;
  }

  public String getPrompt(){
    return _prompt;
  }

  public String getGreeting(){
    return _greeting;
  }

  public void changeGreeting(String greeting){
    _greeting = greeting;
  }

  public void changePrompt(String prompt){
    _prompt = prompt;
  }

  public void initiateGreeting() {
    _initiateGreeting = "true";
  }

  public static void main (String args[]){
    if (args.length < 4 || (args.length > 0 && args[0].indexOf("help") != -1)){
      System.out.println("Usage: java politeGreeter port rhost rport <initiate greeting? (true/false)>");
      return;
    }

    new politeGreeter(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2]), args[3]);
  }
}

/*
 * @(#)politeGreeter.java
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2002: @Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

import java.net.*;
import java.io.*;
import psl.worklets.*;

/**
 * PoliteGreeter
 *
 */
class politeGreeter extends Thread implements Greeter {

  public static void main (String args[]){
    if (args.length < 6 || (args.length > 0 && args[0].indexOf("help") != -1)){
      System.out.println("Usage: java politeGreeter lhost lport name rhost rport <initiate greeting? (true/false)");
      return;
    }

    politeGreeter pierre = new politeGreeter(args[0], Integer.parseInt(args[1]), args[2], args[3], Integer.parseInt(args[4]), args[5]);

    pierre.startListening();
  }


  private ServerSocket _serverSocket;
  private Socket _socket;

  private String _initialGreeting = "Bonjour, comment ca va?";
  private String _greeting = "Ca va bien, et tu, ca va?";
  private String _prompt = "ca va";

  String _initiateGreeting = "false";
  private boolean _interlocute = false;

  private String _host;
  private int _port;
  private String _name;
  private String _rhostname;
  private int _rport;


  politeGreeter(String host, int port, String name, String rhost, int rport, String initiateGreeting){
    if (port == rport){
      System.err.println("error, local port and remote port are equal");
      System.exit(-1);
    }

    _host = host;
    _port = port;
    _name = name;
    _rhostname = rhost;
    _rport = rport;
    _initiateGreeting = initiateGreeting;

    if (_initiateGreeting.equals("true"))
      _interlocute = true;

    WVM wvm = new WVM(this, host, _name);
 }

  private void startListening(){
    try {
      _serverSocket = new ServerSocket(_port);

      // System.out.println("Ready");
      while(true){

	if (_initiateGreeting.equals("true")){
	  sendGreeting(_initialGreeting);
	  _initiateGreeting = "false";
	}

	if (_interlocute)
	  {
	    try {
	      // WVM.out.println("politeGreeter is listening");
	    _socket = _serverSocket.accept();
	    } catch (SocketException se) {}
	  }

	// sleep, just to slow the conversation down a bit
	try {
	  sleep(2000);
	} catch (InterruptedException ie){
	  // System.err.println("Je ne peux pas dormir? " + ie);
	}

	if (_socket !=  null && receivedGreeting(new ObjectInputStream(_socket.getInputStream()))
	    && _interlocute){
	  sendGreeting(_greeting);

	}

	if (_socket != null)
	  _socket.close();
      }
    } catch (IOException e){
      // System.err.println("Greeter IOException: " + e);
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
      // System.out.println("sending to: " + _rhostname + ":" + _rport);
      _socket = new Socket(_rhostname, _rport);
      oos = new ObjectOutputStream(_socket.getOutputStream());
      oos.writeUTF(message);
      oos.close();
      _socket.close();
      return true;
    } catch (UnknownHostException uhe){
      //System.err.println("Greeter UnknownHostException: " + uhe);
    } catch (IOException ioe){
      // System.err.println("Greeter IOException: " + ioe);
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
    System.out.println("Initiating greeting");
//     try {
//       _serverSocket.close();
//     } catch (IOException ioe) {
//       System.err.println("Error closing server socket: " + ioe);
//     }
    _initiateGreeting = "true";
  }

  public boolean interlocuting() {
    return _interlocute;
  }

  public void stopInterlocuting() {
    System.out.println("interlocute = false");
    _interlocute = false;
  }

  public void startInterlocuting() {
    System.out.println("interlocute = true");
    _interlocute = true;
  }
}

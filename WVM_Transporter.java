package psl.worklets;

/**
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * 
 * Copyright (c) 2001: @author Gaurav S. Kc
 * 
*/

import java.io.*;
import java.net.*;
import java.util.*;

import psl.worklets.http.*;

class WVM_Transporter extends Thread {
  
  WVM _wvm;
  private ServerSocket _socket;
  
  int     _port;
  String  _host; 
  String  _name;

  private boolean _isActive = false;

  private WVM_ClassLoader _loader;
  ClassFileServer _webserver;
  int _webPort;
  boolean _isClassServer;
  
  private static final String PING_REQUEST = "Hi, I am pinging you :)";
  private static final String PING_RESPONSE = "Ok, pinging you back!";

  private static final String SENDMSG_REQUEST = "Hi, I am sending you a message :)";
  private static final String SENDMSG_RESPONSE = "Ok, I received your message!";

  private static final String GETMSG_REQUEST = "Hi, I am want something that you have :)";
  private static final String GETMSG_RESPONSE = "Ok, here you go, you can have it!";

  private static final String WORKLET_XFER = "Yo, I am sending a worklet";
  private static final String WORKLET_RECV = "Yo, I got your worklet";

  WVM_Transporter(WVM wvm, String host, String name, int port) {
    WVM.out.println("Creating the sockets transporter layer for the WVM");

    // Setup socket
    _host = host;
    _name = name;
    _port = port;
    while (_port++ >= port) {
      try {
        _socket = new ServerSocket(_port);
        WVM.out.println("  SocketListener: " + _host + ":" + _port);
        _isActive = true;
        break;
      } catch (UnknownHostException e) {
        // whut? not possible!
      } catch (IOException e) {
        // oops, must try another port number;
        continue;
      }
    }

    // Setup classFileServer
    try {
      _webPort = _port + 1;
      _webserver = new ClassFileServer(_webPort, null);
      WVM.out.println("  serving classes on http://" + _host + ":" + _webPort + "/");
      _isClassServer = true;
      _webPort = _webserver.getWebPort();
    } catch (IOException e) {
      WVM.out.println("ClassFileServer cannot start: " + e.getMessage());
      e.printStackTrace();

      if (_webserver != null) _webserver.shutdown();
      _webserver = null;
      _isClassServer = false;
    }

    _wvm = wvm;
    _loader = null;
  }

  void shutdown() {
    if (_webserver != null) _webserver.shutdown();
    _isActive = false;
    try {
      _socket.close();
    } catch (IOException e) { }
  }
  
  HashSet classHashSet;
  public void run() {
    
    while (!_isActive) {
      try {
        WVM.out.print(".");
        sleep(50);
      } catch (InterruptedException e) { }
    }
    
    MAIN_LOOP: while (_isActive) {
      WVM.out.println("    ready to accept worklets");

      Socket s = null;
      ObjectOutputStream oos = null;
      ObjectInputStream ois = null;
    
      try {
        if (!_isActive) shutdown();
        s = _socket.accept();
        ois = new ObjectInputStream(s.getInputStream()) {
          protected Class resolveClass(ObjectStreamClass v) throws IOException, ClassNotFoundException {
            Class c = Class.forName(v.getName(), true, _loader);
            // WVM.out.println("In custom ObjectInputStream, trying to resolve class: " + c);
            return ( (c == null) ? super.resolveClass(v) : c );
          }
        };
        oos = new ObjectOutputStream(s.getOutputStream());

        String requestType = ois.readUTF();
        if (requestType.equals(PING_REQUEST)) {
          // ok, this is a ping request
          WVM.out.println("  --  being PINGED thru sockets");
          oos.writeUTF(PING_RESPONSE); oos.flush();
          continue MAIN_LOOP;
        } else if (requestType.equals(SENDMSG_REQUEST)) {
          // ok, peer hostSystem @ WVM is attempting to send a message
          WVM.out.println(" -- received a message thru sockets");
          _wvm.receiveMessage(ois.readObject());
          oos.writeUTF(SENDMSG_RESPONSE); oos.flush();
          continue MAIN_LOOP;
        } else if (requestType.equals(GETMSG_REQUEST)) {
          // ok, peer hostSystem @ WVM is asking for a message
          WVM.out.println(" -- received a message request thru sockets");
          oos.writeObject(_wvm.requestMessage(ois.readObject()));
          oos.writeUTF(GETMSG_RESPONSE); oos.flush();
          continue MAIN_LOOP;
        } else if (! requestType.equals(WORKLET_XFER)) {
          // what kinda request is this anyway?
          WVM.out.println("  --  received a random request!");
          continue MAIN_LOOP;
        }

        WVM.out.println("    received a worklet");
        classHashSet = new HashSet();

        String rHost = ois.readUTF();
        String rName = ois.readUTF();
        int rPort = ois.readInt();

        int numJunctions = ois.readInt();
        while (numJunctions-- > 0) {
          String cName = ois.readUTF();
          String codebase = ois.readUTF();
          try {
            addCodebase(codebase);
            Class loadCl = _loader.loadClass(cName);
            // WVM.out.println("finally loaded class: " + loadCl);
          } catch (ClassNotFoundException e) {
            WVM.out.println ("Class " + cName + " could not be loaded from codebase: " + codebase);
            e.printStackTrace();
            continue MAIN_LOOP;
          }
        }

        Worklet wkl = null;
        try {
          wkl = (Worklet) ois.readObject();
          // TODO: Okay, now that the LEAST REQUIRED SET (LRS) of class bytecode
          // has been downloaded from the source WVM, send out a BytecodeRetrieverWJ
          // to get the relevant classes, viz. all those classes that the source
          // HTTP server served up to this WVM
          {
            // send out BytecodeRetrieverWJ w/ a Worklet to retrieve all URLLoaded classes
            // new BytecodeRetrieval(classHashSet, _wvm, _host, _name, _port, rHost, rName, rPort);
          }
          
          // Now, send acknowledgement back to sender WVM
          oos.writeUTF(WORKLET_RECV);
          oos.flush();

        } catch (Exception e) {
          WVM.out.println("This is BAD!");
          e.printStackTrace();
          System.exit(0);
        }

        // adding to WVM's in-tray, and *do* need to send out BytecodeRetrieval worklet
        wkl.retrieveBytecode = true;
        _wvm.installWorklet(wkl);
      } catch (ClassNotFoundException cnfe) {
        WVM.out.println("ClassNotFoundException when receiving message from peer, cnfe: " + cnfe);
        cnfe.printStackTrace();
      } catch (SocketException se) {
        WVM.out.println("WVM Socket died se: " + se);
        se.printStackTrace();
      } catch (IOException ioe) {
        WVM.out.println("IOException in Worklet receive loop, ioe: " + ioe);
        ioe.printStackTrace();
      } finally {
        WVM.out.println ("\n\n    getting ready to accept worklets again");
        try {
          if (ois != null) ois.close();
          if (oos != null) oos.close();
          if (s != null) s.close();
        } catch (IOException ioe) { }
      }
      WVM.out.println();
    }
  }

  private void addCodebase(String codebase) {
    StringTokenizer st = new StringTokenizer (codebase, " ");
    Vector urlsVec = new Vector();
    int count = 0;
    while (st.hasMoreTokens()) {
      try {
        URL url = new URL(st.nextToken());
        urlsVec.add(url);
        count++;
      } catch (MalformedURLException e) {
        WVM.out.println("MalformedURLException in addCodebase, e: " + e.getMessage());
      }
    }

    // urlsVec = new Vector(new HashSet(urlsVec));
    URL urls[] = new URL[urlsVec.size()];
    urlsVec.toArray(urls);
    if (_loader == null) {
      _loader = new WVM_ClassLoader_New(urls);
    } else {
      _loader.addCodebase(urls);
    }
  }
  
  boolean sendWorklet(Worklet wkl, WorkletJunction wj) {
    String targetHost = wj._host;
    int targetPort = wj._port;
    
    boolean transmissionComplete = false;
    
    Socket s = null;
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    
    try {
      WVM.out.println("  --  Sending worklet thru sockets");
      s = new Socket(targetHost, targetPort);
      oos = new ObjectOutputStream(s.getOutputStream());

      oos.writeUTF(WORKLET_XFER);


      oos.writeUTF(_host);
      oos.writeUTF(_name);
      oos.writeInt(_port);

      {
        ClassLoader sysCll = ClassLoader.getSystemClassLoader();
        Enumeration e = wkl.getClasses();
        oos.writeInt(wkl.getNumClasses());
        String cName = null;
        String codebase = null;
        while (e.hasMoreElements()) {
          Class c = (Class) e.nextElement();
          ClassLoader cl = c.getClassLoader();
          cName = c.getName();
          /* Sending multiple codebases: 19 June 2001 - gskc
          if (sysCll == cl) {
            codebase = "http://" + _host + ":" + _webPort + "/";
            // WVM.out.println("class: " + cName + ", codebase: " + codebase);
          } else {
            URL url = cl.getResource(cName.replace('.', '/') + ".class");
            codebase = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/";
            // WVM.out.println("class: " + cName + ", codebase: " + codebase);
          }
          */

          codebase = "http://" + _host + ":" + _webPort + "/";
          if (sysCll != cl) {
            // This class was originally loaded from a remote server
            URL url = cl.getResource(cName.replace('.', '/') + ".class");
            if (url != null) {
              // append remote http classServer to the 'local' codebase
              codebase += " " + url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/";
            } else {
              WVM.out.println("mySource unavailable, sending codebase: " + codebase);
            }
          }
          
          // WVM.out.println("codebase of workletJunction: " + codebase);
          oos.writeUTF(cName);
          oos.writeUTF(codebase);
        }
      }

      // TODO: set up the BAG-MULTISET in the ClassFileServer so that the 
      // incoming BytecodeRetrieverWJ can get the data it needs
      oos.writeObject(wkl);
      oos.flush();

      // Receive ACK from the target WVM
      ois = new ObjectInputStream(s.getInputStream());
      transmissionComplete = ois.readUTF().equals(WORKLET_RECV);
    } catch (InvalidClassException e) {
      WVM.out.println("InvalidClassException in sendWorklet, e: " + e.getMessage());
      e.printStackTrace();
    } catch (NotSerializableException e) {
      WVM.out.println("NotSerializableException in sendWorklet, e: " + e.getMessage());
      e.printStackTrace();
    } catch (UnknownHostException e) {
      WVM.out.println("UnknownHostException in sendWorklet, e: " + e.getMessage());
      e.printStackTrace();
    } catch (SecurityException e) {
      WVM.out.println("SecurityException in sendWorklet, e: " + e.getMessage());
      e.printStackTrace();
    } catch (IOException e) {
      WVM.out.println("IOException in sendWorklet, e: " + e.getMessage());
      e.printStackTrace();
    } finally {
      try {
        if (ois != null) ois.close();
        if (oos != null) oos.close();
        if (s != null) s.close();
      } catch (IOException ioe) { }
      return transmissionComplete;
    }
  }
  
  // Client-side - should be overridden ////////////////////////////////////////
  protected boolean ping(String wvmURL) {
    WVM.out.println("SHOULD-BE-OVERRIDDEN! WVM_Transporter.ping(String)");
    return false;
  }
  protected boolean sendMessage(Object message, String wvmURL) {
    WVM.out.println("SHOULD-BE-OVERRIDDEN! WVM_Transporter.sendMessage(Object, String)");
    return false;
  }
  protected Object getMessage(Object messageKey, String wvmURL) {
    WVM.out.println("SHOULD-BE-OVERRIDDEN! WVM_Transporter.getMessage(Object, String)");
    return null;
  }
  // END: Client-side - should be overridden ///////////////////////////////////
  
  // Client-side ///////////////////////////////////////////////////////////////
  final boolean ping(String host, int port) {
    // 2-do: really!
    boolean transmissionComplete = false;
    
    Socket s = null;
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    
    try {
      WVM.out.println("  --  pinging peer WVM thru sockets: " + host + ":" + port);
      s = new Socket(host, port);

      // Send request to the peer WVM
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.writeUTF(PING_REQUEST);
      oos.flush();

      // Receive ACK from the peer WVM
      ois = new ObjectInputStream(s.getInputStream());
      transmissionComplete = ois.readUTF().equals(PING_RESPONSE);
    } catch (UnknownHostException e) {
      WVM.out.println("UnknownHostException in ping, e: " + e.getMessage());
      // e.printStackTrace();
    } catch (IOException e) {
      WVM.out.println("IOException in ping, e: " + e.getMessage());
      // e.printStackTrace();
    } finally {
      try {
        if (ois != null) ois.close();
        if (oos != null) oos.close();
        if (s != null) s.close();
      } catch (IOException ioe) { }
      return transmissionComplete;
    }
  }
  final boolean sendMessage(Object message, String host, int port) {
    // 2-do: really!
    boolean transmissionComplete = false;
    
    Socket s = null;
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    
    try {
      WVM.out.println("  --  sending message to peer WVM thru sockets: " + host + ":" + port);
      s = new Socket(host, port);

      // Send request to the peer WVM
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.writeUTF(SENDMSG_REQUEST);
      oos.writeObject(message);
      oos.flush();

      // Receive ACK from the peer WVM
      ois = new ObjectInputStream(s.getInputStream());
      transmissionComplete = ois.readUTF().equals(SENDMSG_RESPONSE);

    } catch (UnknownHostException uhe) {
      WVM.out.println("UnknownHostException in ping, uhe: " + uhe);
      // e.printStackTrace();
    } catch (IOException ioe) {
      WVM.out.println("IOException in ping, ioe: " + ioe);
      // e.printStackTrace();
    } finally {
      try {
        if (ois != null) ois.close();
        if (oos != null) oos.close();
        if (s != null) s.close();
      } catch (IOException ioe) { }
      return transmissionComplete;
    }
  }
  final Object getMessage(Object messageKey, String host, int port) {
    // 2-do: really!
    boolean transmissionComplete = false;
    Object message = null;
    
    Socket s = null;
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    
    try {
      WVM.out.println("  --  getting message from peer WVM thru sockets: " + host + ":" + port);
      s = new Socket(host, port);

      // Send request to the peer WVM
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.writeUTF(GETMSG_REQUEST);
      oos.writeObject(messageKey);
      oos.flush();

      // Receive ACK from the peer WVM
      ois = new ObjectInputStream(s.getInputStream());
      message = ois.readObject();
      transmissionComplete = ois.readUTF().equals(GETMSG_RESPONSE);

    } catch (ClassNotFoundException cnfe) {
      WVM.out.println("ClassNotFoundException in getMessage, cnfe: " + cnfe);
    } catch (UnknownHostException uhe) {
      WVM.out.println("UnknownHostException in getMessage, uhe: " + uhe);
      // e.printStackTrace();
    } catch (IOException ioe) {
      WVM.out.println("IOException in getMessage, ioe: " + ioe);
      // e.printStackTrace();
    } finally {
      try {
        if (ois != null) ois.close();
        if (oos != null) oos.close();
        if (s != null) s.close();
      } catch (IOException ioe) { }
      return transmissionComplete ? message : null;
    }
  }
  // END: Client-side //////////////////////////////////////////////////////////

}

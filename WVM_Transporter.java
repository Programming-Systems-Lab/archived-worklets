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
      try {
        if (!_isActive) shutdown();
        Socket s = _socket.accept();
        WVM.out.println("    received a worklet");
        ObjectInputStream ois = new ObjectInputStream(s.getInputStream()) {
          protected Class resolveClass(ObjectStreamClass v) throws IOException, ClassNotFoundException {
            Class c = Class.forName(v.getName(), true, _loader);
            WVM.out.println("In custom ObjectInputStream, trying to resolve class: " + c);
            return ( (c == null) ? super.resolveClass(v) : c );
          }
        };

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
            WVM.out.println ("\n\n    getting ready to accept worklets again");
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
            new BytecodeRetrieval(classHashSet, _wvm,
              _host, _name, _port,
              rHost, rName, rPort);
          }
        } catch (Exception e) {
          WVM.out.println("This is BAD!");
          e.printStackTrace();
          System.exit(0);
        }

        // adding to WVM's in-tray, and *do* need to send out BytecodeRetrieval worklet
        wkl.retrieveBytecode = true;
        _wvm.installWorklet(wkl);
      } catch (SocketException e) {
        WVM.out.println("SocketException e: " + e.getMessage());
      } catch (IOException e) {
        WVM.out.println("IOException in Worklet receive loop, e: " + e.getMessage());
        e.printStackTrace();
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

    urlsVec = new Vector(new HashSet(urlsVec));
    URL urls[] = new URL[urlsVec.size()];
    urlsVec.toArray(urls);
    if (_loader == null) {
      _loader = new WVM_ClassLoader(urls);
    } else {
      _loader.addCodebase(urls);
    }
  }
  
  void sendWorklet(Worklet wkl, WorkletJunction wj) {
    String targetHost = wj._host;
    int targetPort = wj._port;
    try {
      WVM.out.println("  --  Sending worklet thru sockets");
      Socket s = new Socket(targetHost, targetPort);
      ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
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
          if (sysCll == cl) {
            codebase = "http://" + _host + ":" + _webPort + "/";
            // WVM.out.println("class: " + cName + ", codebase: " + codebase);
          } else {
            URL url = cl.getResource(cName.replace('.', '/') + ".class");
            codebase = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/";
            WVM.out.println("class: " + cName + ", codebase: " + codebase);
          }
          // WVM.out.println("codebase: " + codebase);
          oos.writeUTF(cName);
          oos.writeUTF(codebase);
        }
      }

      // TODO: set up the BAG-MULTISET in the ClassFileServer so that the 
      // incoming BytecodeRetrieverWJ can get the data it needs
      oos.writeObject(wkl);
      WVM.out.println("sent out wj to target: " + wj);
      oos.close();
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
    }
  }

}

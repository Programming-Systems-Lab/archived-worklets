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
  
  protected WVM _wvm;
  private ServerSocket _socket;
  
  protected int     _port;
  protected String  _host; 
  protected String  _name;

  private boolean _isActive = false;

  private WVM_ClassLoader _loader;
  protected ClassFileServer _webserver;
  protected int _webPort;
  protected boolean _isClassServer;

  protected WVM_Transporter(WVM wvm, String host, String name, int port) {
    System.out.println("Creating the sockets transporter layer for the WVM");

    // Setup socket
    _host = host;
    _name = name;
    _port = port;
    while (_port++ >= port) {
      try {
        _socket = new ServerSocket(_port);
        System.out.println("  SocketListener: " + _host + ":" + _port);
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
      System.out.println("  serving classes on http://" + _host + ":" + _webPort + "/");
      _isClassServer = true;
      _webPort = _webserver.getWebPort();
    } catch (IOException e) {
      System.out.println("ClassFileServer cannot start: " + e.getMessage());
      e.printStackTrace();

      if (_webserver != null) _webserver.shutdown();
      _webserver = null;
      _isClassServer = false;
    }

    _wvm = wvm;
    _loader = null;
    start();
  }
  
  protected void shutdown() {
    _isActive = false;
    try {
      _socket.close();
    } catch (IOException e) { }
  }
  
  public void run() {
    
    while (!_isActive) {
      try {
        System.out.print(".");
        sleep(50);
      } catch (InterruptedException e) { }
    }
    
    MAIN_LOOP: while (_isActive) {
      System.out.println("    ready to accept worklets");
      try {
        if (!_isActive) shutdown();
        Socket s = _socket.accept();
        System.out.println("    received a worklet");
        ObjectInputStream ois = new ObjectInputStream(s.getInputStream()) {
          protected Class resolveClass(ObjectStreamClass v) throws IOException, ClassNotFoundException {
            Class c = Class.forName(v.getName(), true, _loader);
            // System.out.println("Trying to resolve class: " + c);
            return ( (c == null) ? super.resolveClass(v) : c );
          }
        };

        int numJunctions = ois.readInt();
        while (numJunctions-- > 0) {
          String cName = ois.readUTF();
          String codebase = ois.readUTF();
          try {
            addCodebase(codebase);
            Class loadCl = _loader.loadClass(cName);
          } catch (ClassNotFoundException e) {
            System.out.println ("Class " + cName + " could not be loaded from codebase: " + codebase);
            e.printStackTrace();
            System.out.println ("\n\n    getting ready to accept worklets again");
            continue MAIN_LOOP;
          }
        }
        Worklet wkl = null;
        try {
          wkl = (Worklet) ois.readObject(); // GOT PROBLEMS HERE!!!
        } catch (Exception e) {
          System.out.println("Got BIG problems here");
          e.printStackTrace();
          System.exit(0);
        }
        _wvm.installWorklet(wkl);
      } catch (IOException e) {
        System.out.println("IOException e: " + e.getMessage());
        e.printStackTrace();
      }
      System.out.println();
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
      } catch (MalformedURLException e) { }
    }

    URL urls[] = new URL[count];
    urlsVec.toArray(urls);
    if (_loader == null) {
      _loader = new WVM_ClassLoader(urls);
    } else {
      _loader.addCodebase(urls);
    }
  }
  
  protected void sendWorklet(Worklet wkl, WorkletJunction wj) {
    String targetHost = wj._host;
    int targetPort = wj._port;
    try {
      System.out.println("  --  Sending worklet thru sockets");
      Socket s = new Socket(targetHost, targetPort);
      ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());

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
            // System.out.println("cl==SystemClassLoader, codebase: " + codebase);
          } else {
            URL url = cl.getResource(cName.replace('.', '/') + ".class");
            codebase = url.getProtocol() + "://" + url.getHost() + url.getPort() + "/";
            // System.out.println("cl!=SystemClassLoader, codebase: " + codebase);
          }
        }
        // System.out.println("codebase: " + codebase);
        oos.writeUTF(cName);
        oos.writeUTF(codebase);
      }

      oos.writeObject(wkl);
      // System.out.println("sent out wj to target: " + wkl);
    } catch (InvalidClassException e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    } catch (NotSerializableException e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    } catch (UnknownHostException e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    } catch (SecurityException e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    } catch (IOException e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

}

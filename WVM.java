package psl.worklets;


/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *  
 * Copyright (c) 2001: @author Gaurav S. Kc 
 * 
*/

import java.io.*;
import java.net.*;
import java.util.*;

public final class WVM extends Thread {
  public static PrintStream out = System.out;
  WVM_Transporter transporter;
  private Object _system;
  private final Hashtable _peers = new Hashtable();
  private final Vector _installedWorklets = new Vector();

  public WVM(Object system, String host, String name) {
    this(system, host, name, WVM_Host.PORT);
  }  

  public WVM(Object system, String host, String name, int port) {
    WVM.out.println("WVM created");
    start();
    _system = system;
    try {
      transporter = new WVM_RMI_Transporter(this, host, name, port);
    } catch (Exception e) {
      WVM.out.println(e.getMessage());
      e.printStackTrace();
    }
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        shutdown();
      }
    });
  }  

  public void shutdown() {
    _peers.clear();
    _installedWorklets.clear();

    if (transporter != null) {
      transporter.shutdown();
      transporter = null;
    }
    _system = null;
    WVM.out.println("WVM destroyed");
  }

  public void run() {
    Worklet _worklet;
    synchronized (this) {
      while (true) {
        if (_installedWorklets.isEmpty()) {
          try { 
            wait();
          } catch (InterruptedException e) { } 
        }
        _worklet = (Worklet) _installedWorklets.firstElement();
        _installedWorklets.removeElement(_worklet);
        executeWorklet(_worklet);
      }
    }
  }

  synchronized void installWorklet(Worklet _worklet) {
    _installedWorklets.addElement(_worklet);
    if (_installedWorklets.size() == 1) {
      // WVM was asleep coz' in-tray was empty, must wake up!
      this.notifyAll();
    }
  }
  
  private void executeWorklet(Worklet _worklet) {
    _worklet.init(_system, this);
    _worklet.execute();
  }

  public String toString() {
    return (transporter.toString());
  }

  public static void main(String args[]) throws UnknownHostException {
    WVM.out.println("usage: java psl.worklets.WVM <wvmName>");
    String rmiName = args.length == 0 ? "WVM" : args[0];
    WVM wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), rmiName);
  }

}

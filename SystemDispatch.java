package psl.worklets;

/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *  
 * Copyright (c) 2001: @author Gaurav S. Kc 
 * 
*/


import java.io.*;
import java.io.*;
import java.net.*;

public class SystemDispatch implements Serializable {
  public static void main(String args[]) {
    SystemDispatch sd = new SystemDispatch(args);
  }
  private SystemDispatch(String args[]) {
    if (args.length != 4) {
      System.out.println("usage: java SystemDispatch <App> <rHost> <rName> <rPort>");
      System.exit(0);
    }

    final String App = args[0];
    String rHost = args[1];
    String rName = args[2];
    int rPort = Integer.parseInt(args[3]);

    try {
      final Class appClass = Class.forName(App);

      WVM wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "SystemDispatch");
      Worklet wkl = new Worklet(null);
      wkl.addJunction(new WorkletJunction(rHost, rName, rPort) {
        public void execute() {
          System.out.println("\t --- Totally New Component ---");
          try {
            appClass.newInstance();
          } catch (InstantiationException e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
          }
        }
      });
      wkl.deployWorklet(wvm);

    } catch (UnknownHostException e) {
      System.out.println("Exception: " + e.getMessage());
      e.printStackTrace();
      System.exit(0);
    } catch (ClassNotFoundException e) {
      System.out.println("Exception: " + e.getMessage());
      e.printStackTrace();
      System.exit(0);
    } 
  }
}


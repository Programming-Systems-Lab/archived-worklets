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
      WVM.out.println("usage: java SystemDispatch <App> <rHost> <rName> <rPort>");
      System.exit(0);
    }

    final String App = args[0];
    String rHost = args[1];
    String rName = args[2];
    int rPort = Integer.parseInt(args[3]);

    try {
      final Class appClass2 = Class.forName(App);
      final Class appClass = Class.forName("gskc.TicTacToe");

      WVM wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "SystemDispatch");
      Worklet wkl = new Worklet(null);
      wkl.addJunction(new WorkletJunction(rHost, rName, rPort) {
        public void execute() {
          WVM.out.println("\t --- Totally New Component ---");
          try {
            appClass.newInstance();
          } catch (InstantiationException e) {
            WVM.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            WVM.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
          }
        }
      });
      wkl.deployWorklet(wvm);

    } catch (UnknownHostException e) {
      WVM.out.println("Exception: " + e.getMessage());
      e.printStackTrace();
      System.exit(0);
    } catch (ClassNotFoundException e) {
      WVM.out.println("Exception: " + e.getMessage());
      e.printStackTrace();
      System.exit(0);
    } 
  }
}


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
import java.lang.reflect.*;

public class SystemDispatch implements Serializable {
  public static void main(String args[]) {
    SystemDispatch sd = new SystemDispatch(null, null, args);
  }
  SystemDispatch(WVM wvm, WorkletJunction originJunction, String args[]) {

    /*
     * For use from within another program:
     * new SystemDispatch(<WVM>, <ORIGIN_JUNCTION>, new String[] { <S>, <S> ... <S> });
    */

    if (args.length < 4) {
      WVM.out.println("usage: java SystemDispatch <rHost> <rName> <rPort> <App> [<App-param>]* [-f <file>]*");
      System.exit(0);
    }

    String rHost = args[0];
    String rName = args[1];
    int rPort = Integer.parseInt(args[2]);

    final String App = args[3];
    final Vector appArgs = new Vector();
    final Hashtable appFiles = new Hashtable();
    int i;

    for (i=4; i<args.length; i++) {
      if ("-f".equalsIgnoreCase(args[i])) break;
      appArgs.add(args[i]);
    }

    for (; i<args.length; i+=2) {
      if ("-f".equalsIgnoreCase(args[i])) loadFile(args[i+1], appFiles);
    }

    try {
      final Class appClass = Class.forName(App);

      if (wvm == null) {
        wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "SystemDispatch");
      }

      Worklet wkl = new Worklet(originJunction);
      wkl.addJunction(new WorkletJunction(rHost, rName, rPort) {
        private final String methodName = "main";
        private final String [] parameters = new String[appArgs.size()];
        private final Class [] parameterTypes = new Class[] { String[].class };

        public void init(Object _system, WVM _wvm) {
          // get write permissions here?
          String fName;
          Enumeration e = appFiles.keys();
          while (e.hasMoreElements()) {
            fName = (String) e.nextElement();
            try {
              FileWriter fw = new FileWriter(new File(fName));
              fw.write((char []) appFiles.get(fName));
              fw.close();
              System.out.println("  wrote file: " + fName);
            } catch (IOException ioe) {
              System.out.println("  could not write file: " + fName);
            }
          }
        }

        public void execute() {
          WVM.out.println("\t --- Totally New Component ---");
          try {
            // OLDER STUFF - 17:28:03:10:2001 appClass.newInstance();
            Enumeration e = appArgs.elements();
            for (int i=0; i<parameters.length; i++) parameters[i] = (String) e.nextElement();
            appClass.getMethod(methodName, parameterTypes).invoke(null, new Object[] { parameters });
      
/* ----------------------------------------------------------------------------------------------------- 
          // OLDER STUFF - 17:28:03:10:2001
          } catch (InstantiationException e) {
            WVM.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
----------------------------------------------------------------------------------------------------- */

          } catch (IllegalAccessException e) {
            WVM.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
          } catch (IllegalArgumentException e) {
            WVM.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
          } catch (InvocationTargetException e) {
            WVM.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
          } catch (NoSuchMethodException e) {
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
  private static void loadFile(String fName, Hashtable appFiles) {
    try {
      File f = new File(fName);
      char contents[] = new char[(int) f.length()];
      FileReader fr = new FileReader(f);
      fr.read(contents, 0, contents.length);
      fr.close();
      appFiles.put(fName, contents);
    } catch (IOException ioe) {
      System.out.println("  could not load: " + fName);
    }
  }
}


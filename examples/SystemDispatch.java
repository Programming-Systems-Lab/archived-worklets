/*
 * @(#)SystemDispatch.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Gaurav S. Kc
 * Last modified by: Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import psl.worklets.*;

/**
 * The <code>SystemDispatch</code> class is a demo of the Worklets
 * system that shows examples of how to create and send a Worklet.
 *
 * @version	$Revision$ $Date$
 * @author	Gaurav S. Kc (gskc@cs.columbia.edu)
 */

public class SystemDispatch implements Serializable {
  public static void main(String args[]) {
    SystemDispatch sd = new SystemDispatch(null, null, args);
  }

  /**
   * Creates a <code>SystemDispatch</code> object that will send a
   * Worklet through the specified <code>wvm</code>, with the nodal
   * point beginning at <code>originJunction</code>, and with
   * <code>args</code> as parameters.
   *
   * You must specify at least the first 4 <code>args</code>:
   * - <code>args[0]</code>=<code>String rHost</code>: the remote host to send the Worklet to.
   * - <code>args[1]</code>=<code>String rName</code>: the RMI name to try to send the Worklet to.
   * - <code>args[2]</code>=<code>int rPort</code>: the socket port to try to send the Worklet to
   * - <code>args[3]</code>=<code>String App</code>: the application to send
   * - <code>args[4]</code>=<code>int iterations</code>: the number of iterations to execute (default = 1)
   * - <code>args[5]</code>=<code>long interval</code>: the interval to wait in between iterations (default = 0)
   * - <code>args[6]</code>=<code>String label</code>: the label of the WorketJunction (default = App)
   *
   * To create a secure Worklet you need to set the system property "WVM_FILE" to the
   * <a href=wvmfiledoc>WVM properties file</a>.  You can do this either on the command line with the -D option
   * or by using <code>System.setProperty</code>.
   *
   * @param      wvm		WVM used to bootstrap the Worklet into the system.
   * @param      originJunction	pointer to the originating junction.
   * @param      args		array of parameters used to create the WorkletJunctions.
   */

  SystemDispatch(WVM wvm, WorkletJunction originJunction, String args[]) {
    if (args.length < 4) {
      System.out.println("usage: java SystemDispatch rHost rName rPort App iterations waitTime label");
      System.exit(0);
    }

    String rHost = args[0];
    String rName = args[1];
    int rPort = Integer.parseInt(args[2]);
    final String App = args[3];

    int iterations = 1;
    if (args.length >= 5) Integer.parseInt(args[4]);

    long interval = 0;
    if (args.length >= 6) interval = Long.parseLong(args[5]);

    String label = App;
    if (args.length >= 7) label = args[6];

    String name;
    final Vector appArgs = new Vector();
    final Hashtable appFiles = new Hashtable();

    boolean isSecure = false;
    OptionsParser op = null;

    String wvmfile = System.getProperty("WVM_FILE");
    if (wvmfile != null) isSecure = true;

    // This is where we get the security parameters
    if (isSecure) {
      op = new OptionsParser();
      op.loadWVMFile(wvmfile);
      op.setWVMProperties();
    }

    if (op != null && op.securityLevel != 0)
      name="SystemDispatchSecure";
    else
      name="SystemDispatch";

    try {
      // create the local WVM that will bootstrap the Worklet into the system
      if (wvm == null) {
	if (isSecure)
	  wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(),
				     name, op.port, op.keysfile, op.password,
				     op.ctx, op.kmf, op.ks, op.rng, op.securityLevel);
	else
	  wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "SystemDispatch");
      }

    } catch (UnknownHostException e) {
      System.out.println("Exception: " + e.getMessage());
      e.printStackTrace();
      System.exit(0);
    }

    try {
      final Class appClass = Class.forName(App);

      // **************** ADDING THE PLANNER ********************** //
      JunctionPlanner jp = new JunctionPlanner(iterations, interval);
      WorkletID id = new WorkletID(label);

      // **************** ADDING THE WORKLET JUNCTION ********************** //
      Worklet wkl = new Worklet(originJunction);
      boolean dropoff = false; // set this to true if we don't have to wait for the results of this junction.
      int rmiPort = -1; // -1 = use the WVM default
      WorkletJunction wj = new WorkletJunction(rHost, rName, rmiPort, rPort, dropoff, id, jp) {

	  private final String methodName = "main";
	  private final String [] parameters = new String[appArgs.size()];
	  private final Class [] parameterTypes = new Class[] { String[].class };
	  int _state = JunctionPlanner.STATE_WAITING;

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
	    System.out.println(" <----- WorkletJunction.execute() (implemented in SystemDispatch) ----->");

	    try {
	      Enumeration e = appArgs.elements();
	      for (int i=0; i<parameters.length; i++) parameters[i] = (String) e.nextElement();
	      appClass.getMethod(methodName, parameterTypes).invoke(null, new Object[] { parameters });

	    } catch (IllegalAccessException e) {
	      System.out.println("Exception: " + e.getMessage());
	      e.printStackTrace();
	    } catch (IllegalArgumentException e) {
	      System.out.println("Exception: " + e.getMessage());
	      e.printStackTrace();
	    } catch (InvocationTargetException e) {
	      System.out.println("Exception: " + e.getMessage());
	      e.printStackTrace();
	    } catch (NoSuchMethodException e) {
	      System.out.println("Exception: " + e.getMessage());
	      e.printStackTrace();
	    }
	  }
	};

      // in secure situations, it is possible to change the transport methods.
      if (isSecure) {
	wkl.isSecure(false); // it's possible to specify for all WorkletJunction of this worklet
	wj.isSecure(true); // or we can specify each  WorkletJunction
	// or we can manually set the tranport methods with the following
	// String[] tm = {"secureRMI", "plainRMI", "secureSocket", "plainSocket"};
	// wjxn.setTransportMethods(tm);
      }

      wkl.addJunction(wj);	// add the WorkletJunction to the Worklet
      wkl.deployWorklet(wvm);	// send it away!
    } catch (ClassNotFoundException e) {
      System.out.println("Exception: " + e.getMessage());
      e.printStackTrace();
      System.exit(0);
    }

    System.exit(0);
  }
}


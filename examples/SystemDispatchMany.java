/*
 * @(#)SystemDispatchMany.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * 
 * Copyright (c) 2002: @author Dan Phung (dp2041@cs.columbia.edu)
 * This file is a modified version of SystemDispatch.java
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
 * The <code>SystemDispatchMany</code> class is a demo example of the Worklets system that
 * shows how one create and send many WorkletJunctions that can be dropped off.
 *
 * @version	$Revision$ $Date: 2002/09/23 22:19:40
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
public class SystemDispatchMany implements Serializable {
  public static void main(String args[]) {
    WVM wvm = null;
    WorkletJunction originJunction = null;

    if (args.length < 7) {
      WVM.out.println("usage: java SystemDispatchMany rHost rName rPort App iterations waitTime label");
      System.exit(0);
    }
	
    String rHost = args[0];
    String rName = args[1];
    int rPort = Integer.parseInt(args[2]);
    
    final String App = args[3];
    int iterations = Integer.parseInt(args[4]);
    long interval = Long.parseLong(args[5]);
    String label = args[6];
    
    final Vector appArgs = new Vector();
    final Hashtable appFiles = new Hashtable();
    
    try {
      final Class appClass = Class.forName(App);
      if (wvm == null)
	wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "SystemDispatchMany");

    // Add the WorkletJunctions
      Worklet wkl = new Worklet(originJunction);
      for (int i = 0; i<5; i++){
	JunctionPlanner jp = new JunctionPlanner(iterations, interval); // jp = junction planner 
	String newlabel = label + i;
	WorkletID id = new WorkletID(newlabel);
      
	WVM.out.println("adding workletJunction: " + id);
	wkl.addJunction(new WorkletJunction(rHost, rName, -1, rPort, true, id, jp) {
	  
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
		  WVM.out.println("  wrote file: " + fName);
		} catch (IOException ioe) {
		  WVM.out.println("  could not write file: " + fName);
		}
	      }
	    }

	    public void execute() {
	      WVM.out.println(" <----- WorkletJunction.execute() (implemented in SystemDispatchMany) ----->");

	      try {
		// OLDER STUFF - 17:28:03:10:2001 appClass.newInstance();
		Enumeration e = appArgs.elements();
		for (int i=0; i<parameters.length; i++) parameters[i] = (String) e.nextElement();
		appClass.getMethod(methodName, parameterTypes).invoke(null, new Object[] { parameters });

	      } catch (Exception e) {
		WVM.out.println("Exception: " + e.getMessage());
		e.printStackTrace();
	      }
	    }
	  });
      }
    // send the Worklet
    wkl.deployWorklet(wvm);
    } catch (UnknownHostException e) {
      WVM.out.println("Exception: " + e);
      System.exit(0);
    } catch (ClassNotFoundException e) {
      WVM.out.println("Exception: " + e);
      System.exit(0);
    } 

    System.exit(0);
  }
}


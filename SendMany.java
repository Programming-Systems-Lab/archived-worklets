package psl.worklets;

/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * Copyright (c) 2001: @author Gaurav S. Kc 
 *
 * modified from SystemDispatch.java
 * @author: Dan Phung
 * dp2041@cs.columbia.edu
 * 
 * - add many junctions to this Worklet
 * 
*/


import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

public class SendMany implements Serializable {

    public static void main(String args[]) {


	WVM wvm = null;
	WorkletJunction originJunction = null;

	if (args.length < 7) {
	    WVM.out.println("usage: java SendMany <rHost> <rName> <rPort> <App> <iterations> <waitTime> <label>");
	    System.exit(0);
	}
	
	String rHost = args[0];
	String rName = args[1];
	int rPort = Integer.parseInt(args[2]);

	final String App = args[3];
	int iterations = Integer.parseInt(args[4]); // for the JunctionPlanner
	long interval = Long.parseLong(args[5]); // for the JunctionPlanner

	String label = args[6]; // label for the WorkletID

	final Vector appArgs = new Vector();
	final Hashtable appFiles = new Hashtable();

	try {
	    final Class appClass = Class.forName(App);

	    if (wvm == null) {
		wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "SendMany");
	    }

	    // **************** ADDING THE WORKLET JUNCTION ********************** //

	    Worklet wkl = new Worklet(originJunction);

	    for (int i = 0; i<5; i++){
		JunctionPlanner jp = new JunctionPlanner(iterations, interval); // jp = junction planner 
		String newlabel = label + i;
		WorkletID id = new WorkletID(newlabel);

		System.out.println("adding workletJunction: " + id);
		wkl.addJunction(new WorkletJunction(rHost, rName, rPort, true, id, jp) {

			private final String methodName = "main";
			private final String [] parameters = new String[appArgs.size()];
			private final Class [] parameterTypes = new Class[] { String[].class };
			int _state = JunctionPlanner.STATE_WAITING;
			// ((JunctionPlanner)jp).setParent(this);

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
			System.out.println(" <----- WorkletJunction.execute() (implemented in SendMany) ----->");

			try {
			    // OLDER STUFF - 17:28:03:10:2001 appClass.newInstance();
			    Enumeration e = appArgs.elements();
			    for (int i=0; i<parameters.length; i++) parameters[i] = (String) e.nextElement();
			    appClass.getMethod(methodName, parameterTypes).invoke(null, new Object[] { parameters });

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
	    }
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

	System.exit(0);
    }
}


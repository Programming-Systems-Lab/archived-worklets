/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * Copyright (c) 2001: @author Gaurav S. Kc 
 *
 * modified from SystemDispatch.java
 * @author: Dan Phung
 * dp2041@cs.columbia.edu
 * 
 * 
*/


import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

public class SendSecure implements Serializable {

    public static void main(String args[]) {


	psl.worklets.WVM wvm = null;
	psl.worklets.WorkletJunction originJunction = null;

	if (args.length < 7) {
	    System.out.println("usage: java SendSecure <rHost> <rName> <rPort> <App> <iterations> <waitTime> <label>");
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

	    // This is where we get the security parameters
	    psl.worklets.OptionsParser op = new psl.worklets.OptionsParser();
	    op.loadWVMFile(System.getProperty("WVM_FILE"));
	    op.setWVMProperties();

	    try {
	      wvm = new psl.worklets.WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "SendSecure", op.port, 
					 op.keysfile, op.password, op.ctx, op.kmf, op.ks, op.rng, op.securityLevel);
	    } catch (java.net.UnknownHostException e){
	    }

	    // **************** ADDING THE PLANNER ********************** //
	    
	    
	    psl.worklets.JunctionPlanner jp = new psl.worklets.JunctionPlanner(iterations, interval); // jp = junction planner 
	    psl.worklets.WorkletID id = new psl.worklets.WorkletID(label);
	    
	    // **************** ADDING THE WORKLET JUNCTION ********************** //

	    psl.worklets.Worklet wkl = new psl.worklets.Worklet(originJunction);

	    psl.worklets.WorkletJunction wjxn = new psl.worklets.WorkletJunction(rHost, rName, -1, rPort, false, id, jp) {
		    private final String methodName = "main";
		    private final String [] parameters = new String[appArgs.size()];
		    private final Class [] parameterTypes = new Class[] { String[].class };
		    int _state = psl.worklets.JunctionPlanner.STATE_WAITING;
		    // ((JunctionPlanner)jp).setParent(this);

		    public void init(Object _system, psl.worklets.WVM _wvm) {
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
			System.out.println(" <----- WorkletJunction.execute() (implemented in SendSecure) ----->");

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

	    // CHANGING Security level
	    wkl.isSecure(false); // doesn't matter because the lowest level, 
	    wjxn.isSecure(true); // ie. the worklet junction security has the highest priority
	    // we can either set the transport methods here, or rely on the default 
	    // method for "secure" worklet junctions
	    String[] tm = {"secureRMI", "plainRMI", "secureSocket", "plainSocket"};
	    wjxn.setTransportMethods(tm);

	    wkl.addJunction(wjxn);
	    wkl.deployWorklet(wvm);

	} catch (ClassNotFoundException e) {
	    System.out.println("Exception: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(0);
	} 

	System.exit(0);
    }
}


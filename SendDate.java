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
 * 
*/


import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

public class SendDate implements Serializable {

    public static void main(String args[]) {


	WVM wvm = null;
	WorkletJunction originJunction = null;

	if (args.length < 7) {
	    WVM.out.println("usage: java Send <rHost> <rName> <rPort> <Date>");
	    System.exit(0);
	}
	
	String rHost = args[0];
	String rName = args[1];
	int rPort = Integer.parseInt(args[2]);

	int year = Integer.parseInt(args[3]); // for the JunctionPlanner
	int month = Integer.parseInt(args[4]); // for the JunctionPlanner
	int day = Integer.parseInt(args[5]); // for the JunctionPlanner
	int hour = Integer.parseInt(args[6]); // for the JunctionPlanner
	int minute = Integer.parseInt(args[7]); // for the JunctionPlanner

	try {
	    if (wvm == null) {
		wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "SendDate");
	    }
      

	    // **************** ADDING THE PLANNER ********************** //
	    
            Calendar cal = Calendar.getInstance();
            cal.set(1900+year, month, day, hour, minute);
            JunctionPlanner jp = new JunctionPlanner(cal.getTime()); // jp = junction planner 
	    WorkletID id = new WorkletID("dated junction");

	    // **************** ADDING THE WORKLET JUNCTION ********************** //

	    Worklet wkl = new Worklet(originJunction);

	    wkl.addJunction(new WorkletJunction(rHost, rName, -1, rPort, false, id, jp) {

		    private final String methodName = "main";
		    // private final String [] parameters = new String[appArgs.size()];
		    // private final Class [] parameterTypes = new Class[] { String[].class };
		    // int _state = JunctionPlanner.STATE_WAITING;
		    // ((JunctionPlanner)jp).setParent(this);

		    public void init(Object _system, WVM _wvm) {
		    }

		    public void execute() {
			System.out.println(" <----- WorkletJunction.execute() (implemented in SendDate) ----->");

			System.out.println("Hello!  The date is: " + new Date());
		    }
		});

	    wkl.deployWorklet(wvm);

	} catch (UnknownHostException e) {
	    WVM.out.println("Exception: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(0);
	} 

	System.exit(0);
    }
}


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
 * - send the notifier to get a certain worklet junction with a label
 *  and notify it to wakeUp().
 * 
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

public class SendNotify implements Serializable {

    public static void main(String args[]) {

	WVM wvm = null;
	WorkletJunction originJunction = null;

	if (args.length < 3) {
	    WVM.out.println("usage: java SendNotify <rHost> <rName> <rPort> <label>");
	    System.exit(0);
	}
	
	String rHost = args[0];
	String rName = args[1];
	int rPort = Integer.parseInt(args[2]);
	final String mylabel = args[3];

	try {
	    if (wvm == null) {
		wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "SendNotify");
	    }

	    // **************** ADDING THE WORKLET JUNCTION ********************** //

	    Worklet wkl = new Worklet(originJunction);

	    WorkletID id = new WorkletID("notifier");
	    wkl.addJunction(new WorkletJunction(rHost, rName, -1, rPort, 
						false, id, null) {

		    public void init(Object _system, WVM _wvm) {
		    }

		    public void execute() {
			// _wvm is the remote WVM where this junction will execute.
			Vector v = _wvm.getJunctions(new WorkletID(mylabel));
			if (v != null) {
			    Iterator itr = v.iterator();
			    while (itr.hasNext()){
				WorkletJunction wj = (WorkletJunction)itr.next();
				wj.wakeUp();
			    }
			} else {
			    System.out.println("no junctions found with worketID: "+ mylabel);
			}
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

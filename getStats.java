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

public class getStats implements Serializable {

    public static void main(String args[]) {

	WVM wvm = null;
	WorkletJunction originJunction = null;

	if (args.length < 3) {
	    WVM.out.println("usage: java getStats <rHost> <rName> <rPort>");
	    System.exit(0);
	}
	
	String rHost = args[0];
	String rName = args[1];
	int rPort = Integer.parseInt(args[2]);

	try {
	    if (wvm == null) {
		wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "getStats");
	    }

	    // **************** ADDING THE WORKLET JUNCTION ********************** //

	    Worklet wkl = new Worklet(originJunction);

	    WorkletID id = new WorkletID("getStats");

	    wkl.addJunction(new WorkletJunction(rHost, rName, rPort, id) {

		    public void init(Object _system, WVM _wvm) {
		    }

		    public void execute() {
			// _wvm is the remote WVM where this junction will execute.
			_wvm.stats();
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

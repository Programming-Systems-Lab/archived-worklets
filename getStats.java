/*
 * @(#)$Name$ $Date$
 * 
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * Copyright (c) 2001: @author Dan B. Phung
 *
 * CVS version control block - do not edit manually
 *  $Name$
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

package psl.worklets;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;


/*
 * The <code>getStats</code> class is an demo example of the Worklets system that  
 * shows how one would get useful stats from the WVM.  This file is modified from 
 * SystemDispatch.java.
 * 
 * @version	$Revision$ $Date$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 *
*/

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

	    wkl.addJunction(new WorkletJunction(rHost, rName, -1, rPort, false, id, null) {

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


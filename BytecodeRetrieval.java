package psl.worklets;

/**
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * 
 * Copyright (c) 2001: @author Gaurav S. Kc
 * 
*/

import java.io.*;
import java.util.*;
import psl.worklets.http.*;

class BytecodeRetrieval implements Serializable {

  BytecodeRetrieval(final HashSet _hs, WVM wvm,
    String _host, String _name, int _port,
    String rHost, String rName, int rPort) {
    // WVM.out.println("da BytecodeRetrieval is alive");

    Worklet wkl = new Worklet(new WorkletJunction(_host, _name, _port) {
      // This worklet will execute when it comes back to the sender
      // it is the origin junction
      public void execute() {
        // WVM.out.println("da BytecodeRetrieval is alive @ origin");
        Enumeration keys = _payload.keys();
        while (keys.hasMoreElements()) {
          String key = (String) keys.nextElement();
          Hashtable ht = (psl.worklets.http.ClassServer).bytecodeCache;
          if (ht.containsKey(key)) {
            WVM.out.println(key + ": bytecode is already in http cache!!!");
            continue;
          }
          // WVM.out.println(key + ": bytecode put in http cache!!!");
          
          byte []bytecode = (byte []) _payload.get(key);
          ht.put(key, bytecode);
        }
      }
    });

    wkl.addJunction(new WorkletJunction(rHost, rName, rPort) {
      // This worklet will execute at the target site
      // It will extract the bytecode from the tartet http server's
      // cache and add to payload of the origin junction
      HashSet hs = _hs;
      public void execute() {
        // WVM.out.println("da BytecodeRetrieval is alive @ target: " + hs.size());
        Iterator it = hs.iterator();
        while (it.hasNext()) {
          String key = (String) it.next();
          Hashtable ht = (psl.worklets.http.ClassServer).bytecodeCache;
          if (ht.containsKey(key)) {
            // WVM.out.println(key + ": bytecode put in payload of originJunction!!!");
            _originJunction._payload.put(key, ht.get(key));
          } else {
            WVM.out.println(key + ": bytecode is not in http cache!!!");
          }
        }
      }
    });

    wkl.deployWorklet(wvm);

  }

}

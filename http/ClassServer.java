/*
 * @(#)ClassFileServer.java
 *
 * Copyright (c) 1996, 1996, 1997 Sun Microsystems, Inc.p All Rights Reserved.
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * Copyright (c) 2001: Copyright MageLang Institute
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Peppo Valetto
 * modified by Peppo Valetto
 * modified by Gaurav S. Kc [19 February, 2001]
 * Last modified by: Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */
package psl.worklets.http;

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.util.*;
import psl.worklets.*;

/**
 * ClassServer is an abstract class that provides the
 * basic functionality of a mini-webserver, specialized
 * to load class files only. A ClassServer must be extended
 * and the concrete subclass should define the <b>getBytes</b>
 * method which is responsible for retrieving the bytecodes
 * for a class.<p>
 *
 * The ClassServer creates a thread that listens on a socket
 * and accepts  HTTP GET requests. The HTTP response contains the
 * bytecodes for the class that requested in the GET header. <p>
 *
 * For loading remote classes, an RMI application can use a concrete
 * subclass of this server in place of an HTTP server. <p>
 *
 * @see ClassFileServer
 */
public abstract class ClassServer implements Runnable {
  protected ServerSocket server = null;
  protected int port;

  /**
   * Constructs a ClassServer that listens on <b>port</b> and
   * obtains a class's bytecodes using the method <b>getBytes</b>.
   *
   * @param port the port number
   * @exception IOException if the ClassServer could not listen
   *            on <b>port</b>.
   */
  protected ClassServer(int aPort) throws IOException{
    this(aPort, null);
  }

  protected ClassServer(int aPort, WVM_SSLSocketFactory WVM_sf) throws IOException {
    // Setup socket
    this.port = aPort;
    while (this.port >= aPort) {
      try {
	if (WVM_sf != null) server = WVM_sf.createServerSocket(this.port);
	else server = new ServerSocket(this.port);
        break;
      } catch (UnknownHostException e) {
        // whut? not possible!
      } catch (IOException e) {
        // oops, must try another port number;
        port += 1; // gskc:27aug2002:1736 100
      }
    }
    // WVM.out.println ("Class server listening on Web port: " + this.port);
    // server = new ServerSocket(this.port);
    newListener();
  }

  /**
   * Returns an array of bytes containing the bytecodes for
   * the class represented by the argument <b>path</b>.
   * The <b>path</b> is a dot separated class name with
   * the ".class" extension removed.
   *
   * @return the bytecodes for the class
   * @exception ClassNotFoundException if the class corresponding
   * to <b>path</b> could not be loaded.
   * @exception IOException if error occurs reading the class
   */
  public abstract byte[] getBytes(String path) throws IOException, ClassNotFoundException;
  // Will employ WorkgroupCache module instead of a hashtable
  private static final Hashtable bytecodeCache = new Hashtable();
  public static void put(String name, byte[] bytecode) {
    if (! bytecodeCache.containsKey(name)) {
      bytecodeCache.put(name, bytecode);
    }
  }
  public static byte[] get(String name) {
    return ((byte[]) bytecodeCache.get(name));
  }
  public static boolean containsKey(String name) {
    return (bytecodeCache.containsKey(name));
  }
      
  /**
   * The "listen" thread that accepts a connection to the
   * server, parses the header to obtain the class file name
   * and sends back the bytecodes for the class (or error
   * if the class is not found or the response was malformed).
   */
  public void run() {
    Socket socket = null;
    // accept a connection
    try {
      socket = server.accept();
      // create a new thread to accept the next connection
      newListener();
    } catch (IOException e) {
      // WVM.out.println("Class Server died: " + e.getMessage());
      // e.printStackTrace();
      return;
    }
    
    try {
      DataOutputStream out = new DataOutputStream(socket.getOutputStream());
      try {
        // get path to class file from header
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String path = getPath(in);
        // retrieve bytecodes
        byte[] bytecodes = null;
        if (containsKey(path)) {
          bytecodes = get(path);
          // WVM.out.println(" + + + Serving cached bytecode for class: " + path);
        } else {
          // retrieve bytecodes from the file system
          bytecodes = getBytes(path);
          // cache the bytecodes to be sent out
          put(path, bytecodes);
          // WVM.out.println(" + + + Caching binary data for file: " + path);
        }

        // WVM.out.println("Retrieved bytecodes: " + bytecodes.length);

        // NOT RELEVANT ANY MORE: okay, the bytecode is definitely available locally
        // NOT RELEVANT ANY MORE: TODO: update the BAG-MULTISET for the http client
        // InetAddress ip = socket.getInetAddress();
        // int port = socket.getPort();
        // WVM.out.println("ip: " + ip);
        // WVM.out.println("port: " + port);
        // this will pose a problem: how do we figure out which process
        // on the remote site made this request ... ie which BAG-MULTISET
        // do we update for this http request?


        // send bytecodes in response (assumes HTTP/1.0 or later)
        try {
          // TODO: do an http put on the remote site's webserver
          // the remote site's webserver's port is specified in this http request header
          out.writeBytes("HTTP/1.0 200 OK\r\n");
          out.writeBytes("Content-Length: " + bytecodes.length + "\r\n");
          out.writeBytes("Content-Type: application/java\r\n\r\n");
          out.write(bytecodes);
          out.flush();
          // WVM.out.println(WVM.time() + " Wrote: " + path + " out to http client: " + bytecodes.length);
        } catch (IOException ie) {
          return;
        }
      } catch (Exception e) {
        // write out error response
        out.writeBytes("HTTP/1.0 400 " + e.getMessage() + "\r\n");
        out.writeBytes("Content-Type: text/html\r\n\r\n");
        out.flush();
      }
    } catch (IOException ex) {
      // eat exception (could log error to log file, but
      // write out to stdout for now).
      // dp2041: I commented out next two lines.  possible errors are:
      // 1) wrong protol (file/https) from class loading
      // WVM.out.println("error writing response: " + ex.getMessage());
      // ex.printStackTrace();
    } finally {
      try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
    }
  }
      
  public void shutdown() {
    WVM.out.println ("    Shutting down Class Server");
    try {
      if (server != null)
	server.close();
      server = null;
    } catch (IOException e) { }
    // WVM.out.println ("Class Server shut down");
  }

  /**
   * Create a new thread to listen.
   */
  private void newListener() {
    (new Thread(this)).start();
  }

  /**
   * Returns the path to the class file obtained from
   * parsing the HTML header.
   */
  private static String getPath(BufferedReader in) throws IOException {
    String line = in.readLine();
    // WVM.out.println("\n + + + request is: " + line);
    String path = "";
    // extract class from GET line
    if (line.startsWith("GET /")) {
      line = line.substring(5, line.length()-1).trim();
      int index = line.indexOf(".class ");
      if (index != -1) {
        path = line.substring(0, index).replace('/', '.');
      } else {
        path = new StringTokenizer(line, " ").nextToken();
      }
      if (WVM.DEBUG(3)) WVM.out.println("path is: " + path);
    } else {
      throw new IOException("Malformed Header in Class request");
    }

    // eat the rest of header
    do {
      line = in.readLine();
      // WVM.out.println (" + + + " + line);
    } while ((line.length() != 0) && (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));
    if (path.length() != 0) {
      // WVM.out.println("Edited: gskc, 19Feb01 --- returning path: " + path);
      return path;
    } else {
      throw new IOException("Malformed Header");
    }
  }
}

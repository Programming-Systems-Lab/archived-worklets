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
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.jar.*;
import java.util.zip.*;
import psl.worklets.*;

/**
 * The ClassFileServer implements a ClassServer that
 * reads class files from the file system. See the
 * doc for the "Main" method for how to run this
 * server.
 */
public class ClassFileServer extends ClassServer {
  private String default_codebase = null;
  private Vector aliases;

  private final static int DefaultServerPort = 9180;

  /**
   * Constructs a ClassFileServer.
   *
   * @param port the port to connect to this ClassFileServer
   * @param default_codebase the classpath where the server locates classes
   */
  public ClassFileServer(int port, String codebase) throws IOException {
    this(port, codebase, null);
  }

  public ClassFileServer(int port, String codebase, WVM_SSLSocketFactory wvm_sf) throws IOException {
    this("http", "localhost", port, codebase, null);  }
  public ClassFileServer(String protocol, String host, int port, String codebase, WVM_SSLSocketFactory wvm_sf)
    throws IOException {

    super(port, wvm_sf);
    this.default_codebase = codebase;
    aliases = new Vector();

    String sysPath = System.getProperty("java.class.path");
    String separator = System.getProperty("path.separator");
    StringTokenizer cpTok = new StringTokenizer(sysPath, separator);
    if (WVM.DEBUG(4)) WVM.out.println("ClassPath is: " + sysPath);
    String token;

    while (cpTok.hasMoreTokens()) {
      token = cpTok.nextToken();
      if (token.compareTo(".") == 0) {
        token = System.getProperty("user.dir");
      }
      aliases.addElement(token);
      if (WVM.DEBUG(5)) WVM.out.println("\t" + token);
    }
    if (WVM.DEBUG(5)) WVM.out.println("Ended ClassFileServer");        toString = protocol + "://" + host + ":" + getWebPort() + "/";
  }

  public int getWebPort () { return port; }
  public Vector getAliases() { return aliases; }  private final String toString;
  public String toString () { return toString; }


  /**
   * Returns an array of bytes containing the bytecodes for
   * the class represented by the argument <b>path</b>.
   * The <b>path</b> is a dot separated class name with
   * the ".class" extension removed.
   *
   * @return the bytecodes for the class
   * @exception ClassNotFoundException if the class corresponding
   * to <b>path</b> could not be loaded.
   */
  public byte[] getBytes(String path) throws IOException, ClassNotFoundException {
    byte[] bytecodes = null;
    File f = null;

    if (WVM.DEBUG(3)) WVM.out.println ("ClassFileServer.getBytes(" + path + ")");

    if (default_codebase != null) {
      f = findFile(default_codebase, path);
      if (WVM.DEBUG(3)) WVM.out.println("findFile returned: " + f);
      if (f != null && f.exists() && !f.isDirectory()) {
        WVM.out.println (f.getPath() + " found in default classpath");
        String default_codebaseDup = default_codebase.toLowerCase();
        if (f.isFile() && (default_codebaseDup.endsWith(".jar") || default_codebaseDup.endsWith(".zip"))) {
          bytecodes = jarExtract(f, path);
          if (bytecodes != null) {
            return bytecodes;
          }
        } else {
          return classExtract(f);
        }
      }
    }

    // at this point we have to check aliases
    if (WVM.DEBUG(3)) WVM.out.println (aliases.size() + " aliases to check");
    Iterator iter = aliases.iterator();

    while (iter.hasNext()) {

      // WVM.out.println ("Iterating on aliases ...");
      String cpItem = (String) iter.next();
      // WVM.out.println("Classpath for classFileServer: " + cpItem);
      f = findFile (cpItem, path);
      if (WVM.DEBUG(3)) WVM.out.println("findFile returned: " + f);
      if (f!= null && f.exists() && !f.isDirectory()) {
        if (WVM.DEBUG(2)) WVM.out.println ("Path: " + f.getPath() + ", name: " + f.getName());
        String cpItemDup = cpItem.toLowerCase();
        if (f.isFile() && (cpItemDup.endsWith(".jar") || cpItemDup.endsWith(".zip"))) {
          bytecodes = jarExtract(f, path);
          if (bytecodes != null) {
            return bytecodes;
          }
        } else {
          return classExtract(f);
        }
      }
    }

    if (f == null || !f.exists()) {
      // WVM.out.println( path + " Not Found" );
      throw new IOException("Class cannot be served " + path);
    }

    return bytecodes;
  }

  private byte[] classExtract(File f)  throws IOException {
    if (WVM.DEBUG(2)) WVM.out.println ("\tin classExtract: " + f.getPath());
    int length = (int) f.length();
    FileInputStream fin = new FileInputStream(f);
    DataInputStream in = new DataInputStream(fin);

    byte[] classData = new byte[length];
    in.readFully(classData);
    in.close();
    if (WVM.DEBUG(4)) WVM.out.println("\treturning from classExtract w/ " + classData.length);
    return classData;
  }

  private byte[] jarExtract (File f, String path) throws IOException, ZipException {
    if (WVM.DEBUG(2)) WVM.out.println ("\tin jarExtract: " + f.getName());
    byte[] classData = null;
    ZipFile zipfile = new ZipFile(f);

    // Gskc -- 1824-1810-2001
    String jarEntryName = path;
    ZipEntry zipentry = zipfile.getEntry(jarEntryName);

    if (zipentry == null) {
      jarEntryName = path.replace ('.', '/');
      zipentry = zipfile.getEntry(jarEntryName + ".class");
    }

    // WVM.out.println ("\t" + jarEntryName);

    if (zipentry != null) {
      try {
        if (WVM.DEBUG(3)) WVM.out.println (jarEntryName + " found in JAR archive: " + zipfile.getName());
        DataInputStream dis = new DataInputStream(zipfile.getInputStream(zipentry));
        classData = new byte[dis.available()];
        // WVM.out.println ("Reading in: " + dis.available());
        dis.readFully(classData);
        dis.close();
      }  catch(IOException e) {
        // WVM.out.println(path + " in file " + f.getName() + " could not be opened:");
        e.printStackTrace();
      }
    }
    return classData;
  }

  private File findFile(String rootPath, String path) {
    if (WVM.DEBUG(2)) WVM.out.println ("In findFile() searching in: " + rootPath + " for: " + path);
    try {
      File pathItem = new File(rootPath);
      if (pathItem.isDirectory()) {
        if (WVM.DEBUG(3)) WVM.out.println ("IN DIR: " + pathItem.getPath());
        if (! rootPath.endsWith(File.separator)) {
          rootPath = rootPath + File.separator;
        }
        File file;
        String filePath;

        filePath = rootPath +  path;
        if ((file = new File(filePath)).exists()) {
          if (WVM.DEBUG(3)) WVM.out.println ("returning a non .class file");
          return file;
        }

        filePath = rootPath +  path.replace('.', File.separatorChar) + ".class";
        if ((file = new File(filePath)).exists()) {
          if (WVM.DEBUG(3)) WVM.out.println ("returning a .class file");
          return file;
        }

      }
      if (WVM.DEBUG(3)) WVM.out.println ("NO DIR: " + pathItem.getPath());
      return pathItem;
    } catch (SecurityException e) {
      // WVM.out.println ("READ access denied to " + rootPath);
      // e.printStackTrace();
      return null;
    }
  }


  /**
   * Main method to create the class server that reads
   * class files. This takes two command line arguments, the
   * port on which the server accepts requests and the
   * root of the classpath. To start up the server: <br><br>
   *
   * <code>   java ClassFileServer <port> <classpath>
   * </code><br><br>
   *
   * The codebase of an RMI server using this webserver would
   * simply contain a URL with the host and port of the web
   * server (if the webserver's classpath is the same as
   * the RMI server's classpath): <br><br>
   *
   * <code>   java -Djava.rmi.server.codebase=http://zaphod:2001/ RMIServer
   * </code> <br><br>
   *
   * You can create your own class server inside your RMI server
   * application instead of running one separately. In your server
   * main simply create a ClassFileServer: <br><br>
   *
   * <code>   new ClassFileServer(port, classpath);
   * </code>
   */
  public static void main(String args[]) {
    int port = DefaultServerPort;
    String classpath = "";
    String keysFile = "";
    String password = "";
    WVM_SSLSocketFactory wvm_sf = null;

    if (args.length >= 1) {
      port = Integer.parseInt(args[0]);
    }

    if (args.length >= 2) {
      classpath = args[1];
    }

    if (args.length >= 3 && args.length <= 4) {
      keysFile = args[3];
      password = args[4];

      try {
        wvm_sf = new WVM_SSLSocketFactory(keysFile, password);
      } catch (Exception e){
        WVM.err.println("Caught exception creating WVM_SSLSocketFactory: " + e);
      }
    }

    try {
      new ClassFileServer(port, classpath, wvm_sf);
      // WVM.out.println("ClassFileServer started...");
    } catch (IOException e) {
      // WVM.out.println("Unable to start ClassServer: " + e.getMessage());
      e.printStackTrace();
    }
  }
}

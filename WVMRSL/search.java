/*
 * @(#)search.java
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2002: @author Alex Bogomolov
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */
package psl.worklets.WVMRSL;

import java.io.*;
import java.net.*;
import java.util.Date;

public class search{

  final MulticastSocket mcs;
  private static String addr;
  private static InetAddress mcastAddr;
  private static int port;
  private static int listen_port;
  private static String file_name;
  private static String local_host_ip;
  boolean locked = true;
  public static String result;

  public static void main(String args[]) throws IOException {
    System.out.println("usage: java search -m <multicast_addr> -p <port> -f <file_name> -l <listen_port>");
    
    addr = "228.5.6.7";
    port = 1234;
    listen_port = 5000; //default
    file_name = new String("search.xml");//default


    int i = -1;
    while(++i < args.length) {
      if (args[i].equals("-m")) addr = args[++i];
      if (args[i].equals("-p")) port = Integer.parseInt(args[++i]);
      if (args[i].equals("-f")) file_name = args[++i];
      if (args[i].equals("-l")) listen_port = Integer.parseInt(args[++i]);	
    }
    
    local_host_ip = InetAddress.getLocalHost().getHostAddress();
    mcastAddr = InetAddress.getByName(addr);
    search srch = new search();
    while(result == null);
    System.out.println(srch.result);
    //srch.SendRequest();
    //srch.GetResponse();
  }


  //have to be multithreaded
  //otherwise won't get response from server - 
  //not enough time to accept connection
  private search() throws IOException {
    mcs = new MulticastSocket(port);
    mcs.joinGroup(mcastAddr);
    new SearchThread(this,true).start();
    new SearchThread(this,false).start();
  }
  
  public search(int port,String multiaddr,int multi_port,String query) throws IOException{
    mcastAddr = InetAddress.getByName(addr);
    mcs = new MulticastSocket(port);
    if(mcs!= null)
      mcs.joinGroup(mcastAddr);
    new SearchThread(this,true).start();
    new SearchThread(this,false,query).start();
  }

  public static void Search(int port, String id,String multiaddr,int multi_port,String query){
    try{
      addr = multiaddr;
      port = multi_port;
      listen_port = port;
      local_host_ip = InetAddress.getLocalHost().getHostAddress();
      
    } catch (Exception e){
      System.out.println("Search failed to initialize due to networking error:");
      e.printStackTrace();
    }
    if(id != null){
      if(!id.equals(""))
        id = new String("<ID VALUE=\"" + id + "\"/>");
    }
    
    query = new String("<?xml version='1.0' encoding='utf-8'?><WVM_DESCRIPTION>"+id+query+"</WVM_DESCRIPTION>");
    try{
      search s = new search(port,multiaddr,multi_port,query);
      
      System.out.println("Result = " + s.result);
    }catch(Exception e){
      e.printStackTrace();
    }
  }
  

  
  //query is gotten from file
  public void SendRequest(){
    while(locked){
      ;
    }
    try{
      FileInputStream f = new FileInputStream(file_name);
      byte [] b = new byte[10240];
      int i = f.read(b);
      String s = new String(b,0,i);
      String msg_id = new String("search_"+ String.valueOf(new Date().getTime()));
      
      s = "SEARCH:"+msg_id+":"+local_host_ip+":"+listen_port+":" + s;
      try {
        mcs.send(new DatagramPacket(s.getBytes(), s.length(),
                                    mcastAddr, port));
      } catch (IOException ioe) { 
        ioe.printStackTrace();
        System.out.println("Unable to send request");
      }
    } catch (IOException ioe) { 
      ioe.printStackTrace();
      System.out.println("Unable to send request");
    }
  }


  //query is supplied in the param
  public void SendRequest(String q){
    while(locked){
      ;
    }
    
    
    String msg_id = new String("search_"+ String.valueOf(new Date().getTime()));
    
    String s = "SEARCH:"+msg_id+":"+local_host_ip+":"+listen_port+":" + q;
    try {
      mcs.send(new DatagramPacket(s.getBytes(), s.length(),
                                  mcastAddr, port));
    } catch (IOException ioe) { 
      ioe.printStackTrace();
      System.out.println("Unable to send request");
    }
    
  }
  
  public void GetResponse(){
    ServerSocket my_socket = null;
    try {
      my_socket = new ServerSocket(listen_port);
      System.err.println("Created socket on port: " + listen_port);
    } catch (IOException e) {
      System.err.println("Could not listen on port: " + listen_port);
      System.exit(-1);
    }
    locked = false;
    try {	
      Socket socket = my_socket.accept();
      BufferedReader in = null;		
      in = new BufferedReader( new InputStreamReader( socket.getInputStream()));	    
      String incoming = new String();
      String temp;
      while((temp = in.readLine())!=null)
        incoming = incoming + temp;
      
      result = new String(incoming);
      //System.out.println("RECEIVED SEARCH RESPONSE:\n"+incoming);
    }
    catch (Exception e) {
      System.err.println("Error getting server response...");
      e.printStackTrace();
      result = new String("ERROR");
    }
    try{
      my_socket.close();
    }catch(Exception e){}
  }
  


  public class SearchThread extends Thread {
    search r;
    boolean thread_flag;
    boolean use_string = false;
    public String query;

    public SearchThread(search reg,boolean flag){
      r = reg;
      thread_flag = flag;
    }
    
    public SearchThread(search reg,boolean flag,String q){
      query = q;
      r = reg;
      thread_flag = flag;
      use_string = true;
    }

    public void run(){
      if(thread_flag)
        r.GetResponse();
      else{
        try{
          this.sleep(1000);
        }catch (Exception e){}
        if(use_string)
          r.SendRequest(query);
        else
          r.SendRequest();
      }
    }
  }
  
}

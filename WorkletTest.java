/**
 *@author: Peter Davis
 *ptd7@cs.columbia.edu
 *
 *Modification of psl.worklets.SystemDispatch.java
 *
 *This class was made from SystemDispatch.java (author: Gaurav Kc, gskc@cs.columbia.edu)
 *except that modifications have been made to handle multiple junctions.
 
 *This program deploys worklets to multiple junctions,
 *as a demonstration of multiple junction capability
 *
 *
 *Testing: 
 *Worklet containing java program called Test.java (Test.java simply prints something to stdout)
 *sent from diamond.cs.columbia.edu
  to: 
  disco.cs.columbia.edu
  dynamo.cs.columbia.edu
  dynasty.cs.columbia.edu
  *status: 
  *8/7/2001: 
  *2:30 p.m. 


  *works for  config file of:
Test,dynamo.cs.columbia.edu,WVM1,9101
Test,disco.cs.columbia.edu,WVM1,9101
Test,dynasty.cs.columbia.edu,WVM1,910

  setup:
  WorkletTest run on diamond.cs
  psl.worklets.WVM run on {disco.cs,dynamo.cs,dynasty.cs}.columbia.edu
  Application successfully sent to all three hosts through sockets.
  RMI did not work for all three hosts.

  *
  *
  */


import java.io.*;
import java.net.*;
import psl.worklets.*;
import java.util.*;

public class WorkletTest implements Serializable {
   
    private static final int DEFAULT_PORT = 9100;

    public static void main(String args[]) {
	WorkletTest wt = new WorkletTest();
    }

    private WorkletTest() {
	
	
	/*****modification, ptd7***/
	try{
	      	    WVM wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), 
				      "WorkletTest");
	  	    Worklet wkl = new Worklet(null);

		    String App = null;
		    String rHost = null;
		    String rName = null;
		    int rPort = 0;
		    
		    FileHandle fh = new FileHandle("config.txt",FileHandle.IN);
		    String input_info =null;
		    
		    while((input_info = fh.readln()) != null){
			StringTokenizer reader = new StringTokenizer(input_info,",");
			if(reader.countTokens() >=4){
			    
			    //System.out.println("input from cfg file is: " + input_info);
			    if(input_info.charAt(0) != '#' && input_info.charAt(0) != ' '){
				/**make sure that it is valid input**/
				/**input must be of form: <app>,<host>,<name>,<port>**/
				App = reader.nextToken();
				rHost = reader.nextToken();
				rName = reader.nextToken();
				try{
				    rPort = Integer.parseInt(reader.nextToken());
				}catch(NumberFormatException e){
				    System.out.println("error parsing port. . . setting to " 
						       + DEFAULT_PORT +" . . .");
				    rPort = DEFAULT_PORT;
				}
				/*************add new junctions********************/
				/****testing***/
				
				/****end testing***/
				final Class appClass = Class.forName(App);
				//  	    // final Class appClass = Class.forName("gskc.TicTacToe");
				
				
				wkl.addJunction(new WorkletJunction(rHost, rName, rPort) {
					public void execute() {
					    WVM.out.println("\t --- Totally New Component ---");
					    try {
						appClass.newInstance();
					    } catch (InstantiationException e) {
						WVM.out.println("Exception: " + e.getMessage());
						e.printStackTrace();
					    } catch (IllegalAccessException e) {
						WVM.out.println("Exception: " + e.getMessage());
						e.printStackTrace();
					    }
					}
				    });
				System.out.println("just added a junction at: ");
				System.out.println("App: " + App+ " rHost: " + rHost
						   + "  rName: " + rName + " rPort: " + rPort ); 

                               /***********end, add new junctions*****************/
			    }
			}
		    }
		    fh.close();
		    
		    
		    /*****end modification, ptd7***/
		    System.out.println("about to deploy worklet");
		    wkl.deployWorklet(wvm); //deploy worklet, with junctions from config file
		    System.out.println("just deployed worklet");
	
	} catch (UnknownHostException e) {
	    WVM.out.println("Exception: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(0);
	} catch (ClassNotFoundException e) {
	    WVM.out.println("Exception: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(0);
	} 
    }    
}




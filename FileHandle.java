/**


 *enables you to read and write to a file


 *this class handles all of the messiness and details of file I/O


 * 





 *note: not closing file handle may cause file handle not to write anything 


 */

package psl.worklets;
  


import java.io.*;





public class FileHandle{





public static final int IN=1;


public static final int OUT=2;


private FileReader file;


private BufferedReader filein;


private PrintWriter fileout;





public FileHandle(String io,int type){


	if(type ==IN){


	 	file=null;


    		filein=null;


    		fileout=null;


        	try{


	  	   file = new FileReader(io);


	  	   filein = new BufferedReader(file);


	  


  	           


	  


		}catch(Exception e){


	           System.out.println("error reading file. . .exiting. . .");


	           System.exit(1);


	        }


	}else{


		file=null;


    		filein=null;


    		fileout=null;





		try{


		  /*out file stuff. . .*/


	           fileout = new PrintWriter( new BufferedWriter(


                                         new FileWriter( io ) ) );


		}catch(Exception e){


	  	  System.out.println("error reading file. . .exiting. . .");


	  	  System.exit(1);


		}	


	}


}





  /**


   *params: in -- in file


   */


  public FileHandle(String in){


    file=null;


    filein=null;


    fileout=null;


        try{


	  file = new FileReader(in);


	  filein = new BufferedReader(file);


	  


  	  /*out file stuff. . .*/


	  fileout = new PrintWriter(  System.out  );


	  


	}catch(Exception e){


	  System.out.println("error reading file. . .exiting. . .");


	  System.exit(1);


	}


  }





  /**


   *Two arg constructor


   *params: 	in -- in file


   		out -- out file


   *


   */


  public FileHandle(String in, String out){


    file=null;


    filein=null;


    fileout=null;


        try{


	  file = new FileReader(in);


	  filein = new BufferedReader(file);


	  


  	  /*out file stuff. . .*/


	  fileout = new PrintWriter( new BufferedWriter(


                                         new FileWriter( out ) ) );	  


	}catch(Exception e){


	  System.out.println("error reading file. . .exiting. . .");


	  System.exit(1);


	}


  }


  


  public static void main(String [] args){


	





	if(args[0] == null){


	  System.out.println("usage: FileHandle in out");


	  System.exit(1);


	}


	FileHandle handy = new FileHandle(args[0],OUT);


        String input = " ";


	handy.println("This is a test of the emergency broadcast system");


	


	//while((input = handy.readln() ) != null){


	 // handy.println(input);


	//}





	handy.close();


  }








  /*encapsulates writing to file*/


public void println(String x){


 try{


    fileout.println(x);


  }catch(Exception e){


	System.out.println("error writing to file. . .exiting");


	System.exit(1);


  }


}





  /*encapsulates input*/  


  public String readln(){


	try{


	  return filein.readLine();


	}catch(Exception e){


	  return null; //kludge?


	}


  }








  public void close(){


	System.out.println("in fh.close");


     try{


	


	if(filein != null){


	  filein.close();


	  //filein = null;


	  


	}


	System.out.println("done with filein");


	if(fileout != null){


	System.out.println("fileout != null about to close");


	if(fileout.checkError()){


	  


	  System.out.println("error found somewhere in Printwriter stream");


	}


	  fileout.flush();


	  System.out.println("flushed. . .");


  	  fileout.close();


	System.out.println("done closing fileout");


	  //fileout = null;


	}


     }catch(Exception e){


	e.printStackTrace();


	System.out.println("error closing stream");


     }


	System.out.println("FileHandle.close. . .about to return");


	//System.gc();


  }





public boolean isOutFileNull(){


  	return (fileout == null);


}





public boolean isInFileNull(){


	return (filein == null);


}





  public String getText(){


    String retstr = "";


    String input = "";


    do{


	System.out.println("heloo jfkdsajfksdl;");


      input = readln();


	if(input != null){


	   System.out.println(input);


           retstr+= input + "\n";


        }	


    }while(input != null);





	System.out.println("FileHandle.getText. . about to return");


      return retstr;


  }


}
package psl.worklets;

import java.util.*;


public class WorkletIdEntry{
    String wid;
    String codebase;
    Integer index;
    Timer t;
    public WorkletIdEntry(String id, String cb,Integer i){
	wid = id;
	codebase = cb;
	index = i;
	t = new Timer();
	t.schedule(new TimerTask(){
		public void run(){
		    //remove this entry from hashtable
		   WVM_RMI_Transporter.wkltIds.remove(index);
		   this.cancel();
		   t.cancel();
		} 
	    },30000);
    }
}

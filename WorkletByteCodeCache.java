package psl.worklets;

import java.util.*;

// stores worklet specific class loaders along with 
// byte code for required classes

public class WorkletByteCodeCache{
    private static final Hashtable wklSpecificHash = new Hashtable();
    public WorkletByteCodeCache(){
    }
    
    public void put(String name, WorkletClassLoader wklLoader){
	if (!wklSpecificHash.containsKey(name)){
	    wklSpecificHash.put(name,wklLoader);
	}
	Enumeration e = wklSpecificHash.keys();
	while(e.hasMoreElements()){
	    String s = (String)e.nextElement();
	    // System.out.println("KEY: " + s);
	}
    }

    public WorkletClassLoader get(String name){
	return (WorkletClassLoader)wklSpecificHash.get(name);
    }

    public static boolean containsKey(String  name) {
	Enumeration e = wklSpecificHash.keys();
	while(e.hasMoreElements()){
	    String s = (String)e.nextElement();
	    // System.out.println("KEY: " + s);
	}
	return wklSpecificHash.containsKey(name);
    }

    public void putByteCode(String wkltName,String name, byte[] bc){
	if(wklSpecificHash.containsKey(wkltName)){
	    WorkletClassLoader _ldr = (WorkletClassLoader)wklSpecificHash.get(wkltName);
	    _ldr.putByteCode(name,bc);
	}
    }
    public boolean containsKey(String wkltName,String key){
	if(wklSpecificHash.containsKey(wkltName)){
	    WorkletClassLoader _ldr = (WorkletClassLoader)wklSpecificHash.get(wkltName);
	    return _ldr.containsKey(key);
	}
	return false;
    }
    public byte[] get(String wkltName,String name){
	if(wklSpecificHash.containsKey(wkltName)){
	    WorkletClassLoader _ldr = (WorkletClassLoader)wklSpecificHash.get(wkltName);
	    return _ldr.get(name);
	}
	return null;
    }


    
}

/*
    public void put(String wklName,String name,byte []code){
	if (wklSpecificHash.containsKey(name)) {
	    byteCodeEntry bce = (byteCodeEntry)wklSpecificHash.get(wklName);
	    bce.put(name, bytecode);
	} else {
	     byteCodeEntry bce = new byteCodeEntry(wklName);
	     bce.put(name,code);
	     wklSpecificHash.put(wklName,bce);
	}
    }
    
    public static byte[] get(String wklName,String name) {
	if (wklSpecificHash.containsKey(wklName)) {
	    byteCodeEntry bce = (byteCodeEntry)wklSpecificHash.get(wklName);
	    return ((byte[]) bce.get(name));
	}
	return null;
    }
    public static boolean containsKey(String wklName,String name) {
	if (wklSpecificHash.containsKey(name)) {
	    byteCodeEntry bce = (byteCodeEntry)wklSpecificHash.get(wklName);
	    return bce.containsKey(name);
	} else {
	    return false;
	}
    }

    public void removeWorklet(String wklName){
	wklSpecificHash.remove(wklName);
    }


    class byteCodeEntry{
	private String workletId;
	private WorkletClassLoader wklLoader;
	//	private final Hashtable byteCodeHash;// = new Hashtable();

	byteCodeEntry(String wklId){
	    workletId = new String(wklId);
	    byteCodeHash = new Hashtable();
	    wklLoader = ldr;
	}
	
	public void put(String name,byte []code){
	    if (! bytecodeHash.containsKey(name)) {
		bytecodeCache.put(name, bytecode);
	    } 
	}
	
	public  byte[] get(String name) {
	    return ((byte[]) bytecodeHash.get(name));
	}
	public boolean containsKey(String name) {
	    return (bytecodeHash.containsKey(name));
	}
    }
    
    
}*/

package psl.worklets;

import java.util.*;

/* Author: Dan Phung
 * Date: 11 April 2002
 * MultiMap.java: implementation of a map that
 * allows multiple entries for the same key by 
 * associating a vector to a key to contain the
 * multiple value entries.
 * 
 * <Methods>
 * - MultiMap(): constructor
 * - void put(Object key, Object value): add a key,value pair
 * - Object remove(Object key): removes entire key, returns vector of values
 * - Object remove(Object key, Object value): remove value from MultiMap, returns 
 *     value.  if value is last in key vector, key is removed as well. 
 * - boolean contains(Object key): true if key is present
 * - boolean contains(Object key, Object value): true if key,value is present
 * - Object get(Object key), returns object associated with the key without
 *     removing it from the MultiMap.
 * - Set keySet(): return the Set of keys.
 * - int size(): returns number of keys.
 * - int size(): returns number of values.
 * - void printAll(): prints the key,value vector
 * - void clear(): removes all key,vector(value) entries.
 * 
 */

public class MultiMap {
    Map _mmap = Collections.synchronizedMap(new TreeMap());
    
    // public MultiMap(): default ctor
    public MultiMap(){}
    

    // public void put(Object key, Object value)
    // : IF the  already key is already set, insert the
    // value into the vector.
    // ELSE the key isn't set yet, create a new
    // vector, insert the object into the vector, 
    // and then add the key/vector(value) to the map
    public void put(Object key, Object value){

	if (_mmap.containsKey(key)){
	    Vector v = (Vector)_mmap.get(key);
	    v.addElement(value);
	    
	} else {
	    Vector v = new Vector();
	    v.addElement(value);
	    _mmap.put(key, v);
	}
    }


    // public Object remove(Object key)
    // : removes the key (with all the values) from 
    // the map
    // : if key is not present, return Object is null
    public Object remove(Object key){

	if (_mmap.containsKey(key))
	    return _mmap.remove(key);

	return null;
    }

    
    // public Object remove(Object key, Object value)
    // : removes a value from the map, if the value is 
    // the last object in the vector, the key is also 
    // removed
    // : if the key/value is not present, return object 
    // is null
    public Object remove(Object key, Object value){
	Object o = null;
	
	if (_mmap.containsKey(key)){
	    Vector v = (Vector)_mmap.get(key);

	    int i = v.indexOf(value);
	    if (i > 0) {
		o = v.remove(i);
		
		if (v.size() == 1)
		    _mmap.remove(key);
	    }
	} 
	
	return o;
    }


    // public boolean contains(Object key)
    // : return true if key,value pair found
    public boolean contains(Object key){
	if (_mmap.containsKey(key))
	    return true;

	else 
	    return false;
    }


    // public boolean contains(Object key, Object value)
    // : return true if key,value pair found
    public boolean contains(Object key, Object value){
	if (_mmap.containsKey(key)){
	    Vector v = (Vector)_mmap.get(key);

	    if (v.indexOf(value) > 0)
		return true;
	}	    
	
	return false;
    }


    // public Object get(Object key)
    // : looks up key and returns vector of objects associated with key
    public Object get(Object key){
	if (_mmap.containsKey(key))
	    return _mmap.get(key);

	else 
	    return null;
    }


    // public Set keySet()
    // : return a Set of the keys in the MultiMap.
    public Set keySet(){
	return _mmap.keySet();
    }


    // public int size()
    // : return the number of map keys
    public int size(){
	return _mmap.size();
    }


    // public int sizeValues()
    // : return the number of map values
    public int sizeValues(){
	int valueSize = 0;

	Set s = _mmap.keySet();
	Iterator setItr = s.iterator();
	while (setItr.hasNext()) {
	    Vector v = (Vector)_mmap.get(setItr.next());
	    valueSize += v.size();
	} 
    
	return valueSize;
    }


    // public void printAll()
    // : prints the key,value vector
    // Note: this is a cleaner look at the values, as compared to using
    // entrySet() or values()
    public void printAll(){
	Set s = _mmap.keySet();
	Iterator setItr = s.iterator();

	if (s.size() > 0){
	    while (setItr.hasNext()) {
		Object key = setItr.next();
		WVM.out.print(" " + key + " <");
		Vector v = (Vector)_mmap.get(key);
		Iterator vecItr = v.iterator();

		while (vecItr.hasNext()) {		
		    Object value = vecItr.next();
		    WVM.out.print(value + ",");
		}
		WVM.out.println(">");
	    } 
	} 
    }


    // public void clear()
    // - removes all key,vector(value) entries.
    public void clear(){
	_mmap.clear();
    }
}


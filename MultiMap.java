/*
 * @(#)MultiMap.java
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2002: @author Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

package psl.worklets;

import java.util.*;

/**
 * Implementation of a map that allows multiple entries for the same
 * key by associating a vector to a key to contain the multiple value
 * entries
 */
public class MultiMap {

  /** The underlying container is a TreeMap */
  private Map _mmap = Collections.synchronizedMap(new TreeMap());

  /** Creates a MultiMap */
  public MultiMap(){}

  /**
   * Adds a key/value pair
   *
   * @param key: identifier for the value
   * @param value: object corresponding to the key
   */
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


  /**
   * Removes the key (with all the values) from the map
   *
   * @param key: identifier for the values to remove
   * @return values associated with the key or null
   */
  public Object remove(Object key){

    if (_mmap.containsKey(key))
      return _mmap.remove(key);

    return null;
  }


  /**
   * Removes a specific value from the map.  If the value is the last
   * object in the vector, the key is also removed
   *
   * @param key: key associated with the value
   * @param value: specific value to remove
   * @return object associated with the key/value pair or null
   */
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



  /**
   * Checks to see if this key is present
   *
   * @param key: identifier to check for
   * @return presence of the key
   */
  public boolean contains(Object key){
    if (_mmap.containsKey(key))
      return true;

    else
      return false;
  }

  /**
   * Checks to see if this key/value pair is present
   *
   * @param key: identifier to check for
   * @param value: value to check for
   * @return presence of the key/value pair
   */
  public boolean contains(Object key, Object value){
    if (_mmap.containsKey(key)){
      Vector v = (Vector)_mmap.get(key);

      if (v.indexOf(value) > 0)
	return true;
    }

    return false;
  }


  /**
   * Gets the values assoiated with the key
   *
   * @param key: identifier of object to retrieve
   * @return values associated with the key
   */
  public Object get(Object key){
    if (_mmap.containsKey(key))
      return _mmap.get(key);

    else
      return null;
  }

  /**
   * Get the available keys
   *
   * @return <code>Set</code> of available keys
   */
  public Set keySet(){
    return _mmap.keySet();
  }


  /**
   * Get the number of keys in the map
   *
   * @return number of keys in the map
   */
  public int size(){
    return _mmap.size();
  }


  /**
   * Get the number of values in the map
   *
   * @return number of values in the map
   */
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



  /** Prints the key,value vector */
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

  /** Removes all key,vector(value) entries */
  public void clear(){
    _mmap.clear();
  }
}

package psl.worklets;

/* CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

/* WorkletID.java: Unique identifier for Worklet or WorketJunction
 * author: Dan Phung dp2041@cs.columbia.edu, Gaurav S. Kc gksc@cs.columbia.edu
 * 
 * TODO
 * - add more information to classify the id.
 * 
 * 
 * NOTES: 
 * - right now we only have a String to act as the label for a Worklet/WorkletJunction
 * but in the future we can add other things like static ints to keep track of the number
 * of junctions, etc.
 * 
 * - _parent isn't being used right now.  It may not be pertinent and might be
 * removed in the future.
 * 
 * - currently only compares the string label
 * 
 */

import java.io.*;
import java.util.*;

public class WorkletID implements Serializable, Comparable{
    static long _count; // keep track of the WorkletID's made

    private String _label;
    private long _myCount;
    private Object _parent ; // could be worklet or workletJunction

    public WorkletID(String label){
	_label = label;
	_myCount = _count++;  // get the current count then increment the counter.
    }

    public void init(Object parent){
	_parent = parent;
    }

    // currently only returns the label, but when other members are added
    // to the ID, those members should also be added to this return string.
    public String toString(){
	// return _label + _myCount; // this can be used to automatically track id's.
	return _label;
    }

    public String label(){
	return _label;
    }

    // returns my count number
    public long count(){
	return _myCount;
    }

    public int compareTo(Object right_val){
	String rv = ((WorkletID)right_val).label();
	return (_label.toLowerCase()).compareTo(rv.toLowerCase());
    }

}

/*
 * @(#)WorkletID.java
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

import java.io.*;
import java.util.*;



/*
 * TODO
 * - add more information to classify the id.
 *
 * NOTES:
 * - right now we only have a String to act as the label for a Worklet/WorkletJunction
 * but in the future we can add other things like static ints to keep track of the number
 * of junctions, etc.
 * - _parent isn't being used right now.  It may not be pertinent and might be
 * removed in the future.
 * - currently only compares the string label
 */
/** Unique identifier for {@link Worklet} or {@link WorkletJunction} */
public class WorkletID implements Serializable, Comparable{
  /** identifier for the WorkletID */
  private String _label;
  /** owner of the WorkletID, or object that the WorketID refers to */
  private Object _parent ;

  /**
   * Creates a WorkletID with the specified label
   *
   * @param label: the identifier to be used
   */
  public WorkletID(String label){
    _label = label;
  }

  /**
   * Initializes the WorkletID by giving it a reference to its parent.
   *
   * @param parent: owner of the WorkletID, or object that the
   * WorketID refers to
   */
  public void init(Object parent){
    _parent = parent;
  }

  /** @return WorkletID label */
  public String toString(){
    return _label;
  }

  /** @return WorkletID label */
  public String label(){
    return _label;
  }

  /**
   * compares two WorkletID's by their labels
   *
   * @param right_val: the WorkletID to compare to
   * @return -1 if the object is less than, 0 if equal to, or 1 if
   * greater than.
   */
  public int compareTo(Object right_val){
    String rv = ((WorkletID)right_val).label();
    return (_label.toLowerCase()).compareTo(rv.toLowerCase());
  }

}

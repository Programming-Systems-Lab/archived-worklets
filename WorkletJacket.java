package psl.worklets;

/**
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * 
 * Copyright (c) 2001: @author Gaurav S. Kc
 * 
*/

abstract class WorkletJacket {

  public static final int PRIORITY_LOW    = 0;
  public static final int PRIORITY_MEDIUM = 1;
  public static final int PRIORITY_HIGH   = 2;
  public static final int PRIORITY_ULTRA  = 3;

  public static final int STATE_READY      = 0;
  public static final int STATE_WAITING    = 1;
  public static final int STATE_RUNNING    = 2;
  public static final int STATE_TERMINATED = 3;

  private transient WVM _wvm;
  private WorkletJunction _wj;
  private WKL_XMLDesc _desc;
  private String _name;

  private int _priority;
  private int _iterations;
  private boolean _dropOff;

  WorkletJacket() {
    init();
  }

  protected void init() {
    _wvm = null;
    _wj = null;
    _desc = null;
    _name = null;

    _priority = PRIORITY_MEDIUM;
    _iterations = 1;
  }

}

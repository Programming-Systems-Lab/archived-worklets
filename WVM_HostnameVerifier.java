/*
 * @(#)WVM_HostnameVerifier.java
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

import javax.net.ssl.*;

/**
 * Allows different hosts to load classes.  This class is dynamically
 * instantiated when the sending {@link WVM} hostname is not
 * recognized from the CA certificates or keysfile and the receiving
 * {@link WVM} needs to verify the sending WVM.
 */
class WVM_HostnameVerifier implements HostnameVerifier{

  /**
   * Verifies whether the given hostname should be trusted.  This
   * class uses the names listed in the system properties WVM_HOSTS_ALLOW and
   * WVM_HOSTS_DENY to verify the hosts.  If no hosts are specified in either
   * system property then the default is to return false. (more secure)
   *
   * @param hostname: hostname to check
   * @param session: current SSL connection
   * @return true if the hostname can be trusted
   */
  public boolean verify(String hostname, SSLSession session) {
    boolean allowed = false; // lean towards disallowing by default = more security

    String allow = System.getProperty("WVM_HOSTS_ALLOW");
    String deny = System.getProperty("WVM_HOSTS_DENY");

    if (allow != null && allow.matches(".*" + hostname + ".*"))
      allowed = true;
    if (deny != null && deny.matches(".*" + hostname + ".*"))
      allowed = false;

    return allowed;
  }
}

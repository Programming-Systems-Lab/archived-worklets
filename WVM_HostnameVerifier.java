package psl.worklets;

/**
 * to allow different hosts to load classes.
 *
 */

import javax.net.ssl.*;

class WVM_HostnameVerifier implements HostnameVerifier{

  public boolean verify(String hostname, SSLSession session) {
    boolean allowed = true; // lean towards allowing more than disallowing = less security

    String allow = System.getProperty("WVM_HOSTS_ALLOW");
    String deny = System.getProperty("WVM_HOSTS_DENY");

    if (allow != null && allow.matches(".*" + hostname + ".*"))
      allowed = true;
    if (deny != null && deny.matches(".*" + hostname + ".*"))
      allowed = false;

    return allowed;
  }
}


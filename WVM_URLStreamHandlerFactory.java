package psl.worklets;

/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * 
 * Copyright (c) 2001: @author Gaurav S. Kc
 * 
*/

import java.io.*;
import java.net.*;
import java.util.*;

public class WVM_URLStreamHandlerFactory implements URLStreamHandlerFactory {
  public URLStreamHandler createURLStreamHandler(String protocol) {
    WVM.out.println(" + + + WVM_URLStreamHandlerFactory.createURLStreamHandler(" + protocol + ");");
    if (protocol.equalsIgnoreCase("http")) {
      return (new WVM_HttpHandler());
    } else if (protocol.equalsIgnoreCase("file")) {
      return (new sun.net.www.protocol.file.Handler());
    } else if (protocol.equalsIgnoreCase("jar")) {
      return (new sun.net.www.protocol.jar.Handler());
    }

    WVM.out.println("Unsupported protocol: " + protocol);
    return null;
  }
}

class WVM_HttpHandler extends URLStreamHandler {
  protected URLConnection openConnection(URL url) throws IOException {
    return (new WVM_URLConnection(url));
  }
}

// class WVM_URLConnection extends URLConnection {
class WVM_URLConnection extends HttpURLConnection {
  WVM_URLConnection(URL url) throws IOException {
    super(url);
  }

  Socket s;
  synchronized public void connect() throws IOException {
    int port;
    if ((port = url.getPort()) == -1) {
      port = 80;
    }

    s = new Socket(url.getHost(), port);
    OutputStream server = s.getOutputStream();
    PrintStream ps = new PrintStream(server);
    ps.print("GET " + url.getFile() + " HTTP/1.1\r\n");
    // ps.println("Remote: " + WVM.transporter._name + "\r\n");
    // ps.print("User-Agent: WVM_ClassLoader - Java\r\n");
    ps.print("User-Agent: WKLJava1.3.0\r\n");
    ps.print("Host: " + url.getHost() + ":" + port + "\r\n");
    ps.print("Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\r\n");
    ps.print("Connection: keep-alive\r\n");
    // ps.print("WVM URL Connection\r\n");
    ps.print("\r\n");
    ps.flush();
    // is = new BufferedInputStream(s.getInputStream());
    is = s.getInputStream();
    connected = true;
    // try { Thread.currentThread().sleep(5000); } catch (InterruptedException ie) { }
    WVM.out.println(" ~ ~ ~ ~ ~ in connect: " + url + ", is: " + is.available());
  }

  InputStream is;
  synchronized public InputStream getInputStream() throws IOException {
    if (!connected) {
      connect();
    }
    // WVM.out.println(" ~ ~ ~ ~ ~ in getInputStream: " + url);
    return (is);
  }
  public int getResponseCode() throws IOException {
    int superResponse = super.getResponseCode();
    // WVM.out.println(" ~ ~ ~ ~ ~ Response code: " + superResponse);
    // Thread.currentThread().dumpStack();
    return superResponse;
  }

  public String getContentType() {
    return (guessContentTypeFromName(url.getFile()));
  }

  public void disconnect() {
    try {
    if (s != null) s.close();
    } catch (IOException e) { } 
    connected = false;
  }
  public boolean usingProxy() {
    return false;
  }

  /*
  private void writeRequests() throws IOException {
    /* print all message headers in the MessageHeader 
     * onto the wire - all the ones we've set and any
     * others that have been set
     * /
    if (!setRequests) {
      /* We're very particular about the order in which we
       * set the request headers here.  The order should not
       * matter, but some careless CGI programs have been
       * written to expect a very particular order of the
       * standard headers.  To name names, the order in which
       * Navigator3.0 sends them.  In particular, we make *sure*
       * to send Content-type: <> and Content-length:<> second
       * to last and last, respectively, in the case of a POST
       * request.
       * /
      requests.prepend(method + " " + http.getURLFile()+" "  + 
               httpVersion, null);
      requests.setIfNotSet("User-Agent", userAgent);
      int port = url.getPort();
      String host = url.getHost();
      if (port != -1 && port != 80) {
        host += ":" + String.valueOf(port);
      }
      requests.setIfNotSet("Host", host);
      requests.setIfNotSet("Accept", acceptString);
      /* Here, we have to work around an egregious bug in the Netscape
       * proxy (at least in version 2.0p1 and 2.0p1+ by my reckoning):
       * 1) If we're doing a POST
       * 2) And we send "Proxy-Connection: keep-alive"
       * 3) And the server the proxy connects to DOES NOT reply w/ 
       *    "Connection: keep-alive"
       * The proxy will *still* send back with the responses the headers
       * "Connection: keep-alive" and "Proxy-connection: keep-alive",
       * no if's, and's, or but's.
       * Which deceives us, the client, into thinking we have a keep-alive
       * connection with the ultimate server when in fact we do not.
       * The solution, unfortunately, is to never attempt keep-alive
       * when talking to a proxy && (POST || PUT).  I don't want to get 
       * into the game of deciphering which proxy we're talking to. -brown
       * /
      // Try keep-alive only on first attempt
      if (!failedOnce && http.getHttpKeepAliveSet()) {
        if (http.usingProxy && !(method.equals("POST") 
                     || method.equals("PUT"))) {
          requests.setIfNotSet("Proxy-Connection", "keep-alive");
        } else if (!(http.usingProxy)) {
          requests.setIfNotSet("Connection", "keep-alive");
        }
      } else {
        requests.set("Connection", "close");
      }
      // send any pre-emptive authentication
      if (http.usingProxy) {
        AuthenticationInfo pauth
          = AuthenticationInfo.getProxyAuth(http.getProxyHostUsed(),
                           http.getProxyPortUsed());
        if (pauth != null && pauth.supportsPreemptiveAuthorization()) {
          // Sets "Proxy-authorization"
          requests.setIfNotSet(pauth.getHeaderName(), 
                     pauth.getHeaderValue());
        }
      }
      // Set modified since if necessary
      long modTime = getIfModifiedSince();
      if (modTime != 0 ) {
        Date date = new Date(modTime);
        //use the preferred date format according to RFC 2068(HTTP1.1),
        // RFC 822 and RFC 1123
        SimpleDateFormat fo =
          new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
        fo.setTimeZone(TimeZone.getTimeZone("GMT"));
        requests.setIfNotSet("If-Modified-Since", fo.format(date));
      }
      AuthenticationInfo sauth = AuthenticationInfo.getServerAuth(url);
      if (sauth != null && sauth.supportsPreemptiveAuthorization()) {
        // Sets "Authorization"
        requests.setIfNotSet(sauth.getHeaderName(), 
                   sauth.getHeaderValue());
      }
      
      if (poster != null) {
        /* add Content-length & POST/PUT data * /
        synchronized (poster) {
          if (!method.equals("PUT")) {
            String type = "application/x-www-form-urlencoded";
            requests.setIfNotSet("Content-type", type);
          }
          requests.set("Content-length", 
                 String.valueOf(poster.size()));
        }
      }
      setRequests=true;
    }
    http.writeRequests(requests);
    if (poster != null) {
      poster.writeTo(ps);
      ps.flush();
    }
    
    if (ps.checkError()) {
      disconnect();
      if (failedOnce) {
        throw new IOException("Error writing to server");
      } else { // try once more
        failedOnce=true;
        http = getNewClient (url);
        ps = (PrintStream) http.getOutputStream();
        connected=true;
        responses = new MessageHeader();
        requests = new MessageHeader();
        setRequests=false;
        writeRequests();
      }
    }
  }

  public void _connect() throws IOException {
    if (connected) {
      return;
    }
    try {
      if ("http".equals(url.getProtocol()) && !failedOnce) {
        http = HttpClient.New(url);
      } else {
        // make sure to construct new connection if first
        // attempt failed
        http = new HttpClient(url, handler.proxy, handler.proxyPort);
      }
      ps = (PrintStream)http.getOutputStream();
    } catch (IOException e) {
      throw e;
    }
    // constructor to HTTP client calls openserver
    connected = true;
  }
  */

}

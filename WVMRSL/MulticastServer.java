package psl.worklets.WVMRSL;

import java.util.Date;
import java.util.Vector;
import java.util.StringTokenizer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.net.*;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.FactoryConfigurationError;  
import javax.xml.parsers.ParserConfigurationException;
 
import org.xml.sax.SAXException;  
import org.xml.sax.SAXParseException;  

import org.w3c.dom.*;

// For write operation
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;  
import javax.xml.transform.stream.StreamResult; 

import javax.swing.Timer;
import java.io.*;


public class MulticastServer {
    
    
    private static String addr;
    private static InetAddress mcastAddr;
    private static int port;
    private static long born_on;
    private static boolean central_server;
    private static java.util.Vector serverVector; //store servers' info
    private static Vector XMLDocuments; //store WVM descriptions
    final MulticastSocket mcs;
    private static ServerEntry self;
    static DocumentBuilderFactory factory; 
    static DocumentBuilder builder;
    private static Vector requests; //queue for incoming requests
    private static FileOutputStream log_file;
    private static boolean debug;
    
    public static void main(String args[]) throws IOException {
	System.out.println("usage: java MulticastServer -m <multicast_addr> -p <port> -debug");
	
	addr = "228.5.6.7";
	port = 1234;
	born_on = new Date().getTime();
	central_server = false;
	debug = false;
	try{
	    log_file = new FileOutputStream(new String(String.valueOf(born_on)+".log"),true);
	}catch(Exception e){
	    System.err.println("Unable to open log file:");
	    e.printStackTrace();
	}

	    int i = -1;
	while(++i < args.length) {
	    if (args[i].equals("-m")) addr = args[++i];
	    if (args[i].equals("-p")) port = Integer.parseInt(args[++i]);
	    if (args[i].equals("-debug")) debug = true;	
	}
	System.out.println("Birthdate: " + born_on);
	factory = DocumentBuilderFactory.newInstance();
	
        try {
	    builder = factory.newDocumentBuilder();                   
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();
        } 
	
	mcastAddr = InetAddress.getByName(addr);
	new MulticastServer();
    }
    

    //constructor
    private MulticastServer() throws IOException {
	mcs = new MulticastSocket(port);
	mcs.joinGroup(mcastAddr);
	serverVector = new Vector(10);
	requests = new Vector();
	XMLDocuments = new Vector();
	self = new ServerEntry(this,born_on,false,true); // server entry for this server
	serverVector.add(0,self);
	central_server = false;
	SendJoinUpdate();
	
	//on shutdown notify other servers
	Runtime.getRuntime().addShutdownHook(new Thread(){
	    public void run(){
		SendExitUpdate();
	    }
	});
	
	while (true) {
	    byte buf[] = new byte[10000];
	    DatagramPacket recv = new DatagramPacket(buf, buf.length);
	    mcs.receive(recv);
	    ProcessIncoming(new String(buf,0,recv.getLength()));
	}
	
	
    }
    
    public void WriteToLog(String msg){
	if(debug)
		System.out.println(msg);
	msg  = msg + "\n";
	try{
	    log_file.write(msg.getBytes());
	}catch(Exception e){
	    System.err.println("Unable to write to log file");
	}
    }
    
    //takes xml document as a string
    //and builds DOM document from it
    //used to process registration and search requests
    public Document BuildDocumentFromString(String s){
	Document document;
	
	byte [] s_bytes = s.getBytes();
        try{
            ByteArrayInputStream bais = new ByteArrayInputStream(s_bytes);
            document = builder.parse(bais);
            return document;
        } catch (Exception e){
	    e.printStackTrace();
        }
        return null;
    }
    
    //remove WVM description based on supplied key
    public void removeDocument(String key){
	WriteToLog("Removing document with key = " + key);
	for(int i=0;i<XMLDocuments.size();i++){
	    Entry e = (Entry)XMLDocuments.elementAt(i);
	    if(key.equals(e.key))
		XMLDocuments.removeElementAt(i);
	}
    }
    
    //check if registration for given key exists
    public boolean Registered(String key){
	for(int i=0;i<XMLDocuments.size();i++){
	    Entry e = (Entry)XMLDocuments.elementAt(i);
	    if(key.equals(e.key))
		return true;
	}
	return false;
    }
    
    //check if given document exists in the registration vector
    public boolean Registered(Document doc){
	NodeList elist = doc.getElementsByTagName("ID");
	
	Element id_elem = (Element)elist.item(0);//("ID");
	String id;
	if(id_elem != null){
	    id = id_elem.getAttribute("VALUE");
	    //System.out.println("ID: " + id);
	    if(id.equals("")){
		WriteToLog("ID is not present ");
		return true;//so that we don't add this doc to the list
	    }
	}
	else {
	    WriteToLog("ID is not found in supplied document");
	    return true;//so that we don't add this doc to the list
	}
	
	for(int i = 0;i<XMLDocuments.size();i++){
	    //assuming that all documents registered have id
	    Entry entry = (Entry)XMLDocuments.elementAt(i);
	    Document d = entry.doc;
	    NodeList list = d.getElementsByTagName("ID");
	
	Element e = (Element)list.item(0);
	    String   s = e.getAttribute("VALUE");
	    if(s.equals(id)){
		//System.out.println("equals " + s);
		return true;
	    }
	}
	return false;
    }
    

    //takes query document, and compares it to all the
    //documents registered before
    //DOM tree allows effective traversing
    //uses recursion
    //return pair with number of elements used for comparison
    //and number matched
    public String SearchAndBuildResponse(Document doc){
	Vector response = new Vector();	
	String result;
	
	for(int i = 0; i < XMLDocuments.size(); i++){
	    System.out.println("Searching document at " + i);
	    Entry ent = (Entry)XMLDocuments.elementAt(i);
	    Document entry = ent.doc;
	    pair p = CompareDocs(doc,entry);
	    double j = 100*(double)p.matched/(double)p.searched;
	    System.out.println("% = " + j);
	    if(j > 50)
		response.add(entry);
	}
	result = new String("<?xml version='1.0' encoding='utf-8'?>");
	result = result + ("\n<response list>\n<number_of_entries>"+response.size()+"</number_of_entries>\n");
	for(int k=0;k<response.size();k++){
	    Document item = (Document)response.elementAt(k);
	    result = result + ("<result_entry>\n") + DOMToString(item) + ("\n</result_entry>\n");
	}
	result = result + "</response_list>";
	return result;
    }
    

    //send message to multicast address
    public void SendResponse(String s){
	try {
            mcs.send(new DatagramPacket(s.getBytes(), s.length(),
					mcastAddr, port));
        } catch (IOException ioe) { 
	      WriteToLog("Unable to send response");
	}
    }
    
    //send message to multicast address
    //send message to client via direct socket communication
    //using supplied ip and port number of the client
    public void SendResponse(String ip,int port_num,String msg_id,String result){
	try {
            mcs.send(new DatagramPacket(result.getBytes(), result.length(),
					mcastAddr, port));
        } catch (IOException ioe) { 
	 	WriteToLog("Unable to send response");
	}
	
	WriteToLog("Sending response to "+ip+":" + port);
	Socket ResponseSocket = null;
	PrintWriter out = null;
	try {
	    ResponseSocket = new Socket(InetAddress.getByName(ip), port_num);
            out = new PrintWriter(ResponseSocket.getOutputStream(), true);
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + ip);
            return;
        } catch (IOException e) {
            WriteToLog("Couldn't get I/O for the connection to: " + ip);
	    e.printStackTrace();
	    WriteToLog(e.getMessage());
            return;
        }              
	out.println(result);
	out.close();
	try{
	    ResponseSocket.close();
	} catch(Exception e){}

	 return;
    }

    //comaper two documents
    //first one is query, second one is from
    //registration vector
    //just skips root node and calls actual function
    public pair CompareDocs(Node search,Document doc){
	pair p = new pair();
	search = search.getFirstChild();
	while(search != null){
	    p.add(checkNode(search,doc));
	    search = search.getNextSibling();
	}
	return p;
    }


    //actual comparison implementation
    //first checks attributes for a given node,
    //then recursively processes children
    public pair checkNode(Node n,Document doc){
	pair p = new pair();
	if(n.hasAttributes()){
	    String name = n.getNodeName();
	    NodeList elist = doc.getElementsByTagName(name);
	    if(elist == null){
		p.searched++;
		return p;
	    }
	    else{
		for(int r=0;r<elist.getLength();r++){
		    Element e = (Element)elist.item(r);
		    NamedNodeMap atrbs = n.getAttributes();
		    p.searched += atrbs.getLength();
		    for(int i=0;i<atrbs.getLength();i++){
			Node m = atrbs.item(i);
			String a;
			if(m.getNodeType() == Node.ATTRIBUTE_NODE){
			    a = e.getAttribute(((Attr)m).getName());
			    if(a.equals(""))
				;
			    else if( a.matches(   ((Attr)m).getValue())){
				p.matched++;
			    }			
			}
		    }
		}		    
	    }
	}
	
	
	//process Children	
	if(n.hasChildNodes()){
	    NodeList nl = n.getChildNodes();
	    
	    if(doc == null){
		WriteToLog("doc is null");
		return p;
	    }
	    
	    NodeList elist_a = doc.getElementsByTagName((n.getNodeName()));
	    if(elist_a.getLength()==0)
		p.searched++;
	    for(int j=0;j<nl.getLength();j++) {
		boolean found = false;
		Node a = nl.item(j);
		//System.out.println(a.getNodeName());
		if(a.getParentNode() == n) {
		    for (int h=0;h<elist_a.getLength();h++) {
			Element el = (Element)elist_a.item(h);
			if(el == null){
			    p.searched++;
			    return p;
			}
			if(!el.hasChildNodes()){
			    p.searched++;
			    return p;
			}
			NodeList elnl = el.getChildNodes();
			for (int k=0;k<elnl.getLength();k++) { 
			    Node b = elnl.item(k);
			    if(b==null)
				continue;
			    if(b.getParentNode() != el)
				continue;
			    //child contains text, let's compare it here
			    if((a.getNodeType() == Node.TEXT_NODE || a.getNodeType() == Node.CDATA_SECTION_NODE)){
				if((b.getNodeType() == Node.TEXT_NODE || b.getNodeType() == Node.CDATA_SECTION_NODE)){
				    found = true;
				    String text = a.getNodeValue();
				    text.trim();
				    if(text.matches("\\s*") || text.equals(""))
					continue;
				    String pattern = b.getNodeValue();
				    p.searched++;
				    if(pattern.matches(text)){
					p.matched++;
					break;				
				    }
				}
			    }
			    else if (a.getNodeName().equals(b.getNodeName())){
				found = true;
				p.add(checkNode(a,doc));
			    }			    
			}			
		    } 		    
		}
		else {
		    found = true;
		    p.add(checkNode(a,doc));
		}
		if(!found)//did not find a child in the target document
		    p.searched++;
	    }	    
	}
	return p;
    }
    
    //takes DOM document and converts it to string 
    //used to build response for search queries
    public String DOMToString(Document document){
	String s;
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	try {            
            // Use a Transformer for output
            TransformerFactory tFactory =
                TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();	    
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(baos);
            transformer.transform(source, result);
	    
        } catch (TransformerConfigurationException tce) {
	    // Error generated by the parser
	    //System.out.println ("\n** Transformer Factory error");
	    //System.out.println("   " + tce.getMessage() );	    
	    // Use the contained exception, if any
	    Throwable x = tce;
	    if (tce.getException() != null)
		x = tce.getException();
	    x.printStackTrace();
	    
        } catch (TransformerException te) {
	    // Error generated by the parser
	    //System.out.println ("\n** Transformation error");
	    //System.out.println("   " + te.getMessage() );
	    
	    // Use the contained exception, if any
	    Throwable x = te;
	    if (te.getException() != null)
		x = te.getException();
	    x.printStackTrace();
	    
	}
	s = baos.toString();
	return s;	
    }
    
    //process search request here
    public void ProcessSearch(String s){
	String msg_id;
	String ip;
	String port_num;
	String xml_query;
	
	StringTokenizer st = new StringTokenizer(s,":");
	if(st.hasMoreTokens())
	    msg_id = st.nextToken();
	else {
	    SendInvalid();
	    return;
	}
	if(st.hasMoreTokens())
	  ip = st.nextToken();
	else{
	    SendInvalid();
	    return;
	}
	if(st.hasMoreTokens())
	  port_num = st.nextToken();
	else{
	    SendInvalid();
	    return;
	}
	if(st.hasMoreTokens())
	    xml_query = st.nextToken();
	else{
	    SendInvalid();
	    return;
	}
	int r_port = Integer.parseInt(port_num);
	if(central_server){
	    //convert query xml to DOM
	    Document doc = BuildDocumentFromString(xml_query);
	    if(doc == null){
		WriteToLog("Failed to build the DOM document from query");
	    }
	    String result = SearchAndBuildResponse(doc);
	    result = new String("RESPONSE:"+msg_id+":PROCESSED:"+result);
	    WriteToLog("Sending response for search request");
	    SendResponse(ip,r_port,msg_id,result);
	} 
	Request r = new Request(xml_query,msg_id,1,ip,r_port,this);
	requests.add(r);
	   
    }
    
    //timed out search request processing
    public void ProcessSearch(String query,String msg_id,String ip,int r_port){
	WriteToLog("Processing Search");
	Document doc  = BuildDocumentFromString(query);
	if(doc == null){
		WriteToLog("Failed to build the DOM document from query");
	}
	String result = SearchAndBuildResponse(doc);
	result = new String("RESPONSE:"+msg_id+":PROCESSED:"+result);
	WriteToLog("Sending response for search request");
	SendResponse(ip,r_port,msg_id,result);
    }
	
    //process registration request
    public void ProcessRegistration(String s){
	String msg_id;
	String ip;
	String port_num;
	String xml_query;

	WriteToLog("Processing Registration");
	String result;
	String key;
	StringTokenizer st = new StringTokenizer(s,":");
	if(st.hasMoreTokens())
	 msg_id = st.nextToken();
	else{
	    SendInvalid();
	    return;
	}
	if(st.hasMoreTokens())
	   ip = st.nextToken();
	else{
	    SendInvalid();
	    return;
	}
	if(st.hasMoreTokens())
	     port_num = st.nextToken();
	else{
	    SendInvalid();
	    return;
	}
	if(st.hasMoreTokens())
	  xml_query = st.nextToken();
	else{
	    SendInvalid();
	    return;
	}
	System.out.println("Get here");
	int r_port = Integer.parseInt(port_num);
	if(central_server){
		System.out.println("we are central server");
	    //build DOm from supplied XML string
	    Document doc = BuildDocumentFromString(xml_query);
	    if(doc == null){
		WriteToLog("Failed to build the DOM document from query");
	    }
	    key = String.valueOf(new Date().getTime());	    
	    if(!Registered(doc)){
		Entry e = new Entry(doc,key);
		XMLDocuments.add(e);
		result = new String("RESPONSE:"+msg_id+":REGISTERED:"+key);
	    }
	    else {
		result = new String("RESPONSE:"+msg_id+":ALREADY_REGISTERED");
	    }	
	    WriteToLog("SENDING RESPONSE for REGISTRATION: " + result);
	    SendResponse(ip,r_port,msg_id,result);
	} 
	System.out.println("We are not central server");
	//do not register until response from central server is received
	Request r = new Request(xml_query,msg_id,2,ip,r_port,this);
	requests.add(r);
	   
    }
    
    //process timed out registration request
    public void ProcessRegistration(String query,String msg_id,String ip,int r_port){
	//if we get here,we know we are a central server
	WriteToLog("Processing Registration");
	Document doc = BuildDocumentFromString(query);
	if(doc == null){
	    WriteToLog("Failed to build the DOM document from query");
	}
	String key;
	String result;
	key = String.valueOf(new Date().getTime());	    
	if(!Registered(doc)){
	    Entry e = new Entry(doc,key);
	    XMLDocuments.add(e);
	    result = new String("RESPONSE:"+msg_id+":REGISTERED:"+key);
	}
	else {
	    result = new String("RESPONSE:"+msg_id+":ALREADY_REGISTERED");
	}	
	SendResponse(ip,r_port,msg_id,result);
    }
    
    //process registration response
    //if document already registered before
    //do nothing, otherwise proceed with registration
    public void ProcessRegistrationResponse(String s){
	String t;
	String msg_id;
	String status;

	StringTokenizer st = new StringTokenizer(s,":");
	t =st.nextToken();
	msg_id = st.nextToken();
	status = st.nextToken();
	if(status.equals("ALREADY_REGISTERED")){
	    removeRequest(msg_id);
	    return;
	} else if(status.equals("REGISTERED")){
	    String key = st.nextToken();
	    if(!Registered(key)){
		Request r = findRequest(msg_id);
		Document doc = BuildDocumentFromString(r.request);
		if(doc == null){
		    WriteToLog("Failed to build the DOM document from query");
		}
		Entry e = new Entry(doc,key);
		XMLDocuments.add(e);
	    }
	    removeRequest(msg_id);
	}
    }


    //process response for client's request
    //eother registration, unregistration or search
    public void ProcessResponse(String s){
	WriteToLog("Processing Response");
	StringTokenizer st = new StringTokenizer(s,":");
	String r = st.nextToken();
	String msg_id = st.nextToken();
	Request req = findRequest(msg_id);
	if(req == null){//probably received before server started
	    WriteToLog("Response does not have a corresponding request in my que");
	    return;
	}
	if(req.action == 1)
	    removeRequest(msg_id);
	else if(req.action == 2)
	    ProcessRegistrationResponse(s);
	else if(req.action == 3)
	    removeRequest(msg_id);
	else //should not get here
	    removeRequest(msg_id);
    }

    //remove request from queue
    public void removeRequest(String msg_id){
	WriteToLog("Removing request: " + msg_id);
	for(int i=0;i<requests.size();i++){
	    Request r = (Request)requests.elementAt(i);
	    if(msg_id.equals(r.id)){
		r.t.stop();
		requests.removeElementAt(i);
		break;
	    }
	}
    }

    //find request in the queue given msg id of the request
    public Request findRequest(String msg_id){
	for(int i=0;i<requests.size();i++){
	    Request r = (Request)requests.elementAt(i);
	    if(msg_id.equals(r.id)){
		return r;
	    }
	}
	return null;
    }

    //process unregistration request 
    public void ProcessUnRegistration(String s){
	String msg_id;
	String ip;
	String key;
	String port_num;

	String result;
	StringTokenizer st = new StringTokenizer(s,":");
	if(st.hasMoreTokens())
	     msg_id = st.nextToken();
	else {
	    SendInvalid();
	    return;
	}
	WriteToLog("Processing Unregistration: " + msg_id);
	if(st.hasMoreTokens())
	     ip = st.nextToken();
	else {
	    SendInvalid();
	    return;
	}
	if(st.hasMoreTokens())
	     port_num = st.nextToken();
	else{
	    SendInvalid();
	    return;
	}
	if(st.hasMoreTokens())
	     key = st.nextToken();
	else{
	    SendInvalid();
	    return;
	}
	int r_port = Integer.parseInt(port_num);
	if(!Registered(key)){
	    result = new String("RESPONSE:"+msg_id+":NOT_REGISTERED");
	}else {
	    removeDocument(key);
	    result = new String("RESPONSE:"+msg_id+":UNREGISTERED:"+key);
	}
	if(central_server){
	    WriteToLog("Sending response: " + result);
	    SendResponse(ip,r_port,msg_id,result);
	} 
	//store result , so if request times out, just send it to the client
	Request r = new Request(result,msg_id,3,ip,r_port,this);
	requests.add(r);
	  
    }
    
    //process timed out unregistration request
    public void ProcessUnRegistration(String result,String msg_id,String ip,int r_port){
	//if we get here, we know we are a central server
	WriteToLog("Processing Unregistration: " + msg_id);
	SendResponse(ip,r_port,msg_id,result);
    }

    //process timed out request
    //this is called if this server is central
    public void processUnservicedRequest(Request r){
	if(r.action == 1)
	    ProcessSearch(r.request,r.id,r.ip,r.port);
	else if(r.action == 2)
	    ProcessRegistration(r.request,r.id,r.ip,r.port);
	else if(r.action == 3)
	    ProcessUnRegistration(r.request,r.id,r.ip,r.port);
    }

    //timed out request processing
    //remove current central server
    //if central server, process request
    public void unservicedRequest(Request r){
	WriteToLog("Processing unserviced request: " + r.id);
	//assume central server is dead
	ServerEntry centr = (ServerEntry)serverVector.elementAt(0);
	if(centr.birth_date == born_on)
	    ;//do nothing, we just started up
	else //some other server not responding
	    serverVector.removeElementAt(0);
	ServerEntry se = (ServerEntry)serverVector.elementAt(0);
	if (se.birth_date == born_on){ // this is central server now
	    central_server = true;
	    SendUpdate();
	    //since we are central server now
	    //let's process all of them
	    for(int i=0;i<requests.size();i++){
		Request rr = (Request)requests.elementAt(i);
		rr.t.restart();
		processUnservicedRequest(rr);
	    }		
	}
	else {
	    //new central server is designated
	    //restart timers and wait for new server to process them
	    //which should be pretty soon
	    for(int j=0;j<requests.size();j++){
		Request rrr = (Request)requests.elementAt(j);
		rrr.t.restart();
	    }
	}
    }

    //log error message
    public void UnsupportedRequest(String s){
	WriteToLog("Received Unsupported request: " + s.substring(0,20));
    }
    
    //process incoming message
    public void ProcessIncoming(String s){
	WriteToLog("RECEIVED: " + s );
	if(s.startsWith("UPDATE:"))
	    ProcessServerUpdate(s);
        else if(s.startsWith("JOINING:"))
            ProcessServerJoining(s);
	else if(s.startsWith("REGISTER:"))
	    ProcessRegistration(s.substring(9));
	else if(s.startsWith("UNREGISTER:"))
	    ProcessUnRegistration(s.substring(11));
	else if(s.startsWith("SEARCH:"))
	    ProcessSearch(s.substring(7));
	else if(s.startsWith("RESPONSE:"))
	    ProcessResponse(s);
	else if(s.startsWith("INAVLID"))
	    ;//do nothing
	else 
	    UnsupportedRequest(s);
    }
    
    //server status update
    //if live, add server if not added (which it should be)
    //if exiting, remove it
    //if message says that it's central server, 
    //make sure that it is
    public void ProcessServerUpdate(String s){
	WriteToLog("Received update: " + s);
        StringTokenizer st = new StringTokenizer(s,":");
        String token = st.nextToken();
        String bd = st.nextToken();
        String status = st.nextToken();        
        long b_d = Long.parseLong(bd,10);
        if(b_d == born_on) return;//own message
        if(st.hasMoreTokens()){
            String a = st.nextToken();
            if(a.equals("CENTRAL"))
                CheckCentralServer(b_d);
        }
        if(status.equals("LIVE"))
            addServer(b_d);
        else
            removeServer(b_d);
    }
    
    //checks that the server that says it's central,
    //really is central
    //if not , makes it central
    public void CheckCentralServer(long birth_date){
	boolean found = false;
        ServerEntry cse = (ServerEntry)serverVector.elementAt(0);
        if(cse.birth_date == birth_date) return;
        else {//check if we have this server already
	    for(int i=1; i<serverVector.size();i++){
		ServerEntry se = (ServerEntry)serverVector.elementAt(i);
		if(se.birth_date == birth_date){
		    serverVector.removeElementAt(i);
		    serverVector.insertElementAt(se,0);
		    found = true;
		    break;
		}
	    }
	    if(!found) //don't have this server in the vector
		serverVector.insertElementAt(new ServerEntry(this,birth_date,true,false),0);
        }
    }
    
    //makes this server central server
    public void MakeCentral(){
         ServerEntry se = (ServerEntry)serverVector.elementAt(0);
         if(se.birth_date == this.born_on && !central_server){
             central_server = true;
	     WriteToLog("CENTRAL SERVER");
	     SendUpdate();
	 }
	System.out.println("In MakeCentral()");
	 self.t1.stop();
    }
    
    //new server came up
    //add it to the vector and
    //send update so it has our info
    public void ProcessServerJoining(String s){
        StringTokenizer st = new StringTokenizer(s,":");
        String token = st.nextToken();
        String bd = st.nextToken();
        String status = st.nextToken();
        long b_d = Long.parseLong(bd,10); 
        if(b_d == born_on) return;//own message
	else
	    WriteToLog("RECEIVED UPDATE: " + s);
        addServer(b_d);
        SendUpdate();
    }
    
    //remove server from the vector
    //it died or timed out
    public void removeServer(long birth_date){
	WriteToLog("removing server: " + birth_date+ " :");
	for(int i=0; i<serverVector.size();i++){
	    ServerEntry se = (ServerEntry)serverVector.elementAt(i);
	    if(se.birth_date == birth_date){
		se.t.stop();
	
                if(serverVector.removeElement(se))
			WriteToLog("removed");
		else
			WriteToLog("server was not found");
	    }
	}
	if(serverVector.indexOf(self)==0){
	    WriteToLog("CENTRAL SERVER");
	    central_server = true;
	    SendUpdate();
	}
	else{ 
	    central_server = false;
	    
	}
    }
    
    //remove server
    //called from ServerEntry upon time out
    public void removeServer(ServerEntry se){
	se.t.stop();
	serverVector.removeElement(se);
	if(serverVector.indexOf(self)==0){
	    WriteToLog("CENTRAL SERVER");
	    central_server = true;
	    SendUpdate();
	}
	else {
	    central_server = false;
	}
    }
    
    
    //check if this server already exists in the vector
    //if not, add it
    public void addServer(long birth_date){
	//System.out.println("Add: " + birth_date);
	boolean added = false;
	boolean exists = false;
	for(int i =0;i<serverVector.size();i++){
	    ServerEntry se = (ServerEntry)serverVector.elementAt(i);
	    if(se.birth_date > birth_date){
		serverVector.insertElementAt(new ServerEntry(this,birth_date,true,false),i);
		added = true;
		break;
	    } else if(se.birth_date == birth_date){
		exists = true;
		((ServerEntry)serverVector.elementAt(i)).t.restart();   
		break;
            }
	}
	if(!added && !exists){
	    //System.out.println("ADDED");
            serverVector.add(new ServerEntry(this,birth_date,true,false));
	}
	if(serverVector.indexOf(self)==0){
	    WriteToLog("CENTRAL SERVER");
	    central_server = true;
	}
	else{ 
	    central_server = false;
	    WriteToLog("BACKUP SERVER");
	}
	
    }
    
    //notify others that request is invalid
    //so that they remove it from their queue
    public void SendInvalid(){
	String line = new String("INAVLID REQUEST RECEIVED: IGNORING");
	WriteToLog(line);
	if(central_server){
	    try {
		mcs.send(new DatagramPacket(line.getBytes(), line.length(),
					    mcastAddr, port));
	    } catch (IOException ioe) { 
		WriteToLog("Error sending multicast message");
	    }
	}
    }

    //notify others that this server is dying
    public void SendExitUpdate(){
	String line  = new String("UPDATE:"+born_on+":EXITING"); 
	try {
            mcs.send(new DatagramPacket(line.getBytes(), line.length(),
					mcastAddr, port));
        } catch (IOException ioe) { }
    }

    //notify others that we are live and kicking
    public void SendUpdate(){
        String line = new String("UPDATE:"+born_on+":LIVE");
        if(central_server)
            line = line + ":CENTRAL";
        try {
            mcs.send(new DatagramPacket(line.getBytes(), line.length(),
					mcastAddr, port));
        } catch (IOException ioe) { 
		WriteToLog("Error sending update to multicast");
	}
    }
    
    //notify others that this server just came up
    //so that they have our info, and send us theirs
    public void SendJoinUpdate(){
        String line = new String("JOINING:"+born_on+":LIVE");
        try {
            mcs.send(new DatagramPacket(line.getBytes(), line.length(),
					mcastAddr, port));
        } catch (IOException ioe) { }
    }
    
    //class for storing server info
    //contains timers for time out
    //and start up
    public class ServerEntry{
        MulticastServer parent;
        long birth_date;
        javax.swing.Timer t;
        javax.swing.Timer t1;
        ServerEntry(MulticastServer p,long bd,boolean use_timer,boolean self){
            final ServerEntry reference = this;
            parent = p;
            birth_date = bd;
            if(use_timer){
                t = new javax.swing.Timer(25000,new ActionListener() {
                    public void actionPerformed(ActionEvent e){
                        parent.removeServer(reference);
                    }
                });
                t.start();
            }
            else {
                t = new javax.swing.Timer(20000,new ActionListener() {
                    public void actionPerformed(ActionEvent e){
                        parent.SendUpdate();
                    }
                });
                t.start();
            }
            if(self){
                   t1 = new javax.swing.Timer(15000,new ActionListener() {
                    public void actionPerformed(ActionEvent e){
        		System.out.println("Making self central from timer");
	                parent.MakeCentral();
                    }
                });
                t1.start();
		System.out.println("Started  timer");
            } 
        }
    }      

    //clas to store request info in the queue
    public class Request{
	String request;
	MulticastServer owner;
	Timer t;
	String id;
	String ip;
	int port;
	String result;
	
	int action; //1 for search,2 for registration
	
	
	public Request(String s,String i,int a,String address,int port_num,MulticastServer p){
	    request = new String (s);
	    owner = p;
	    id = new String(i);
	    action = a;
	    ip = new String(address);
	    // result = new String(res);
	    port = port_num;
	    final Request reference = this;
	    t = new Timer(10000,new ActionListener() {
		    public void actionPerformed(ActionEvent e){
			owner.unservicedRequest(reference);
		    }
		});
	    t.start();
	}
    }

    //class to store registration info
    //contains DOM document and key assigned by central server
    public class Entry{
	Document doc;
	String key;
	public Entry(Document d, String k){
	    doc = d;
	    key = k;
	}
    }
    
    //store results of the compare operation for DOM documents
    //searched - number of nodes tried for comparison
    //matched  - number of nodes that matched the query
    public class pair{
	int searched;
	int matched;
	
	public void pair(){
	    searched = 0;
	    matched = 0;
	}
	
	public void pair(int i,int j){
	    searched = i;
	    matched = j;
	}
	
	public pair add(pair p){
	    this.searched += p.searched;
	    this.matched += p.matched;
	    return this;
	}
    }

}   



import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
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
import java.util.Date;
import java.net.*;
import java.util.StringTokenizer;

public class Registration{

    final MulticastSocket mcs;
    private static String addr;
    private static InetAddress mcastAddr;
    private static int port;
    private static int listen_port;
    private static String file_name;
    private static String xml_query;
    private static String key;
    private static String id;
    private static String WVM_ip;
    private static int WVM_port;
    private static boolean unregister;
    ServerSocket my_socket = null;
    boolean locked =true;

    public static void main(String args[]) throws IOException {
	System.out.println("usage: java search -m <multicast_addr> -p <port> ");//-u <user>");
	
	String address = "228.5.6.7";
	int port_num = 1234;
	int l_port = 5000;
	String f_name = new String("");
	String query = new String("");
	String Id = new String("");
	String wvmip = InetAddress.getLocalHost().getHostAddress();
	WVM_port = 80;
	unregister = false;
	int i = -1;
	while(++i < args.length) {
	    if (args[i].equals("-m")) address = args[++i];
	    if (args[i].equals("-p")) port_num = Integer.parseInt(args[++i]);
	    if (args[i].equals("-f")) f_name = new String(args[++i]);
	    if (args[i].equals("-l")) l_port = Integer.parseInt(args[++i]);
	    if (args[i].equals("-i")) Id = new String(args[++i]);
	    if (args[i].equals("-unregister")){
		key = new String(args[++i]);
		unregister = true;
	    }
	    // if (args[i].equals("-u")) user = args[++i];
	}
	
	
	
	Registration reg = new Registration(address,port_num,l_port,query,f_name,Id,wvmip,WVM_port);
	
    }


    private Registration(String address, int port_num, int l_port, String query,String f_name,String Id,String ip,int wvmport) throws IOException {

	addr = address;
	port = port_num;
	listen_port = l_port;
	xml_query = query;
	file_name = f_name;
	id = Id;
	WVM_ip = ip;
	WVM_port = wvmport;

	mcastAddr = InetAddress.getByName(addr);
	mcs = new MulticastSocket(port);
	mcs.joinGroup(mcastAddr);
	
	new RegistrationThread(this,true).start();
	new RegistrationThread(this,false).start();
	//SendRequest();
	//GetResponse();
    }


    public void SendRequest(){
	while(locked){
	    System.out.println("LOCKED");;//do nothing
	}
	if(!unregister){
	    if(file_name != null && !file_name.equals("")){ //read query from the file
		SendRequestFromFile();
	    }else{
		SendRequestFromQuery();
	    }
	} else {
	    SendUnRegistrationRequest();
	}
	

    }
    
    public void SendUnRegistrationRequest(){
	String msg_id = new String("registration_"+String.valueOf(new Date().getTime()));
	String local_ip = "0.0.0.0";
	try{
	    local_ip = InetAddress.getLocalHost().getHostAddress();
	} catch (Exception e){
	    System.out.println("Unable to get local host ip...");
	    e.printStackTrace();
	}
	String to_send = new String("UNREGISTER:"+msg_id+":" + local_ip + ":" + listen_port +":"+key);
	try {
	    mcs.send(new DatagramPacket(to_send.getBytes(), to_send.length(),
					mcastAddr, port));
	} catch (IOException ioe) { 
	    System.out.println("Unable to send request");
	}
    }


    public void SendRequestFromFile(){
	String msg_id = new String("registration_"+String.valueOf(new Date().getTime()));
	DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();
	System.out.println("WE get here");
	try {
	    File f = new File(file_name);
	    DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(f);
	    NodeList elist = document.getElementsByTagName("ID");
	
	    Element id_elem = (Element)elist.item(0);//("ID");
	    String id;
	    String local_ip = InetAddress.getLocalHost().getHostAddress();
	    
	    if(id_elem != null){
		id = id_elem.getAttribute("VALUE");
		if(id.equals("")){
		    System.out.println("ID IS MISSING");
		    System.exit(-1);
		}
		else{
		    System.out.println("Trying to register ID: " + id);
		    String to_send = new String("REGISTER:"+msg_id+":" + local_ip+":"+listen_port+":" +DOMToString(document));
		    try {
			mcs.send(new DatagramPacket(to_send.getBytes(), to_send.length(),
						    mcastAddr, port));
		    System.out.println("SENDING");
		    } catch (IOException ioe) { 
			System.out.println("Unable to send request");
		    }
		}
	    }
		    

	} catch (SAXException sxe) {
	    // Error generated by this application
	    // (or a parser-initialization error)
	    Exception  x = sxe;
	    if (sxe.getException() != null)
		x = sxe.getException();
	    x.printStackTrace();
	    
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();
	    
        } catch (IOException ioe) {
	    // I/O error
	    ioe.printStackTrace();
        }
	System.out.println("Sent request");
    }

    
    public void SendRequestFromQuery(){
	String msg_id = new String("registration_"+ String.valueOf(new Date().getTime()));
	String local_ip = "0.0.0.0";
	try{
	    local_ip = InetAddress.getLocalHost().getHostAddress();
	} catch (Exception e){
	    System.out.println("Unable to get local host ip...");
	    e.printStackTrace();
	}
	String query =new String("<?xml version='1.0' encoding='utf-8'?> <WVM_DESCRIPTION> <ID VALUE=\"" + id +"\"/>");
	query = query + "<IP>"+ WVM_ip+"</IP><port>"+WVM_port+"</port>"+query+"</WVM+DESCRIPTION>";
	query  = new String("REGISTER:"+msg_id+":" + local_ip+":"+listen_port+":" + query);
	    try {
	    mcs.send(new DatagramPacket(query.getBytes(), query.length(),
					mcastAddr, port));
	} catch (IOException ioe) { 
	    System.out.println("Unable to send request");
	}
    }



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
	    System.out.println ("\n** Transformer Factory error");
	    System.out.println("   " + tce.getMessage() );
	    
	    // Use the contained exception, if any
	    Throwable x = tce;
	    if (tce.getException() != null)
		x = tce.getException();
	    x.printStackTrace();
	    
        } catch (TransformerException te) {
	    // Error generated by the parser
	    System.out.println ("\n** Transformation error");
	    System.out.println("   " + te.getMessage() );
	    
	    // Use the contained exception, if any
	    Throwable x = te;
	    if (te.getException() != null)
		x = te.getException();
	    x.printStackTrace();
	    
	}
	s = baos.toString();
	return s;
	
    }


    public void GetResponse(){
	try {
            my_socket = new ServerSocket(listen_port);
	    System.err.println("Created socket on port: " + listen_port);
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + listen_port);
            System.exit(-1);
        }
	locked = false;
	try {	
		
	    Socket socket = my_socket.accept();
	    
	    BufferedReader in = null;		
	    in = new BufferedReader( new InputStreamReader( socket.getInputStream()));	    
	    String incoming = new String();
	    String temp;
	    while((temp = in.readLine())!=null)
		incoming = incoming + temp;
	    
	    System.out.println("RECEIVED SEARCH RESPONSE:\n"+incoming);
	    StringTokenizer st = new StringTokenizer(incoming,":");
	    String s = st.nextToken();
	    s = st.nextToken();
	    s = st.nextToken();
	    if(s.equals("REGISTERED")){
		key = st.nextToken();
		System.out.println("STORED KEY: " + key);
	    }
	}
	catch (Exception e) {
	    System.err.println("Error getting server response...");
	    e.printStackTrace();
	}
	try{
	    my_socket.close();
	}catch(Exception e){}
	
    }
	
    public class RegistrationThread extends Thread {
	Registration r;
	boolean thread_flag;
	public RegistrationThread(Registration reg,boolean flag){
	    r = reg;
	    thread_flag = flag;
	}
	public void run(){
	    if(thread_flag)
		r.GetResponse();
	    else{
		try{
		    this.sleep(1000);
		}catch (Exception e){}
		r.SendRequest();
	    }
	}
    }

}

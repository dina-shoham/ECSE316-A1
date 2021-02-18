import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Random;


public class DnsClient {
  
  
  public static void main(String[] args) throws IOException {
    int timeout = 5000;;
    int maxRetries = 3;
    int port = 53;
    String serverType = "A"; // serverType = 0 if IP address (A), 1 if mail server (MX), 2 if name server (NS)   
    byte[] ipAddress = new byte[4];
    String name = "";
    int QNAME_size = 0;
    
    // parsing arguments
    // start at 1 because first arg is DnsClient
    int i;
    for(i = 0; i < args.length; i++) {
//      System.out.println("args[i] = "+args[i]+", i="+i);
      if(args[i].equals("-t")) {
        timeout = Integer.parseInt(args[i+1]) * 1000;
        i++;
        continue;
      }
      else if(args[i].equals("-r")) {
        maxRetries = Integer.parseInt(args[i+1]);
        i++;
        continue;
      }
      else if(args[i].equals("-p")) {
        port = Integer.parseInt(args[i+1]);
        i++;
        continue;
      }
      else if(args[i].equals("-mx")) {
        serverType = "MX";
        continue;
      }
      else if(args[i].equals("-ns")) {
        serverType = "NS";
        continue;
      }
      ipAddress = convertToIP(args[i]);
      i++;
      name = args[i];
      
      // Update the size needed for QNAME
      for (String word : name.split("//.")) {
    	  QNAME_size += (1 + word.length());
      }
      QNAME_size++;
    }
    
    System.out.println("DnsClient sending request for "+name+"\n Server: "+args[i-2]+"\n Request type: "+serverType);
    
    System.out.println("ip address: "+Arrays.toString(ipAddress));
    
    sendRequest(timeout, ipAddress, name, serverType, port, QNAME_size);
    
//    System.out.println("timeout="+timeout+", maxRetries="+maxRetries+", port="+port+", serverType="+serverType);
//    System.out.println("server="+server+", name="+name);
    
  }
  
  public static byte[] convertToIP(String s) {
    if (s.charAt(0) != '@') {
      //@TODO good errors
      System.out.println("invalid ip format");
    }
    
    s = s.replace("@", "");
    
    String sSplit[] = s.split("\\.");
    
    byte[] ip = new byte[4];
    for(int i = 0; i < sSplit.length; i++) {
      ip[i] = Byte.valueOf(sSplit[i]);
    }
    
    return ip;
  }
  
  public static void sendRequest(int timeout, byte[] ipAddress, String name, 
		  String serverType, int port, int QNAME_size) throws IOException {
    try {
      DatagramSocket clientSocket = new DatagramSocket();
      clientSocket.setSoTimeout(timeout);
      InetAddress address = InetAddress.getByAddress(ipAddress);
      
      // Forming the DNS packet
      byte[] sendDNSbytes = new byte[16 + QNAME_size]; // Not sure about size yet
      
      byte[] id = new byte[2];
      Random r = new Random();
      r.nextBytes(id);
      sendDNSbytes[0] = id[0];
      sendDNSbytes[1] = id[1];	// Set random ID as first part of header
      sendDNSbytes[2] = (byte) 0x01; // QR = 0, OPCODE = 0000, AA = 0?, TC = 0?, RD = 1
      sendDNSbytes[3] = (byte) 0x00; // RA = 0?, Z = 000, RCODE = 0000
      sendDNSbytes[4] = (byte) 0x00;
      sendDNSbytes[5] = (byte) 0x01; // QDCODE = 0x0001
      sendDNSbytes[6] = (byte) 0x00;
      sendDNSbytes[7] = (byte) 0x00; // ANCOUNT = 0x0000?
      sendDNSbytes[8] = (byte) 0x00;
      sendDNSbytes[9] = (byte) 0x00; // NSCOUNT = 0x0000?
      sendDNSbytes[10] = (byte) 0x00;
      sendDNSbytes[11] = (byte) 0x00; // ARCOUNT = 0x0000?
      // End of header section
      
      // Fill out QNAME section
      int index = 12;
      for (String word : name.split("//.")) {
    	  sendDNSbytes[index] = (byte) word.length();
    	  index++;
    	  for (int i = 0; i < word.length(); i++, index++) {
    		  sendDNSbytes[index] = (byte) word.charAt(i);
    	  }
      }
      sendDNSbytes[index] = (byte) 0x00; // End QNAME with byte 0
      index++;
      sendDNSbytes[index] = (byte) 0x00; // Start QTYPE with byte 0
      if (serverType.equals("MX")) {
    	  sendDNSbytes[index] = (byte) 0x0f; 
    	  index++;
      }
      else if (serverType.equals("NX")) {
    	  sendDNSbytes[index] = (byte) 0x02; 
    	  index++;
      }
      else { // Type "A"
    	  sendDNSbytes[index] = (byte) 0x01; 
    	  index++;
      }
      sendDNSbytes[index] = (byte) 0x00;
      index++;
      sendDNSbytes[index] = (byte) 0x01; // QCLASS = 0x0001
      
      // No need for other sections of packet?
      
      // Send the created packet
      byte[] receiveDNSbytes = new byte[1024]; // Unsure about size
      DatagramPacket sendPacket = 
    		  new DatagramPacket(sendDNSbytes, sendDNSbytes.length, address, port);
      DatagramPacket receivePacket = 
    		  new DatagramPacket(receiveDNSbytes, receiveDNSbytes.length);
      
      long startTime = System.nanoTime();
      clientSocket.send(sendPacket);
      clientSocket.receive(receivePacket);
      clientSocket.close();
      double timeTaken = (double)((System.nanoTime() - startTime) / 1000000000);
      
      System.out.println("Response received after "+timeTaken+" seconds and ? retries");
      
      
      
    } catch (SocketException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } 
  }  
}

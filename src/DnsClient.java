import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public class DnsClient {
  public static int timeout = 5000;
  public static int maxRetries = 3;
  public static int port = 53;
  public static String serverType = "A"; // serverType = 0 if IP address (A), 1 if mail server (MX), 2 if name server (NS)   
  public static byte[] ipAddress = new byte[4];
  public static String name = "";
  public static int QNAME_size = 0;
  
  
  public static void main(String[] args) throws IOException {
   
    // parsing arguments
    // start at 1 because first arg is DnsClient
    int i;
    for(i = 0; i < args.length; i++) {
//      System.out.println("args[i] = "+args[i]+", i="+i);
      if(args[i].contains("-t")) {
//    	System.out.println("In if");
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
      else if (args[i].charAt(0) == '@'){
    	  if (args[i].charAt(0) != '@') {
    	      //@TODO good errors
    	      System.out.println("invalid ip format");
    	    }
    	  ipAddress = convertToIP(args[i]);

    	  i++;
    	  name = args[i];

    	  // Update the size needed for QNAME
    	  for (String word : name.split("\\.")) {
    		  QNAME_size += (1 + word.length());
    	  }
    	  QNAME_size++;
      }
      else System.out.println("Invalid format");
    }
    
    System.out.println("DnsClient sending request for "+name+"\nServer: "+args[i-2]+"\nRequest type: "+serverType);
    
    System.out.println("ip address: "+Arrays.toString(ipAddress));
    System.out.println("timeout="+timeout+", maxRetries="+maxRetries+", port="+port+", serverType="+serverType);
    System.out.println("server="+name+", name="+name);
        
    receiveResponse(sendRequest(0));
 
  }
  
  public static byte[] convertToIP(String s) {
    System.out.println("IP " + s);
    
    s = s.replace("@", "");
    
    String sSplit[] = s.split("\\.");
    
    byte[] ip = new byte[4];
    for(int i = 0; i < sSplit.length; i++) {
      ip[i] = Byte.valueOf(sSplit[i]);
    }
    
    return ip;
  }
  
  public static byte[] sendRequest(int retries) throws IOException {
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
      for (String word : name.split("\\.")) {
    	  sendDNSbytes[index] = (byte) word.length();
    	  index++;
    	  for (int i = 0; i < word.length(); i++, index++) {
    		  sendDNSbytes[index] = (byte) word.charAt(i);
    	  }
      }
      sendDNSbytes[index] = (byte) 0x00; // End QNAME with byte 0
      index++;
      sendDNSbytes[index] = (byte) 0x00; // Start QTYPE with byte 0
      index++;
      if (serverType.equals("MX")) {
    	  sendDNSbytes[index] = (byte) 0x0f; 
    	  index++;
      }
      else if (serverType.equals("NS")) {
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
      
      System.out.println("Byte array is: "+Arrays.toString(sendDNSbytes));
      
      // No need for other sections of packet?
      
      // Send the created packet
      byte[] receiveDNSbytes = new byte[128]; // Unsure about size
      DatagramPacket sendPacket = 
    		  new DatagramPacket(sendDNSbytes, sendDNSbytes.length, address, port);
      DatagramPacket receivePacket = 
    		  new DatagramPacket(receiveDNSbytes, receiveDNSbytes.length);
      
      long startTime = System.nanoTime();
      clientSocket.send(sendPacket);
      clientSocket.receive(receivePacket);
      clientSocket.close();
      double timeTaken = ((double)(System.nanoTime() - startTime)) / 1000000000;
      
      System.out.println("Response received after "+timeTaken+" seconds and "+retries+" retries");
      return receiveDNSbytes;
      
      
    } catch (SocketTimeoutException e) {
      System.out.println("Timeout"); //TODO better error message
      if(retries < maxRetries) {
        retries++;
        sendRequest(retries);
      } else {
        throw new SocketTimeoutException("ERROR\tMaximum number of retries "+maxRetries+" exceeded.");
      }
    } catch (Exception e) {
      throw e;
    }
    return null;
  }
  
  public static void receiveResponse(byte[] data) {
    int numAnswers = ((data[6] & 0xff) << 8) + (data[7] & 0xff);
    System.out.println("***Answer Section ("+numAnswers+" records)***");
    
    System.out.println(Arrays.toString(data));
    
    // Start at index of answer
    int i = 16 + QNAME_size;
    
    i = analyzeResponse(numAnswers, i, data);
    
    int numAdditional = ((data[10] & 0xff) << 8) + (data[11] & 0xff);
    
    if (numAdditional == 0) System.out.println("NOTFOUND");
    else {
    	System.out.println("***Additional Section ("+numAdditional+" records)***");
    	analyzeResponse(numAdditional,i, data);
    }  
  }    
  
  public static int analyzeResponse(int numAnswers, int i, byte[] data) {
	  
	  for (int responseIndex = 0; responseIndex < numAnswers; responseIndex++) {

	    	// Move to QTYPE bit
	    	while (data[i] != 0) i++;
	    	i++;

	    	// Get the "seconds can cache" 
	    	byte[] TTLBytes = new byte[4];
	    	TTLBytes[0] = data[i+3];
	    	TTLBytes[1] = data[i+4];
	    	TTLBytes[2] = data[i+5];
	    	TTLBytes[3] = data[i+6];
	    	int TTL = ByteBuffer.wrap(TTLBytes).getInt();

	    	// Check if authoritative
	    	int AA = (data[2] >> 5) & 1;
	    	String auth = "";
	    	if (AA == 0) auth += "nonauth";
	    	else auth += "auth";

	    	switch(data[i]) {
	    	case 1: 
	    		//type A
	    		System.out.println("A");
	    		System.out.println("IP\t"
	    				+Byte.toUnsignedInt(data[i+9])+"."
	    				+Byte.toUnsignedInt(data[i+10])+"."
	    				+Byte.toUnsignedInt(data[i+11])+"."
	    				+Byte.toUnsignedInt(data[i+12])
	    				+"\tseconds can cache\t"+TTL+"\t"+auth);
	    		i += 9;
	    		break;
	    	case 2: 
	    		//type NS
	    		System.out.println("NS");

	    		String name = "";
	    		int j = i + 9;

	    		name += getName(data, j, "");

	    		System.out.println("NS\t"+name.substring(0, (name.length() - 1))+"\tseconds can cache\t"+TTL+"\t"+auth);
	    		i += 9;
	    		break;
	    	case 5:
	    		//type CNAME
	    		System.out.println("CNAME");

	    		String alias = "";
	    		int j2 = i + 9;

	    		alias += getName(data, j2, "");

	    		System.out.println("CNAME\t"+alias.substring(0, (alias.length() - 1))+"\tseconds can cache\t"+TTL+"\t"+auth);
	    		i += 9;
	    		break;
	    	case 15:
	    		//type MX
	    		System.out.println("MX");

	    		// Getting pref
	    		int pref = Byte.toUnsignedInt(data[i+9])*256 + Byte.toUnsignedInt(data[i+10]);

	    		// Getting the alias
	    		String alias2 = "";
	    		int j3 = i + 11;
	    		alias2 += getName(data, j3, "");

	    		System.out.println("MX\t"+alias2.substring(0, (alias2.length() - 1))+"\t"+pref+"\tseconds can cache\t"+TTL+"\t"+auth);
	    		i += 11;
	    		break;
	    	default:
	    		System.out.println("no valid type detected"); //TODO error
	    	}
	  }
	  
	  return i;
  }

  // Helper method to generate alias
  public static String getName(byte[] data, int j, String alias) {
	  
	  while (data[j] != 0) {
		  
      	// Compressed data
      	if (data[j] == -64) {
      	  alias += getName(data, data[j+1], "");
      	  j += 2;
      	}
      	else {
          int length = Byte.toUnsignedInt(data[j]);
      	  j++;
      	  while (length > 0) {
      	    alias += (char) data[j];
      		j++;
      		length--;
      	  }
      	  alias += ".";
      	}
	  } 
	  return alias;
  }  
}

import java.io.*;
import java.net.*;
import java.util.Arrays;


public class DnsClient {
  
  
  public static void main(String[] args) {
    int timeout = 5;;
    int maxRetries = 3;
    int port = 53;
    String serverType = "A"; // serverType = 0 if IP address (A), 1 if mail server (MX), 2 if name server (NS)   
    byte[] ipAddress = new byte[4];
    String name = "";
    
    // parsing arguments
    // start at 1 because first arg is DnsClient
    int i;
    for(i = 0; i < args.length; i++) {
//      System.out.println("args[i] = "+args[i]+", i="+i);
      if(args[i].equals("-t")) {
        timeout = Integer.parseInt(args[i+1]);
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
    }
    
    System.out.println("DnsClient sending request for "+name+"\n Server: "+args[i-2]+"\n Request type: "+serverType);
    
    System.out.println("ip address: "+Arrays.toString(ipAddress));
    
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
  
  public static void sendRequest() {
    try {
      DatagramSocket clientSocket = new DatagramSocket();
    } catch (SocketException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
//    clientSocket.
  }
  
}

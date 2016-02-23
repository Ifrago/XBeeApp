package tfg.sensornetwork.readxbee;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import encrypt.StringEncrypt;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import tfg.sensornetwork.readxbee.PortRead;
import tfg.sensornetwork.readxbee.PortWrite;
import tfg.sensornetwork.readxbee.model.XBee;

public class RouterPAN {
	private static PortRead portRead;
	private static PortWrite portWrite;
	private static String nonce="";
	private static String key="";	
	private static OutputStream Output= null;
	
	static XBee xbee = new XBee();
	static StringEncrypt encrypt = new StringEncrypt();
	@SuppressWarnings("deprecation")
	private static void initContador(){
		java.util.Date fecha = new Date();
		nonce=Integer.toString(fecha.getMinutes()+fecha.getHours()+fecha.getDay());
		System.out.println("Contador: "+nonce);
	}
	
	public static void main(String[] args) throws IOException, PortInUseException {
		initContador();
		portRead.comproveNativeLibrary();
		System.out.println("Intentamos abrir el Puerto Serie COM 3...");
		SerialPort portSerie = portRead.openCommPort();
		
		for(;;){
			String readPS =bytesToHex(portRead.readPortSerial(portSerie, 5000));
			String sequency = readPayload(readPS);
			if(sequency!=null){
				switch (sequency) { 
				 case "01":
					System.out.println("Response: "+readPS+" Data: "+sequency);
					xbee.setPass(nonce+""+xbee.getAddress());				
					Output = portSerie.getOutputStream();
					Output.write(xbee.getPass().getBytes());
					System.out.println("Enviamos "+ xbee.getPass());
					System.out.println("Caso 1: HandShake");
					portSerie.close();
					break;
				 case "02":
					System.out.println("Caso 2: Info");
					/*System.out.println("Enviaremos al server");
					java.util.Date fecha = new Date();
					System.out.println (fecha);
					System.out.println("Mensaje:"+response[1]+"  - "+fecha);*/
					portSerie.close();
					break;
				default:
					System.out.println("Error mensaje recibido");
					break;		 
				 }
			}else{
				portSerie.close();
			}
			portSerie = portRead.openCommPort();
			
		}
		
	}
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	public static String readPayload(String packet){
		int j=0;
		String orden="";
		int packetLeng=hex2decimal(packet.substring(2,6));
		char position;
		xbee.setAddress(packet.substring(8,12));
		System.out.println("Long del paquete: "+packetLeng+ "en HEX: "+packet.substring(2,6)+" Dirección del XBee: "+xbee.getAddress());		
		for(int i = 8; i<packetLeng*2;i++ ){
			position=packet.charAt(i);
			if(packet.charAt(i)=='2'){
				i++;
				if(packet.charAt(i)=='F'){
					j=i+1;
					i++;
					while(packet.charAt(i)!= 'F'){
						i++;
					}
					orden= convertHexToString(packet.substring(j,i-1));
					System.out.println("Orden ASCII: "+orden+ " Orden HEX: "+packet.substring(j,i-1));
				}
			}
		}
		return orden;
	}
	
    public static int hex2decimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = digits.indexOf(c);
            val = 16*val + d;
        }
        return val;
    }
    
    public static String convertHexToString(String hex){

  	  StringBuilder sb = new StringBuilder();
  	  StringBuilder temp = new StringBuilder();
  	  
  	  //49204c6f7665204a617661 split into two characters 49, 20, 4c...
  	  for( int i=0; i<hex.length()-1; i+=2 ){
  		  
  	      //grab the hex in pairs
  	      String output = hex.substring(i, (i + 2));
  	      //convert hex to decimal
  	      int decimal = Integer.parseInt(output, 16);
  	      //convert the decimal to character
  	      sb.append((char)decimal);
  		  
  	      temp.append(decimal);
  	  }
  	  System.out.println("Decimal : " + temp.toString());
  	  
  	  return sb.toString();
    }
    
   
}

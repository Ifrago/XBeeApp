package tfg.sensornetwork.readxbee;

import encrypt.StringEncrypt;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import tfg.sensornetwork.readxbee.PortRead;
import tfg.sensornetwork.readxbee.PortWrite;
import tfg.sensornetwork.readxbee.model.XBee;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;

import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.XBeeDevice;

public class OperateFrame {
	
	static XBee xbee = new XBee();
	static String key="92AE31A79FEEB2A3";
	static ArrayList <String> blackList= new ArrayList <String> ();//@MAC que descartaremos;
	static StringEncrypt encrypt = new StringEncrypt();
	private static OutputStream Output= null;
	
	public static boolean comproveAddress(String frame){
		if(!blackList.contains(frame.substring(8,12))){
			xbee.setAddress(frame.substring(8,12));//Guardamos la @MAC
			return false;
		}else{ 
			return true;
		}
	}	
	public static String idFrame(String frame){
		String idFrame=null;
		//Comprobamos que no sea el paquete inicial que siempre recibimos , 7E
		if(frame.length()<=2){
			idFrame=frame.substring(0,frame.length());
		}
		//Enviamos el idFrame, que nos lo dice en el byte 4 del frame
		int frameLength=hex2decimal(frame.substring(6,8));		
		return idFrame;
	}
	public static void operateInfo(String payload,XBeeDevice myDevice, RemoteXBeeDevice remoteDevice) throws Exception{
		
		if(payload.equals("/01/")){
			createChallenge();
			byte[] bytes = ByteBuffer.allocate(4).putInt(123456789).array();
			System.out.println("Data to send: "+bytes);
			myDevice.sendData(remoteDevice,bytes);
		}else if(payload.equals("02")){
			//Enviamos mensaje al server			
		}else if(payload.equals("03")){			//Enviamos mensaje al server
		}else if(payload.equals("99")){
			//Comprobamos que el nonce es bueno, y lo metemos como password
		}else{
			int chalenge = 123456789;
			String payloadDecrypt= StringEncrypt.decrypt(String.valueOf(chalenge),payload);
			System.out.println("Decrypt: "+payloadDecrypt);
		}
		
	}
	public static XBee getXBee (){
		
		return xbee;
	}
	public static String readPayload(String frame){
		int j=0;
		String orden="";
		int packetLeng=hex2decimal(frame.substring(2,6));
		xbee.setAddress(frame.substring(8,12));
		System.out.println("Long del paquete: "+packetLeng+ "en HEX: "+frame.substring(2,6)+" Dirección del XBee: "+xbee.getAddress());		
		for(int i = 8; i<packetLeng*2;i++ ){
			if(frame.charAt(i)=='2'){
				i++;
				if(frame.charAt(i)=='F'){
					j=i+1;
					i++;
					while(frame.charAt(i)!= 'F'){
						i++;
					}
					orden= convertHexToString(frame.substring(j,i-1));
					System.out.println("Orden ASCII: "+orden+ " Orden HEX: "+frame.substring(j,i-1));
				}
			}
		}
		return orden;
	}
	
	static public void createChallenge() throws Exception{
		java.util.Date fecha = new Date();
		String nonce=Integer.toString(fecha.getSeconds()+fecha.getMinutes()+fecha.getHours()+fecha.getDay());
		xbee.setChallenge(encrypt.encrypt(key,nonce+""+xbee.getAddress()));
	}
	
	//Conversores HEX---------------------------------------------------
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

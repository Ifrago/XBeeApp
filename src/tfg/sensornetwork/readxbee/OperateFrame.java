package tfg.sensornetwork.readxbee;

import encrypt.StringEncrypt;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import tfg.sensornetwork.readxbee.PortRead;
import tfg.sensornetwork.readxbee.PortWrite;
import tfg.sensornetwork.readxbee.model.XBee;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.XBeeDevice;

public class OperateFrame {
	
	static String URL_HTTP_BASE= "http://192.168.56.101:3000/";
	static XBee xbee = new XBee();
	static ArrayList<XBee> xbees = new ArrayList<XBee> ();
	static String key="92AE31A79FEEB2A3";
	static ArrayList <String> blackList= new ArrayList <String> ();//@MAC que descartaremos;
	static StringEncrypt encrypt = new StringEncrypt();
	private static OutputStream Output= null;
	
	public static boolean comproveAddress(String frame){
		if(!blackList.contains(frame.substring(8,12))){
			return false;
		}else{ 
			return true;
		}
	}
	public static void setBlackList(String devilMAC){
		blackList.add(devilMAC);
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
		System.out.println("Operate Info------------------------------------");
		if(payload.equals("/01/")){//Enviar al server ( se mueve )
			System.out.println("OPInf: 01");
			System.out.println(httpPostSimple(createMessageServer(1,myDevice.get16BitAddress().toString())));
		}else if(payload.equals("/02/")){//Enviar al server ( no se mueve )
			System.out.println("OPInf: 01");
			System.out.println(httpPostSimple(createMessageServer(2,myDevice.get16BitAddress().toString())));			
		}else{//Error de datos posiblemente corruptos
			System.out.println("OPInf: err, payload corrupt");
			setBlackList(remoteDevice.get16BitAddress().toString());
		}
		
	}
	public static XBee getXBee (){
		
		return xbee;
	}
	public static String readPayload(String frame){
		System.out.println("readPayload---------------------------");
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
	
	@SuppressWarnings("deprecation")
	static public String getTime() throws Exception{
		System.out.println("getTime---------------------------");
		java.util.Date fecha = new Date();
		String time= fecha.getHours()+":"+fecha.getMinutes()+":"+fecha.getSeconds()+" "+fecha.getDay()+"-"+fecha.getMonth()+"-"+fecha.getYear();
		System.out.println("TIME: "+time);
		return time;
	}
	public static String createMessageServer(int action, String MAC) throws Exception{
		System.out.println("createMessageServer---------------------------");
		System.out.println("creating message to server...");
		String message = "";
		if(action==1){
			message= "details={\"mac\":\""+MAC+"\",\"history\":\"Move- "+getTime()+"\"}";
			System.out.println("Message created!: "+message);
			return message;
		}else{
			message= "details={\"mac\":\""+MAC+"\",\"history\":\"Not Move- "+getTime()+"\"}";
			System.out.println("Message created!: "+message);
			return message;
		}
		
	}
	
	public static String httpPostSimple(String message){
		System.out.println("httPostSimple---------------------------");
		  HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead 
		  String URL_FINAL=URL_HTTP_BASE+"xbees/history";
		  HttpResponse response=null;
		  
		  
		try {
		        HttpPost request = new HttpPost(URL_FINAL);
		        StringEntity params =new StringEntity(message);
		        request.addHeader("content-type", "application/x-www-form-urlencoded");
		        request.setEntity(params);
		        response = httpClient.execute(request);
		        System.out.println(response);
		} catch (IOException e) {
		    e.printStackTrace();
		}
		return response.toString() ;
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

package tfg.sensornetwork.readxbee;

import encrypt.StringEncrypt;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;

import tfg.sensornetwork.connector.ConnectorBBDD;
import tfg.sensornetwork.readxbee.model.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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
import com.digi.xbee.api.exceptions.TimeoutException;
import com.digi.xbee.api.exceptions.XBeeException;

public class OperateFrame {
	
	private static String URL_HTTP_BASE= "http://localhost:3000/";
	private static final String DATA_TO_SEND_OK_AUTH = "1";
	private static final String DATA_TO_SEND_NO_AUTH  = "00";
	static XBee xbee = new XBee();
	static String key="92AE31A79FEEB2A3";
	private static XBeeDevice myDevice=null;
	private static ConnectorBBDD mySQLBBDD = new ConnectorBBDD();
	
	public static  boolean comproveAddress(String xbeeAddress) throws SQLException{//Devolvemos FALSE si es una @MAC limpia.
		System.out.println("ComproveAddress("+xbeeAddress+")");	
		BlackList bList = mySQLBBDD.getBlackList(xbeeAddress);
		if(bList.getTiempo()==null) return false;
		else{
			System.out.println("Fecha BlackList: "+bList.getTiempo());
			System.out.println("Fecha Now: "+mySQLBBDD.timeNow());
			
			if(operateDate(bList.getTiempo())<=600000)return true;
			else{
				mySQLBBDD.deleteBlackList(xbeeAddress);
				return false;
			}
		}
	}
	private static boolean comproveNonce(String nonceXBee, XBee xbee) throws SQLException{
		String nonceGood = Integer.toString(Integer.parseInt(xbee.getLastNonce())+1);
		System.out.println("Comprove Nonce("+nonceXBee+","+nonceGood+")");		
		if(nonceXBee==nonceGood)return true;
		else return false;		
	}
	public static void operateInfo(String payloads,XBeeDevice myDeviceF, RemoteXBeeDevice remoteDevice) throws Exception{
		System.out.println("Operate Info------------------------------------");
		String [] payload = payloads.split("/");
		myDevice=myDeviceF;
			if(payload[1].equals("01")){//Enviar al server ( se mueve )
				if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null){
					System.out.println("OPInf: 01||| Nonce: "+payload[0]);
					if(!comproveNonce(payload[0], mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString()))){
						System.out.println(httpPostSimple(1,createMessageServer(1, remoteDevice),remoteDevice));
					}else{
						mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Err_Nonce");
						mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());
						myDevice.sendData(remoteDevice, DATA_TO_SEND_NO_AUTH.getBytes());//Lo desautenticamos						
					}
				}else{
					mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Intrusion");
					mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());
					myDevice.sendData(remoteDevice, DATA_TO_SEND_NO_AUTH.getBytes());//Lo desautenticamos					
				}
			}else if(payload[1].equals("02")){//Enviar al server ( no se mueve )
				if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null){				
					System.out.println("OPInf: 02||| Nonce: "+payload[0]);
					if(!comproveNonce(payload[0], mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString()))){
						System.out.println(httpPostSimple(1,createMessageServer(2, remoteDevice),remoteDevice));
					}else{
						mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Error_Nonce");
						mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());
						myDevice.sendData(remoteDevice, DATA_TO_SEND_NO_AUTH.getBytes());//Lo desautenticamos
					}
				}else{
					mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Intrusion");
					mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());
					myDevice.sendData(remoteDevice, DATA_TO_SEND_NO_AUTH.getBytes());//Lo desautenticamos					
				}
			}else if(payload[1].equals("03")){//XBee se quiere autenticar.
				System.out.println("OPInf: 03");
				System.out.println(httpPostSimple(2,createMessageServer(3, remoteDevice),remoteDevice));	
			}
		
	}	
	public static String createMessageServer(int action, RemoteXBeeDevice remoteDevice) throws Exception{
		System.out.println("createMessageServer---------------------------");
		System.out.println("creating message to server...");
		String message = "";
		if(action==1){
			message= "{\"mac\":\""+myDevice.get64BitAddress().toString()+"\",\"history\":\"Move- "+mySQLBBDD.timeNow()+"\"}";
			System.out.println("Message created!: "+message);
			return message;
		}else if(action==2){
			message= "{\"mac\":\""+myDevice.get64BitAddress().toString()+"\",\"history\":\"Not Move- "+mySQLBBDD.timeNow()+"\"}";
			System.out.println("Message created!: "+message);
			return message;
		}else if(action==3){
			message= "{\"mac\":\""+myDevice.get64BitAddress().toString()+"\",\"macnet\":\""+remoteDevice.get64BitAddress().toString()+"\"}";
			System.out.println("Message created!: "+message);
			return message;
		}else return null;
				
	}
	
	public static String httpPostSimple(int action, String message, RemoteXBeeDevice remoteDevice) throws TimeoutException, XBeeException, SQLException{
		System.out.println("httPostSimple---------------------------");
		  HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead 
		  String URL_FINAL=null;
		  String dataSend= null;
		  BlackList blist = new BlackList();
		 if(action==1)URL_FINAL=URL_HTTP_BASE+"xbees/xbeepan/history";
		 else  URL_FINAL=URL_HTTP_BASE+"xbees/auth/xbeenet";
		  HttpResponse response=null;
		  String nonce=null;
		  if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null) //miramos si XBee ya lo teniamos en la BBDD
			  nonce=mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString()).getLastNonce();
		  
		try {
			if(message!=null){				
		        HttpPost request = new HttpPost(URL_FINAL);
		        StringEntity params =new StringEntity(message);
		        request.addHeader("content-type", "application/json");
		        request.setEntity(params);
		        response = httpClient.execute(request);
		        System.out.println("operacion: "+action);
		        System.out.println("respusta: "+response.getStatusLine().toString());		        
		        if(action==2 && response.getStatusLine().toString().equals("HTTP/1.1 200 OK")){
		        	System.out.println("Auth Valid!");
		        	nonce = createNonce();
		        	if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())==null) mySQLBBDD.addXBee(remoteDevice.get64BitAddress().toString(), nonce, 1);
		        	else if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null) mySQLBBDD.AuthXBee(remoteDevice.get64BitAddress().toString(), 1);
		        	dataSend= nonce+DATA_TO_SEND_OK_AUTH;
		        	System.out.println("Datasend to XBee: "+dataSend);
		        	mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Auth");
		        	myDevice.sendData(remoteDevice, dataSend.getBytes());
		        	mySQLBBDD.updateLastNonce(remoteDevice.get64BitAddress().toString(), Integer.toString(Integer.parseInt(nonce)+1));
		        }else if(action==2 && !response.getStatusLine().toString().equals("HTTP/1.1 200 OK")){
		        	System.out.println("Auth Invalid!");
		        	if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null) mySQLBBDD.AuthXBee(remoteDevice.get64BitAddress().toString(),0);
					mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());
		        	System.out.println("UnAtuh XBee");
		        	myDevice.sendData(remoteDevice, DATA_TO_SEND_NO_AUTH.getBytes());
		        	mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Err_Auth");
		        }else if(action!=2 && !response.getStatusLine().toString().equals("HTTP/1.1 200 OK")){
		        	System.out.println("History Invalid!");
					mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());
					if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null) mySQLBBDD.AuthXBee(remoteDevice.get64BitAddress().toString(),0);
		        	System.out.println("Datasend to XBee: 0");
		        	myDevice.sendData(remoteDevice, DATA_TO_SEND_NO_AUTH.getBytes());
		        	mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Err_History");
		        }else if(action!=2 && response.getStatusLine().toString().equals("HTTP/1.1 200 OK")){
		        	System.out.println("History Valid!");
		        	dataSend= nonce+DATA_TO_SEND_OK_AUTH;
		        	System.out.println("Datasend to XBee: "+dataSend);
		        	myDevice.sendData(remoteDevice, dataSend.getBytes());
		        	mySQLBBDD.updateLastNonce(remoteDevice.get64BitAddress().toString(), Integer.toString(Integer.parseInt(nonce)+1));
		        }else System.out.println("Error Server: "+response.getStatusLine().toString());
				
		       
			}else{
				mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Msg_error");
			}		        
		} catch (IOException e) {
		    e.printStackTrace();
		}
		 return response.toString() ;
	}
	
	static String createNonce(){
		System.out.println("createNonce()---------------------------");
		java.util.Random rnd = new java.util.Random();		
		int numRandom =(int) (rnd.nextDouble()*1000000000);
		String nonce = Integer.toString(numRandom);
		System.out.println("Number random created is "+numRandom+" and your length is: "+nonce.length());
		return nonce;
	}
	//Operaciones-------------------------------------------------------
	private static long operateDate(String timeOld){	
		System.out.println("OPERATEDATE()------------");
		long resultado = System.currentTimeMillis()-Long.parseLong(timeOld);
		System.out.println("Milisegundos castigados: "+resultado+'\n'+"---------------------------------");
		return resultado;	
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

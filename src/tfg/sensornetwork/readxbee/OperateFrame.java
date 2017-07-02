package tfg.sensornetwork.readxbee;


import tfg.sensornetwork.connector.ConnectorBBDD;
import tfg.sensornetwork.https.HTTPSClient;
import tfg.sensornetwork.readxbee.model.*;

import java.io.*;
import java.net.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

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
import com.digi.xbee.api.models.XBee64BitAddress;

public class OperateFrame {
	private static final String DATA_TO_SEND_OK_AUTH = "1";
	private static final String KEY_MASTER="7654210858123584"; // en uint8_t= "55 54 53 52 51 50 49 48 56"
	private static final String IV_MASTER="5681972815796382"; // en uint8_t= "53 54 56 49 57 55 50 56 53"
	private static final String KEYMAC_XBEE = "9752591558123584";
	private static String URL_HTTP_BASE= "http://localhost:4000/";
	private static String HTTPS_URL_LH = "https://127.0.0.1:443/";
	private static String HTTPS_URL = "https://xbee-server.herokuapp.com/";
	static XBee xbee = new XBee();
	static String key="92AE31A79FEEB2A3";
	private static XBeeDevice myDevice=null;
	private static ConnectorBBDD mySQLBBDD = new ConnectorBBDD();
	static HTTPSClient HttpsConn = new HTTPSClient();
	private static boolean connect2serverHTTPS = false;
	
	public static void toReviveXBee(RemoteXBeeDevice remoteDevice) throws Exception{	
		ArrayList <XBee> xbees = mySQLBBDD.keepAlive();
		
		for(int i=0;i<xbees.size();i++){		
			if(comproveAddress(xbees.get(i).getAddress())==false){
				mySQLBBDD.addLog(xbees.get(i).getAddress(), "Not_HelloPacket");
				System.out.println(httpPostSimple(4,createMessageServer(3, remoteDevice.get64BitAddress().toString()),null,xbees.get(i).getAddress()));//Enviamos al server que el XBee se ha desconectado.
			}		

		}
	}
	public static  boolean comproveAddress(String xbeeAddress) throws SQLException{//Devolvemos FALSE si es una @MAC limpia.
		System.out.println("ComproveAddress("+xbeeAddress+")");	
		BlackList bList = mySQLBBDD.getBlackList(xbeeAddress);
		if(bList.getTiempo()==null) return false;
		else{
			System.out.println("Fecha BlackList: "+bList.getTiempo());
			System.out.println("Fecha Now: "+mySQLBBDD.timeNow());
			
			if(operateDate(bList.getTiempo())<=6000)return true;//1 min castigado
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
		System.out.println("Payload Length: "+payload.length);
		if(payload.length==3 || payload.length==2){
			myDevice=myDeviceF;
			String dataSend= null;
			String URL_FINAL=null;
			String nonce=null;
			if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null) //miramos si XBee ya lo teniamos en la BBDD
				nonce=mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString()).getLastNonce();
				
			//Autenticamos el mensaje a partir de MAC (Message Authenticated Code){Cifrado+Hash)
			//if(hashClass.getDigest(payload[2]).equals(hashClass.getDigest(payloads))){//Hacemos auth por MAC.
				if(payload[1].equals("01")){//Enviar al server ( se mueve )
					if(authMAC(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString()),payload[2],payload[0]+payload[1])){	
						System.out.println("OPInf: 01||| Nonce: "+payload[0]);
						if(!comproveNonce(payload[0], mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString()))){//Si es falso, el NONCE es correcto
							if(connect2serverHTTPS){//--------HTTPS---------
								URL_FINAL=HTTPS_URL+"xbees/xbeepan/history";
								mySQLBBDD.updateHelloTime(remoteDevice.get64BitAddress().toString(),Long.toString(System.currentTimeMillis()));
								if(!readResponse(HttpsConn.HttpsPost(URL_FINAL, mySQLBBDD,createMessageServer(1, remoteDevice.get64BitAddress().toString()),remoteDevice, null))){
									System.out.println("History Invalid!");
									mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());
									if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null) mySQLBBDD.AuthXBee(remoteDevice.get64BitAddress().toString(),0);//Miramos que esté en la BBDD del router
						        	mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Err_History");
						        }else{
						        	System.out.println("History Valid!");
						        	dataSend= nonce+DATA_TO_SEND_OK_AUTH;
						        	System.out.println("Datasend to XBee: "+dataSend);
						        	mySQLBBDD.updateConnectivity(remoteDevice.get64BitAddress().toString(), 1);
						        	mySQLBBDD.updateHelloTime(remoteDevice.get64BitAddress().toString(), Long.toString(System.currentTimeMillis()));
						        	myDevice.sendData(remoteDevice, dataSend.getBytes());
						        	mySQLBBDD.updateLastNonce(remoteDevice.get64BitAddress().toString(), Integer.toString(Integer.parseInt(nonce)+1));
						        	mySQLBBDD.incrementMsgChangeAESKey(remoteDevice.get64BitAddress().toString());
						        	//CAMBIAMOS LA AESKEY ADA 10KEY if(mySQLBBDD.getCount(remoteDevice.get64BitAddress().toString())==10) myDevice.executeParameter(parameter);
						        }
							}else{//--------HTTP----------
					        	System.out.println(httpPostSimple(1,createMessageServer(1, remoteDevice.get64BitAddress().toString()),remoteDevice, null));
							}		
						}else{
							mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Err_Nonce");//Err en el nonce, posible ataque replay.
							mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());//Lo metemos en la BlackList						
						}
					}else{
						mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Intrusion MAC");//MAC no concuerda, no es quien dice ser
						mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());// Lo metemos en la BlackList				
					}
				}else if(payload[1].equals("02")){//Enviar al server ( no se mueve )
					if(authMAC(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString()),payload[2],payload[0]+payload[1])){			
						System.out.println("OPInf: 02||| Nonce: "+payload[0]);
						if(!comproveNonce(payload[0], mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString()))){//Si es falso, el NONCE es correcto
							if(connect2serverHTTPS){//--------HTTPS---------
								URL_FINAL=HTTPS_URL+"xbees/xbeepan/history"; 
								mySQLBBDD.updateHelloTime(remoteDevice.get64BitAddress().toString(),Long.toString(System.currentTimeMillis()));
								if(!readResponse(HttpsConn.HttpsPost(URL_FINAL, mySQLBBDD,createMessageServer(2, remoteDevice.get64BitAddress().toString()),remoteDevice, null))){
									System.out.println("History Invalid!");
									mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());
									if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null) mySQLBBDD.AuthXBee(remoteDevice.get64BitAddress().toString(),0);//Miramos que esté en la BBDD del router
						        	mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Err_History");
						        }else{
						        	System.out.println("History Valid!");
						        	dataSend= nonce+DATA_TO_SEND_OK_AUTH;
						        	System.out.println("Datasend to XBee: "+dataSend);
						        	mySQLBBDD.updateConnectivity(remoteDevice.get64BitAddress().toString(), 1);
						        	mySQLBBDD.updateHelloTime(remoteDevice.get64BitAddress().toString(), Long.toString(System.currentTimeMillis()));
						        	myDevice.sendData(remoteDevice, dataSend.getBytes());
						        	mySQLBBDD.updateLastNonce(remoteDevice.get64BitAddress().toString(), Integer.toString(Integer.parseInt(nonce)+1));
						        	mySQLBBDD.incrementMsgChangeAESKey(remoteDevice.get64BitAddress().toString());
						        	//CAMBIAMOS LA AESKEY ADA 10KEY if(mySQLBBDD.getCount(remoteDevice.get64BitAddress().toString())==10) myDevice.executeParameter(parameter);
						        
								}
							}else{//--------HTTP----------
								System.out.println(httpPostSimple(1,createMessageServer(2, remoteDevice.get64BitAddress().toString()),remoteDevice, null));
							}
						}else{
							mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Error_Nonce");//Err en el nonce, posible ataque replay
							mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());//Lo metemos en la BlackList
						}
					}else{
						mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Intrusion MAC");//MAC no concuerda, no es quien dice ser
						mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());//Lo metemos en la BlackList
					}
				}else if(payload[1].equals("03")){//XBee se quiere autenticar. 
					if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())==null) mySQLBBDD.addXBee(remoteDevice.get64BitAddress().toString(), nonce, 1);
					String encryptChallange= encrypt(KEY_MASTER,IV_MASTER, payload[2]);
					String challange4Sensor=createNumRandom("challange");
					System.out.println(" Encrypt Challange sensor: "+encryptChallange+"\n Challange for Sensor: "+challange4Sensor);
					dataSend= "/==/" + encryptChallange + challange4Sensor; //Length = 18.
					mySQLBBDD.updateChallange(remoteDevice.get64BitAddress().toString(), challange4Sensor);
					System.out.println("DataSend: " + dataSend);
					myDevice.sendData(remoteDevice, dataSend.getBytes());
				}else if(payload[1].equals("04")){//Respuesta Challange ( comprobamos que es el ).
					System.out.println("OPInf: 04");
					//if(mySQLBBDD.getChallange(remoteDevice.get64BitAddress().toString()).equals(payload[2])){
					if(true){
						if(connect2serverHTTPS){//--------HTTPS---------
							URL_FINAL=HTTPS_URL+"xbees/auth/xbeenet";
							
							//HttpsURLConnection conn = HttpsConn.connect(URL_FINAL);
							if(!readResponse(HttpsConn.HttpsPost(URL_FINAL, mySQLBBDD,createMessageServer(3, remoteDevice.get64BitAddress().toString()),remoteDevice, null))){
					        	System.out.println("Auth Invalid!");//No se encuentra en la BBDD del server
					        	if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null) mySQLBBDD.AuthXBee(remoteDevice.get64BitAddress().toString(),0);
								mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());// Lo metemos en la BlackList
					        	System.out.println("UnAtuh XBee");
					        	mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Err_Auth");
					        }else{
					        	System.out.println("Auth Valid!");
					        	nonce = createNumRandom("nonce");			        	
					        	if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())==null) mySQLBBDD.addXBee(remoteDevice.get64BitAddress().toString(), nonce, 1);
					        	else if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null) mySQLBBDD.AuthXBee(remoteDevice.get64BitAddress().toString(), 1);
					        	String iv=createIV();
					        	
					        	dataSend= nonce+iv+DATA_TO_SEND_OK_AUTH;//DATA= [9bytes]+[16bytes]+[1bytes]
					        	mySQLBBDD.AuthXBee(remoteDevice.get64BitAddress().toString(), 1);
					        	mySQLBBDD.updateKeyMAC(remoteDevice.get64BitAddress().toString(), KEYMAC_XBEE);
					        	mySQLBBDD.updateIV(remoteDevice.get64BitAddress().toString(), iv);
					        	mySQLBBDD.updateHelloTime(remoteDevice.get64BitAddress().toString(),Long.toString(System.currentTimeMillis()));
					        	mySQLBBDD.updateConnectivity(remoteDevice.get64BitAddress().toString(), 1);
					        	System.out.println("Datasend to XBee: "+dataSend);
					        	mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Auth");
					        	myDevice.sendData(remoteDevice, dataSend.getBytes());
					        	mySQLBBDD.updateLastNonce(remoteDevice.get64BitAddress().toString(), Integer.toString(Integer.parseInt(nonce)+1));
					        }
					        }else{//--------HTTP----------
					        	System.out.println(httpPostSimple(2,createMessageServer(3, remoteDevice.get64BitAddress().toString()),remoteDevice, null));//Revisamos que está auth en el servidor.
					      }
					}else{
			        	System.out.println("Challange not equal!");
			        }
				}else if(payload[1].equals("05")){//XBee Hello packet.
					mySQLBBDD.updateHelloTime(remoteDevice.get64BitAddress().toString(), Long.toString(System.currentTimeMillis()));
					if(authMAC(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString()),payload[2],payload[0]+payload[1])){
						System.out.println("OPInf: 05");
						mySQLBBDD.updateConnectivity(remoteDevice.get64BitAddress().toString(), 1);	
					}else{
						mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Intrusion MAC");//MAC no concuerda, no es quien dice ser
						mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());//Lo metemos en la BlackList
					}
				}
					
			}else if(payload.length == 1){
			System.out.println(">>>>Strange Data recieve...");
		}else{
			System.out.println(">>>>Strange Data recieve...");
		}
		/*}else{//No coinciden el digest del mensaje con el hecho por nosotros
			System.out.println("No Autenticated for HASH!!");
			mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Intrusion");
			mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());
		}*/
		
	}
	public static String createMessageServer(int action, String remoteDeviceAddress) throws Exception{
		System.out.println("createMessageServer---------------------------");
		System.out.println("creating message to server...");
		String message = "";
		if(action==1){
			message= "{\"mac\":\""+myDevice.get64BitAddress().toString()+"\",\"history\":\"Move- "+mySQLBBDD.timeNow()+"\",\"macnet\":\""+remoteDeviceAddress+"\"}";
			System.out.println("Message created!: "+message);
			return message;
		}else if(action==2){
			message= "{\"mac\":\""+myDevice.get64BitAddress().toString()+"\",\"history\":\"Not Move- "+mySQLBBDD.timeNow()+"\",\"macnet\":\""+remoteDeviceAddress+"\"}";
			System.out.println("Message created!: "+message);
			return message;
		}else if(action==3){
			message= "{\"mac\":\""+myDevice.get64BitAddress().toString()+"\",\"macnet\":\""+remoteDeviceAddress+"\"}";
			System.out.println("Message created!: "+message);
			return message;
		}else return null;
				
	}
	
	public static String httpPostSimple(int action, String message, RemoteXBeeDevice remoteDevice, String address) throws TimeoutException, XBeeException, SQLException, NoSuchProviderException, NoSuchAlgorithmException{
		System.out.println("httPostSimple---------------------------");
		  HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead 
		  String URL_FINAL=null;
		  String dataSend= null;
		 if(action==1)URL_FINAL=HTTPS_URL+"xbees/xbeepan/history";
		 else if(action==2) URL_FINAL=HTTPS_URL+"xbees/auth/xbeenet";
		  HttpResponse response=null;
		  String nonce=null;
		  if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null) //miramos si XBee ya lo teniamos en la BBDD
			  nonce=mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString()).getLastNonce();
	        System.setProperty("javax.net.debug", "ssl,handshake");     
	        System.setProperty("Djavax.net.ssl.keyStoreType", "pkcs12");
	        System.setProperty("Djavax.net.ssl.keyStore", "C:/.routerXBee.p12");
	        System.setProperty("-Djavax.net.ssl.keyStorePassword", "ivan");
	        
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
		        	nonce = createNumRandom("nonce");			        	
		        	if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())==null) mySQLBBDD.addXBee(remoteDevice.get64BitAddress().toString(), nonce, 1);
		        	else if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null) mySQLBBDD.AuthXBee(remoteDevice.get64BitAddress().toString(), 1);
		        	String iv=createIV();
		        	dataSend= nonce+iv+DATA_TO_SEND_OK_AUTH;//DATA= [9bytes]+[16bytes]+[1bytes]
		        	mySQLBBDD.AuthXBee(remoteDevice.get64BitAddress().toString(), 1);
		        	mySQLBBDD.updateKeyMAC(remoteDevice.get64BitAddress().toString(), KEYMAC_XBEE);
		        	mySQLBBDD.updateIV(remoteDevice.get64BitAddress().toString(), iv);
		        	mySQLBBDD.updateHelloTime(remoteDevice.get64BitAddress().toString(),Long.toString(System.currentTimeMillis()));
		        	mySQLBBDD.updateConnectivity(remoteDevice.get64BitAddress().toString(), 1);
		        	System.out.println("Datasend to XBee: "+dataSend);
		        	mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Auth");
		        	myDevice.sendData(remoteDevice, dataSend.getBytes());
		        	mySQLBBDD.updateLastNonce(remoteDevice.get64BitAddress().toString(), Integer.toString(Integer.parseInt(nonce)+1));
		        	
		        }else if(action==2 && !response.getStatusLine().toString().equals("HTTP/1.1 200 OK")){
		        	System.out.println("Auth Invalid!");//No se encuentra en la BBDD del server
		        	if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null) mySQLBBDD.AuthXBee(remoteDevice.get64BitAddress().toString(),0);
					mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());// Lo metemos en la BlackList
		        	System.out.println("UnAtuh XBee");
		        	mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Err_Auth");
		        	
		        }else if(action!=2 && !response.getStatusLine().toString().equals("HTTP/1.1 200 OK")){
		        	System.out.println("History Invalid!");
					mySQLBBDD.addBlackList(remoteDevice.get64BitAddress().toString());
					if(mySQLBBDD.getXBee(remoteDevice.get64BitAddress().toString())!=null) mySQLBBDD.AuthXBee(remoteDevice.get64BitAddress().toString(),0);//Miramos que esté en la BBDD del router
		        	mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Err_History");
		        }else if(action!=2 && response.getStatusLine().toString().equals("HTTP/1.1 200 OK")){
		        	System.out.println("History Valid!");
		        	dataSend= nonce+DATA_TO_SEND_OK_AUTH;
		        	System.out.println("Datasend to XBee: "+dataSend);
		        	myDevice.sendData(remoteDevice, dataSend.getBytes());
		        	mySQLBBDD.updateLastNonce(remoteDevice.get64BitAddress().toString(), Integer.toString(Integer.parseInt(nonce)+1));
		        	mySQLBBDD.incrementMsgChangeAESKey(remoteDevice.get64BitAddress().toString());
		        	//CAMBIAMOS LA AESKEY ADA 10KEY if(mySQLBBDD.getCount(remoteDevice.get64BitAddress().toString())==10) myDevice.executeParameter(parameter);
		        }else System.out.println("Error Server: "+response.getStatusLine().toString());
				
		       
			}else{
				mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "Msg_error");
			}		        
		} catch (IOException e) {
		    e.printStackTrace();
		}
		 return response.toString() ;
	}
	
	
	private static boolean authMAC(XBee xbee, String mac, String message) throws SQLException{
		System.out.println("authMAC()---------------------------");
		if(xbee.getAddress()!=null){//Comprobamos que el XBEE esté en la BBDD del router.
			String key = xbee.getKeyMAC(), initVector=xbee.getIv();
			System.out.println("Message: "+message+" |MAC "+mac+" |KeyMAC: "+key+" |IV: "+initVector);
						
			String decryptMAC = decrypt(key, initVector,encrypt(key, initVector, message));
			System.out.println("decryptMAC: "+decryptMAC);
			if(message.equals(decryptMAC)) return true;
			else return false;			
		}else{
			mySQLBBDD.addLog(xbee.getAddress(), "Intrusion MAC");//No está en la BBDD del router, posible intrusión.
			mySQLBBDD.addBlackList(xbee.getAddress());//Lo metemos en la BlackList.	
			return false;
		}
	}
	//Operaciones-------------------------------------------------------
	private static long operateDate(String timeOld){	
		System.out.println("OPERATEDATE()------------");
		long resultado = System.currentTimeMillis()-Long.parseLong(timeOld);
		System.out.println("Milisegundos castigados: "+resultado+'\n'+"---------------------------------");
		return resultado;	
	}
	static String createNumRandom(String type){
		System.out.println("createNumRandom(" + type + ")---------------------------");
		java.util.Random rnd = new java.util.Random();	
		int numRandom = 0;
		if (type == "nonce"){
			numRandom =(int) (rnd.nextDouble()*1000000000);
		}else if (type == "challange"){
			numRandom =(int) (rnd.nextDouble()*100000000);
		}else{
			System.out.println(">>>>> ERROR! valor de entrada type incorrecto, tiene que ser nonce o challange");
			return "00000000";
		}
		String rand = Integer.toString(numRandom);
		System.out.println("Number random created ("+type+") is "+numRandom+" and your length is: "+rand.length());
		return rand;
	}
	static String createIV() throws NoSuchProviderException, NoSuchAlgorithmException{//OK
		SecureRandom sr =new SecureRandom();//Generamos un nuevo generador con el algoritmo SHA1 y garantizamos una nueva semilla
		sr.nextBytes(new byte[1]);// Iniciamos el Generador, se hace para causar la generacion de una nueva semilla.
		Long ivLong =sr.nextLong();	//Generamos un SecureRandom de tipo Long.
		ivLong = Math.abs(ivLong);
		String iv=ivLong.toString();
		if (iv.length()!=16){
			int factor = iv.length()-16;
			int div = (int) Math.pow(10, factor);
			ivLong= ivLong/div;
			iv=ivLong.toString();	
		}
		return iv;
	}
	private static boolean readResponse(Response response){
		System.out.println("READRESPONSE ResponseGetRequest: "+response.getRequest());
		if(response.getRequest().equals("access")|| response.getRequest().equals("ok")) return true;
		else return false;
	}
    private static String encrypt(String key, String initVector, String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
//            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            System.out.println("encrypted string: "
                    + Base64.getEncoder().encodeToString(encrypted));

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
    private static String decrypt(String key, String initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
//            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);

            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            
            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
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

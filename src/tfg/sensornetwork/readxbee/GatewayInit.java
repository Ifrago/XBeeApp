package tfg.sensornetwork.readxbee;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import encrypt.StringEncrypt;
//Imports antiguos
import gnu.io.PortInUseException;
import gnu.io.SerialPort;


import tfg.sensornetwork.readxbee.PortRead;
import tfg.sensornetwork.readxbee.PortWrite;
import tfg.sensornetwork.readxbee.model.XBee;
import tfg.sensornetwork.readxbee.*;

import com.digi.xbee.api.RemoteXBeeDevice;
//Imports de la API JAVA XBEE
import com.digi.xbee.api.XBeeDevice;
import com.digi.xbee.api.XBeeNetwork;
import com.digi.xbee.api.exceptions.XBeeException;
import com.digi.xbee.api.models.XBeeMessage;
import com.digi.xbee.api.utils.HexUtils;



public class GatewayInit {
	//Parametros del puerto serie
	private static final String PORT = "COM3";
	private static final int BAUD_RATE = 9600;
	private static final String REMOTE_NODE_IDENTIFIER = "TX";

	
	static XBee xbee = new XBee();

	public static void main(String[] args) throws Exception {
		XBeeDevice myDevice = new XBeeDevice(PORT, BAUD_RATE);
		
		for(;;){
			
			try{
				myDevice.open();
				XBeeNetwork xbeeNetwork = myDevice.getNetwork();
				RemoteXBeeDevice remoteDevice = xbeeNetwork.discoverDevice(REMOTE_NODE_IDENTIFIER);
				XBeeMessage xbeeMessage =myDevice.readData();
				if (xbeeMessage != null){
					
					System.out.format("From %s >> %s | %s%n", xbeeMessage.getDevice().get16BitAddress(), 
							HexUtils.prettyHexString(HexUtils.byteArrayToHexString(xbeeMessage.getData())), 
							new String(xbeeMessage.getData()));
					OperateFrame.operateInfo(new String(xbeeMessage.getData()),myDevice,remoteDevice);
				}
					
				System.out.println("\n>> Waiting for data...");
				
			} catch (XBeeException e) {
	            System.out.println(" >> Error");
	            e.printStackTrace();
	            System.exit(1);
	        } finally {
	            myDevice.close();
	        }
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

}

package tfg.sensornetwork.readxbee;

import tfg.sensornetwork.connector.*;

//Imports de la API JAVA XBEE
import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.XBeeDevice;
import com.digi.xbee.api.XBeeNetwork;
import com.digi.xbee.api.exceptions.XBeeException;
import com.digi.xbee.api.models.XBeeMessage;
import com.digi.xbee.api.utils.HexUtils;




public class GatewayInit {
	//Parametros del puerto serie
	private static final String PORT = "COM4";
	private static final int BAUD_RATE = 9600;
	private static final String REMOTE_NODE_IDENTIFIER = "TX";
	
	static ConnectorBBDD mySQLBBDD = new ConnectorBBDD();
	

	public static void main(String[] args) throws Exception {
		XBeeDevice myDevice = new XBeeDevice(PORT, BAUD_RATE);
		mySQLBBDD.connectBBDD();
		
//		ViewHelloMsgThread viewHelloThread = new  ViewHelloMsgThread(myDevice);
//		if (viewHelloThread.getState() == Thread.State.NEW)
//		{
//			viewHelloThread.start();
//		}

		
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
					
					if(remoteDevice != null){
						mySQLBBDD.addLog(remoteDevice.get64BitAddress().toString(), "connection");
					
						boolean beBlackList = OperateFrame.comproveAddress(remoteDevice.get64BitAddress().toString());
						System.out.println(beBlackList);
						if(!beBlackList)OperateFrame.operateInfo(new String(xbeeMessage.getData()),myDevice,remoteDevice);
					}else
						System.out.println("remoteDevice is null!! Fix it!");
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


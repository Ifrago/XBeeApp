package tfg.sensornetwork.readxbee;

import com.digi.xbee.api.listeners.IDataReceiveListener;
import com.digi.xbee.api.models.XBeeMessage;
import com.digi.xbee.api.utils.HexUtils;

/**
 * Class to manage the XBee received data that was sent by other modules in the 
 * same network.
 * 
 * <p>Acts as a data listener by implementing the 
 * {@link IDataReceiveListener} interface, and is notified when new 
 * data for the module is received.</p>
 * 
 * @see IDataReceiveListener
 *
 */
public class MyDataReceiveListener implements IDataReceiveListener {

	@Override
	public void dataReceived(XBeeMessage xbeeMessage) {
		System.out.format("From %s >> %s | %s%n", xbeeMessage.getDevice().get16BitAddress(), 
				HexUtils.prettyHexString(HexUtils.byteArrayToHexString(xbeeMessage.getData())), 
				new String(xbeeMessage.getData()));
	}
}
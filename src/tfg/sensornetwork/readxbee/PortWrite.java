package tfg.sensornetwork.readxbee;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import gnu.io.SerialPort;

public class PortWrite extends Thread {

	private static OutputStream Output= null;
	
	public void sendNonce (SerialPort portSerie, int nonce) throws IOException{
		System.out.println("enviando Nonce!");
		Output = portSerie.getOutputStream();
		Output.write(Integer.toString(nonce).getBytes());
		portSerie.close();
	}

	public void receiveMessage(SerialPort portSerie) {
		System.out.println("Enviaremos al server");
		java.util.Date fecha = new Date();
		System.out.println (fecha);
		System.out.println("Mensaje:  - "+fecha);
		portSerie.close();		
	}
}

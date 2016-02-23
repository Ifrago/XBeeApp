package tfg.sensornetwork.readxbee;

import java.util.*;
import java.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ShutdownChannelGroupException;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

public class PortRead extends Thread{
	private static RouterPAN routerPAN;
	
	private OutputStream output= null;
	

	public static void comproveNativeLibrary(){
		/*Comprobamos que tenemos la librerianativa para leer puertos serie.
		 * */
		try {
			System.loadLibrary("rxtxSerial");
			System.out.println("Se ha cargado la librería nativa corectamente");
		} catch (UnsatisfiedLinkError u) {
			System.err.println("No se ha encontrado la librería nativa de puerto serie.");
		}
	}
	public static SerialPort openCommPort() throws IOException, PortInUseException{
		/*Enumeramos los puertos serie disponibles en el sistema,
		si el puerto serie está ocupado por un programa, no se podrá
		ver en el listado que generaremos.
		*/
		Enumeration portsList= CommPortIdentifier.getPortIdentifiers();
		CommPortIdentifier idPort=null;
		boolean found = false;
		while(portsList.hasMoreElements() && !found){//Buscamos todos los puertos disponibles.
			idPort=(CommPortIdentifier) portsList.nextElement();
			if(idPort.getPortType() == CommPortIdentifier.PORT_SERIAL){
				if(idPort.getName().equals("COM3")){
					found=true;
					SerialPort portSerie = null;
					portSerie= (SerialPort) idPort.open("XBeeS1",2000);
					portSerie = configurePortSerie(portSerie);
					return portSerie;				
				}else{
					System.out.println("No se ha encontrado XBee!");				
				}
			}
		}
		return null;
	}
	private static SerialPort configurePortSerie( SerialPort portSerie ) throws IOException{
		try{
			portSerie.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			portSerie.notifyOnDataAvailable(true);
			return portSerie;
		}catch(UnsupportedCommOperationException e){
			System.out.println("Error configurando puerto serie");
			return null;
		}
	}
	 public static byte[] readPortSerial(SerialPort portSerie,int time) throws IOException{
			
		 try {
				Thread.sleep(time);
			} catch (InterruptedException e) {
				e.printStackTrace(); 
			}
		InputStream in = portSerie.getInputStream();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];
		boolean frame=false;
		while((nRead = in.read(data, 0, data.length)) != -1 && frame==false) {
			
		  buffer.write(data, 0, nRead);	
		  if(buffer.size()>0){
			  System.out.println(" nRead: "+nRead+" Buffer: "+routerPAN.bytesToHex(buffer.toByteArray())+" Size: "+buffer.size());
			  frame=true;
		  }
		}
		frame=false;

		buffer.flush();

		return buffer.toByteArray();
	}
}

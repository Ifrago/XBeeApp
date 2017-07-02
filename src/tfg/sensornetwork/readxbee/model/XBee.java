package tfg.sensornetwork.readxbee.model;

public class XBee {

	String address;
	String lastNonce;
	String helloTime;
	String keyMAC;
	String challange;
	String iv;

	int auth;
	int count;
	int connectivity;
	
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getLastNonce() {
		return lastNonce;
	}
	public void setLastNonce(String lastNonce) {
		this.lastNonce = lastNonce;
	}
	public int getAuth() {
		return auth;
	}
	public void setAuth(int auth) {
		this.auth = auth;
	}
	public String getHelloTime() {
		return helloTime;
	}
	public void setHelloTime(String helloTime) {
		this.helloTime = helloTime;
	}
	public String getKeyMAC() {
		return keyMAC;
	}
	public void setKeyMAC(String keyMAC) {
		this.keyMAC = keyMAC;
	}	
	public String getIv() {
		return iv;
	}
	public void setIv(String iv) {
		this.iv = iv;
	}
	public int getConnectivity() {
		return connectivity;
	}
	public void setConnectivity(int connectivity) {
		this.connectivity = connectivity;
	}
	public String getChallange() {
		return challange;
	}
	public void setChallange(String challange) {
		this.challange = challange;
	}
	
	
}
package tfg.sensornetwork.readxbee.model;

public class XBee {

	String address;
	String lastNonce;
	int auth;
	
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
}
package tfg.sensornetwork.connector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;

import tfg.sensornetwork.readxbee.model.BlackList;
import tfg.sensornetwork.readxbee.model.XBee;;

public class ConnectorBBDD {
	private static Connection conn = null;
	
	public void connectBBDD(){
		
		try{
			String url ="jdbc:mysql://localhost:3306/xbeerouter";
			String user="root";
			String password="ivan";
			
			conn=DriverManager.getConnection(url,user,password);
			if(conn!=null)System.out.println("Connected to the database");
		}catch(SQLException ex){
			System.out.println("An error ocurred. Maybe user or password is invalid");
		}
	}
	public  BlackList getBlackList(String address) throws SQLException{
		System.out.println("GetBlackList("+address+")------------------------");
		PreparedStatement stmt = conn.prepareStatement("SELECT * FROM blacklist WHERE xbee = ?");
		stmt.setString(1, address);
		
		ResultSet rs = stmt.executeQuery();
		BlackList blist = new BlackList();
		System.out.println("BlackList:");
		while(rs.next()){
			blist.setAddress(rs.getString("xbee"));
			blist.setTiempo(rs.getString("tiempo"));
			System.out.println("MAC: "+blist.getAddress()+ " Tiempo: "+blist.getTiempo());
			
		}
		
		rs.close();
		stmt.close();
		System.out.println("------------------------------------------");
		return blist;
	}
	public boolean getXBeeList()throws SQLException{
		System.out.println("getXBeeList();-------------------------------");
		PreparedStatement stmt = conn.prepareStatement("SELECT * FROM xbees");
		
		ResultSet rs = stmt.executeQuery();
		boolean result = rs.next();
		rs.close();
		stmt.close();
		System.out.println("------------------------------------------");
		if(result==false) return false;
		return true;
	}
	public XBee getXBee(String address)throws SQLException{
		System.out.println("GetXBee("+address+");-------------------------------");
		PreparedStatement stmt = conn.prepareStatement("SELECT * FROM xbees WHERE address = ?");
		stmt.setString(1, address);
		
		ResultSet rs = stmt.executeQuery();
		boolean result = rs.next();
		if(result==false) return null;
		XBee xbee = new XBee();
		while(result){
			
			xbee.setAddress(rs.getString("address"));
			xbee.setLastNonce(rs.getString("lastnonce"));
			xbee.setAuth(rs.getInt("auth"));
			xbee.setCount(rs.getInt("count"));
			xbee.setConnectivity(rs.getInt("connectivity"));
			xbee.setKeyMAC(rs.getString("keyMAC"));
			xbee.setIv(rs.getString("iv"));
			xbee.setHelloTime(rs.getString("hellotime"));
			System.out.println("MAC: "+xbee.getAddress()+ " LastNonce: "+xbee.getLastNonce()+ " Auth: "+xbee.getAuth()+ " Count: "+xbee.getCount() + " HelloTime: "+xbee.getHelloTime()+ " keyMAC: "+xbee.getKeyMAC() + " IV: "+xbee.getIv()+ " Connectivity: "+xbee.getConnectivity());
			result=rs.next();
		}
		
		rs.close();
		stmt.close();
		System.out.println("------------------------------------------");
		return xbee;
	}
	public int getCount(String address)throws SQLException{
		System.out.println("getCount("+address+");-------------------------------");
		PreparedStatement stmt = conn.prepareStatement("SELECT count FROM xbees WHERE address = ?");
		stmt.setString(1, address);
		
		ResultSet rs = stmt.executeQuery();
		boolean result = rs.next();
		if(result==false) return 99999;
		int countMsg = 0;
		while(result){
			countMsg=rs.getInt("count");
			result=rs.next();
		}
		
		rs.close();
		stmt.close();
		System.out.println("------------------------------------------");
		return countMsg;
	}
	public String getChallange(String address)throws SQLException{
		System.out.println("getCount("+address+");-------------------------------");
		PreparedStatement stmt = conn.prepareStatement("SELECT challange FROM xbees WHERE address = ?");
		stmt.setString(1, address);
		
		ResultSet rs = stmt.executeQuery();
		String challange = "";
		if(rs.next())
			challange =rs.getString("challange");	
		rs.close();
		stmt.close();
		
		System.out.println("------------------------------------------");
		if(challange==null) return "";
		return challange;
	}
	public void addBlackList(String address) throws SQLException{
		String time = Long.toString(System.currentTimeMillis());
		System.out.println("AddBlackList("+address+","+time+")---------------------------------------");
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO blacklist (xbee, tiempo) VALUES (?,?)");
		stmt.setString(1, address);
		stmt.setString(2, time);
		
		int count = stmt.executeUpdate();
		
		System.out.println("Insert count BlackList: "+count);
		System.out.println("------------------------------------------");
		stmt.close();
		
	}
	public  void addLog(String address, String operation) throws SQLException{

		String tiempo = timeNow();
		System.out.println("AddLog("+address+","+operation+","+tiempo+")-------------------------------");		
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO log (fecha, xbee, operation) VALUES (?,?,?)");
		stmt.setString(1, tiempo);
		stmt.setString(2, address);
		stmt.setString(3, operation);
		
		int count = stmt.executeUpdate();
		
		System.out.println("Insert count Log line: "+count);
		System.out.println("------------------------------------------");
		stmt.close();		
	}
	public  void addXBee(String address, String nonce, int auth) throws SQLException{
			System.out.println("AddXBee("+address+","+nonce+","+auth+")-----------------------------------");
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO xbees (address, lastnonce, auth, keyMAC, iv, connectivity, challange) VALUES (?,?,?,?,?,?,?)");
			stmt.setString(1, address);
			stmt.setString(2, nonce);
			stmt.setInt(3, 1);
			stmt.setString(4, null);
			stmt.setString(5, null);
			stmt.setInt(6, 1);
			stmt.setString(7, null);
			
			int count = stmt.executeUpdate();
			
			System.out.println("Insert count Xbee: "+count);
			System.out.println("------------------------------------------");
			stmt.close();
	}
	public void AuthXBee (String address, int auth)throws SQLException{
		System.out.println("changeAuthXBee("+address+",0)---------------------------------------");		
		PreparedStatement stmt = conn.prepareStatement("UPDATE xbees SET auth = ? WHERE address = ?");
		if(auth==0)stmt.setInt(1, 0);
		else if(auth==1)stmt.setInt(1, 1);
		stmt.setString(2, address);
		
		int count = stmt.executeUpdate();
		
		System.out.println("Insert count Auth: "+count);
		System.out.println("------------------------------------------");
		stmt.close();
		
		
	}
	
	public ArrayList<XBee> keepAlive()throws SQLException{
		System.out.println("keepAlive()BBDD---------------------------------");
		PreparedStatement stmt = conn.prepareStatement("SELECT * FROM xbees WHERE auth=1");
		ResultSet rs = stmt.executeQuery();
		ArrayList<XBee> xbees = new ArrayList<XBee>();
		while(rs.next()){
			String helloTime=rs.getString("hellotime");
			XBee xbee = new XBee();
			if(helloTime == null) helloTime = "0";
			System.out.println("TIME NOW: "+System.currentTimeMillis() + " - " + helloTime);
			if(System.currentTimeMillis()-Long.parseLong(helloTime)>600000){
				xbee.setAddress(rs.getString("address"));
				xbee.setLastNonce(rs.getString("lastnonce"));
				xbee.setAuth(rs.getInt("auth"));
				xbee.setCount(rs.getInt("count"));
				xbee.setConnectivity(rs.getInt("connectivity"));
				xbee.setHelloTime(rs.getString("hellotime"));
				System.out.println("XBEE not live --> MAC: "+xbee.getAddress()+ " LastNonce: "+xbee.getLastNonce()+ " Auth: "+xbee.getAuth()+ " Count: "+xbee.getCount() + " HelloTime: "+xbee.getHelloTime());
				xbees.add(xbee);
			}			
		}		
		rs.close();
		stmt.close();
		System.out.println("------------------------------------------");
		return xbees;
		
		
	}
	public  void updateLastNonce(String address, String lastNonce) throws SQLException{
		System.out.println("updateLastNonce("+address+","+lastNonce+")-----------------------------------------");
		PreparedStatement stmt = conn.prepareStatement("UPDATE xbees SET lastnonce = ? WHERE address = ?");
		stmt.setString(1, lastNonce);
		stmt.setString(2, address);
		
		int count = stmt.executeUpdate();
		
		System.out.println("Insert count Nonce: "+count);
		System.out.println("------------------------------------------");
		stmt.close();
	}
	public  void updateKeyMAC(String address, String keyMAC) throws SQLException{
		System.out.println("updateKeyMAC("+address+","+keyMAC+")-----------------------------------------");
		PreparedStatement stmt = conn.prepareStatement("UPDATE xbees SET keyMAC = ? WHERE address = ?");
		stmt.setString(1, keyMAC);
		stmt.setString(2, address);
		
		int count = stmt.executeUpdate();
		
		System.out.println("Insert count keyMAC: "+count);
		System.out.println("------------------------------------------");
		stmt.close();
	}
	public  void updateIV(String address, String iv) throws SQLException{
		System.out.println("updateIV("+address+","+iv+")-----------------------------------------");
		PreparedStatement stmt = conn.prepareStatement("UPDATE xbees SET iv = ? WHERE address = ?");
		stmt.setString(1, iv);
		stmt.setString(2, address);
		
		int count = stmt.executeUpdate();
		
		System.out.println("Insert count IV: "+count);
		System.out.println("------------------------------------------");
		stmt.close();
	}
	public  void updateChallange(String address, String challange) throws SQLException{
		System.out.println("updateChallange("+address+","+challange+")-----------------------------------------");
		PreparedStatement stmt = conn.prepareStatement("UPDATE xbees SET challange = ? WHERE address = ?");
		stmt.setString(1, challange);
		stmt.setString(2, address);
		
		int count = stmt.executeUpdate();
		
		System.out.println("Insert count Challange: "+count);
		System.out.println("------------------------------------------");
		stmt.close();
	}
	public void updateHelloTime(String address, String helloTime) throws SQLException{
		System.out.println("updateHelloTime("+address+","+helloTime+")-----------------------------------------");
		PreparedStatement stmt = conn.prepareStatement("UPDATE xbees SET hellotime = ? WHERE address = ?");
		stmt.setString(1, helloTime);
		stmt.setString(2, address);
		
		int count = stmt.executeUpdate();
		
		System.out.println("Insert count HelloTime: "+count);
		System.out.println("------------------------------------------");
		stmt.close();
	}
	public  void updateConnectivity(String address, int connectivity) throws SQLException{
		System.out.println("updateIV("+address+","+connectivity+")-----------------------------------------");
		PreparedStatement stmt = conn.prepareStatement("UPDATE xbees SET connectivity = ? WHERE address = ?");
		stmt.setInt(1, connectivity);
		stmt.setString(2, address);
		
		int count = stmt.executeUpdate();
		
		System.out.println("Insert count Connectivity: "+count);
		System.out.println("------------------------------------------");
		stmt.close();
	}
	public void deleteBlackList(String address)throws SQLException{
		System.out.println("deleteBlackList("+address+")-----------------------------------------");
		PreparedStatement stmt = conn.prepareStatement("DELETE FROM blacklist WHERE xbee = ?");
		stmt.setString(1, address);	
		
		int count = stmt.executeUpdate();
		
		System.out.println("Deleted count XBee in the BlackList: "+count);
		System.out.println("------------------------------------------");
		stmt.close();
	}
	public void incrementMsgChangeAESKey(String address)throws SQLException{
		System.out.println("incrementMsgChangeAESKey("+address+")---------------------------------");
		PreparedStatement stmt = conn.prepareStatement("UPDATE xbees SET count = count + 1 WHERE address= ?");
		stmt.setString(1, address);	
		
		int count = stmt.executeUpdate();
		
		System.out.println("Deleted count XBee in the BlackList: "+count);
		System.out.println("------------------------------------------");
		stmt.close();		
	}
	public  String timeNow(){
		System.out.println("TimeNow()------------------------------");
		Calendar calendar = Calendar.getInstance();
		
		int year, month, day, hours, minutes, seconds, miliseconds;
		year = calendar.get(Calendar.YEAR);
		month = calendar.get(Calendar.MONTH)+1; 	//Se suma uno porque ne la libreria Calendar, los meses enpiezan con el número 0( = Enero ).
		day = calendar.get(Calendar.DAY_OF_MONTH);
		hours = calendar.get(Calendar.HOUR_OF_DAY);
		minutes =calendar.get(Calendar.MINUTE);
		seconds =calendar.get(Calendar.SECOND);
		miliseconds = calendar.get(Calendar.MILLISECOND);
		
		String timeNow = Integer.toString(day)+"/"+Integer.toString(month)+"/"+Integer.toString(year)+" "+Integer.toString(hours)+":"+Integer.toString(minutes)+":"+Integer.toString(seconds)+":"+Integer.toString(miliseconds);
		System.out.println(timeNow);
		System.out.println("------------------------------------------");
		return timeNow;
		
	}
}

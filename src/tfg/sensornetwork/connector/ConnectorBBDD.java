package tfg.sensornetwork.connector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
			System.out.println("MAC: "+xbee.getAddress()+ " LastNonce: "+xbee.getLastNonce()+ " Auth: "+xbee.getAuth());
			result=rs.next();
		}
		
		rs.close();
		stmt.close();
		System.out.println("------------------------------------------");
		return xbee;
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
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO xbees (address, lastnonce, auth) VALUES (?,?,?)");
			stmt.setString(1, address);
			stmt.setString(2, nonce);
			stmt.setInt(3, 1);
			
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
	public void deleteBlackList(String address)throws SQLException{
		System.out.println("deleteBlackList("+address+")-----------------------------------------");
		PreparedStatement stmt = conn.prepareStatement("DELETE FROM blacklist WHERE xbee = ?");
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

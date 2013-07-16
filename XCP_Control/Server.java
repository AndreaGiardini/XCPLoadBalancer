package main;

import java.net.URL;

import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Session;

public class Server {

	private String hostname;
	private String username;
	private String password;
	private boolean connected=false;
	private Connection connection;
	private Session XenAPI;
	
	public Server (String hostname, String username, String password){
		this.hostname=hostname;
		this.username=username;
		this.password=password;
	}
	
	public void connect() throws Exception
    {
        connection = new Connection(new URL("http://" + this.hostname));
        //System.out.println("logging in to " + this.hostname + " as " + this.username + "...");
        XenAPI = Session.loginWithPassword(connection, this.username, this.password, APIVersion.latest().toString());
    }
	
	public Session getSession(){
		return XenAPI;
	}

	public boolean isConnected() {
		return connected;
	}
	
	public String getHostname() {
		return hostname;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}

	public Connection getConnection(){
		return connection;
	}
}

package work;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.json.JSONObject;
import org.json.JSONTokener;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class WorkServer extends HttpServlet
{
	private static final Map<Character, Integer> VAL = new HashMap<Character, Integer>();	
	static
	{
		VAL.put('0', 0);
		VAL.put('1', 1);
		VAL.put('2', 2);
		VAL.put('3', 3);
		VAL.put('4', 4);
		VAL.put('5', 5);
		VAL.put('6', 6);
		VAL.put('7', 7);
		VAL.put('8', 8);
		VAL.put('9', 9);
		VAL.put('A', 10);
		VAL.put('B', 11);
		VAL.put('C', 12);
		VAL.put('D', 13);
		VAL.put('E', 14);
		VAL.put('F', 15);
	}
	
	
	private static Work<String> workManager;
	
	public static void setWorkManager(Work<String> work)
	{
		workManager = work;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String username = req.getAttribute("From").toString();
		System.out.println(username);
		workManager.requestJob(username);
		String header = bytesToHex(workManager.getHeader(username));
		String target = bytesToHex(workManager.getTarget());
		String json = "{ \"HEADER\":\""+header+"\",\n\"TARGET\":\""+target+"\"\n}";
		resp.setContentType("application/json");
		resp.setStatus(HttpServletResponse.SC_OK);
		
		OutputStream os = resp.getOutputStream();
		os.write(json.getBytes());
		os.flush();
	}
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
	{
		String username = req.getAttribute("From").toString();
		JSONObject json = new JSONObject(new JSONTokener(req.getReader()));
		System.out.println("POST FROM "+username);
		boolean didWork = workManager.checkSolution(username, parseHex(json.getString("NONCE")));
		if(didWork)
		{
			JOptionPane.showMessageDialog(null, "THEY DID THE WORK!!!!!!!!!!");
		}
	}
	
	private static final byte[] parseHex(String hex)
	{
		hex = hex.toUpperCase();
		if(!hex.substring(0,2).contentEquals("0X") || hex.length()%2 != 0)
			throw new RuntimeException("Improperly formatted hex string.");
		byte[] ret = new byte[(hex.length()/2)-1];
		for(int i = 0; i < ret.length; i++)
			ret[i] = (byte)(VAL.get(hex.charAt(i*2+2))<<4 | VAL.get(hex.charAt(i*2+3)));		
		return ret;
	}
	
	
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.json.JSONException;
import org.json.JSONObject;

import web.DAppServer;
import work.Work;
import work.WorkServer;
import packet.PacketInterface;
import clientServerEnums.Packet;
import lombok.Getter;
import clientServerEnums.DataSlot;

/**
 * Entry point for server-side implementation of work hard play hard protocol.
 * @author Antonio Juan Ponce
 */
public final class W2HPServer 
{
	private W2HPServer()
		{throw new UnsupportedOperationException(W2HPServer.class.getName()+" is not instantiatable.");}
	
	@Getter private static boolean argumentsLoaded = false;
	
	//Objects used for internal state management
	public static final Map<String, Consumer<String>> ARGUMENTS;
	
	//Configurable application wide variables
	@Getter private static PacketInterface<Packet,DataSlot> packetInterface = null;
	@Getter private static int dAppPort = -1;
	@Getter private static String dAppURI = null;
	@Getter private static String dAppPath = null; 
	@Getter private static int servicePort = -1;
	
	@Getter private static String serviceURI = null;
	
	//Configurable internal variables
	private static boolean useConfig = true;
	private static String configPath = "config/config.json";
	
	private static byte[] address;
	private static byte[] checkNum;
	private static byte[] seed;
	private static long targetValue;
	private static int entries;
	private static boolean genKeys;
	private static String workFile;
	private static Work<String> workManager;
	
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
		//maps + consumers = easy command parser.
		ARGUMENTS = new HashMap<String, Consumer<String>>();
		ARGUMENTS.put("-WebPort",     (s) -> dAppPort = Integer.parseInt(s));
		ARGUMENTS.put("-WebURI",      (s) -> dAppURI = s);
		ARGUMENTS.put("-WebPath",     (s) -> dAppPath = s);
		ARGUMENTS.put("-ServicePort", (s) -> servicePort = Integer.parseInt(s));
		ARGUMENTS.put("-UseConfig",   (s) -> useConfig = Boolean.parseBoolean(s));
		ARGUMENTS.put("-Config",      (s) -> configPath = s);
		ARGUMENTS.put("-WorkFile", (s) -> workFile = s);
		ARGUMENTS.put("-Entries", (s) -> entries = Integer.parseInt(s));
		ARGUMENTS.put("-Address", (s) -> address = parseHex(s));
		ARGUMENTS.put("-CheckNum", (s) -> checkNum = parseHex(s));
		ARGUMENTS.put("-Seed", (s) -> seed = parseHex(s));
		ARGUMENTS.put("-Target", (s) -> targetValue = Long.parseLong(s));
		ARGUMENTS.put("-GenKeys", (s) -> genKeys = Boolean.parseBoolean(s));
		ARGUMENTS.put("-ServiceURI", (s) -> serviceURI = s);
	}
	
	//TOP SECRET   : 0x5EC2B251EECC7681F1F3F159FF6D1EE414B066245F7372DE0999EAC8A01B5397
	//TOP SOLUTION : 0xE3E945013CDCDF35470ECDD2707342CBD82610EEC3D6CF5D9FE98922000D2B71
	public static void main(String[] args)
	{
		loadArguments(args);
		if(useConfig)
			try //configurations loaded over arguments, reload arguments again.
				{loadConfig(configPath);loadArguments(args);}
			catch(IOException e)
				{throw new ExceptionInInitializerError(e);}
		argumentsLoaded = true;
		
		try {
			if(genKeys)
				workManager = new Work<String>(entries, address, checkNum, seed, targetValue, workFile);
			else
				workManager = new Work<String>(workFile);
			workManager.setSeed(seed);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("Something went wrong here!");
			System.exit(-1);
		}
		
		WorkServer.setWorkManager(workManager);
		System.out.println(bytesToHex(workManager.getCurrentSolution()));
		printBytes(workManager.getPublicHeader());
		
		//good resource for embedded HTTP servers.
		//https://docs.huihoo.com/jetty/the-definitive-reference/embedding-jetty.html
		DAppServer dApp = new DAppServer();
		try
			{dApp.start();}
		catch (Exception e)
			{throw new ExceptionInInitializerError(e);}
	}
	
	private static final void loadArguments(String[] args)
	{
		if(args.length%2 != 0)
			throw new IllegalArgumentException("All arguments must have values.");
		for(int i = 0; i < args.length; i+=2)
			if(ARGUMENTS.get(args[i]) == null)
				throw new IllegalArgumentException("Unrecognized argument "+args[i]+" for main.");
			else
				ARGUMENTS.get(args[i]).accept(args[i+1]);	
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
	
	private static void printBytes(byte[] bytes)
	{
		String print = "{";
		for(int i = 0; i < bytes.length-1; i++)
			print += bytes[i] + ", ";
		print += bytes[bytes.length-1] + "};";
		System.out.println(print);
	}
	
	private static final void loadConfig(String configPath) throws IOException, JSONException
	{
		String content = new String(Files.readAllBytes(Paths.get(configPath)));
		JSONObject config = new JSONObject(content).getJSONObject("SERVER-CONFIG");
		config.keys().forEachRemaining((k) -> {if(ARGUMENTS.get(k) != null) ARGUMENTS.get(k).accept(config.getString(k));});
	}
}
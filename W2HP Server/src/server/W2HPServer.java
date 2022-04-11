package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.json.JSONException;
import org.json.JSONObject;

import packet.PacketInterfaceFactory;
import web.DAppServer;
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
	
	//Configurable internal variables
	private static boolean useConfig = true;
	private static String configPath = "config/config.json";
	private static String packetConfigPath = null;
	private static String packetPackage = null;
	
	static
	{
		//maps + consumers = easy command parser.
		ARGUMENTS = new HashMap<String, Consumer<String>>();
		ARGUMENTS.put("-WebPort",     (s) -> dAppPort = Integer.parseInt(s));
		ARGUMENTS.put("-WebURI",      (s) -> dAppURI = s);
		ARGUMENTS.put("-WebPath",     (s) -> dAppPath = s);
		ARGUMENTS.put("-ServicePort", (s) -> servicePort = Integer.parseInt(s));
		ARGUMENTS.put("-UseConfig",   (s) -> useConfig = Boolean.parseBoolean(s));
		ARGUMENTS.put("-Config",      (s) -> configPath = s);
		ARGUMENTS.put("-Dictionary",  (s) -> packetConfigPath = s);
		ARGUMENTS.put("-Package",     (s) -> packetPackage = s);
	}
	
	public static void main(String[] args)
	{
		loadArguments(args);
		if(useConfig)
			try //configurations loaded over arguments, reload arguments again.
				{loadConfig(configPath);loadArguments(args);}
			catch(IOException e)
				{throw new ExceptionInInitializerError(e);}
		argumentsLoaded = true;
		
		try
			{packetInterface = new PacketInterfaceFactory(packetConfigPath).getInterface(packetPackage, Packet.class);}
		catch (IOException | ParseException e)
			{throw new ExceptionInInitializerError(e);}
		
		//good resource for embedded HTTP servers.
		//https://docs.huihoo.com/jetty/the-definitive-reference/embedding-jetty.html
		DAppServer dApp = new DAppServer();
		try
			{dApp.start();}
		catch (Exception e)
			{throw new ExceptionInInitializerError(e);}
		
		System.out.println("I built this using Apache Maven!");
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
	private static final void loadConfig(String configPath) throws IOException, JSONException
	{
		String content = new String(Files.readAllBytes(Paths.get(configPath)));
		JSONObject config = new JSONObject(content).getJSONObject("SERVER-CONFIG");
		config.keys().forEachRemaining((k) -> ARGUMENTS.get(k).accept(config.getString(k)));
	}
}
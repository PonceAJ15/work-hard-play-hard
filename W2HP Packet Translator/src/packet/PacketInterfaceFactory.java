package packet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import util.FieldLabel;
import util.FieldLabelList;
import util.Type;

public final class PacketInterfaceFactory
{
	//Work around for lack of friend classes in java
	public static final class PacketInterfaceFactoryKey
	{
		public boolean secret = false;
		private PacketInterfaceFactoryKey() {secret = true;}
	};
	static final PacketInterfaceFactoryKey KEY = new PacketInterfaceFactoryKey();
	
	//package > packet > data slots
	private final HashMap<String, HashMap<String, List<PacketDataSlot>>> PACKET_DEFINITIONS = 
			  new HashMap<String, HashMap<String, List<PacketDataSlot>>>();
	
	public PacketInterfaceFactory(String packetDefinitionPath) throws IOException, ParseException
	{
		String content = new String(Files.readAllBytes(Paths.get(packetDefinitionPath)));
		JSONObject definition = new JSONObject(content);
		checkTypes(definition.getJSONArray("data-types"));
		translatePacketDefinitions(definition.getJSONArray("packets"));
	}
	
	/**
	 * 
	 * @param <P extends Enum<P> & FieldLabelList<T>>
	 * @param <T extends Enum<T> & FieldLabel>
	 * @param _package package of packets
	 * @param bindings instance of enumerator for packet field bindings
	 * @return a PacketInterface object to read and write packets
	 */
	public <P extends Enum<P> & FieldLabelList<T>, T extends Enum<T> & FieldLabel> PacketInterface<P, T> getInterface(String _package, Class<P> bindings)
	{
		return new PacketInterface<P, T>(PACKET_DEFINITIONS.get(_package), bindings, KEY);
	}

	/*
	 * Protocol definition contains a list of types and their respective sizes,
	 * this function checks that the size of these types are consistent with
	 * assumptions made by the PackectInterfaceFactory class.
	 */
	private void checkTypes(JSONArray typeList) throws ParseException
	{
		for(Object typeObject: typeList)
		{
			JSONObject type = (JSONObject)typeObject;
			if(type.get("size") instanceof JSONObject)
				if(Type.TYPE_SIZE_ASSUMPTIONS.get(type.getString("type")) != null)
					throw new ParseException("Type "+type.getString("type")+" is inconsistent with type assumptions.",0);
				else if(!type.getJSONObject("size").getString("type").contentEquals("byte")) //"byte" != "byte"?
					throw new ParseException("Type "+type.getString("type")+" is inconsistent with type assumptions.",0);
				else;
			else if(Type.TYPE_SIZE_ASSUMPTIONS.get(type.getString("type")) == null)
				throw new ParseException("Type "+type.getString("type")+" is inconsistent with type assumptions.",0);
			else if(Type.TYPE_SIZE_ASSUMPTIONS.get(type.getString("type")) != type.getInt("size"))
				throw new ParseException("Type "+type.getString("type")+" is inconsistent with type assumptions.",0);
		}
	}
	/*
	 * This function converts JSONField object to more relevant data structure
	 */
	private void translatePacketDefinitions(JSONArray packages) throws ParseException
	{
		//translate configuration definition to easier data structure in PACKET_DEFINITIONS
		for(Object packetPackageObject: packages)
		{
			//package
			JSONObject packetPackage = (JSONObject)packetPackageObject;
			HashMap<String, List<PacketDataSlot>> _package = new HashMap<String, List<PacketDataSlot>>(); 
			PACKET_DEFINITIONS.put(packetPackage.getString("package"), _package);
			for(Object packetObject: packetPackage.getJSONArray("packets"))
			{
				//packet
				JSONObject packet = (JSONObject)packetObject;
				ArrayList<PacketDataSlot> slots = new ArrayList<PacketDataSlot>();
				_package.put(packet.getString("packet"), slots);
				for(Object dataSlotObject: packet.getJSONArray("definition"))
				{	//data slot
					JSONObject dataSlot = (JSONObject)dataSlotObject;
					slots.add(new PacketDataSlot
					(
						dataSlot.getString("name"),
						Type.valueOf(dataSlot.getString("type").toUpperCase()),
						dataSlot.get("value") != JSONObject.NULL?dataSlot.get("value"):null //TODO: check if dataSlot.get("value") causes error
					));
				}
				if((!slots.get(0).getName().contentEquals("HEADER")) || slots.get(0).getType() != Type.BYTE)
					throw new ParseException("Packets must start with header byte to identify packet to reciever.",0);
			}
		}
	}
}
package packet;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import parser.JSONObject;
import parser.JSONObject.JSONField;
import parser.JSONObject.JSONFieldType;
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
	
	public PacketInterfaceFactory(String packetDefinitionPath) throws FileNotFoundException, ParseException
	{
		JSONObject definition = new JSONObject(packetDefinitionPath);
		checkTypes(definition.getField("data-types"));
		translatePacketDefinitions(definition.getField("packets"));
	}
	
	/**
	 * 
	 * @param <P extends Enum<P> & FieldLabelList<T>>
	 * @param <T extends Enum<T> & FieldLabel>
	 * @param _package package of packets
	 * @param bindings instance of enumerator for packet field bindings
	 * @return a PacketInterface object to read and write packets
	 */
	public <P extends Enum<P> & FieldLabelList<T>, T extends Enum<T> & FieldLabel> PacketInterface<P, T> getInterface(String _package, P bindings)
	{
		return new PacketInterface<P, T>(PACKET_DEFINITIONS.get(_package), bindings, KEY);
	}

	/*
	 * Protocol definition contains a list of types and their respective sizes,
	 * this function checks that the size of these types are consistent with
	 * assumptions made by the PackectInterfaceFactory class.
	 */
	private void checkTypes(JSONField typeList) throws ParseException
	{
		if(typeList.TYPE != JSONFieldType.LIST)
			throw new ParseException("Type definitions expected in LIST type, but JSONField was "+typeList.TYPE+" instead.",0);
		for(JSONField type: typeList)
			if(type.getField("size").TYPE == JSONFieldType.OBJECT)
				if(Type.TYPE_SIZE_ASSUMPTIONS.get(type.getField("type").string()) != null)
					throw new ParseException("Type "+type.getField("type").string()+" is inconsistent with type assumptions.",0);
				else if(!type.getField("size").getField("type").string().contentEquals("byte")) //"byte" != "byte"?
					throw new ParseException("Type "+type.getField("type").string()+" is inconsistent with type assumptions.",0);
				else;
			else if(Type.TYPE_SIZE_ASSUMPTIONS.get(type.getField("type").string()) == null)
				throw new ParseException("Type "+type.getField("type").string()+" is inconsistent with type assumptions.",0);
			else if(Type.TYPE_SIZE_ASSUMPTIONS.get(type.getField("type").string()) != type.getField("size").number().intValue())
				throw new ParseException("Type "+type.getField("type").string()+" is inconsistent with type assumptions.",0);
	}
	/*
	 * This function converts JSONField object to more relevant data structure
	 */
	private void translatePacketDefinitions(JSONField packages) throws ParseException
	{
		//translate configuration definition to easier data structure in PACKET_DEFINITIONS
		for(JSONField packetPackage: packages)
		{
			//package
			HashMap<String, List<PacketDataSlot>> _package = new HashMap<String, List<PacketDataSlot>>(); 
			PACKET_DEFINITIONS.put(packetPackage.getField("package").string(), _package);
			for(JSONField packet: packetPackage.getField("packets"))
			{
				//packet
				ArrayList<PacketDataSlot> slots = new ArrayList<PacketDataSlot>();
				_package.put(packet.getField("packet").string(), slots);
				for(JSONField dataSlot: packet.getField("definition"))
					//data slot
					slots.add(new PacketDataSlot
					(
						dataSlot.getField("name").string(),
						Type.valueOf(dataSlot.getField("type").string().toUpperCase()),
						dataSlot.getField("value") != null?dataSlot.getField("value").getObject():null
					));
				if((!slots.get(0).getName().contentEquals("HEADER")) || slots.get(0).getType() != Type.BYTE)
					throw new ParseException("Packets must start with header byte to identify packet to reciever.",0);
			}
		}
	}
}
package packet;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;

import packet.PacketInterfaceFactory.PacketInterfaceFactoryKey;
import util.FieldLabel;
import util.FieldLabelList;
import util.Type;
import util.BufferReader;
import util.BufferWriter;

public final class PacketInterface<Packet extends Enum<Packet> & FieldLabelList<DataSlot>, DataSlot extends Enum<DataSlot> & FieldLabel>
{	
	public boolean checkReadValues  = true;
	public boolean checkReadTypes   = true;
	public boolean checkWriteValues = true;
	public boolean autoWriteValues = true;
	public boolean checkWriteTypes  = true;
	
	public final class ReadablePacket
	{
		private final byte[] buffer = new byte[1024];
		private final Object[] values;
		public final Packet packet;
		private final EnumMap<DataSlot, Byte> index;
		private ReadablePacket(InputStream is) throws IOException
		{
			is.read(buffer);
		    BufferReader br = new BufferReader(buffer);
			byte header = br.readByte();
			this.packet = readerHeaderBindings.get(header);
			this.index = readerIndexBindings.get(packet);
			if(this.packet == null)
				throw new IOException("Unrecognized packet header byte, no definition for "+header+".");
			this.values = new Object[interpreterValueBindings[header].length];
			values[0] = header;
			for(int v = 1; v < interpreterValueBindings[header].length; v++)
			{
				values[v] = br.readValue(interpreterTypeBindings[header][v]);
				if(checkReadValues &&
				   interpreterValueBindings[header][v] != null &&
				   !interpreterValueBindings[header][v].equals(values[v]))
					throw new IOException("Unexpected value "+interpreterTypeBindings[header][v].asString(values[v])+" in slot "+v+".");
			}
		}
		
		public byte readByte(DataSlot slot)
			{return readValue(slot);}
		public char readChar(DataSlot slot)
			{return readValue(slot);}
		public int readInt(DataSlot slot)
			{return readValue(slot);}
		public long readLong(DataSlot slot)
			{return readValue(slot);}
		public byte[] readHash(DataSlot slot)
			{return readValue(slot);}
		public byte[] readByteArray(DataSlot slot)
			{return readValue(slot);}
		
		//TIL: generic methods don't need types to be declared when invoking, they just infer them. 
		@SuppressWarnings("unchecked") //ClassCastException intended
		public <T> T readValue(DataSlot slot)
			{return (T) values[index.get(slot)];}
 	}
	public final class PacketWriter
	{
		private final EnumMap<DataSlot, Byte> index;
		private final Type[] types;
		private final Object[] values;
		private final Object[] defaults;
		private PacketWriter(Packet packet)
		{
			this.index = readerIndexBindings.get(packet);
			this.defaults = interpreterValueBindings[writerHeaderBindings.get(packet)];
			this.values = new Object[defaults.length];
			this.types = interpreterTypeBindings[writerHeaderBindings.get(packet)];
			if(autoWriteValues)
				System.arraycopy(defaults, 0, values, 0, defaults.length);
		}
		
		public void writeToBuffer(byte[] buffer)
		{
			BufferWriter bw = new BufferWriter(buffer);
			if(checkWriteValues)
				for(int i = 0; i < values.length; i++)
					if(defaults[i] != null && values[i] != defaults[i])
						throw new IllegalStateException("Expected default value in data slot "+i+".");
					else if(values[i] == null)
						throw new IllegalStateException("Expectet value in data slot "+i+" but found null instead.");
					else
						bw.writeType(types[i], values[i]);
			else
				for(int i = 0; i < values.length; i++)
					bw.writeType(types[i], values[i]);
		}
		
		public void writeByte(DataSlot slot, byte b)
		{
			if(checkWriteValues && types[index.get(slot)] != Type.BYTE)
				throw new IllegalArgumentException("Expect type "+types[index.get(slot)]+" in field "+slot+" got byte instead.");
			values[index.get(slot)] = b;
		}
		public void writeChar(DataSlot slot, char c)
		{
			if(checkWriteValues && types[index.get(slot)] != Type.CHAR)
				throw new IllegalArgumentException("Expect type "+types[index.get(slot)]+" in field "+slot+" got char instead.");
			values[index.get(slot)] = c;
		}
		public void writeInt(DataSlot slot, int i)
		{
			if(checkWriteValues && types[index.get(slot)] != Type.INT)
				throw new IllegalArgumentException("Expect type "+types[index.get(slot)]+" in field "+slot+" got int instead.");
			values[index.get(slot)] = i;
		}
		public void writeLong(DataSlot slot, long l)
		{
			if(checkWriteValues && types[index.get(slot)] != Type.LONG)
				throw new IllegalArgumentException("Expect type "+types[index.get(slot)]+" in field "+slot+" got long instead.");
			values[index.get(slot)] = l;
		}
		public void writeHash(DataSlot slot, byte[] h)
		{
			if(checkWriteValues && types[index.get(slot)] != Type.HASH)
				throw new IllegalArgumentException("Expect type "+types[index.get(slot)]+" in field "+slot+" got hash instead.");
			values[index.get(slot)] = h;
		}
		public void writeByteArray(DataSlot slot, byte[] b)
		{
			if(checkWriteValues && types[index.get(slot)] != Type.BYTE_ARRAY)
				throw new IllegalArgumentException("Expect type "+types[index.get(slot)]+" in field "+slot+" got byte[] instead.");
			values[index.get(slot)] = b;
		}
		public void writeString(DataSlot slot, String s)
		{
			if(checkWriteValues && types[index.get(slot)] != Type.STRING)
				throw new IllegalArgumentException("Expect type "+types[index.get(slot)]+" in field "+slot+" got string instead.");
			values[index.get(slot)] = s;
		}
	}
	
	//Bindings to interpret incoming packets.
	private final Type[][] interpreterTypeBindings = new Type[256][];
	private final Object[][] interpreterValueBindings = new Object[256][];
	
	//Bindings to read/write values from packet
	private final EnumMap<Packet, EnumMap<DataSlot, Byte>> readerIndexBindings;
	private final EnumMap<Packet, Byte> writerHeaderBindings;
	private final Map<Byte, Packet> readerHeaderBindings;

	@SuppressWarnings("unchecked") //No ClassCastException risk, type erasure makes it impossible to check cast
	public PacketInterface(Map<String, List<PacketDataSlot>> definitions, Class<Packet> bindings, PacketInterfaceFactoryKey key)
	{
		if(key == null || key.secret != true)
			throw new SecurityException("PacketInterface can only be instantiated by PacketInterfaceFactory.");
		readerIndexBindings = new EnumMap<Packet, EnumMap<DataSlot, Byte>>(bindings);
		writerHeaderBindings = new EnumMap<Packet, Byte>(bindings);
		readerHeaderBindings = new HashMap<Byte, Packet>();
		
		for(Packet packet: bindings.getEnumConstants())
		{
			//get definition of packet
			List<PacketDataSlot> definition = definitions.get(packet.getLabel());
			byte header = definition.get(0).getByte();
			readerHeaderBindings.put(header, packet);
			writerHeaderBindings.put(packet, header);

			//bind types and values to data slots.
			//map names to indices
			Map<String, Byte> nameToIndex = new HashMap<String, Byte>();
			interpreterTypeBindings[header] = new Type[definition.size()];
			interpreterValueBindings[header] = new Object[definition.size()];
			for(int i = 0;  i < definition.size(); i++)
			{
				interpreterTypeBindings[header][i] = definition.get(i).getType();
				interpreterValueBindings[header][i] = definition.get(i).getValue();
				nameToIndex.put(definition.get(i).getName(), (byte) i);
			}
			
			//bind packet enumerator constant to data slot list
			EnumMap<DataSlot, Byte> dataSlots = new EnumMap<DataSlot, Byte>(packet.getFieldClass());
			readerIndexBindings.put(packet, dataSlots);
			
			//bind field enumerator constant to data slot
			for(DataSlot dataSlot: (DataSlot[])packet.getElements())
				dataSlots.put(dataSlot, nameToIndex.get(dataSlot.getLabel()));
		}
	}
	
	/**
	 * This is a blocking method that reads 1 packet from the InputStream is
	 * @param is input stream to read packet from
	 * @return A ReadablePacket object who's values can be read using enumerator constants
	 * @throws IOException 
	 */
	public ReadablePacket readPacket(InputStream is) throws IOException
	{
		return new ReadablePacket(is);
	}
	
	public PacketWriter getPacketWriter(Packet packet)
	{
		return new PacketWriter(packet);
	}
}
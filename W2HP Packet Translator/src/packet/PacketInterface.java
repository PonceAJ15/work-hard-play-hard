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
		private final byte[] BUFFER = new byte[1024];
		private final Object[] VALUES;
		public final Packet PACKET;
		private final EnumMap<DataSlot, Byte> INDEX;
		private ReadablePacket(InputStream is) throws IOException
		{
			is.read(BUFFER);
		    BufferReader br = new BufferReader(BUFFER);
			byte header = br.readByte();
			this.PACKET = READER_HEADER_BINDINGS.get(header);
			this.INDEX = READER_INDEX_BINDINGS.get(PACKET);
			if(this.PACKET == null)
				throw new IOException("Unrecognized packet header byte, no definition for "+header+".");
			this.VALUES = new Object[INTERPRETER_VALUE_BINDINGS[header].length];
			VALUES[0] = header;
			for(int v = 1; v < INTERPRETER_VALUE_BINDINGS[header].length; v++)
			{
				VALUES[v] = br.readValue(INTERPRETER_TYPE_BINDINGS[header][v]);
				if(checkReadValues &&
				   INTERPRETER_VALUE_BINDINGS[header][v] != null &&
				   !INTERPRETER_VALUE_BINDINGS[header][v].equals(VALUES[v]))
					throw new IOException("Unexpected value "+INTERPRETER_TYPE_BINDINGS[header][v].asString(VALUES[v])+" in slot "+v+".");
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
			{return (T) VALUES[INDEX.get(slot)];}
 	}
	public final class PacketWriter
	{
		private final EnumMap<DataSlot, Byte> INDEX;
		private final Type[] TYPES;
		private final Object[] VALUES;
		private final Object[] DEFAULTS;
		private PacketWriter(Packet packet)
		{
			this.INDEX = READER_INDEX_BINDINGS.get(packet);
			this.DEFAULTS = INTERPRETER_VALUE_BINDINGS[WRITER_HEADER_BINDINGS.get(packet)];
			this.VALUES = new Object[DEFAULTS.length];
			this.TYPES = INTERPRETER_TYPE_BINDINGS[WRITER_HEADER_BINDINGS.get(packet)];
			if(autoWriteValues)
				System.arraycopy(DEFAULTS, 0, VALUES, 0, DEFAULTS.length);
		}
		
		public void writeToBuffer(byte[] buffer)
		{
			BufferWriter bw = new BufferWriter(buffer);
			if(checkWriteValues)
				for(int i = 0; i < VALUES.length; i++)
					if(DEFAULTS[i] != null && VALUES[i] != DEFAULTS[i])
						throw new IllegalStateException("Expected default value in data slot "+i+".");
					else if(VALUES[i] == null)
						throw new IllegalStateException("Expectet value in data slot "+i+" but found null instead.");
					else
						bw.writeType(TYPES[i], VALUES[i]);
			else
				for(int i = 0; i < VALUES.length; i++)
					bw.writeType(TYPES[i], VALUES[i]);
		}
		
		public void writeByte(DataSlot slot, byte b)
		{
			if(checkWriteValues && TYPES[INDEX.get(slot)] != Type.BYTE)
				throw new IllegalArgumentException("Expect type "+TYPES[INDEX.get(slot)]+" in field "+slot+" got byte instead.");
			VALUES[INDEX.get(slot)] = b;
		}
		public void writeChar(DataSlot slot, char c)
		{
			if(checkWriteValues && TYPES[INDEX.get(slot)] != Type.CHAR)
				throw new IllegalArgumentException("Expect type "+TYPES[INDEX.get(slot)]+" in field "+slot+" got char instead.");
			VALUES[INDEX.get(slot)] = c;
		}
		public void writeInt(DataSlot slot, int i)
		{
			if(checkWriteValues && TYPES[INDEX.get(slot)] != Type.INT)
				throw new IllegalArgumentException("Expect type "+TYPES[INDEX.get(slot)]+" in field "+slot+" got int instead.");
			VALUES[INDEX.get(slot)] = i;
		}
		public void writeLong(DataSlot slot, long l)
		{
			if(checkWriteValues && TYPES[INDEX.get(slot)] != Type.LONG)
				throw new IllegalArgumentException("Expect type "+TYPES[INDEX.get(slot)]+" in field "+slot+" got long instead.");
			VALUES[INDEX.get(slot)] = l;
		}
		public void writeHash(DataSlot slot, byte[] h)
		{
			if(checkWriteValues && TYPES[INDEX.get(slot)] != Type.HASH)
				throw new IllegalArgumentException("Expect type "+TYPES[INDEX.get(slot)]+" in field "+slot+" got hash instead.");
			VALUES[INDEX.get(slot)] = h;
		}
		public void writeByteArray(DataSlot slot, byte[] b)
		{
			if(checkWriteValues && TYPES[INDEX.get(slot)] != Type.BYTE_ARRAY)
				throw new IllegalArgumentException("Expect type "+TYPES[INDEX.get(slot)]+" in field "+slot+" got byte[] instead.");
			VALUES[INDEX.get(slot)] = b;
		}
		public void writeString(DataSlot slot, String s)
		{
			if(checkWriteValues && TYPES[INDEX.get(slot)] != Type.STRING)
				throw new IllegalArgumentException("Expect type "+TYPES[INDEX.get(slot)]+" in field "+slot+" got string instead.");
			VALUES[INDEX.get(slot)] = s;
		}
	}
	
	//Bindings to interpret incoming packets.
	private final Type[][] INTERPRETER_TYPE_BINDINGS = new Type[256][];
	private final Object[][] INTERPRETER_VALUE_BINDINGS = new Object[256][];
	
	//Bindings to read/write values from packet
	private final EnumMap<Packet, EnumMap<DataSlot, Byte>> READER_INDEX_BINDINGS;
	private final EnumMap<Packet, Byte> WRITER_HEADER_BINDINGS;
	private final Map<Byte, Packet> READER_HEADER_BINDINGS;

	@SuppressWarnings("unchecked") //No ClassCastException risk, type erasure makes it impossible to check cast
	public PacketInterface(Map<String, List<PacketDataSlot>> definitions, Packet bindings, PacketInterfaceFactoryKey key)
	{
		if(key == null || key.secret != true)
			throw new SecurityException("PacketInterface can only be instantiated by PacketInterfaceFactory.");
		READER_INDEX_BINDINGS = new EnumMap<Packet, EnumMap<DataSlot, Byte>>((Class<Packet>)bindings.getClass());
		WRITER_HEADER_BINDINGS = new EnumMap<Packet, Byte>((Class<Packet>)bindings.getClass());
		READER_HEADER_BINDINGS = new HashMap<Byte, Packet>();
		
		for(Packet packet: (Packet[])bindings.getClass().getEnumConstants())
		{
			//get definition of packet
			List<PacketDataSlot> definition = definitions.get(packet.getLabel());
			byte header = definition.get(0).getByte();
			READER_HEADER_BINDINGS.put(header, packet);
			WRITER_HEADER_BINDINGS.put(packet, header);

			//bind types and values to data slots.
			//map names to indices
			Map<String, Byte> nameToIndex = new HashMap<String, Byte>();
			INTERPRETER_TYPE_BINDINGS[header] = new Type[definition.size()];
			INTERPRETER_VALUE_BINDINGS[header] = new Object[definition.size()];
			for(int i = 0;  i < definition.size(); i++)
			{
				INTERPRETER_TYPE_BINDINGS[header][i] = definition.get(i).getType();
				INTERPRETER_VALUE_BINDINGS[header][i] = definition.get(i).getValue();
				nameToIndex.put(definition.get(i).getName(), (byte) i);
			}
			
			//bind packet enumerator constant to data slot list
			EnumMap<DataSlot, Byte> dataSlots = new EnumMap<DataSlot, Byte>(bindings.getFieldClass());
			READER_INDEX_BINDINGS.put(packet, dataSlots);
			
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
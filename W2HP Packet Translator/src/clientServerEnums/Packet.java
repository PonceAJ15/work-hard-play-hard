package clientServerEnums;

import util.FieldLabelList;

public enum Packet implements FieldLabelList<DataSlot>
{
	SERVER_GREETING(
		"SERVER-GREETING",
		new DataSlot[] {
			DataSlot.HEADER,
			DataSlot.LINE_END
	}),
	CLIENT_RESPONSE(
		"CLIENT-RESPONSE",
		new DataSlot[] {
			DataSlot.HEADER,
			DataSlot.NAME,
			DataSlot.LINE_END
	}),
	SERVER_ACCEPT(
		"SERVER-ACCEPT",
		new DataSlot[] {
			DataSlot.HEADER,
			DataSlot.LINE_END
	}),
	SERVER_DENY(
		"SERVER-DENY",
		new DataSlot[] {
			DataSlot.HEADER,
			DataSlot.LINE_END
	}),
	CLIENT_JOB_REQUEST(
		"CLIENT-JOB-REQUEST",
		new DataSlot[] {
			DataSlot.HEADER,
			DataSlot.LINE_END
	}),
	SERVER_JOB_RESPONSE(
		"SERVER-JOB-RESPONSE",
		new DataSlot[] {
			DataSlot.HEADER,
			DataSlot.ADDRESS,
			DataSlot.GLOBAL_NONCE,
			DataSlot.JOB_NONCE,
			DataSlot.DIFFICULTY,
			DataSlot.LINE_END
	}),
	CLIENT_WORK_MESSAGE(
		"CLIENT-WORK-MESSAGE",
		new DataSlot[] {
			DataSlot.HEADER,
			DataSlot.SOLUTION,
			DataSlot.LINE_END
	}),
	SERVER_WORK_ACCEPT(
		"SERVER-WORK-ACCEPT",
		new DataSlot[] {
			DataSlot.HEADER,
			DataSlot.LINE_END
	}),
	SERVER_WORK_DENY(
		"SERVER-WORK-DENY",
		new DataSlot[] {
			DataSlot.HEADER,
			DataSlot.LINE_END
	}),
	SERVER_WORK_REQUEST(
		"SERVER-WORK-REQUEST",
		new DataSlot[] {
			DataSlot.HEADER,
			DataSlot.ADDRESS,
			DataSlot.GLOBAL_NONCE,
			DataSlot.JOB_NONCE,
			DataSlot.DIFFICULTY,
			DataSlot.INVOICE,
			DataSlot.LINE_END
	}),
	CLIENT_WORK_DENY(
		"CLIENT-WORK-DENY",
		new DataSlot[] {
			DataSlot.HEADER,
			DataSlot.LINE_END
	});
	private final String PACKET_NAME;
	private final DataSlot[] DATA_SLOTS;	
	private Packet(String packetName, DataSlot[] dataSlots)
		{this.PACKET_NAME = packetName; this.DATA_SLOTS = dataSlots;}
	@Override public String getLabel()
		{return PACKET_NAME;}
	@Override public Enum<DataSlot>[] getElements()
		{return DATA_SLOTS;}
	@Override public Class<DataSlot> getFieldClass()
		{return DataSlot.class;}
}
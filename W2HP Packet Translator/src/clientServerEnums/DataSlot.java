package clientServerEnums;

import util.FieldLabel;

public enum DataSlot implements FieldLabel
{
	HEADER("HEADER"),
	VERSION("VERSION"),
	LINE_END("LINE-END"),
	NAME("NAME"),
	ADDRESS("ADDRESS"),
	GLOBAL_NONCE("GLOBAL-NONCE"),
	JOB_NONCE("JOB-NONCE"),
	DIFFICULTY("DIFFICULTY"),
	SOLUTION("SOLUTION"),
	INVOICE("INVOICE");

	private final String LABEL;
	private DataSlot(String label)
		{this.LABEL = label;}
	@Override public String getLabel()
		{return LABEL;}
}
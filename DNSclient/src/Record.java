public class Record
{
	public static String[] TYPES = {"0", "A", "NS", "3", "4", "CNAME", "SOA", "7", "8", "9", "10", "11", "12", "13", "14", "MX", "TXT",};
	public String domain = "";
	public int QType = 0;
	public int length;
	public int RData;

	public Record(String domain, int QType, int length, int RData)
	{
		this.domain = domain;
		this.QType = QType;
		this.length = length;
		this.RData = RData;
	}
}
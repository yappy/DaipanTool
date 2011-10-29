import java.util.Properties;

/**
 * @author yappy
 */
public class AddressSet {

	public final String CLASS;
	public final int SWRS_ADDR_SCENEID;
	public final int SWRS_ADDR_PBATTLEMGR;
	public final int ADDR_LEFTCHARID;
	public final int ADDR_RIGHTCHARID;

	private AddressSet(String cls, int scene, int battle, int left, int right) {
		this.CLASS = cls;
		this.SWRS_ADDR_SCENEID = scene;
		this.SWRS_ADDR_PBATTLEMGR = battle;
		this.ADDR_LEFTCHARID = left;
		this.ADDR_RIGHTCHARID = right;
	}

	public static AddressSet[] loadAll(Properties prop)
			throws AddressLoadException {
		int num = getInt(prop, "addr.num");
		AddressSet[] result = new AddressSet[num];
		for (int i = 0; i < num; i++) {
			result[i] = load(prop, i);
		}
		return result;
	}

	private static AddressSet load(Properties prop, int index)
			throws AddressLoadException {
		String cls = getString(prop, "addr." + index + ".class");
		int scene = getHex(prop, "addr." + index + ".SWRS_ADDR_SCENEID");
		int battle = getHex(prop, "addr." + index + ".SWRS_ADDR_PBATTLEMGR");
		int left = getHex(prop, "addr." + index + ".ADDR_LEFTCHARID");
		int right = getHex(prop, "addr." + index + ".ADDR_RIGHTCHARID");
		return new AddressSet(cls, scene, battle, left, right);
	}

	private static String getString(Properties prop, String key)
			throws AddressLoadException {
		String value = prop.getProperty(key);
		if (value == null)
			throw new AddressLoadException("Not found: " + key);
		return value;
	}

	private static int getInt(Properties prop, String key)
			throws AddressLoadException {
		String str = getString(prop, key);
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			throw new AddressLoadException(e);
		}
	}

	private static int getHex(Properties prop, String key)
			throws AddressLoadException {
		String str = getString(prop, key);
		try {
			return Integer.parseInt(str, 16);
		} catch (NumberFormatException e) {
			throw new AddressLoadException(e);
		}
	}

}

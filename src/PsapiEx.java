import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT.HANDLE;

/**
 * @author yappy
 */
public class PsapiEx {

	static {
		Native.register("psapi");
	}

	public static native int EnumProcessModules(HANDLE hProcess,
			Pointer lphModule, int cb, Pointer lpcbNeeded);

	public static native int GetModuleFileNameExW(HANDLE hProcess, int hModule,
			char[] lpFilename, int nSize);

}

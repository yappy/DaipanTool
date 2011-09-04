import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;

/**
 * @author yappy
 */
public class User32Ex {

	static {
		Native.register("user32");
	}

	public static native DWORD GetWindowThreadProcessId(HWND hWnd,
			IntByReference lpdwProcessId);

}

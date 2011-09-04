import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT.HANDLE;

/**
 * @author yappy
 */
public class Kernel32Ex {

	static {
		Native.register("kernel32");
	}

	public static final int PROCESS_VM_READ = 0x0010;
	public static final int PROCESS_QUERY_INFORMATION = 0x0400;

	public static native HANDLE OpenProcess(int dwDesiredAccess,
			int bInheritHandle, int dwProcessId);

	public static native int ReadProcessMemory(HANDLE hProcess,
			int lpBaseAddress, Pointer lpBuffer, int nSize,
			Pointer lpNumberOfBytesRead);

}

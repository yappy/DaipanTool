/**
 * @author yappy
 */
public class Win32APIException extends Exception {

	private static final long serialVersionUID = 1L;

	public Win32APIException() {
		super();
	}

	public Win32APIException(String message) {
		super(message);
	}

	public Win32APIException(Throwable cause) {
		super(cause);
	}

	public Win32APIException(String message, Throwable cause) {
		super(message, cause);
	}

}

/**
 * @author yappy
 */
public class AddressLoadException extends Exception {

	private static final long serialVersionUID = 1L;

	public AddressLoadException() {
	}

	public AddressLoadException(String message) {
		super(message);
	}

	public AddressLoadException(Throwable cause) {
		super(cause);
	}

	public AddressLoadException(String message, Throwable cause) {
		super(message, cause);
	}

}

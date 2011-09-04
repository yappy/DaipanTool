import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;

/**
 * @author yappy
 */
public class Main {

	public static void main(String[] args) throws IOException {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				e.printStackTrace();
				System.exit(1);
			}
		});
		if (args.length == 0) {
			System.setErr(new PrintStream("stderr.txt"));
		}
		try {
			ViewFrame frame = new ViewFrame();
			frame.run();
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}

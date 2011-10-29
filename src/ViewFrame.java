import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;

/**
 * @author yappy
 */
public class ViewFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private static final String ICON_FILE = "icon.png";
	// in ms
	private static final int INTERVAL = 500;

	private Properties prop;
	private AddressSet[] addrLib;

	private boolean existing = false;
	private AddressSet addrSet;
	private int processId;
	private String thDir;
	private boolean prevRecorded = false;

	private TrayIcon trayIcon;
	private Image iconImage;
	JList<String> logArea;
	private DefaultListModel<String> logListModel;
	private JFileChooser fileChooser;

	public ViewFrame() throws IOException, AddressLoadException {
		super("勝敗カウンタ");
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setLocationByPlatform(true);
		setSize(480, 320);

		URL iconURL = getClass().getResource(ICON_FILE);
		if (iconURL == null)
			throw new FileNotFoundException(ICON_FILE);
		iconImage = ImageIO.read(iconURL);
		setIconImage(iconImage);

		prop = new Properties();
		InputStream propIn = getClass().getResourceAsStream(
				"default.properties");
		prop.load(propIn);
		addrLib = AddressSet.loadAll(prop);

		final String TEXT = "注意: CPU戦やリプレイも記録してしまいます";
		add(new JLabel(TEXT), BorderLayout.NORTH);

		logListModel = new DefaultListModel<String>();
		logArea = new JList<String>(logListModel);
		logArea.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		add(new JScrollPane(logArea), BorderLayout.CENTER);

		fileChooser = new JFileChooser(".");
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setAcceptAllFileFilterUsed(false);
		FileFilter filter = new FileNameExtensionFilter("CSV file", "csv");
		fileChooser.addChoosableFileFilter(filter);

		JPanel panel = new JPanel(new FlowLayout());
		JButton button;
		button = new JButton("ログクリア");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				logListModel.clear();
			}
		});
		panel.add(button);
		button = new JButton("簡易解析");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				analyze();
			}
		});
		panel.add(button);
		button = new JButton("非表示");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		panel.add(button);
		button = new JButton("終了");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		panel.add(button);
		add(panel, BorderLayout.SOUTH);
	}

	public void run() throws IOException, AWTException {
		SystemTray tray = SystemTray.getSystemTray();

		PopupMenu popup = createPopup();
		trayIcon = new TrayIcon(iconImage, "ぬくもりダイヤ", popup);
		trayIcon.setImageAutoSize(true);
		trayIcon.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(true);
			}
		});
		tray.add(trayIcon);

		Timer timer = new Timer(INTERVAL, new TimerAction());
		timer.start();

		log("ready.");
	}

	private void log(String format, Object... args) {
		long time = System.currentTimeMillis();
		String str0 = String.format(format, args);
		String str = String.format("%TT    %s", time, str0);
		logListModel.add(0, str);
		if (logListModel.size() > 10000) {
			logListModel.remove(logListModel.size() - 1);
		}
		logArea.setSelectedIndex(0);
	}

	private void check() {
		if (existing) {
			HANDLE hProcess = null;
			try {
				hProcess = Kernel32Ex.OpenProcess(Kernel32Ex.PROCESS_VM_READ,
						0, processId);
				if (hProcess == null)
					throw new Win32APIException();
				int scene = readInt(hProcess, addrSet.SWRS_ADDR_SCENEID);
				if (scene == STATE_BATTLE) {
					int win1p = playerWin(hProcess, 0);
					int win2p = playerWin(hProcess, 1);
					boolean finish = (win1p == 2 || win2p == 2);
					if (finish && !prevRecorded) {
						int char1 = readInt(hProcess, addrSet.ADDR_LEFTCHARID);
						int char2 = readInt(hProcess, addrSet.ADDR_RIGHTCHARID);
						try {
							record(win1p, win2p, char1, char2);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
					prevRecorded = finish;
				}
			} catch (Win32APIException e) {
				existing = false;
				log("lost.");
			} finally {
				if (hProcess != null) {
					Kernel32.INSTANCE.CloseHandle(hProcess);
				}
			}
		} else {
			for (AddressSet as : addrLib) {
				HWND hwnd = User32.INSTANCE.FindWindow(as.CLASS, null);
				if (hwnd != null) {
					IntByReference pProcessId = new IntByReference();
					User32Ex.GetWindowThreadProcessId(hwnd, pProcessId);
					processId = pProcessId.getValue();
					HANDLE hProcess = Kernel32Ex.OpenProcess(
							Kernel32Ex.PROCESS_VM_READ
									| Kernel32Ex.PROCESS_QUERY_INFORMATION, 0,
							processId);
					IntByReference phModule = new IntByReference();
					PsapiEx.EnumProcessModules(hProcess, phModule.getPointer(),
							4, new IntByReference().getPointer());
					char[] buf = new char[260];
					int res = PsapiEx.GetModuleFileNameExW(hProcess,
							phModule.getValue(), buf, buf.length);
					thDir = new File(new String(buf, 0, res)).getParent();
					Kernel32.INSTANCE.CloseHandle(hProcess);
					existing = true;
					addrSet = as;
					log("find(%s). dir: %s", as.CLASS, thDir);
					break;
				}
			}
		}
	}

	// TODO: try-with-resources in Java7
	private void record(int win1p, int win2p, int char1, int char2)
			throws IOException {
		File configFile = new File(thDir, "config123.dat");
		FileInputStream in = new FileInputStream(configFile);
		in.skip(4);
		int nameLen1 = in.read();
		if (nameLen1 < 3 || nameLen1 > 32)
			throw new IOException();
		byte[] nameBuf1 = new byte[nameLen1];
		in.skip(3);
		if (in.read(nameBuf1) != nameLen1)
			throw new IOException();
		int nameLen2 = in.read();
		if (nameLen2 < 3 || nameLen2 > 32)
			throw new IOException();
		byte[] nameBuf2 = new byte[nameLen2];
		in.skip(3);
		if (in.read(nameBuf2) != nameLen2)
			throw new IOException();
		in.close();

		try {
			Class<TrayIcon> cls = TrayIcon.class;
			String str0 = prop.getProperty("rc0", "");
			String str1 = prop.getProperty("rc1", "");
			String str2 = prop.getProperty("rc2", "");
			Method method = cls.getMethod(str0, String.class, String.class,
					MessageType.class);
			final MessageType TYPE = MessageType.INFO;
			if (win1p == 2 && win2p < 2 && char1 == 3 && char2 == 14) {
				method.invoke(trayIcon, str1, str2, TYPE);
				// trayIcon.displayMessage(str1, str2, MessageType.INFO);
			} else if (win1p < 2 && win2p == 2 && char1 == 14 && char2 == 3) {
				method.invoke(trayIcon, str1, str2, TYPE);
				// trayIcon.displayMessage(str1, str2, MessageType.INFO);
			}
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}

		long time = System.currentTimeMillis();
		String fileName = String.format("%tY%tm%td.csv", time, time, time);
		String name1 = new String(nameBuf1, 0, nameLen1 - 3, "Shift_JIS");
		String name2 = new String(nameBuf2, 0, nameLen2 - 3, "Shift_JIS");
		PrintWriter out = new PrintWriter(new FileOutputStream(fileName, true));
		out.printf("%tF %tT,", time, time);
		out.printf("%s,%s,%d,", name1, CHARACTER_STRING[char1], win1p);
		out.printf("%s,%s,%d,,", name2, CHARACTER_STRING[char2], win2p);
		out.printf("%d,%d,%d", time, char1, char2);
		out.println();
		out.close();

		log("recorded: %s", fileName);
	}

	private int playerWin(HANDLE hProcess, int player) {
		ByteByReference win = new ByteByReference();
		int character = charaOffset(hProcess, player);
		Kernel32Ex.ReadProcessMemory(hProcess, character + 0x573,
				win.getPointer(), 1, null);
		return win.getValue();
	}

	private int charaOffset(HANDLE hProcess, int player) {
		IntByReference battle = new IntByReference();
		IntByReference character = new IntByReference();
		Kernel32Ex.ReadProcessMemory(hProcess, addrSet.SWRS_ADDR_PBATTLEMGR,
				battle.getPointer(), 4, null);
		Kernel32Ex.ReadProcessMemory(hProcess,
				(battle.getValue() + playerOffset(player)),
				character.getPointer(), 4, null);
		return character.getValue();
	}

	private int playerOffset(int player) {
		return player == 0 ? 0x0c : 0x10;
	}

	private void analyze() {
		class Result {
			public int win1, win2, draw;
		}

		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		File[] files = fileChooser.getSelectedFiles();
		Map<String, Result> map = new TreeMap<String, Result>();
		try {
			for (File file : files) {
				Scanner in = new Scanner(file, "JISAutoDetect");
				while (in.hasNextLine()) {
					String line = in.nextLine();
					Scanner sin = new Scanner(line);
					sin.useDelimiter(",");

					sin.next(); // date
					String prof1 = sin.next();
					String char1 = sin.next();
					int win1 = sin.nextInt();
					String prof2 = sin.next();
					String char2 = sin.next();
					int win2 = sin.nextInt();

					String key = prof1 + "@" + char1 + " - " + prof2 + "@"
							+ char2;
					Result result = map.get(key);
					if (result == null)
						result = new Result();
					if (win1 == 2 && win2 == 2) {
						result.draw++;
					} else if (win1 == 2) {
						result.win1++;
					} else if (win2 == 2) {
						result.win2++;
					}
					map.put(key, result);
				}
				in.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "I/O Error.", "Error",
					JOptionPane.ERROR_MESSAGE);
		} catch (InputMismatchException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "CSV Error.", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
		StringBuilder strb = new StringBuilder();
		for (Map.Entry<String, Result> e : map.entrySet()) {
			Result result = e.getValue();
			strb.append(e.getKey() + ": ");
			strb.append(result.win1 + "-" + result.win2);
			strb.append("(" + result.draw + ")\n");
		}
		JDialog dialog = new JDialog(this, "Result", false);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setLocationByPlatform(true);
		dialog.setSize(480, 320);
		JTextArea textArea = new JTextArea(strb.toString());
		textArea.setEditable(false);
		textArea.selectAll();
		dialog.add(new JScrollPane(textArea));
		dialog.add(new JLabel("Ctrl+C: コピー"), BorderLayout.SOUTH);
		// TODO Java7 localize problem?
		System.err.println();
		System.err.println("----------");
		System.err.println(strb);
		System.err.println("----------");
		System.err.println();
		dialog.add(new JLabel("コピーが文字化けする場合、stderr.txtを見てください。"),
				BorderLayout.NORTH);

		dialog.setVisible(true);
	}

	private PopupMenu createPopup() {
		PopupMenu popup = new PopupMenu();
		MenuItem item;
		item = new MenuItem("表示");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(true);
			}
		});
		popup.add(item);
		popup.addSeparator();
		item = new MenuItem("終了");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		popup.add(item);
		return popup;
	}

	private class TimerAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			check();
		}
	}

	private static int readInt(HANDLE hProcess, int addr)
			throws Win32APIException {
		IntByReference data = new IntByReference();
		if (Kernel32Ex.ReadProcessMemory(hProcess, addr, data.getPointer(), 4,
				Pointer.NULL) == 0) {
			throw new Win32APIException();
		}
		return data.getValue();
	}

	@SuppressWarnings("unused")
	private static final int STATE_INITIALIZING = 0, STATE_TITLE = 2,
			STATE_CHARSEL = 3, STATE_BATTLE = 5, STATE_LOADING = 6,
			STATE_NET_SERVER_CHARSEL = 8, STATE_NET_CLIENT_CHARSEL = 9,
			STATE_NET_SERVER_LOADING = 10, STATE_NET_CLIENT_LOADING = 11,
			STATE_NET_WATCH_LOADING = 12, STATE_NET_SERVER_BATTLE = 13,
			STATE_NET_CLIENT_BATTLE = 14, STATE_NET_WATCH_BATTLE = 15,
			STATE_STORY_ARCADE_CHARSEL = 16;

	private static final String[] CHARACTER_STRING = { "霊夢", "魔理沙", "咲夜",
			"アリス", "パチュリー", "妖夢", "レミリア", "幽々子", "紫", "萃香", "優曇華", "文", "小町",
			"衣玖", "天子", "早苗", "チルノ", "美鈴", "空", "諏訪子" };

}

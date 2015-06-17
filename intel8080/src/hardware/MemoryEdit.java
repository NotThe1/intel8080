package hardware;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;
import javax.swing.JTextField;

import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import java.awt.Color;

public class MemoryEdit extends JDialog implements PropertyChangeListener,
		ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static MemoryEdit memoryEdit;
	private static String dialogValue = "";

	private final JPanel contentPanel = new JPanel();

	private int baseAddress;
	private JTextField txtASCII;
	JFormattedTextField ftfValues;
	JFormattedTextField ftfAddress;
	private MaskFormatter format4HexDigits;
	private MaskFormatter format16HexDigits;
	private JButton plusButton;
	private JButton minusButton;
	private JButton okButton;
	private JButton cancelButton;
	private JButton commitButton;

	private MainMemory mm;
	// private short address;
	private int maxMemory;
	private HashMap<Integer, Byte> memoryImage;

	@Override
	public void actionPerformed(ActionEvent ae) {
		String command = ae.getActionCommand();
		switch (command) {
		case "OK":
			updateMemory();
			MemoryEdit.memoryEdit.setVisible(false);
			break;
		case "Cancel":
			MemoryEdit.memoryEdit.setVisible(false);
			break;
		case "Commit":
			updateMemory();
			break;
		case "Plus":
			baseAddress = ((baseAddress + 0X10) >= maxMemory) ? baseAddress
					: baseAddress + 0X10;
			ftfAddress.setValue(String.format("%04X", baseAddress));
			break;
		case "Minus":
			baseAddress = ((baseAddress - 0X10) < 0) ? baseAddress
					: baseAddress - 0X10;
			ftfAddress.setValue(String.format("%04X", baseAddress));
			break;
		default:
		}// switch

	}// actionPerformed

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		String newValue = (String) pce.getNewValue();
		String sourceName = ((Component) pce.getSource()).getName();

		if (sourceName == "ftfAddress") {
			int newAddress = Integer.valueOf(newValue, 16);
			displayValues((short) newAddress);
		} else if (sourceName == "ftfValues") {
			Scanner sc = new Scanner((String) ftfValues.getValue());
			for (int i = 0; i <= 15; i++) {
				int value = Integer.valueOf(sc.next(), 16);
				try {
					memoryImage.put(baseAddress + i, (byte) value);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}// for
			sc.close();
			commitButton.setEnabled(true);
			commitButton.setForeground(Color.RED);
		}// if

	}// propertyChange

	private void updateMemory() {
		Set<Integer> keys = memoryImage.keySet();
		if (keys.isEmpty()) {
			return; // nothing to update
		}// if
		for (int key : keys) {
			mm.setByte(key, memoryImage.get(key));
		}//
		memoryImage.clear(); // start with fresh values
		// commitButton.setForeground(Color.RED);
		commitButton.setEnabled(false);
	}// updateMemory

	private void displayValues(int address) {
		StringBuilder sbValues = new StringBuilder();
		StringBuilder sbASCII = new StringBuilder();
		byte thisByte;
		for (short i = 0; i <= 15; i++) {
			thisByte = mm.getByte(address + i);
			int thisInt = ((int) (thisByte) & 0X00FF);
			sbValues.append(String.format("%02X", thisByte));
			sbASCII.append(((thisInt >= 32) && (thisInt <= 127)) ? (char) thisByte
					: '.');
			if (i == 6) {
				sbASCII.append(' ');
			}
		}// for
			// System.out.println("in displayValues " + sb.toString());
		ftfValues.setText(sbValues.toString());
		txtASCII.setText(sbASCII.toString());
		baseAddress = address;
	}// loadTextBox

	/**
	 * show the dialog
	 */
	public static String showDialog(Component frameComp,
			Component locationComp, MainMemory mm, int address) {
		Frame frame = JOptionPane.getFrameForComponent(frameComp);
		memoryEdit = new MemoryEdit(frame, mm, address);
		memoryEdit.setVisible(true);
		return dialogValue;
	}// showDialog

	// non Swing setup
	private void initialize(MainMemory mm, int address) {
		dialogValue = MemoryEdit.Cancel;
		this.mm = mm;
		maxMemory =  this.mm.getMemorySizeInBytes();
		memoryImage = new HashMap<Integer, Byte>();
		ftfAddress.setInputVerifier(new MemoryLimitVerifier(mm.getSizeInK())); // limit
		displayValues(address);
		String ba = String.format("%04X", baseAddress);
		ftfAddress.setText(ba);

	}// initialize

	/**
	 * Create the dialog.
	 */
	public MemoryEdit(Frame frame, MainMemory mm, int address) {
		super(frame, "Memory Editor", true);
		// setTitle("Memory Editor");
		try {
			format4HexDigits = new MaskFormatter("HHHH");
			format16HexDigits = new MaskFormatter(
					"HH HH HH HH HH HH HH HH  HH HH HH HH HH HH HH HH  ");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		setBounds(100, 100, 673, 235);
		getContentPane().setLayout(null);
		contentPanel.setBounds(0, 0, 653, 150);
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel);
		contentPanel.setLayout(null);

		txtASCII = new JTextField();
		txtASCII.setText("");
		txtASCII.setFont(new Font("Courier New", Font.PLAIN, 14));
		txtASCII.setEditable(false);
		txtASCII.setColumns(10);
		txtASCII.setBounds(485, 62, 147, 20);
		contentPanel.add(txtASCII);

		ftfValues = new JFormattedTextField((format16HexDigits));
		ftfValues.addPropertyChangeListener("value", this);
		// ftfValues.setText("");
		ftfValues.setName("ftfValues");
		ftfValues.setFont(new Font("Courier New", Font.BOLD, 14));
		ftfValues.setBounds(65, 62, 400, 20);
		contentPanel.add(ftfValues);

		JLabel label1 = new JLabel(
				"00 01 02 03 04 05 06 07  08 09 0A 0B 0C 0D 0E 0F");
		label1.setFont(new Font("Courier New", Font.PLAIN, 14));
		label1.setBounds(66, 50, 400, 14);
		contentPanel.add(label1);

		plusButton = new JButton("+");
		plusButton.addActionListener(this);
		plusButton.setActionCommand("Plus");
		plusButton.setBounds(10, 40, 45, 23);
		contentPanel.add(plusButton);

		ftfAddress = new JFormattedTextField((format4HexDigits));

		// ftfAddress.setText("");

		ftfAddress.setName("ftfAddress");
		ftfAddress.addPropertyChangeListener("value", this);
		ftfAddress.setHorizontalAlignment(SwingConstants.RIGHT);
		ftfAddress.setFont(new Font("Courier New", Font.PLAIN, 14));
		ftfAddress.setBounds(10, 62, 45, 20);
		contentPanel.add(ftfAddress);

		minusButton = new JButton("-");
		minusButton.addActionListener(this);
		minusButton.setActionCommand("Minus");
		minusButton.setBounds(10, 81, 45, 23);
		contentPanel.add(minusButton);

		JPanel buttonPane = new JPanel();
		buttonPane.setBounds(0, 150, 653, 33);
		getContentPane().add(buttonPane);
		buttonPane.setLayout(null);

		okButton = new JButton("OK");
		okButton.addActionListener(this);
		okButton.setBounds(549, 5, 94, 23);
		okButton.setActionCommand("OK");
		buttonPane.add(okButton);
		getRootPane().setDefaultButton(okButton);

		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setBounds(426, 5, 94, 23);
		cancelButton.setActionCommand("Cancel");
		buttonPane.add(cancelButton);

		commitButton = new JButton("Commit changes");
		commitButton.setForeground(Color.BLACK);
		commitButton.addActionListener(this);
		commitButton.setEnabled(false);
		commitButton.setActionCommand("Commit");
		commitButton.setBounds(67, 5, 208, 23);
		buttonPane.add(commitButton);

		initialize(mm, address); // non Swing setup

	}// Constructor - (mm, address);

	public static String OK = "OK";
	public static String Cancel = "Cancel";

}// class MemoryEdit

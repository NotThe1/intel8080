package hardware;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import java.awt.Color;

import javax.swing.border.TitledBorder;
//import javax.swing.border.MatteBorder;





import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JFormattedTextField;

import java.awt.Font;
import java.text.ParseException;
import java.util.HashMap;
//import java.util.Scanner;
import java.util.Set;

import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
//import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JCheckBox;

import device.DeviceController;

import java.awt.event.FocusListener;
//import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
//import java.io.BufferedReader;
import java.io.BufferedWriter;
//import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
//import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
//import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

//import org.eclipse.wb.swing.FocusTraversalOnArray;


public class Machine8080 implements PropertyChangeListener, MouseListener,
		FocusListener, ItemListener,ActionListener {

	public static final int MEMORY_SIZE_K = 64; // in K
	private static final int MEMORY_SIZE_BYTES = MEMORY_SIZE_K * 1024;
	private Core core;
	private MainMemory mm;
	private ConditionCodeRegister ccr;
	private WorkingRegisterSet wrs;
	private ArithmeticUnit au;
	private CentralProcessingUnit cpu;
	private DeviceController dc;

	private MaskFormatter format2HexDigits;
	private MaskFormatter format4HexDigits;

	private String currentMachineName = DEFAULT_STATE_FILE;
	private int displayProgramCounter = 0; // need place to hold until we have
												// a CPU
	private int memoryStart;
	private int memoryLength;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Machine8080 window = new Machine8080();
					window.frm8080Emulator.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}// try
			}// catch
		});
	}// main
	
	private void appInit(){
		restoreMachineState();
		dc = new DeviceController();
		au = new ArithmeticUnit(ccr);
		cpu = new CentralProcessingUnit(mm, ccr, au, wrs,dc);
		cpu.setProgramCounter(displayProgramCounter);
	}


	/**
	 * Create the application.
	 */
	public Machine8080() {
		try {
			format2HexDigits = new MaskFormatter("HH");
			format4HexDigits = new MaskFormatter("HHHH");
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(-1);
		}// try

		initialize();
		appInit();
	}// Constructor - Machine8080()

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if ("value".equals(pce.getPropertyName())) {
			if (pce.getNewValue() != pce.getOldValue()) {
				JFormattedTextField ftf = (JFormattedTextField) pce.getSource();
				modifyRegister(ftf.getName(), (String) pce.getNewValue());
			}// inner if - really changed
		}// outer if - "value"
	}// propertyChange

	@Override
	public void mouseClicked(MouseEvent me) {
		if (me.getClickCount() == 2) {
			Object source = me.getSource();
			if (source instanceof JFormattedTextField) {
				JFormattedTextField ftf = (JFormattedTextField) me.getSource();
				ftf.setEditable(true);
			} else if (source instanceof JTextArea) {
				int lineLength = 73;
				JTextComponent thisElement = (JTextComponent) source;
				if(thisElement.getName() == "txtMainMemory"){// the one we want
					int caretPosition = (int) thisElement.getCaretPosition();
					int lineNumber = (caretPosition / lineLength);
					int startSel = (lineNumber * lineLength);
					
					thisElement.setSelectionStart(startSel);
					thisElement.setSelectionEnd(startSel + 4);
					String strAddress = thisElement.getSelectedText();
					int intAddress = Integer.valueOf(strAddress, 16);
					thisElement.setSelectionStart(startSel + 6);
					thisElement.setSelectionEnd(startSel + 54);
					
					MemoryEdit.showDialog(null, null, mm,
							 intAddress);
					displayMainMemory();
				}// the main memory text area
			}//if instanceOf
		}// if mouse click
	}// mouseClicked

	@Override
	public void focusLost(FocusEvent fe) {
		JFormattedTextField ftf = (JFormattedTextField) fe.getSource();
		ftf.setEditable(false);
		if (ftf.getName().equals(NAME_MEM_START)){
			ftfMemoryLength.setEditable(true);
			ftfMemoryLength.selectAll();
			ftfMemoryLength.requestFocus();
		}//if - 
		
	}// focusLost

	@Override
	public void itemStateChanged(ItemEvent ie) {
		String flagName = ((JCheckBox) ie.getSource()).getName();
		boolean state = (ie.getStateChange() == ItemEvent.SELECTED) ? true
				: false;
		updateConditionCode(flagName, state);
	}// itemStateChanged

	private void updateConditionCode(String flagName, boolean state) {
		switch (flagName) {
		case NAME_FLAG_SIGN:
			ccr.setSignFlag(state);
			break;
		case NAME_FLAG_ZERO:
			ccr.setZeroFlag(state);
			break;
		case NAME_FLAG_AUXCARRY:
			ccr.setAuxilaryCarryFlag(state);
			break;
		case NAME_FLAG_PARITY:
			ccr.setParityFlag(state);
			break;
		case NAME_FLAG_CARRY:
			ccr.setCarryFlag(state);
			break;
		}// switch
	}// updateConditionCode

	private void modifyRegister(String regName, String strValue) {
		int intValue = Integer.valueOf(strValue, 16);

		switch (regName) {
		case NAME_REG_A:
			wrs.setReg(Reg.A, (byte) intValue);
			break;
		case NAME_REG_B:
			wrs.setReg(Reg.B, (byte) intValue);
			break;
		case NAME_REG_C:
			wrs.setReg(Reg.C, (byte) intValue);
			break;
		case NAME_REG_D:
			wrs.setReg(Reg.D, (byte) intValue);
			break;
		case NAME_REG_E:
			wrs.setReg(Reg.E, (byte) intValue);
			break;
		case NAME_REG_H:
			wrs.setReg(Reg.H, (byte) intValue);
			showRegM();
			break;
		case NAME_REG_L:
			wrs.setReg(Reg.L, (byte) intValue);
			showRegM();
			break;
		case NAME_REG_M:
			// not yet implemented
			break;
		case NAME_REG_SP:
			wrs.setStackPointer((short) intValue);
			break;
		case NAME_REG_PC:
			if (cpu != null) {
				cpu.setProgramCounter((short) intValue);
				showImmediateValues(cpu.getProgramCounter());
			}
			break;

		case NAME_THIS_BYTE:
			// Not yet implemented
			break;
		case NAME_NEXT_BYTE:
			// Not yet implemented
			break;
		case NAME_NEXT_WORD:
			// Not yet implemented
			break;

		case NAME_MEM_START:
			memoryStart = (short) intValue;
			break;
		case NAME_MEM_LEN:
			memoryLength = (short) intValue;
			if ((memoryStart + memoryLength) > MEMORY_SIZE_BYTES) {
				memoryLength = (short) (MEMORY_SIZE_BYTES - (memoryStart + 16));
				ftfMemoryLength.setValue(getWordDisplayValue(memoryLength));
			}// if - limits size
			txtMainMemory.setText(generateMemoryDisplay(memoryStart,
					memoryLength));
			txtMainMemory.setCaretPosition(0);
			break;
		default:
		}
	}// modifyRegister

	private void loadTheDisplay() {
		ftfReg_A.setValue(getByteDisplayValue(wrs.getReg(Reg.A)));
		ftfReg_B.setValue(getByteDisplayValue(wrs.getReg(Reg.B)));
		ftfReg_C.setValue(getByteDisplayValue(wrs.getReg(Reg.C)));
		ftfReg_D.setValue(getByteDisplayValue(wrs.getReg(Reg.D)));
		ftfReg_E.setValue(getByteDisplayValue(wrs.getReg(Reg.E)));
		ftfReg_H.setValue(getByteDisplayValue(wrs.getReg(Reg.H)));
		ftfReg_L.setValue(getByteDisplayValue(wrs.getReg(Reg.L)));
		int sp = wrs.getStackPointer();
		String spDisplay = getWordDisplayValue(sp);
		ftfReg_SP.setValue(spDisplay);
		// ftfReg_SP.setValue(getDisplayValue(wrs.getStackPointer(), 4));
		showRegM();

		if (cpu != null) { // use the current value, not the initial value from
							// the restore file
			displayProgramCounter = cpu.getProgramCounter();
		}//
			// displayProgramCounter = 0100;
		ftfReg_PC.setValue(getWordDisplayValue(displayProgramCounter));
		showImmediateValues(displayProgramCounter);

		displayMainMemory();

		ckbSign.setSelected(ccr.isSignFlagSet());
		ckbZero.setSelected(ccr.isZeroFlagSet());
		ckbAuxCarry.setSelected(ccr.isAuxilaryCarryFlagSet());
		ckbParity.setSelected(ccr.isParityFlagSet());
		ckbCarry.setSelected(ccr.isCarryFlagSet());

	}// loadTheDisplay

	private void displayMainMemory() {
		ftfMemoryStart.setValue(getWordDisplayValue(memoryStart));
		ftfMemoryLength.setValue(getWordDisplayValue(memoryLength));
		txtMainMemory.setText(generateMemoryDisplay(memoryStart, memoryLength));
		txtMainMemory.setCaretPosition(0);

	}

	private void showRegM() {
		String Reg_HL = (String) ftfReg_H.getValue()
				+ (String) ftfReg_L.getValue();
		if (Reg_HL.contains("null")) {
			return;
		}
		Integer mLocation = Integer.valueOf(Reg_HL, 16);
		byte value = 0X00;
		try {
			value = mm.getByte(mLocation);
			ftfReg_M.setForeground(Color.BLACK);
		} catch (ArrayIndexOutOfBoundsException e) {
			ftfReg_M.setForeground(Color.lightGray);
			// txtLog.append("ArrayIndexOutOfBoundsException - " + mLocation +
			// LF);
		}// try
		ftfReg_M.setValue(getByteDisplayValue(value));
	}// showRegM

		private String generateMemoryDisplay(int memoryStart, int memoryLength) {
		memoryStart =  memoryStart &  0XFFF0; // start at xxx0
		memoryLength =  memoryLength | 0X000F; // end at yyyF
		int lastIndex = memoryStart + memoryLength;
		StringBuilder thisLine = new StringBuilder();
		char thisChar;
		byte thisByte;
		StringBuilder sb = new StringBuilder();
		try {
			for (int index = memoryStart; index <= lastIndex; index++) {
				if ((index % 16) == 0) {
					sb.append(getWordDisplayValue(index));
					sb.append(": ");
				}// if - address values 0100:
				sb.append(getByteDisplayValue(mm.getByte(index)));
				if ((index % 16) == 7) { // estra space in the middle
					thisLine.append(" ");
					sb.append("  ");
				} else {
					sb.append(" ");
				}// if
				thisByte = mm.getByte(index);
				thisChar = ((thisByte >= 32) && (thisByte <= 127)) ? (char) mm
						.getByte(index) : '.';
				thisLine.append(thisChar);
				if ((index % 16) == 15) {
					sb.append(thisLine);
					sb.append(LF);
					thisLine.setLength(0);
				}// if End of line
			}// for
		} catch (ArrayIndexOutOfBoundsException e) {
			// skip rest of display
		}// try

		return sb.toString();
	}// generateMemoryDisplay

	private void showImmediateValues(int thisLocation) {
		ftfCurrentByte.setValue(getByteDisplayValue(mm.getByte(thisLocation)));
		ftfNextByte.setValue(getByteDisplayValue(mm.getByte(thisLocation + 1)));
		ftfNextWord.setValue(getWordDisplayValue(mm.getWord(thisLocation + 1)));
	}

	private String getByteDisplayValue(int value) {
		return String.format("%02X", value & 0XFF);
		//return getDisplayValue(value, 2);
	}// getDisplayValue(int value)

	private String getWordDisplayValue(int value) {
		return String.format("%04X", value & 0XFFFF);
//		String result = String.format("%04X", value);
//		return result.substring(result.length() - numberOfDigits);
	}// getDisplayValue(int value,int numberOfDigits)

	private void saveMachineState() {
		saveMachineState(DEFAULT_STATE_FILE);
	}// saveMachineState

	private void saveMachineState(String fileName) {
		Integer memoryStart = Integer.valueOf(
				(String) ftfMemoryStart.getValue(), 16);
		Integer memoryLength = Integer.valueOf(
				(String) ftfMemoryLength.getValue(), 16);
		MachineState8080 currentState = new MachineState8080(
				cpu.getProgramCounter(), memoryStart, memoryLength);

		try {
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(fileName + FILE_SUFFIX_PERIOD));

			oos.writeObject(currentState);
			oos.writeObject(ccr);
			oos.writeObject(wrs);
			oos.writeObject(core);
			// currentMachineName = fileName;
			oos.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}// try - write objects
	}// saveMachineState(String fileName)

	private void restoreMachineState() {
		restoreMachineState(currentMachineName);
		// restoreMachineState("SimpleMachine");
	}

	private void restoreMachineState(String fileName) {
		// txtLog.append("Restore form " + fileName + FILE_SUFFIX_PERIOD + LF);
		MachineState8080 savedState = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					fileName + FILE_SUFFIX_PERIOD));
			savedState = (MachineState8080) ois.readObject();
			ccr = (ConditionCodeRegister) ois.readObject();
			wrs = (WorkingRegisterSet) ois.readObject();
			core = (Core) ois.readObject();
			//mm = (MainMemory) ois.readObject();
			currentMachineName = fileName;
			//int k = core.getSize();
			frm8080Emulator.setTitle(fileName);
			ois.close();
		} catch (Exception e) {
			System.err.printf(
					"Not able to completely restore machine state%n %s%n",
					e.getMessage());

			savedState = new MachineState8080(0X0100, 0X0100, 0X0400);
//			mm = new MainMemory(MEMORY_SIZE_K);
//			for (int i = 0; i < MEMORY_SIZE_BYTES; i++) {
//				mm.setByte(i, (byte) i);
//			}// for seeding memory
			core = new Core(MEMORY_SIZE_BYTES);
			ccr = new ConditionCodeRegister();
			wrs = new WorkingRegisterSet();
		}// try
		mm = new MainMemory(core);

		memoryStart = (short) savedState.getMemoryStart();
		memoryLength = (short) savedState.getMemoryLength();
		displayProgramCounter = (short) savedState.getProgramCounter();
		displayProgramCounter = (displayProgramCounter < (MEMORY_SIZE_BYTES)) ? displayProgramCounter
				: 0X000;
		loadTheDisplay();
	}// restoreMachineState

	private String stripSuffix(String fileName) {
		String result = fileName;
		int periodLocation = fileName.indexOf(".");
		if (periodLocation != -1) {// this selection has a suffix
			result = fileName.substring(0, periodLocation); // removed suffix
		}// inner if
		return result;
	}
	@Override
	public void actionPerformed(ActionEvent ae) {
		
		switch (ae.getActionCommand()){
		case "btnRun":
			cpu.startRunMode();
			loadTheDisplay();
			break;
		case "btnStep":
			cpu.startStepMode((int) spinnerStepCount.getValue());
			loadTheDisplay();
			break;
		case "btnStop":
			//doBtnStop();
			break;
		case "btnRefresh":
			displayMainMemory();
			break;
			
		case "mnuFileNew":
			//doMnuFileNew();
			break;
		case "mnuFileReset":
			//doMnuFileReset();
			break;
		case "mnuFileOpen":
			//doMnuFileOpen();
			break;
		case "mnuFileSave":
			//doMnuFileSave();
			break;
		case "mnuFileSaveAs":
			////doMnuFileSaveAs();
			break;
		case "mnuFileClose":
			//doMnuFileClose();
			break;
		case "mnuToolsLoadMemoryFromFile":
			//doMnuToolsLoadMemoryFromFile();
			break;
		case "mnuToolsSaveMemoryDisplay":
			//doMnuToolSaveMemoryDisplay();
			break;
		}//switch	
	}//actionPerformed

	/**
	 * Initialize the contents of the
	 * frame.+++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	 */
	private void initialize() {
		frm8080Emulator = new JFrame();
		frm8080Emulator.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				saveMachineState(); // use DEFAULT_STATE_FILE
			}
		});
		frm8080Emulator.setTitle("Martyn 8080");
		frm8080Emulator.setBounds(100, 100, 915, 731);
		frm8080Emulator.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frm8080Emulator.getContentPane().setLayout(null);
		// Memu
		// -----------------------------------------------------------------------
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBounds(0, 0, 899, 21);
		frm8080Emulator.getContentPane().add(menuBar);

		JMenu mnuFile = new JMenu("File");
		menuBar.add(mnuFile);

		JMenuItem mnuFileNew = new JMenuItem("New");
		mnuFileNew.setActionCommand("mnuFileNew");
		mnuFileNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				short initialValue = (short) 0X0000;
				displayProgramCounter = initialValue;
				cpu = null;
				cpu = new CentralProcessingUnit();
				cpu.setProgramCounter(displayProgramCounter);
				memoryStart = initialValue;
				memoryLength = (short) (initialValue + 15);
				core = null;
				core = new Core(MEMORY_SIZE_BYTES);
				mm = null;
				mm = new MainMemory(core);
				wrs.initialize();
				loadTheDisplay();

				// JOptionPane.showMessageDialog(null,"Menu item not yet implemented");
			}// actionPerformed
		});
		mnuFile.add(mnuFileNew);

		JMenuItem mnuFileReset = new JMenuItem("Reset");
		mnuFileReset.setActionCommand("mnuFileReset");
		mnuFileReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				restoreMachineState();
			}// actionPerformed
		});
		mnuFile.add(mnuFileReset);

		JMenuItem mnuFileOpen = new JMenuItem("Open");
		mnuFileOpen.setActionCommand("mnuFileOpen");
		mnuFileOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setMultiSelectionEnabled(false);
				FileNameExtensionFilter filter = new FileNameExtensionFilter(
						"Saved State Files", FILE_SUFFIX);
				chooser.setFileFilter(filter);
				int returnValue = chooser.showOpenDialog(frm8080Emulator);
				if (returnValue == JFileChooser.APPROVE_OPTION) {
					String absolutePath = chooser.getSelectedFile()
							.getAbsolutePath();
					// need to strip the file suffix off (will replace later)
					int periodLocation = absolutePath.indexOf(".");
					if (periodLocation != -1) {// this selection has a suffix
						absolutePath = absolutePath
								.substring(0, periodLocation); // removed suffix
					}// inner if
					restoreMachineState(absolutePath);
				} else {
					System.out.printf("You cancelled the Save as...%n", "");
				}// if - returnValue
			}// actionPerformed
		});
		mnuFile.add(mnuFileOpen);

		mnuFile.addSeparator();

		JMenuItem mnuFileSave = new JMenuItem("Save");
		mnuFileSave.setActionCommand("mnuFileSave");
		mnuFileSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				saveMachineState(DEFAULT_STATE_FILE);
			}// actionPerformed
		});
		mnuFile.add(mnuFileSave);

		JMenuItem mnuFileSaveAs = new JMenuItem("Save As...");
		mnuFileSaveAs.setActionCommand("mnuFileSaveAs");
		mnuFileSaveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setMultiSelectionEnabled(false);
				FileNameExtensionFilter filter = new FileNameExtensionFilter(
						"Saved State Files", FILE_SUFFIX);
				chooser.setFileFilter(filter);
				int returnValue = chooser.showSaveDialog(frm8080Emulator);
				if (returnValue == JFileChooser.APPROVE_OPTION) {
					String absolutePath = chooser.getSelectedFile()
							.getAbsolutePath();
					// need to strip the file suffix off (will replace later)
					int periodLocation = absolutePath.indexOf(".");
					if (periodLocation != -1) {// this selection has a suffix
						absolutePath = absolutePath
								.substring(0, periodLocation); // removed suffix
					}// inner if
					saveMachineState(absolutePath);
				} else {
					System.out.printf("You cancelled the Save as...%n", "");
				}// if - returnValue
			}// actionPerformed
		});
		mnuFile.add(mnuFileSaveAs);

		mnuFile.addSeparator();

		JMenuItem mnuFileClose = new JMenuItem("Close");
		mnuFileClose.setActionCommand("mnuFileClose");
		mnuFileClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveMachineState();
				System.exit(-1);
			}// actionPerformed
		});
		mnuFile.add(mnuFileClose);

		JMenu mnuTools = new JMenu("Tools");
		menuBar.add(mnuTools);

		JMenuItem mnuToolsLoadMemoryFromFile = new JMenuItem(
				"Load Memory From File...");
		mnuToolsLoadMemoryFromFile.setActionCommand("mnuToolsLoadMemoryFromFile");
		mnuToolsLoadMemoryFromFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				LoadMemoryImage lmi = new LoadMemoryImage(MEMORY_SIZE_BYTES,
						null);
				lmi.doIt();
				HashMap<Integer, Byte> memoryImage = lmi.getMemoryImage();
				int minKey = MEMORY_SIZE_BYTES, maxKey = 0;
				Set<Integer> keys = memoryImage.keySet();
				try {
					for (int key : keys) {
						mm.setByte(key, memoryImage.get(key));
						minKey = (minKey < key) ? minKey : key;
						maxKey = (maxKey > key) ? maxKey : key;
					}// for - key
				} catch (ArrayIndexOutOfBoundsException oobe) {
					JOptionPane
							.showMessageDialog(
									null,
									"Attempted to load into locattion above max memory location",
									"memory size error",
									JOptionPane.ERROR_MESSAGE);
					return; // exit gracefully
				}// try
				int memorySpan = maxKey - minKey;
				memoryStart = (short) minKey;
				memoryLength = (short) memorySpan;
				displayMainMemory();
			}// actionPerformed
		});

		JMenuItem mnuToolsSaveMemoryDisplay = new JMenuItem(
				"Save Memory Display to File...");
		mnuToolsSaveMemoryDisplay.setActionCommand("mnuToolsSaveMemoryDisplay");
		mnuToolsSaveMemoryDisplay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				JFileChooser chooser = new JFileChooser();
				chooser.setMultiSelectionEnabled(false);
				FileNameExtensionFilter filter = new FileNameExtensionFilter(
						"Saved State Files", ".txt", "TXT","mem");
				chooser.setFileFilter(filter);
				int returnValue = chooser.showSaveDialog(frm8080Emulator);

				if (returnValue == JFileChooser.APPROVE_OPTION) {
					String destinationPath = chooser.getSelectedFile()
							.getPath();
					destinationPath = stripSuffix(destinationPath) + ".mem";

					// File destinationFile = chooser.getSelectedFile();

					try {
						FileWriter fileWriter = new FileWriter(destinationPath);
						BufferedWriter writer = new BufferedWriter(fileWriter);
						writer.write(generateMemoryDisplay(memoryStart,
								memoryLength));
						writer.close();
					} catch (Exception e1) {
						e1.printStackTrace();
						return;
					}// try
				}// file was selected
			}// actionPerformed
		});
		mnuTools.add(mnuToolsSaveMemoryDisplay);
		mnuTools.add(mnuToolsLoadMemoryFromFile);
		// Memu
		// -----------------------------------------------------------------------

		JPanel pnlFrontPanel = new JPanel();
		pnlFrontPanel.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		pnlFrontPanel.setBounds(21, 42, 840, 320);
		frm8080Emulator.getContentPane().add(pnlFrontPanel);
		pnlFrontPanel.setLayout(null);

		JPanel pnlRegisters = new JPanel();
		pnlRegisters.setBorder(new TitledBorder(new LineBorder(new Color(0, 0,
				0), 1, true), "Registers", TitledBorder.LEADING,
				TitledBorder.TOP, null, Color.RED));
		pnlRegisters.setBounds(10, 11, 566, 130);
		pnlFrontPanel.add(pnlRegisters);
		pnlRegisters.setLayout(null);

		JPanel pnlReg_A = new JPanel();
		pnlReg_A.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null,
				null, null));
		pnlReg_A.setBounds(8, 20, 60, 80);
		pnlRegisters.add(pnlReg_A);
		pnlReg_A.setLayout(new BorderLayout(0, 0));

		JLabel lblReg_A = new JLabel("A");
		lblReg_A.setHorizontalAlignment(SwingConstants.CENTER);
		lblReg_A.setFont(new Font("Tahoma", Font.PLAIN, 22));
		pnlReg_A.add(lblReg_A, BorderLayout.NORTH);

		ftfReg_A = new JFormattedTextField(format2HexDigits);
		ftfReg_A.addPropertyChangeListener(this);
		ftfReg_A.addMouseListener(this);
		ftfReg_A.addFocusListener(this);
		// ftfReg_A.addFocusListener(new FocusAdapter() {
		// @Override
		// public void focusLost(FocusEvent e) {
		// // ftfReg_A.setEditable(false);
		// JFormattedTextField ftf = (JFormattedTextField) e.getSource();
		// ftf.setEditable(false);
		// }
		// });

		ftfReg_A.setName(NAME_REG_A);
		ftfReg_A.setEditable(false);
		ftfReg_A.setHorizontalAlignment(SwingConstants.CENTER);
		// tftReg_A.setValue("AA");
		ftfReg_A.setFont(new Font("Tahoma", Font.PLAIN, 38));
		pnlReg_A.add(ftfReg_A, BorderLayout.CENTER);
		ftfReg_A.setColumns(2);

		JPanel pnlReg_B = new JPanel();
		pnlReg_B.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null,
				null, null));
		pnlReg_B.setBounds(70, 20, 60, 80);
		pnlRegisters.add(pnlReg_B);
		pnlReg_B.setLayout(new BorderLayout(0, 0));

		JLabel lblReg_B = new JLabel("B");
		lblReg_B.setHorizontalAlignment(SwingConstants.CENTER);
		lblReg_B.setFont(new Font("Tahoma", Font.PLAIN, 22));
		pnlReg_B.add(lblReg_B, BorderLayout.NORTH);

		ftfReg_B = new JFormattedTextField(format2HexDigits);
		ftfReg_B.addPropertyChangeListener(this);
		ftfReg_B.addMouseListener(this);
		ftfReg_B.addFocusListener(this);
		ftfReg_B.setName(NAME_REG_B);
		ftfReg_B.setEditable(false);
		// tftReg_B.setValue("0B");
		ftfReg_B.setHorizontalAlignment(SwingConstants.CENTER);
		ftfReg_B.setFont(new Font("Tahoma", Font.PLAIN, 38));
		ftfReg_B.setColumns(2);
		pnlReg_B.add(ftfReg_B, BorderLayout.SOUTH);

		JPanel pnlReg_C = new JPanel();
		pnlReg_C.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null,
				null, null));
		pnlReg_C.setBounds(132, 20, 60, 80);
		pnlRegisters.add(pnlReg_C);
		pnlReg_C.setLayout(new BorderLayout(0, 0));

		JLabel lblReg_C = new JLabel("C");
		lblReg_C.setHorizontalAlignment(SwingConstants.CENTER);
		lblReg_C.setFont(new Font("Tahoma", Font.PLAIN, 22));
		pnlReg_C.add(lblReg_C, BorderLayout.NORTH);

		ftfReg_C = new JFormattedTextField(format2HexDigits);
		ftfReg_C.addPropertyChangeListener(this);
		ftfReg_C.addMouseListener(this);
		ftfReg_C.addFocusListener(this);
		ftfReg_C.setName(NAME_REG_C);
		ftfReg_C.setEditable(false);
		// tftReg_C.setValue("0C");
		// tftReg_C.setText("0C");
		ftfReg_C.setHorizontalAlignment(SwingConstants.CENTER);
		ftfReg_C.setFont(new Font("Tahoma", Font.PLAIN, 38));
		ftfReg_C.setColumns(2);
		pnlReg_C.add(ftfReg_C, BorderLayout.SOUTH);

		JPanel pnlReg_D = new JPanel();
		pnlReg_D.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null,
				null, null));
		pnlReg_D.setBounds(194, 20, 60, 80);
		pnlRegisters.add(pnlReg_D);
		pnlReg_D.setLayout(new BorderLayout(0, 0));

		JLabel lblReg_D = new JLabel("D");
		lblReg_D.setHorizontalAlignment(SwingConstants.CENTER);
		lblReg_D.setFont(new Font("Tahoma", Font.PLAIN, 22));
		pnlReg_D.add(lblReg_D, BorderLayout.NORTH);

		ftfReg_D = new JFormattedTextField(format2HexDigits);
		ftfReg_D.addPropertyChangeListener(this);
		ftfReg_D.addMouseListener(this);
		ftfReg_D.addFocusListener(this);
		ftfReg_D.setName(NAME_REG_D);
		ftfReg_D.setEditable(false);
		// tftReg_D.setValue("08");
		ftfReg_D.setHorizontalAlignment(SwingConstants.CENTER);
		ftfReg_D.setFont(new Font("Tahoma", Font.PLAIN, 38));
		ftfReg_D.setColumns(2);
		pnlReg_D.add(ftfReg_D, BorderLayout.SOUTH);

		JPanel pnlReg_E = new JPanel();
		pnlReg_E.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null,
				null, null));
		pnlReg_E.setBounds(256, 20, 60, 80);
		pnlRegisters.add(pnlReg_E);
		pnlReg_E.setLayout(new BorderLayout(0, 0));

		JLabel lblReg_E = new JLabel("E");
		lblReg_E.setHorizontalAlignment(SwingConstants.CENTER);
		lblReg_E.setFont(new Font("Tahoma", Font.PLAIN, 22));
		pnlReg_E.add(lblReg_E, BorderLayout.NORTH);

		ftfReg_E = new JFormattedTextField(format2HexDigits);
		ftfReg_E.addPropertyChangeListener(this);
		ftfReg_E.addMouseListener(this);
		ftfReg_E.addFocusListener(this);
		ftfReg_E.setName(NAME_REG_E);
		ftfReg_E.setEditable(false);
		// tftReg_E.setValue("05");
		ftfReg_E.setHorizontalAlignment(SwingConstants.CENTER);
		ftfReg_E.setFont(new Font("Tahoma", Font.PLAIN, 38));
		ftfReg_E.setColumns(2);
		pnlReg_E.add(ftfReg_E, BorderLayout.SOUTH);

		JPanel pnlReg_H = new JPanel();
		pnlReg_H.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null,
				null, null));
		pnlReg_H.setBounds(318, 20, 60, 80);
		pnlRegisters.add(pnlReg_H);
		pnlReg_H.setLayout(new BorderLayout(0, 0));

		JLabel lblReg_H = new JLabel("H");
		lblReg_H.setHorizontalAlignment(SwingConstants.CENTER);
		lblReg_H.setFont(new Font("Tahoma", Font.PLAIN, 22));
		pnlReg_H.add(lblReg_H, BorderLayout.NORTH);

		ftfReg_H = new JFormattedTextField(format2HexDigits);
		ftfReg_H.addPropertyChangeListener(this);
		ftfReg_H.addMouseListener(this);
		ftfReg_H.addFocusListener(this);
		ftfReg_H.setName(NAME_REG_H);
		ftfReg_H.setEditable(false);
		// tftReg_H.setValue("08");
		ftfReg_H.setHorizontalAlignment(SwingConstants.CENTER);
		ftfReg_H.setFont(new Font("Tahoma", Font.PLAIN, 38));
		ftfReg_H.setColumns(2);
		pnlReg_H.add(ftfReg_H, BorderLayout.SOUTH);

		JPanel pnlReg_L = new JPanel();
		pnlReg_L.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null,
				null, null));
		pnlReg_L.setBounds(380, 20, 60, 80);
		pnlRegisters.add(pnlReg_L);
		pnlReg_L.setLayout(new BorderLayout(0, 0));

		JLabel lblReg_L = new JLabel("L");
		lblReg_L.setHorizontalAlignment(SwingConstants.CENTER);
		lblReg_L.setFont(new Font("Tahoma", Font.PLAIN, 22));
		pnlReg_L.add(lblReg_L, BorderLayout.NORTH);

		ftfReg_L = new JFormattedTextField(format2HexDigits);
		ftfReg_L.addPropertyChangeListener(this);
		ftfReg_L.addMouseListener(this);
		ftfReg_L.addFocusListener(this);
		ftfReg_L.setName(NAME_REG_L);
		ftfReg_L.setEditable(false);
		// tftReg_L.setValue("FF");
		ftfReg_L.setHorizontalAlignment(SwingConstants.CENTER);
		ftfReg_L.setFont(new Font("Tahoma", Font.PLAIN, 38));
		ftfReg_L.setColumns(2);
		pnlReg_L.add(ftfReg_L, BorderLayout.SOUTH);

		JPanel pnlReg_M = new JPanel();
		pnlReg_M.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null,
				null, null));
		pnlReg_M.setBounds(475, 20, 60, 80);
		pnlRegisters.add(pnlReg_M);
		pnlReg_M.setLayout(new BorderLayout(0, 0));

		JLabel lblReg_M = new JLabel("M");
		lblReg_M.setHorizontalAlignment(SwingConstants.CENTER);
		lblReg_M.setFont(new Font("Tahoma", Font.PLAIN, 22));
		pnlReg_M.add(lblReg_M, BorderLayout.NORTH);

		ftfReg_M = new JFormattedTextField(format2HexDigits);
		// ftfReg_M.addPropertyChangeListener(this);
		// ftfReg_M.addMouseListener(this);
		// ftfReg_M.addFocusListener(this);
		ftfReg_M.setName(NAME_REG_M);
		ftfReg_M.setEditable(false);
		ftfReg_M.setBackground(Color.LIGHT_GRAY);
		// tftReg_M.setValue("00");
		ftfReg_M.setHorizontalAlignment(SwingConstants.CENTER);
		ftfReg_M.setFont(new Font("Tahoma", Font.PLAIN, 38));
		ftfReg_M.setColumns(2);
		pnlReg_M.add(ftfReg_M, BorderLayout.SOUTH);

		JPanel pnlPointers = new JPanel();
		pnlPointers.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		pnlPointers.setBounds(619, 18, 210, 142);
		pnlFrontPanel.add(pnlPointers);
		pnlPointers.setLayout(null);

		JLabel lblReg_SP = new JLabel("SP");
		lblReg_SP.setFont(new Font("Tahoma", Font.PLAIN, 22));
		lblReg_SP.setBounds(10, 11, 25, 52);
		pnlPointers.add(lblReg_SP);

		ftfReg_PC = new JFormattedTextField(format4HexDigits);
		ftfReg_PC.setInputVerifier(new MemoryLimitVerifier(MEMORY_SIZE_K)); // limit
																			// input
																			// to
																			// size
																			// of
																			// memory
		ftfReg_PC.addPropertyChangeListener(this);
		ftfReg_PC.addMouseListener(this);
		ftfReg_PC.addFocusListener(this);
		ftfReg_PC.setName(NAME_REG_PC);
		ftfReg_PC.setEditable(false);
		ftfReg_PC.setBounds(58, 80, 110, 40);
		pnlPointers.add(ftfReg_PC);
		// tftReg_PC.setValue("1000");
		ftfReg_PC.setHorizontalAlignment(SwingConstants.CENTER);
		ftfReg_PC.setFont(new Font("Tahoma", Font.PLAIN, 38));
		ftfReg_PC.setColumns(4);

		JLabel lblReg_PC = new JLabel("PC");
		lblReg_PC.setBounds(10, 74, 25, 52);
		pnlPointers.add(lblReg_PC);
		lblReg_PC.setFont(new Font("Tahoma", Font.PLAIN, 22));

		ftfReg_SP = new JFormattedTextField(format4HexDigits);
		ftfReg_SP.setHorizontalAlignment(SwingConstants.CENTER);
		ftfReg_SP.setInputVerifier(new MemoryLimitVerifier(MEMORY_SIZE_K)); // limit
																			// input
																			// to
																			// size
																			// of
																			// memory
		ftfReg_SP.addPropertyChangeListener(this);
		ftfReg_SP.addMouseListener(this);
		ftfReg_SP.addFocusListener(this);
		ftfReg_SP.setName(NAME_REG_SP);
		ftfReg_SP.setFont(new Font("Tahoma", Font.PLAIN, 38));
		ftfReg_SP.setEditable(false);
		// ftfReg_SP.setValue("0000");
		ftfReg_SP.setColumns(4);
		ftfReg_SP.setBounds(58, 17, 110, 40);
		pnlPointers.add(ftfReg_SP);

		JPanel pnlRun = new JPanel();
		pnlRun.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		pnlRun.setBounds(619, 170, 210, 142);
		pnlFrontPanel.add(pnlRun);
		pnlRun.setLayout(null);

		JButton btnRun = new JButton("RUN");
		btnRun.setActionCommand("btnRun");
		btnRun.addActionListener(this);
		btnRun.setFont(new Font("Tahoma", Font.PLAIN, 16));
		btnRun.setBounds(10, 23, 90, 42);
		pnlRun.add(btnRun);

		JButton btnStep = new JButton("STEP");
		btnStep.setActionCommand("btnStep");
		btnStep.addActionListener(this);
		btnStep.setFont(new Font("Tahoma", Font.PLAIN, 16));
		btnStep.setBounds(110, 23, 90, 42);
		pnlRun.add(btnStep);

		JButton btnStop = new JButton("STOP");
		btnStop.setActionCommand("btnStop");
		btnStop.addActionListener(this);
		btnStop.setFont(new Font("Tahoma", Font.PLAIN, 16));
		btnStop.setBounds(10, 76, 90, 42);
		pnlRun.add(btnStop);

		spinnerStepCount = new JSpinner();
		spinnerStepCount.setModel(new SpinnerNumberModel(new Integer(1),
				new Integer(1), null, new Integer(1)));
		spinnerStepCount.setFont(new Font("Tahoma", Font.PLAIN, 30));
		spinnerStepCount.setBounds(110, 76, 90, 42);
		pnlRun.add(spinnerStepCount);

		JPanel pnlConditions = new JPanel();
		pnlConditions.setBorder(new TitledBorder(new LineBorder(new Color(0, 0,
				0), 1, true), "Condition Codes", TitledBorder.LEADING,
				TitledBorder.TOP, null, Color.RED));
		pnlConditions.setBounds(20, 153, 556, 67);
		pnlFrontPanel.add(pnlConditions);
		pnlConditions.setLayout(null);

		ckbSign = new JCheckBox("Sign");
		ckbSign.addItemListener(this);
		ckbSign.setName(NAME_FLAG_SIGN);
		ckbSign.setFont(new Font("Tahoma", Font.PLAIN, 16));
		ckbSign.setBounds(28, 21, 58, 23);
		pnlConditions.add(ckbSign);

		ckbZero = new JCheckBox("Zero");
		ckbZero.addItemListener(this);
		ckbZero.setName(NAME_FLAG_ZERO);
		ckbZero.setFont(new Font("Tahoma", Font.PLAIN, 16));
		ckbZero.setBounds(114, 21, 58, 23);
		pnlConditions.add(ckbZero);

		ckbCarry = new JCheckBox("Carry");
		ckbCarry.addItemListener(this);
		ckbCarry.setName(NAME_FLAG_CARRY);
		ckbCarry.setFont(new Font("Tahoma", Font.PLAIN, 16));
		ckbCarry.setBounds(445, 21, 78, 23);
		pnlConditions.add(ckbCarry);

		ckbParity = new JCheckBox("Parity");
		ckbParity.addItemListener(this);
		ckbParity.setName(NAME_FLAG_PARITY);
		ckbParity.setFont(new Font("Tahoma", Font.PLAIN, 16));
		ckbParity.setBounds(332, 21, 85, 23);
		pnlConditions.add(ckbParity);

		ckbAuxCarry = new JCheckBox("Aux Carry");
		ckbAuxCarry.addItemListener(this);
		ckbAuxCarry.setName(NAME_FLAG_AUXCARRY);
		ckbAuxCarry.setFont(new Font("Tahoma", Font.PLAIN, 16));
		ckbAuxCarry.setBounds(200, 21, 104, 23);
		pnlConditions.add(ckbAuxCarry);

		JPanel pnlImmediate = new JPanel();
		pnlImmediate.setBounds(20, 240, 560, 70);
		pnlFrontPanel.add(pnlImmediate);
		pnlImmediate.setLayout(null);

		JLabel lblCurrentByte = new JLabel("Current Byte");
		lblCurrentByte.setHorizontalAlignment(SwingConstants.LEFT);
		lblCurrentByte.setFont(new Font("Tahoma", Font.PLAIN, 20));
		lblCurrentByte.setBounds(0, 11, 112, 40);
		pnlImmediate.add(lblCurrentByte);

		ftfCurrentByte = new JFormattedTextField(format2HexDigits);
		ftfCurrentByte.setBackground(Color.LIGHT_GRAY);
		// ftfCurrentByte.addPropertyChangeListener(this);
		// ftfCurrentByte.addMouseListener(this);
		// ftfCurrentByte.addFocusListener(this);
		ftfCurrentByte.setName(NAME_THIS_BYTE);
		ftfCurrentByte.setEditable(false);
		// tftCurrentByte.setValue("CC");
		ftfCurrentByte.setHorizontalAlignment(SwingConstants.CENTER);
		ftfCurrentByte.setFont(new Font("Tahoma", Font.PLAIN, 35));
		ftfCurrentByte.setColumns(2);
		ftfCurrentByte.setBounds(122, 11, 60, 40);
		pnlImmediate.add(ftfCurrentByte);

		JLabel lblNextByte = new JLabel("Next Byte");
		lblNextByte.setHorizontalAlignment(SwingConstants.LEFT);
		lblNextByte.setFont(new Font("Tahoma", Font.PLAIN, 20));
		lblNextByte.setBounds(184, 11, 92, 40);
		pnlImmediate.add(lblNextByte);

		ftfNextByte = new JFormattedTextField(format2HexDigits);
		ftfNextByte.setBackground(Color.LIGHT_GRAY);
		// ftfNextByte.addPropertyChangeListener(this);
		// ftfNextByte.addMouseListener(this);
		// ftfNextByte.addFocusListener(this);
		ftfNextByte.setName(NAME_NEXT_BYTE);
		// tftNextByte.setValue("FF");
		ftfNextByte.setFont(new Font("Tahoma", Font.PLAIN, 35));
		ftfNextByte.setEditable(false);
		ftfNextByte.setColumns(2);
		ftfNextByte.setBounds(274, 11, 60, 40);
		pnlImmediate.add(ftfNextByte);

		JLabel lblNextWord = new JLabel("Next Word");
		lblNextWord.setHorizontalAlignment(SwingConstants.LEFT);
		lblNextWord.setFont(new Font("Tahoma", Font.PLAIN, 20));
		lblNextWord.setBounds(339, 11, 100, 40);
		pnlImmediate.add(lblNextWord);

		ftfNextWord = new JFormattedTextField(format4HexDigits);
		ftfNextWord.setBackground(Color.LIGHT_GRAY);
		// ftfNextWord.addPropertyChangeListener(this);
		// ftfNextWord.addMouseListener(this);
		// ftfNextWord.addFocusListener(this);
		ftfNextWord.setName(NAME_NEXT_WORD);
		ftfNextWord.setHorizontalAlignment(SwingConstants.CENTER);
		ftfNextWord.setFont(new Font("Tahoma", Font.PLAIN, 35));
		ftfNextWord.setEditable(false);
		ftfNextWord.setColumns(4);
		ftfNextWord.setBounds(440, 11, 120, 40);
		pnlImmediate.add(ftfNextWord);

		JPanel pnlMainMemory = new JPanel();
		pnlMainMemory.setBorder(new TitledBorder(new LineBorder(new Color(0, 0,
				0), 1, true), "Main Memory", TitledBorder.LEADING,
				TitledBorder.TOP, null, Color.RED));
		pnlMainMemory.setBounds(21, 380, 840, 300);
		frm8080Emulator.getContentPane().add(pnlMainMemory);
		pnlMainMemory.setLayout(null);

		JScrollPane scrollMainMemory = new JScrollPane();
		scrollMainMemory.setBounds(17, 55, 600, 230);
		pnlMainMemory.add(scrollMainMemory);

		txtMainMemory = new JTextArea();
		txtMainMemory.setName("txtMainMemory");
		txtMainMemory.addMouseListener(this);
		txtMainMemory.setEditable(false);
		txtMainMemory.setText((String) null);
		txtMainMemory.setRows(10);
		txtMainMemory.setFont(new Font("Monospaced", Font.PLAIN, 14));
		txtMainMemory.setColumns(72);
		txtMainMemory.setCaretPosition(0);
		scrollMainMemory.setViewportView(txtMainMemory);

		JLabel lblStartingLocation = new JLabel("Starting Location");
		lblStartingLocation.setHorizontalAlignment(SwingConstants.LEFT);
		lblStartingLocation.setFont(new Font("Tahoma", Font.PLAIN, 20));
		lblStartingLocation.setBounds(17, 11, 163, 40);
		pnlMainMemory.add(lblStartingLocation);

		ftfMemoryStart = new JFormattedTextField(format4HexDigits);
		ftfMemoryStart.setInputVerifier(new MemoryLimitVerifier(MEMORY_SIZE_K)); // limit
																					// input
																					// to
																					// size
																					// of
																					// memory
		ftfMemoryStart.addPropertyChangeListener(this);
		ftfMemoryStart.addMouseListener(this);
		ftfMemoryStart.addFocusListener(this);
		ftfMemoryStart.setName(NAME_MEM_START);
		ftfMemoryStart.setEditable(false);
		// tftStartingLocation.setValue("9876");
		ftfMemoryStart.setHorizontalAlignment(SwingConstants.CENTER);
		ftfMemoryStart.setFont(new Font("Tahoma", Font.PLAIN, 38));
		ftfMemoryStart.setColumns(4);
		ftfMemoryStart.setBounds(180, 11, 110, 40);
		pnlMainMemory.add(ftfMemoryStart);

		JLabel lblMemoryLength = new JLabel("Length");
		lblMemoryLength.setHorizontalAlignment(SwingConstants.LEFT);
		lblMemoryLength.setFont(new Font("Tahoma", Font.PLAIN, 20));
		lblMemoryLength.setBounds(300, 11, 70, 40);
		pnlMainMemory.add(lblMemoryLength);

		ftfMemoryLength = new JFormattedTextField(format4HexDigits);
		ftfMemoryLength
				.setInputVerifier(new MemoryLimitVerifier(MEMORY_SIZE_K)); // limit
																			// input
																			// to
																			// size
																			// of
																			// memory
		ftfMemoryLength.addPropertyChangeListener(this);
		ftfMemoryLength.addMouseListener(this);
		ftfMemoryLength.addFocusListener(this);
		ftfMemoryLength.setName(NAME_MEM_LEN);
		ftfMemoryLength.setEditable(false);
		// tftMemoryLength.setValue("9876");
		ftfMemoryLength.setHorizontalAlignment(SwingConstants.CENTER);
		ftfMemoryLength.setFont(new Font("Tahoma", Font.PLAIN, 38));
		ftfMemoryLength.setColumns(4);
		ftfMemoryLength.setBounds(370, 11, 110, 40);
		pnlMainMemory.add(ftfMemoryLength);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(640, 55, 180, 230);
		pnlMainMemory.add(scrollPane);

		txtLog = new JTextArea();
		txtLog.setRows(10);
		txtLog.setColumns(15);
		scrollPane.setViewportView(txtLog);
		
		JButton btnRefresh = new JButton("Refresh");
		btnRefresh.setActionCommand("btnRefresh");
		btnRefresh.addActionListener(this);
		btnRefresh.setBounds(490, 13, 110, 35);
		pnlMainMemory.add(btnRefresh);
		//pnlMainMemory.setFocusTraversalPolicy(new FocusTraversalOnArray(new Component[]{ftfMemoryStart, ftfMemoryLength}));
		
//		appInit();
	}// Initialize +++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	// Declarations
	private JFrame frm8080Emulator;
	private JCheckBox ckbSign;
	private JCheckBox ckbZero;
	private JCheckBox ckbAuxCarry;
	private JCheckBox ckbParity;
	private JCheckBox ckbCarry;
	private JFormattedTextField ftfReg_A;
	private JFormattedTextField ftfReg_B;
	private JFormattedTextField ftfReg_C;
	private JFormattedTextField ftfReg_D;
	private JFormattedTextField ftfReg_E;
	private JFormattedTextField ftfReg_H;
	private JFormattedTextField ftfReg_L;
	private JFormattedTextField ftfReg_M;
	private JFormattedTextField ftfReg_PC;
	private JFormattedTextField ftfCurrentByte;
	private JFormattedTextField ftfNextByte;
	private JFormattedTextField ftfNextWord;
	private JFormattedTextField ftfMemoryStart;
	private JFormattedTextField ftfMemoryLength;
	private JFormattedTextField ftfReg_SP;

	private JTextArea txtLog;
	private JSpinner spinnerStepCount;

	private final static String NAME_REG_A = "Reg_A";
	private final static String NAME_REG_B = "Reg_B";
	private final static String NAME_REG_C = "Reg_C";
	private final static String NAME_REG_D = "Reg_D";
	private final static String NAME_REG_E = "Reg_E";
	private final static String NAME_REG_H = "Reg_H";
	private final static String NAME_REG_L = "Reg_L";
	private final static String NAME_REG_M = "Reg_M";
	private final static String NAME_REG_SP = "Reg_SP";
	private final static String NAME_REG_PC = "Reg_PC";

	private final static String NAME_THIS_BYTE = "thisByte";
	private final static String NAME_NEXT_BYTE = "nextByte";
	private final static String NAME_NEXT_WORD = "nextWord";
	private final static String NAME_MEM_START = "memoryStart";
	private final static String NAME_MEM_LEN = "memeoryLength";

	private final static String NAME_FLAG_SIGN = "signFlag";
	private final static String NAME_FLAG_ZERO = "zeroFlag";
	private final static String NAME_FLAG_AUXCARRY = "auxCarryFlag";
	private final static String NAME_FLAG_PARITY = "parityFlag";
	private final static String NAME_FLAG_CARRY = "carryFlag";

	private final static String LF = "\n";
	private final static String DEFAULT_STATE_FILE = "defaultMachineState";
	private final static String FILE_SUFFIX = "ser";
	private final static String FILE_SUFFIX_PERIOD = "." + FILE_SUFFIX;
	private JTextArea txtMainMemory;

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// Not implemented
	}// mouseEntered

	@Override
	public void mouseExited(MouseEvent arg0) {
		// Not implemented
	}// mouseExited

	@Override
	public void mousePressed(MouseEvent arg0) {
		// Not implemented
	}// mousePressed

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// Not implemented
	}// mouseReleased

	@Override
	public void focusGained(FocusEvent arg0) {
		// Not implemented
	}// focusGained


}// class Machine8080

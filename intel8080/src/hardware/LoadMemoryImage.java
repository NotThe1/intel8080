package hardware;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
//import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class LoadMemoryImage {
	private static final int DEFAULT_MEMORY_SIZE = 16 * 1024;
	private HashMap<Integer, Byte> memoryImage;
	private int memorySize;
	private JComponent jc;
	private Scanner scanner;

	public LoadMemoryImage() {
		this(DEFAULT_MEMORY_SIZE);
	}// Constructor - LoadMemoryImage()

	public LoadMemoryImage(int memorySize) {
		this(memorySize, null);
	}// Constructor - LoadMemoryImage()

	public LoadMemoryImage(int memorySize, JComponent jc) {
		this.memorySize = memorySize;
		this.jc = jc;
	}// Constructor - LoadMemoryImage()

	public void doIt() {
		memoryImage = new HashMap<Integer, Byte>();
		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(false);
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"Saved State Files", ".txt", "TXT","mem");
		chooser.setFileFilter(filter);
		int returnValue = chooser.showOpenDialog(jc);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File sourceFile = chooser.getSelectedFile();

			try {
				FileReader fileReader = new FileReader(sourceFile);
				BufferedReader reader = new BufferedReader(fileReader);
				String line;
				while ((line = reader.readLine()) != null) {
					parseAndLoadImage(line);
				}
				reader.close();
			} catch (FileNotFoundException fnfe) {
				JOptionPane.showMessageDialog(jc, sourceFile.getAbsolutePath()
						+ "not found", "unable to locate",
						JOptionPane.ERROR_MESSAGE);
				return; // exit gracefully
			} catch (IOException ie) {
				JOptionPane.showMessageDialog(jc, sourceFile.getAbsolutePath()
						+ ie.getMessage(), "IO error",
						JOptionPane.ERROR_MESSAGE);
				return; // exit gracefully
			}// try - reading from file
		}// if -APPROVE_OPTION
	}// doIt()

	private void parseAndLoadImage(String line) {
		if (line.length() == 0){
			return;		//skip the line
		}//if empty line
		scanner = new Scanner(line);
		String strAddress = scanner.next();
		strAddress = strAddress.replace(":", "");
		// System.out.printf("address = %s%n", strAddress);

		String strValue;
		int value;
		try {
			int address = Integer.valueOf(strAddress, 16);
			if ((address + 0X0F) >= this.memorySize) {
				JOptionPane.showMessageDialog(null,
						"Address out of current memory on address line: "
								+ strAddress, "Out of bounds",
						JOptionPane.ERROR_MESSAGE);
				return;
			}// if address is greater than memory size
			for (int i = 0; i < 16; i++) {
				strValue = scanner.next();
				// System.out.printf("   value = %s%n", strValue);
				value = Integer.valueOf(strValue, 16);
				memoryImage.put(address + i, (byte) value);
			}// for
		} catch (NumberFormatException nfe) {
			JOptionPane.showMessageDialog(null,
					"Data not properly formatted on address line: "
							+ strAddress, "Parse error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}// try - address
	}// parseAndLoadImage

	public HashMap<Integer, Byte> getMemoryImage() {
		return memoryImage;
	}

}// class LoadMemoryImage

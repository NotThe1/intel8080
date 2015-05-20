package hardware;

import java.awt.Color;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

public class MemoryLimitVerifier extends InputVerifier {

	int Kbytes = 1024;
	private static final Color INVALID_COLOR = Color.red;
	private static final Color VALID_COLOR = Color.black;
	private int maxValue; // maximum value

	public MemoryLimitVerifier(int memorySize) {
		this.setMaxValue(memorySize * Kbytes); // memorySize is in K ie 1024
												// bytes
	}// Constructor - MemoryLimitVerifier(memorySize)

	@Override
	public boolean verify(JComponent jc) {
		try {
			String text = ((JTextComponent) jc).getText();
			Integer val = Integer.valueOf(text, 16);
			if (val > maxValue) {
				jc.setForeground(INVALID_COLOR);
				return false;
			}
		} catch (Exception e) {
			jc.setForeground(INVALID_COLOR);
			return false;
		}
		jc.setForeground(VALID_COLOR);
		return true;
	}// verify

	public int getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}

}// class MemoryLimitVerifier

package hardware;

import java.io.Serializable;

import javax.swing.JOptionPane;

//need to throw address out of bounds error, protection violation
public class MainMemory implements Serializable, MemoryAccessErrorListener,
		MemoryTrapListener {

	private static final long serialVersionUID = 1L;
	int K = 1024;
	int PROTECTED_MEMORY = 0; // 100;
	int MINIMUM_MEMORY = 8 * K;
	int MAXIMUM_MEMORY = 64 * K;
	int DEFAULT_MEMORY = 16 * K;

	// private byte[] memory;
	Core core;
//	private int allocatedMemory;

	public MainMemory(Core core) {
		this.core = core;
		core.addMemoryAccessErrorListener(this);
		core.addMemoryTrapListener(this);

//		allocatedMemory = core.getSize();
	}

	public byte getByte(int location) {
			return core.read(location);
	}// getByte

	public void setByte(int location, byte value) {
		core.write(location, value);
	}// putByte

	public int getWord(int location) {

		int hiByte = (core.read(location) << 8) & 0XFF00;
		int loByte = core.read(location +1) & 0X00FF;
		return 0XFFFF & (hiByte + loByte);
	}// getWord

	/*
	 * reverses the order of the immediate word byte 2 is lo byte byte 3 is hi
	 * byte
	 */
	public int getWordReversed(int location) {

		int loByte = (core.read(location + 1) << 8) & 0XFF00;
		int hiByte = core.read(location) & 0X00FF;
		return 0XFFFF & (hiByte + loByte);

	}// getWord

	public void setWord(int location, byte hiByte, byte loByte) {
		core.write(location, hiByte);
		core.write(location + 1, loByte);

	}// putWord

	public void pushWord(int location, byte hiByte, byte loByte) {
		core.write(location - 1, hiByte);
		core.write(location - 2, loByte);
	}// pushWord used for stack work

	public int popWord(int location) {
		return (int) ((core.read(location)) + ((int) core.read(location+1) <<8));
		//return (int) ((memory[location] & 0XFF) + ((int) memory[location + 1] << 8));
	}// popWord

	

	public int getMemorySizeInBytes() {
		return core.getSize();
	}// getSize

	public int getSizeInK() {
		return (int) (core.getSize() / K);
	}// getSize

	@Override
	public void memoryTrap(MemoryTrapEvent mte) {
		JOptionPane.showConfirmDialog(null, "choose one", "choose one",
				JOptionPane.YES_NO_OPTION);
	}

	@Override
	public void memoryAccessError(MemoryAccessErrorEvent mae) {
		System.err.printf("%n%nFatal memory error%n%n");
		System.err.printf(String.format("Location: %s%n", mae.getLocation()));
		System.err.printf(String.format("%s%n", mae.getMessage()));
		// System.exit(-1);

	}

}// class MainMemory

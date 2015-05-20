package hardware;

import java.io.Serializable;
//import java.util.HashMap;
import java.util.Vector;

//need to throw address out of bounds error, protection violation
public class MainMemory implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int K = 1024;
	int PROTECTED_MEMORY = 0; // 100;
	int MINIMUM_MEMORY = 8 * K;
	int MAXIMUM_MEMORY = 64 * K;
	int DEFAULT_MEMORY = 16 * K;

	private byte[] memory;
	private int allocatedMemory;

	public MainMemory(int sizeInK) {
		// check size of memory
		allocatedMemory = ((sizeInK * K > MAXIMUM_MEMORY) || (sizeInK * K < MINIMUM_MEMORY)) ? DEFAULT_MEMORY : sizeInK
				* K;
		memory = new byte[allocatedMemory];
	}// Constructor - MainMemory(size)

	public MainMemory() {
		allocatedMemory = DEFAULT_MEMORY;
		memory = new byte[allocatedMemory];
	}// Constructor - MainMemory(size)

	public byte getByte(int location) {
		location = location & 0XFFFF;
		checkAddress(location);
		try {
			return memory[location];
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("location = " + location);
			System.exit(-1);
			return (byte) 0;
		}
	}// getByte

	public void setByte(int location, byte value) {
		location = location & 0XFFFF;
		checkAddress(location);
		memory[location] = value;
	}// putByte

	public int getWord(int location) {
		location = location & 0XFFFF;
		checkAddress(location - 1);
		int hiByte = (memory[location] << 8) & 0XFF00;
		int loByte = memory[location + 1] & 0X00FF;
		return hiByte + loByte;
	}// getWord

	/*
	 * reverses the order of the immediate word byte 2 is lo byte byte 3 is hi
	 * byte
	 */
	public int getWordReversed(int location) {
		location = location & 0XFFFF;
		checkAddress(location - 1);
		
		int loByte = (memory[location+1] << 8) & 0XFF00;
		int hiByte = memory[location ] & 0X00FF;
		return hiByte + loByte;

	}// getWord

	public void setWord(int location, byte hiByte, byte loByte) {
		location = location & 0XFFFF;
		checkAddress(location - 1);
		memory[location] = hiByte;
		memory[location + 1] = loByte;
	}// putWord

	public void pushWord(int location, byte hiByte, byte loByte) {
		location = location & 0XFFFF;
		checkAddress(location);
		memory[location - 1] = hiByte;
		memory[location - 2] = loByte;
	}// pushWord used for stack work

	public short popWord(int location) {
		location = location & 0XFFFF;
		return (short) ((memory[location] & 0XFF) + ((short) memory[location + 1] << 8));
	}// popWord

	private void checkAddress(int location) {
		int shortLocation = (location & 0XFFFF);
		if (shortLocation < PROTECTED_MEMORY) {
			// protection violation
			fireProtectedMemoryAccess(location);

		} else if (shortLocation > allocatedMemory) {
			// out of bounds error
			fireInvalidMemoryAccess(location);

		}// if
		return; // all is good
	}// checkAddress

	// Handle memory events
	private Vector<MemoryListener> memoryListeners = new Vector<MemoryListener>();

	public synchronized void addMemoryListener(MemoryListener ml) {
		if (memoryListeners.contains(ml)) {
			return; // Already here
		}// if
		memoryListeners.addElement(ml);
	}// addMemoryListener

	public synchronized void removeMemoryListener(MemoryListener ml) {
		memoryListeners.remove(ml);
	}// removeMemoryListener

	private void fireProtectedMemoryAccess(int location) {
		Vector<MemoryListener> mls;
		synchronized (this) {
			mls = (Vector<MemoryListener>) memoryListeners.clone();
		}// sync
		int size = mls.size();
		if (0 == size) {
			return; // no listeners
		}// if
		MemoryEvent memoryEvent = new MemoryEvent(this, location);
		for (int i = 0; i < size; i++) {
			MemoryListener listener = (MemoryListener) mls.elementAt(i);
			listener.protectedMemoryAccess(memoryEvent);
		}// for
	}// fireProtectedMemoryAccess

	private void fireInvalidMemoryAccess(int location) {
		Vector<MemoryListener> mls;
		synchronized (this) {
			mls = (Vector<MemoryListener>) memoryListeners.clone();
		}// sync
		int size = mls.size();
		if (0 == size) {
			return; // no listeners
		}// if
		MemoryEvent memoryEvent = new MemoryEvent(this, location);
		for (int i = 0; i < size; i++) {
			MemoryListener listener = (MemoryListener) mls.elementAt(i);
			listener.invalidMemoryAccess(memoryEvent);
		}// for
	}// fireProtectedMemoryAccess

	// private void fireInvalidMemoryAccess(){
	//
	// }//fireProtectedMemoryAccess

	public int getMemorySizeInBytes() {
		return allocatedMemory;
	}// getSize

	public int getSizeInK() {
		return (int) (allocatedMemory / K);
	}// getSize

}// class MainMemory

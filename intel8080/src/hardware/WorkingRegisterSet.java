package hardware;

import java.io.Serializable;
import java.util.HashMap;

public class WorkingRegisterSet implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int stackPointer = 0000;

	private HashMap<Reg, Byte> registers;

	public WorkingRegisterSet() {
		registers = new HashMap<Reg,Byte>();
		initialize();
	}// Constructor - Register()

	public void initialize() {
		Byte b = 0;
		for (Reg r : Reg.values()) {
			//getRegisters().put(r, b);
			registers.put(r, b);
		}// load all the registers with zeros
		stackPointer = 0100; // set to a non zero value
	}//initialize

	/**
	 * @return the stackPointer
	 */
	protected int getStackPointer() {
		return stackPointer;
	}// getStackPointer

	/**
	 * @param stackPointer
	 *            the stackPointer to set
	 */
	protected void setStackPointer(int stackPointer) {
		this.stackPointer = stackPointer;
	}// setStackPointer

	protected void setStackPointer(byte hiByte, byte loByte) {
		this.stackPointer = (int) (((hiByte << 8) + loByte) & 0XFFFF);
	}//setStackPointer

	public void setReg(Reg reg, byte value) {
		registers.put(reg, value);
	}// loadReg

	public byte getReg(Reg reg) {
		return registers.get(reg);
	}// getReg

	public void setDoubleReg(Reg reg, int value) {
		// should only be for HL
		int hi = value & 0XFF00;
		byte hiByte = (byte) ((hi >> 8) & 0XFF);
		byte loByte = (byte) (value & 0XFF);

		switch (reg) {
		case BC:
			setReg(Reg.B, hiByte);
			setReg(Reg.C, loByte);		
			break;
		case DE:
			setReg(Reg.D, hiByte);
			setReg(Reg.E, loByte);
			break;
		case HL:
			setReg(Reg.H, hiByte);
			setReg(Reg.L, loByte);
			break;
		case SP:
			setStackPointer(value);
			break;
		default:
			break;
		}// switch
		return;
	}// setDoubleReg

	public int getDoubleReg(Reg reg) {
		byte hi = 0;
		byte lo = 0;

		switch (reg) {
		case BC:
			hi = this.getReg(Reg.B);
			lo = this.getReg(Reg.C);
			break;
		case DE:
			hi = this.getReg(Reg.D);
			lo = this.getReg(Reg.E);
			break;
		case HL:
		case M:
			hi = this.getReg(Reg.H);
			lo = this.getReg(Reg.L);
			break;
		case SP:
			return this.getStackPointer();
			// exits here for SP
		default:
			// just use 0;
		}// switch
		int result =  (((hi << 8) + (lo & 0XFF)) & 0XFFFF);
		// short hiShort = (short) ((hi << 8) & 0XFF00);
		// result = (short) ((hiShort | lo) & 0XFFFF);
		return result;
	}// getDoubleReg

}// class WorkingRegisterSet

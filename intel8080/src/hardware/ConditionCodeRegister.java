package hardware;

import java.io.Serializable;

public class ConditionCodeRegister implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// private short programCounter = 0000;;
	private boolean auxilaryCarryFlag = false;
	private boolean carryFlag = false;
	private boolean signFlag = false; // set to most signifcant bit (7)
	private boolean parityFlag = false; // set to one if even parity, reset if
										// odd
	private boolean zeroFlag = false; // set if result = 0;

	public ConditionCodeRegister() {

	}// Constructor - ConditionCodeRegister()

	public byte getConditionCode() {
		// |7|6|5|4|3|2|1|0
		// |S|Z|0|A|0|P|1|C

		byte conditionCode = (byte) 0B00000010;
		conditionCode = (byte) ((signFlag) ? conditionCode | MASK_SIGN
				: conditionCode & MASK_SIGN_NOT);
		conditionCode = (byte) ((zeroFlag) ? conditionCode | MASK_ZERO
				: conditionCode & MASK_ZERO_NOT);
		conditionCode = (byte) ((auxilaryCarryFlag) ? conditionCode
				| MASK_AUX_CARRY : conditionCode & MASK_AUX_CARRY_NOT);
		conditionCode = (byte) ((parityFlag) ? conditionCode | MASK_PARITY
				: conditionCode & MASK_PARITY_NOT);
		conditionCode = (byte) ((carryFlag) ? conditionCode | MASK_CARRY
				: conditionCode & MASK_CARRY_NOT);

		return conditionCode;
	}// getConditionCode
	
	public void setConditionCode(byte flags){
		// |7|6|5|4|3|2|1|0
		// |S|Z|0|A|0|P|1|C
		
		setSignFlag( (flags & MASK_SIGN) == MASK_SIGN);
		setZeroFlag((flags & MASK_ZERO) == MASK_ZERO);
		setAuxilaryCarryFlag((flags & MASK_AUX_CARRY) == MASK_AUX_CARRY);
		setParityFlag((flags & MASK_PARITY) == MASK_PARITY);
		setCarryFlag((flags & MASK_CARRY) == MASK_CARRY);

	}//setConditionCode
	
	/*
	 * set Bits for Zero, Sign, and Parity
	 */
	public void setZSP(byte value) {
		this.setZeroFlag(value == 0);
		this.setSignFlag((value & MASK_SIGN) != 0);
		this.setParityFlag((Integer.bitCount(value) % 2) == 0);
	}// setZSP

	public void clearAllCodes() {
		auxilaryCarryFlag = false;
		carryFlag = false;
		signFlag = false;
		parityFlag = false;
		zeroFlag = false;

	}//clearAllCodes

	/**
	 * @return the auxilaryCarryFlag
	 */
	public boolean isAuxilaryCarryFlagSet() {
		return auxilaryCarryFlag;
	}// isAuxilaryCarryFlagSet

	/**
	 * @param auxilaryCarryFlag
	 *            the auxilaryCarryFlag to set
	 */
	public void setAuxilaryCarryFlag(boolean auxilaryCarryFlag) {
		this.auxilaryCarryFlag = auxilaryCarryFlag;
	}// setAuxilaryCarryFlag

	/**
	 * @return the carryFlag
	 */
	public boolean isCarryFlagSet() {
		return carryFlag;
	}// isCarryFlagSet

	/**
	 * @param carryFlag
	 *            the carryFlag to set
	 */
	public void setCarryFlag(boolean carryFlag) {
		this.carryFlag = carryFlag;
	}// setCarryFlag

	/**
	 * @return the signFlag
	 */
	public boolean isSignFlagSet() {
		return signFlag;
	}// isSignFlagSet

	/**
	 * @param signFlag
	 *            the signFlag to set
	 */
	void setSignFlag(boolean signFlag) {
		this.signFlag = signFlag;
	}// setSignFlag

	/**
	 * @return the parityFlag
	 */
	public boolean isParityFlagSet() {
		return parityFlag;
	}// isParityFlagSet

	/**
	 * @param parityFlag
	 *            the parityFlag to set
	 */
	void setParityFlag(boolean parityFlag) {
		this.parityFlag = parityFlag;
	}// setParityFlag

	/**
	 * @return the zeroFlag
	 */
	public boolean isZeroFlagSet() {
		return zeroFlag;
	}// isZeroFlagSet

	/**
	 * @param zeroFlag
	 *            the zeroFlag to set
	 */
	void setZeroFlag(boolean zeroFlag) {
		this.zeroFlag = zeroFlag;
	}// setZeroFlag
	
	public static byte MASK_SIGN = (byte) 0B10000000;
	public static byte MASK_SIGN_NOT = (byte) 0B01111111;
	public static byte MASK_ZERO = (byte) 0B01000000;
	public static byte MASK_ZERO_NOT = (byte) 0B10111111;
	public static byte MASK_AUX_CARRY = (byte) 0B00010000;
	public static byte MASK_AUX_CARRY_NOT = (byte) 0B11101111;
	public static byte MASK_PARITY = (byte) 0B00000100;
	public static byte MASK_PARITY_NOT = (byte) 0B11111011;
	public static byte MASK_CARRY = (byte) 0B00000001;
	public static byte MASK_CARRY_NOT = (byte) 0B11111110;


}// ConditionCodeRegister

package hardware;

public class ArithmeticUnit {
	private ConditionCodeRegister ccr;

	public ArithmeticUnit(ConditionCodeRegister ccr) {
		this.ccr = ccr;
	}// Constructor - AritmeticUnit
/*
	public ArithmeticUnit() {

	}// Constructor - AritmeticUnit
*/
	@SuppressWarnings("unused")
	private boolean getAuxilaryCarryFlag() {
		return ccr.isAuxilaryCarryFlagSet();

	}// getAuxilaryCarry

	private boolean getCarryFlag() {
		return ccr.isCarryFlagSet();
	}// getStandardCarry

	/*
	 * for testing only make private************
	 */
	private void setCarryFlag(boolean state) {
		//carryFlag = state;
		ccr.setCarryFlag(state);
	}// test only - setCarry

	/*
	 * additon : operand1 + operand2 = result
	 */
	public byte add(byte operand1, byte operand2) {
		byte result = (byte) add((int) operand1, (int) operand2, Byte.SIZE);
		ccr.setZSP(result);
		return result;
	}// add(byte operand1, byte operand2)

	public int add(int operand1, int operand2) { // add words
		return  add( operand1,  operand2,16);
		//set Carry , but does not set Sign, Zero or Parity
	}// add(short operand1, short operand2)

	public byte addWithCarry(byte operand1, byte operand2) {
		//refactored - confirm new logic vs Pass2
		boolean carryFlagIn = ccr.isCarryFlagSet();
		boolean carryFromIncrement = false;
		if (carryFlagIn) {
			operand2 =  (byte) this.add(operand2, 1, Byte.SIZE);	
			carryFromIncrement = ccr.isCarryFlagSet(); // carry flag from increment
		}// if carryFlagIn

		byte result = (byte) add((int) operand1, (int) operand2, Byte.SIZE);
		boolean carryFlagOut = ccr.isCarryFlagSet()|carryFromIncrement;// carry is true if either flag is set
		ccr.setCarryFlag(carryFlagOut); 
		ccr.setZSP(result);
		return result;
	}// add(byte operand1, byte operand2)

	private int add(int operand1, int operand2, int wordSize) {
		int fullMask;
		if (wordSize == Byte.SIZE) {// need to handle Auxilary Carry
			boolean auxilaryCarryFlag = carryOut(operand1, operand2, 0X000F);
			ccr.setAuxilaryCarryFlag(auxilaryCarryFlag);
			fullMask = 0X00FF; // two nibbles
		} else {
			fullMask = 0XFFFF; // two bytes
		}//
		boolean carryFlag = carryOut(operand1, operand2, fullMask);
		ccr.setCarryFlag(carryFlag);

		return operand1 + operand2;
	}// add

	/*
	 * support method for addition mask: Auxilary Carry = 0X0F; Carrry for Byte
	 * = 0XFF, Carrry for dWORD = 0XFFFF
	 */
	private boolean carryOut(int operand1, int operand2, int mask) {
		int result = (operand1 & mask) + (operand2 & mask);
		return (result > mask) ? true : false;
	}// carryOut

	/*
	 * subtraction : minuend - subtrahend = difference
	 */
	private int subtract(int minuend, int subtrahend, int wordSize) {
		int takeAway = ~subtrahend + 1; // twos complement
		int result = add(minuend, takeAway, wordSize);
		boolean carryFlag = !ccr.isCarryFlagSet();
		ccr.setCarryFlag(carryFlag);
		return result;
	}// subtract(int minuend, int subtrahend, int wordSize)

	public byte subtract(byte minuend, byte subtrahend) {
		byte result = (byte) subtract((int) minuend, (int) subtrahend, Byte.SIZE);
		ccr.setZSP(result);
		return result;
	}// subtract(byte minuend,byte subtrahend)

	public short subtract(short minuend, short subtrahand) {
		return (short) subtract((int) minuend, (int) subtrahand, Short.SIZE);
	}// subtract(short minuend,short subtrahand)

	public byte subtractWithBorrow(byte minuend, byte subtrahand) {
		//refactored confirm vs Pass2
		boolean carryFromIncrement = false;
		boolean carryFlagIn = ccr.isCarryFlagSet();
		if (carryFlagIn) {// add carry to the subtrhend, the do the subtraction
			subtrahand =  (byte) this.add(subtrahand, 1, Byte.SIZE) ;
			carryFromIncrement = ccr.isCarryFlagSet(); // carry flag from increment
		}// if carryFlag

		byte result = (byte) subtract((int) minuend, (int) subtrahand,
				Byte.SIZE);
		boolean carryFlag = ccr.isCarryFlagSet() | carryFromIncrement; // carry is true if either flag is set
		ccr.setCarryFlag(carryFlag);
		ccr.setZSP(result);
		return result;
	}// add(byte operand1, byte operand2)
		// Simple Increment operations

	/*
	 * Incremet values
	 */
	public byte increment(byte value) {
		byte result =(byte) add((int) value, 1, Byte.SIZE);
		ccr.setZSP(result);											
		return result;
	}// increment(byte value)

	public short increment(short value) {
		return (short) add((int) value, 1, Short.SIZE);
	}// increment(byte value)
	/*
	 * Deccremet values
	 */
	public byte decrement(byte value) {
		byte result =(byte) subtract((int) value, 1, Byte.SIZE);
		ccr.setZSP(result);											
		return result;
	}// increment(byte value)

	public short decrement(short value) {
		return (short) subtract((int) value, 1, Short.SIZE);
	}// dencrement(byte value)
	
		// Shift Operations ////////////////////////////////

	/*
	 * Rotate left - thruCarry = true for RAL, false for RLC
	 */

	public byte rotateLeft(byte source, boolean thruCarry) {
		boolean oldFlag = this.getCarryFlag();
		boolean originalBit7Set = ((source & 0X80) != 0) ? true : false;

		int s = (int) source << 1;
		this.setCarryFlag(originalBit7Set);
		if (thruCarry) { // rotate thru carry
			s = oldFlag ? (s | 0X01) : s & 0XFE;
		} else {
			s = originalBit7Set ? (s | 0X01) : s & 0XFE;
		}// if for Bit0

		return (byte) (s & (byte) 0XFF);
	};// rotateLeft

	/*
	 * Rotate right - thruCarry = true for RAL, false for RLC
	 */

	public byte rotateRight(byte source, boolean thruCarry) {
		boolean oldFlag = this.getCarryFlag();
		boolean originalBit0Set = ((source & 0X01) != 0) ? true : false;

		int s = (source >> 1) & 0X7F;
		this.setCarryFlag(originalBit0Set);
		if (thruCarry) { // rotate thru carry
			s = oldFlag ? (s | 0X80) : s & 0X7F;
		}// if for thruCarry
		return (byte) (s & (byte) 0XFF);
	};// rotateRight

	// Logical operations //////////////////////////

	/*
	 * complements ( one's complement) value
	 */
	public byte complement(byte value) {
		return (byte) ~value;
	}// complement

	/*
	 * Logical AND : ANA & ANI
	 */
	public byte logicalAnd(byte operand1, byte operand2) {
		byte result = (byte) (operand1 & operand2);
		this.setCarryFlag(false);
		ccr.setZSP(result);
		return result;
	}// logicalAnd

	/*
	 * Logical XOR : ANA & ANI
	 */
	public byte logicalXor(byte operand1, byte operand2) {
		byte result = (byte) (operand1 ^ operand2);
		this.setCarryFlag(false);
		ccr.setZSP(result);
		return result;
	}// logicalAnd

	/*
	 * Logical OR : ORA & ORI
	 */
	public byte logicalOr(byte operand1, byte operand2) {
		byte result = (byte) (operand1 | operand2);
		this.setCarryFlag(false);
		ccr.setZSP(result);
		return result;
	}// logicalAnd
	public byte decimalAdjustByte(byte value){
		byte loNibble = (byte) (value & 0X0F);
		byte ans = value;
		boolean auxCarryTemp = ccr.isAuxilaryCarryFlagSet();
		boolean carryTemp = ccr.isCarryFlagSet();
//		System.out.printf("value = %04X, , loNibble =%04X%n",value,loNibble);
		if((loNibble > 9) || auxCarryTemp){
			ans = this.add(value, (byte) 0X06);
			auxCarryTemp = ccr.isAuxilaryCarryFlagSet(); // remember  the Aux flag for exit
		}//if 
		byte hiNibble = (byte) ((ans & 0XF0) >>4);
//		System.out.printf("step1 = %04X, , hiNibble =%04X%n",ans,hiNibble);
		if ((hiNibble > 9) || carryTemp)  {// carry when we enered the operation
			ans = this.add(ans, (byte) 0X60);
		}
		ccr.setAuxilaryCarryFlag(auxCarryTemp);
//		System.out.printf("step1 = %04X, AuxCarry = %s, Carry = %s%n",ans,ccr.isAuxilaryCarryFlagSet(),ccr.isCarryFlagSet());
		return ans;
	}//
	
	public void setCarry(){
		ccr.setCarryFlag(true);
	}//setCarry
	public void complementCarry(){
		ccr.setCarryFlag(!ccr.isCarryFlagSet());
	}//complementCarry

}// class ArithmeticUnit

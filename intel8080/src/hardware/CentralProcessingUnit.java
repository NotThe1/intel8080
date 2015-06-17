package hardware;

import javax.swing.JOptionPane;

import device.DeviceController;


public class CentralProcessingUnit implements MemoryListener {

	private boolean running = false;
	// private boolean stepMode = false;
	// private int stepCount = 0;

	int page;
	int yyy;
	int zzz;

	// for debug only
	byte opCode;
	// int memoryBase = 1024;

	private static final int MEMORY_SIZE = 16; // in K
	private int memorySizeInBytes;

	private MainMemory mm;
	private WorkingRegisterSet wrs;
	private ConditionCodeRegister ccr;
	private ArithmeticUnit au;
	private DeviceController dc;

	// private int stackPointer;
	private int programCounter;

	private Reg[] registerDecode = { Reg.B, Reg.C, Reg.D, Reg.E, Reg.H, Reg.L,
			Reg.M, Reg.A };
	private Reg[] registerPairDecodeSet1 = { Reg.BC, Reg.DE, Reg.HL, Reg.SP };
	private Reg[] registerPairDecodeSet2 = { Reg.BC, Reg.DE, Reg.HL, Reg.AF };
	// private String[] conditionDecode = { "NZ", "Z", "NC", "C", "PO", "PE",
	// "P", "M" };
	private CodeConditional[] conditionDecode = { CodeConditional.NZ,
			CodeConditional.Z, CodeConditional.NC, CodeConditional.C,
			CodeConditional.PO, CodeConditional.PE, CodeConditional.P,
			CodeConditional.M };

	public CentralProcessingUnit() {
		mm = new MainMemory(new Core(MEMORY_SIZE));
		this.memorySizeInBytes = mm.getMemorySizeInBytes();
		ccr = new ConditionCodeRegister();
		au = new ArithmeticUnit(ccr);
		wrs = new WorkingRegisterSet();
	}// Constructor - CentralProcessingUnit

	public CentralProcessingUnit(MainMemory mm, ConditionCodeRegister ccr,
			ArithmeticUnit au, WorkingRegisterSet wrs, DeviceController dc) {
		this.mm = mm;
		this.memorySizeInBytes = mm.getMemorySizeInBytes();
		this.ccr = ccr;
		this.au = au;
		this.wrs = wrs;
		this.dc = dc;
	}// Constructor - CentralProcessingUnit(mm,ccr,au,wrs)

	public void startStepMode(int stepCount) {
		int opCodeLength = 0;
		setRunning(true);
		for (int count = stepCount; count > 0; count--) {
			opCode = mm.getByte(programCounter);
			opCodeLength = execute8080Instruction(opCode);
			incrementProgramCounter(opCodeLength);
			if (!running) {
				break;
			}//
		}// for
		setRunning(false);
	}// startStepMode

	public void startRunMode() {
		int opCodeLength = 0;
		setRunning(true);
		while (running) {
			opCode = mm.getByte(programCounter);
			opCodeLength = execute8080Instruction(opCode);
			incrementProgramCounter(opCodeLength);
		}// while
	}// startRunMode

	public boolean isRunning() {
		return running;
	}// isRunning

	public void setRunning(boolean state) {
		running = state;
	}// setRunning

	public int getProgramCounter() {
		return programCounter;
	}// getProgramCounter

	public void setProgramCounter(int value) {
		this.programCounter = value;
	}// setProgramCounter

	public void incrementProgramCounter(int count) {
		this.programCounter += count;
	}// incrementProgramCounter

	private int execute8080Instruction(byte opCode) {
		// XX YYY ZZZ : XX = page number
		page = (opCode >> 6) & 0X03;
		yyy = (opCode >> 3) & 0X07;
		zzz = opCode & 0X07;
		int instructionLength = 0;

		// System.out.printf("opCode %02X is on page %02X%n", opCode, page);
		switch (page) {
		case 0: // 00
			instructionLength = opCodePage00(yyy, zzz);
			break;
		case 1: // 0
			instructionLength = opCodePage01(yyy, zzz);
			break;
		case 2: // 10
			instructionLength = opCodePage02(yyy, zzz);
			break;
		case 3: // 11
			instructionLength = opCodePage03(yyy, zzz);
			break;
		default:
			badOpCode();
		}// switch
		return instructionLength;
	}// decode8080Instruction

	private int opCodePage00(int yyy, int zzz) {
		// 00 YYY ZZZ : ZZZ = instructionPart1
		// System.out.printf("ZZZ = %02X%n", zzz);
		// identifyOpCode(ShowOpCode.None);
		Reg regPair;
		Reg regSingle;
		int memoryLocation;
		byte value;
		int opCodeSize = 0;

		switch (zzz) {
		case 0: // zzz = 000 : 08,10,18,20,28,30,38 - not implemented
			if (yyy == 0) {// its a NOP OO len =1, cycles = 4
				// System.out.printf("NOP %s%n", "");
			} else {// treat as if it is a NOP
				// System.out.printf("*NOP %s%n", "");
			}//
			opCodeSize = 1;
			break;
		case 1: // zzz = 001
			regPair = registerPairDecodeSet1[yyy >> 1]; // only want the two
														// high bits
			if ((yyy & 0X1) == 0) { // LXI Register Pair len =3, cycles = 10
				byte hiByte = mm.getByte(programCounter + 1);
				byte loByte = mm.getByte(programCounter + 2);
				switch (regPair) {
				case BC:
					wrs.setReg(Reg.B, loByte);
					wrs.setReg(Reg.C, hiByte);
					break;
				case DE:
					wrs.setReg(Reg.D, loByte);
					wrs.setReg(Reg.E, hiByte);
					break;
				case HL:
					wrs.setReg(Reg.H, loByte);
					wrs.setReg(Reg.L, hiByte);
					break;
				case SP:
					wrs.setStackPointer(loByte, hiByte);
					break;
				default:
					badmemoryOpCode(programCounter);
				}// switch
				opCodeSize = 3;
				// System.out.printf("LXI %s%n", regPair);
			} else { // DAD RegisterPair len =1, cycles = 10
				// System.out.printf("DAD %s%n", regPair);
				ccr.clearAllCodes();
				int operand1 = wrs.getDoubleReg(regPair);
				int operand2 = wrs.getDoubleReg(Reg.HL);
				int result = au.add(operand1, operand2);
				wrs.setDoubleReg(Reg.HL, result);
				opCodeSize = 1;
			}// if
			break;
		case 2: // zzz = 010
			// int memoryLocation;
			switch (yyy) {
			case 0: // yyy = 000 STAX B len =1, cycles = 1
				memoryLocation = wrs.getDoubleReg(Reg.BC);
				if (!isValidMemoryLocation(memoryLocation)) {
					reportInvalidMemoryReference(memoryLocation);
				} else {
					mm.setByte(memoryLocation, wrs.getReg(Reg.A));
					opCodeSize = 1;
				}// if
					// System.out.printf("STAX %s%n", "B");
				break;
			case 1: // yyy = 001 LDAX B len =1, cycles = 1
				memoryLocation = wrs.getDoubleReg(Reg.BC);
				if (!isValidMemoryLocation(memoryLocation)) {
					reportInvalidMemoryReference(memoryLocation);
				} else {
					wrs.setReg(Reg.A, mm.getByte(memoryLocation));
					opCodeSize = 1;
				}// if
					// System.out.printf("LDAX %s%n", "B");
				break;
			case 2: // yyy = 010 STAX D len =1, cycles = 1
				memoryLocation = wrs.getDoubleReg(Reg.DE);
				if (!isValidMemoryLocation(memoryLocation)) {
					reportInvalidMemoryReference(memoryLocation);
				} else {
					mm.setByte(memoryLocation, wrs.getReg(Reg.A));
					opCodeSize = 1;
				}// if
					// System.out.printf("STAX %s%n", "D");
				break;
			case 3: // yyy = 011 LDAX D len =1, cycles = 1
				memoryLocation = wrs.getDoubleReg(Reg.DE);
				if (!isValidMemoryLocation(memoryLocation)) {
					reportInvalidMemoryReference(memoryLocation);
				} else {
					wrs.setReg(Reg.A, mm.getByte(memoryLocation));
					opCodeSize = 1;
				}// if // System.out.printf("LDAX %s%n", "D");
				break;
			case 4: // yyy = 100 SHLD len =3, cycles = 6
				memoryLocation = mm.getWordReversed(programCounter + 1);
				if (!isValidMemoryLocation(memoryLocation)) {
					reportInvalidMemoryReference(memoryLocation + 1);
				} else {
					mm.setByte(memoryLocation, wrs.getReg(Reg.L));
					mm.setByte(memoryLocation + 1, wrs.getReg(Reg.H));
					opCodeSize = 3;
				}//
					// System.out.printf("SHLD %s%n", "");
				break;
			case 5: // yyy = 101 LHLD len =3, cycles = 6
				memoryLocation = mm.getWordReversed(programCounter + 1);
				if (!isValidMemoryLocation(memoryLocation)) {
					reportInvalidMemoryReference(memoryLocation + 1);
				} else {
					wrs.setReg(Reg.L, mm.getByte(memoryLocation));
					wrs.setReg(Reg.H, mm.getByte(memoryLocation + 1));
					opCodeSize = 3;
				}//
				break;
			// System.out.printf("LHLD %s%n", "");
			case 6: // yyy = 110 STA len =3, cycles = 13
				memoryLocation = mm.getWordReversed(programCounter + 1);
				if (!isValidMemoryLocation(memoryLocation)) {
					reportInvalidMemoryReference(memoryLocation);
				} else {
					mm.setByte(memoryLocation, wrs.getReg(Reg.A));
					opCodeSize = 3;
				}//
					// System.out.printf("STA %s%n", "");
				break;
			case 7: // yyy = 111 LDA len =3, cycles = 13
				memoryLocation = mm.getWordReversed(programCounter + 1);
				if (!isValidMemoryLocation(memoryLocation)) {
					reportInvalidMemoryReference(memoryLocation);
				} else {
					wrs.setReg(Reg.A, mm.getByte(memoryLocation));
					// mm.setByte(memoryLocation, wrs.getReg(Reg.A));
					opCodeSize = 3;
				}//
					// System.out.printf("LDA %s%n", "");
				break;
			default:
				badOpCode();
			}// switch(yyy)
			break;
		case 3: // zzz = 011
			regPair = registerPairDecodeSet1[yyy >> 1]; // only want the two
														// high
														// bits
			if ((yyy & 0X1) == 0) { // INX Register Pair len =1, cycles = 5
				if (regPair == Reg.SP) {// stack Pointer
					int ans = wrs.getStackPointer() + 1;
					wrs.setStackPointer(ans);
				} else {
					int ans = wrs.getDoubleReg(regPair) + 1;
					wrs.setDoubleReg(regPair, ans);
				}// inner if
					// System.out.printf("INX %s%n", regPair);
			} else { // DCX RegisterPair len =1, cycles = 5
				if (regPair == Reg.SP) {// stack Pointer
					int ans = wrs.getStackPointer() - 1;
					wrs.setStackPointer(ans);
				} else {
					int ans = wrs.getDoubleReg(regPair) - 1;
					wrs.setDoubleReg(regPair, ans);
				}// inner if
					// System.out.printf("DCX %s%n", regPair);
			}// if
			opCodeSize = 1;
			break;
		case 4: // zzz = 100
			// byte value;
			regSingle = registerDecode[yyy];
			if (regSingle == Reg.M) { // INR M len =1, cycles = 10
				memoryLocation = wrs.getDoubleReg(Reg.HL);
				if (!isValidMemoryLocation(memoryLocation)) {
					reportInvalidMemoryReference(memoryLocation);
				} else {
					value = mm.getByte(memoryLocation);
					mm.setByte(memoryLocation, au.increment(value));
				}// inner if
					// System.out.printf("INC(INR) %s%n", "M"); // Indirect
			} else {// INR len =1, cycles = 5
				value = wrs.getReg(regSingle);
				wrs.setReg(regSingle, au.increment(value));
				// System.out.printf("INC(INR) %s%n", regSingle);
			}// outer if
			opCodeSize = 1;
			break;
		case 5: // zzz = 101
			regSingle = registerDecode[yyy];
			if (regSingle == Reg.M) { // DCR M len =1, cycles = 10
				memoryLocation = wrs.getDoubleReg(Reg.HL);
				if (!isValidMemoryLocation(memoryLocation)) {
					reportInvalidMemoryReference(memoryLocation);
				} else {
					value = mm.getByte(memoryLocation);
					mm.setByte(memoryLocation, au.decrement(value));
				}// inner if
					// System.out.printf("DCR %s%n", "M"); // Indirect
			} else {// DCR len =1, cycles = 5
				value = wrs.getReg(regSingle);
				wrs.setReg(regSingle, au.decrement(value));
				// System.out.printf("DCR %s%n", regSingle);
			}// outer if
			opCodeSize = 1;
			break;
		case 6: // zzz = 110
			// value = this.getByteImmediate();
			// value = mm.getByte(programCounter+1);
			value = mm.getByte(programCounter + 1);

			regSingle = registerDecode[yyy];
			if (regSingle == Reg.M) { // MVI M len =1, cycles = 10
				memoryLocation = wrs.getDoubleReg(Reg.HL);
				if (!isValidMemoryLocation(memoryLocation)) {
					reportInvalidMemoryReference(memoryLocation);
				} else {
					mm.setByte(memoryLocation, value);
				}// inner if
			} else {
				wrs.setReg(regSingle, value);
				// System.out.printf("MVI %s%n", "M"); // Indirect
			} // outer if
			opCodeSize = 2;
			break;
		case 7: // zzz = 111
			// all operations: len =1, cycles = 4
			value = wrs.getReg(Reg.A);
			byte ans;
			switch (yyy) {
			case 0:// yyy = 000
				ans = au.rotateLeft(value, false);
				wrs.setReg(Reg.A, ans);
				// System.out.printf("RLC %s%n", "");
				break;
			case 1:// yyy = 001
				ans = au.rotateRight(value, false);
				wrs.setReg(Reg.A, ans);
				// System.out.printf("RRC %s%n", "");
				break;
			case 2:// yyy = 010
				ans = au.rotateLeft(value, true);
				wrs.setReg(Reg.A, ans);
				// System.out.printf("RAL %s%n", "");
				break;
			case 3:// yyy = 011
				ans = au.rotateRight(value, true);
				wrs.setReg(Reg.A, ans);
				// System.out.printf("RAR %s%n", "");
				break;
			case 4:// yyy = 100
				ans = au.decimalAdjustByte(value);
				wrs.setReg(Reg.A, ans);
				// System.out.printf("DAA %s%n", "");
				break;
			case 5:// yyy = 101
				ans = au.complement(value);
				wrs.setReg(Reg.A, ans);
				// System.out.printf("CMA %s%n", "");
				break;
			case 6:// yyy = 110
				ccr.setCarryFlag(true);
				// System.out.printf("STC %s%n", "");
				break;
			case 7:// yyy = 111
				boolean cFlag = ccr.isCarryFlagSet();
				ccr.setCarryFlag(!cFlag);
				// System.out.printf("CMC %s%n", "");
				break;
			default:
				badOpCode();
			}// switch (yyy)
			opCodeSize = 1;
			break;
		default:
			badOpCode();
		}// switch (zzz)
		return opCodeSize;
	}// opCodePage00

	private int opCodePage01(int yyy, int zzz) {
		// XX YYY ZZZ : XX = page
		// identifyOpCode(ShowOpCode.None);
		int opCodeSize = 0;
		Reg destRegister = registerDecode[yyy];
		Reg sourceRegister = registerDecode[zzz];
		int memoryLocation;

		if ((yyy == 6) & (zzz == 6)) { // HLT 76
			setRunning(false);
			opCodeSize = 1;
		} else if (destRegister == Reg.M) {
			// System.out.printf("MOV to Memory %s,%s%n",
			// destRegister.toString(),
			// sourceRegister.toString());
			memoryLocation = wrs.getDoubleReg(Reg.HL);
			if (!isValidMemoryLocation(memoryLocation)) {
				reportInvalidMemoryReference(memoryLocation);
				// opCodeSize = 0, exit this opcode
			} else {
				mm.setByte(memoryLocation, wrs.getReg(sourceRegister));
				opCodeSize = 1;
			}// if (yyy == 6)
		} else if (sourceRegister == Reg.M) {
			// System.out.printf("MOV from Memory %s,%s%n",
			// destRegister.toString(),
			// sourceRegister.toString());
			memoryLocation = wrs.getDoubleReg(Reg.HL);
			if (!isValidMemoryLocation(memoryLocation)) {
				reportInvalidMemoryReference(memoryLocation);
				// opCodeSize; // exit this opcode
			} else {
				wrs.setReg(destRegister, mm.getByte(memoryLocation));
				opCodeSize = 1;
			}// if (zzz == 6)
		} else { // MOV r1,r2
			// System.out.printf("MOV %s,%s%n", destRegister.toString(),
			// sourceRegister.toString());
			wrs.setReg(destRegister, wrs.getReg(sourceRegister));
			opCodeSize = 1;
		}//
			// if
		return opCodeSize;
	}// opCodePage01

	private int opCodePage02(int yyy, int zzz) {
		// identifyOpCode(ShowOpCode.None);
		int opCodeSize = 0;
		int memoryLocation;
		byte value = 00;
		byte ans;
		byte acc = wrs.getReg(Reg.A);

		Reg sourceRegister = registerDecode[zzz];
		if (sourceRegister == Reg.M) { // INR M len =1, cycles = 10
			memoryLocation = wrs.getDoubleReg(Reg.HL);
			if (!isValidMemoryLocation(memoryLocation)) {
				reportInvalidMemoryReference(memoryLocation);
				return opCodeSize;
			} else {
				value = mm.getByte(memoryLocation);
			}// inner if
				// System.out.printf("INC(INR) %s%n", "M"); // Indirect
		} else {
			value = wrs.getReg(sourceRegister);
		}// outer if
		switch (yyy) {
		case 0:// yyy = 000
			ans = au.add(value, acc);
			wrs.setReg(Reg.A, ans);
			opCodeSize = 1;
			// System.out.printf("ADD %s%n",sourceRegister.toString());
			break;
		case 1:// yyy = 001
			ans = au.addWithCarry(value, acc);
			wrs.setReg(Reg.A, ans);
			opCodeSize = 1;
			// System.out.printf("ADC %s%n",sourceRegister.toString());
			break;
		case 2:// yyy = 010
			ans = au.subtract(acc, value);
			wrs.setReg(Reg.A, ans);
			opCodeSize = 1;
			// System.out.printf("SUB %s%n",sourceRegister.toString());
			break;
		case 3:// yyy = 011
			ans = au.subtractWithBorrow(acc, value);
			wrs.setReg(Reg.A, ans);
			opCodeSize = 1;
			// System.out.printf("SBB %s%n",sourceRegister.toString());
			break;
		case 4:// yyy = 100
				// ans = (byte) (acc & value);
			ans = au.logicalAnd(acc, value);
			wrs.setReg(Reg.A, ans);
			opCodeSize = 1;
			// System.out.printf("ANA %s%n",sourceRegister.toString());
			break;
		case 5:// yyy = 101
				// ans = (byte) (acc ^ value);
			ans = au.logicalXor(acc, value);
			wrs.setReg(Reg.A, ans);
			opCodeSize = 1;
			// System.out.printf("XRA %s%n",sourceRegister.toString());
			break;
		case 6:// yyy = 110
				// ans = (byte) (acc | value);
			ans = au.logicalOr(acc, value);
			wrs.setReg(Reg.A, ans);
			opCodeSize = 1;
			// System.out.printf("ORA %s%n",sourceRegister.toString());
			break;
		case 7:// yyy = 111
			ans = au.subtract(acc, value); // just set the conditon codes
			opCodeSize = 1;
			// System.out.printf("CMP %s%n",sourceRegister.toString());
			break;
		default:
			badOpCode();
		}// switch(yyy)
			// XX YYY ZZZ : XX = page
		return opCodeSize;
	}// opCodePage02

	private int opCodePage03(int yyy, int zzz) {
		// XX YYY ZZZ : XX = page
		// identifyOpCode(ShowOpCode.None);
		int opCodeSize = 0;
		CodeConditional condition = conditionDecode[yyy];
		Reg regPair = registerPairDecodeSet2[yyy >> 1]; // only want the two
														// high bits
		byte valueByte;
		int valueInt;
		switch (zzz) {
		case 0:// zzz = 000
			if (opCodeConditionTrue(condition)) {
				opCode_Return();
				opCodeSize = 0;
			} else {// don't jump, increment the program counter
				opCodeSize = 1;
			}// if

			// System.out.printf("R%s%n", condition);
			break;
		case 1:// zzz = 001
				// int value;

			if ((yyy & 0X01) != 1) {
				int stackLocation = wrs.getStackPointer();
				int newStackLocation = stackLocation + 2;
				if (!isValidMemoryLocation(newStackLocation)) {
					reportInvalidMemoryReference(newStackLocation);
					return opCodeSize;
				}// bad stack pointer
				valueInt = mm.popWord(stackLocation);
				if (regPair == Reg.AF) {
					byte hiByte = (byte) ((valueInt >> 8) & 0X00FF);
					byte loByte = (byte) (valueInt & 0X00FF);
					wrs.setReg(Reg.A, hiByte);
					ccr.setConditionCode(loByte);
				} else {
					wrs.setDoubleReg(regPair, valueInt);
				}// if
				wrs.setStackPointer(newStackLocation);
				opCodeSize = 1;
				// System.out.printf("POP %s%n", regPair.toString());
			} else
				switch (yyy) {
				case 1:// yyy = 001
					opCode_Return();
					opCodeSize = 0; // don't adjust PC
					// System.out.printf("RET %n", "");
					break;
				case 3:// yyy = 011
						// System.out.printf("*RET %n", "");
					break;
				case 5:// yyy = 101
					programCounter = wrs.getDoubleReg(Reg.HL);
					opCodeSize = 0; // don't adjust PC
					// System.out.printf("PCHL %n", "");
					break;
				case 7:// yyy = 111
					wrs.setStackPointer(wrs.getDoubleReg(Reg.HL));
					opCodeSize = 1;
					// System.out.printf("SPHL%n", "");
					break;
				default:
					// ignore
				}// switch(yyy)
			break;
		case 2:// zzz = 010
			if (opCodeConditionTrue(condition)) {
				opCode_Jump();
				opCodeSize = 0;
			} else {// don't jump, increment the program counter
				opCodeSize = 3;
			}// if
				// System.out.printf("J%s%n", condition);
			break;
		case 3:// zzz = 011
			switch (yyy) {
			case 0:// yyy = 000
				opCode_Jump();
				opCodeSize = 0;
				// System.out.printf("JMP %n", "");
				break;
			case 1:// yyy = 001
					// System.out.printf("*JMP %n", "");
				break;
			case 2:// yyy = 010
				valueByte = wrs.getReg(Reg.A);
				Byte IOaddress = mm.getByte(programCounter + 1);
				dc.byteToDevice(IOaddress, (byte) valueByte);
				opCodeSize = 2;
				// System.out.printf("OUT %n", "");
				break;
			case 3:// yyy = 011
				IOaddress = mm.getByte(programCounter + 1);
				byte ans = dc.byteFromDevice(IOaddress);
				wrs.setReg(Reg.A, ans);
				opCodeSize = 2;
				// System.out.printf("IN %n", "");
				break;
			case 4:// yyy = 100
				int hlValue = wrs.getDoubleReg(Reg.HL);
				int stackLocation = wrs.getStackPointer();
				valueInt = mm.popWord(stackLocation);
				wrs.setDoubleReg(Reg.HL, valueInt);

				byte hiByte = (byte) ((hlValue >> 8) & 0X00FF);
				// mm.setByte(stackLocation, hiByte);
				byte loByte = (byte) (hlValue & 0X00FF);
				// mm.setByte(stackLocation+1,loByte);
				mm.setWord(stackLocation, loByte, hiByte);
				opCodeSize = 1;
				// System.out.printf("XTHL %n", "");
				break;
			case 5:// yyy = 101
				int deValue = wrs.getDoubleReg(Reg.DE);
				wrs.setDoubleReg(Reg.DE, wrs.getDoubleReg(Reg.HL));
				wrs.setDoubleReg(Reg.HL, deValue);
				opCodeSize = 1;
				// System.out.printf("XCHG %n", "");
				break;
			case 6:// yyy = 110
					// System.out.printf("DI %n", "");
				break;
			case 7:// yyy = 111
					// System.out.printf("EI %n", "");
				break;
			default:
				// ignore
			}// switch(yyy)
			break;
		case 4:// zzz = 100
				// System.out.printf("C%s%n", condition);
			if (opCodeConditionTrue(condition)) {
				opCode_Call();
				opCodeSize = 0;
			} else {// don't jump, increment the program counter
				opCodeSize = 3;
			}// if

			break;
		case 5:// zzz = 101
			if ((yyy & 0X01) != 1) {
				int intValue;
				if (regPair == Reg.AF) {
					byte hiByte = wrs.getReg(Reg.A);
					byte loByte = ccr.getConditionCode();
					intValue = (hiByte << 8) + (loByte & 0XFF);
				} else {
					intValue = wrs.getDoubleReg(regPair);
				}// if
				opCode_Push(intValue);
				opCodeSize = 1;
				// System.out.printf("PUSH %s%n", regPair.toString());
			} else
				switch (yyy) {
				case 1:// yyy = 001
					opCode_Call();
					opCodeSize = 0;
					// System.out.printf("CALL %n", ""); // use next 16 bits
					// for
					// destination
					break;
				case 3:// yyy = 011
						// System.out.printf("*CALL %n", "");
					break;
				case 5:// yyy = 101
						// System.out.printf("*CALL %n", "");
					break;
				case 7:// yyy = 111
						// System.out.printf("*CALL%n", "");
					break;
				default:
					// ignore
				}// switch(yyy)
			break;
		case 6:// zzz = 110
			byte ans;
			byte accumulator = wrs.getReg(Reg.A);
			byte immediateValue = mm.getByte(programCounter + 1);
			switch (yyy) {
			case 0:// yyy = 000
				ans = au.add(accumulator, immediateValue);
				wrs.setReg(Reg.A, ans);
				opCodeSize = 2;
				// System.out.printf("ADI%n", "");
				break;
			case 1:// yyy = 001
				ans = au.addWithCarry(accumulator, immediateValue);
				wrs.setReg(Reg.A, ans);
				opCodeSize = 2;
				// System.out.printf("ACI%n", "");
				break;
			case 2:// yyy = 010
				ans = au.subtract(accumulator, immediateValue);
				wrs.setReg(Reg.A, ans);
				opCodeSize = 2;
				// System.out.printf("SUI%n", "");
				break;
			case 3:// yyy = 011
				ans = au.subtractWithBorrow(accumulator, immediateValue);
				wrs.setReg(Reg.A, ans);
				opCodeSize = 2;
				// System.out.printf("SBI%n", "");
				break;
			case 4:// yyy = 100
				ans = (byte) (accumulator & immediateValue);
				wrs.setReg(Reg.A, ans);
				opCodeSize = 2;
				// System.out.printf("ANI%n", "");
				break;
			case 5:// yyy = 101
				ans = (byte) (accumulator ^ immediateValue);
				wrs.setReg(Reg.A, ans);
				opCodeSize = 2;
				// System.out.printf("XRI%n", "");
				break;
			case 6:// yyy = 110
				ans = (byte) (accumulator | immediateValue);
				wrs.setReg(Reg.A, ans);
				opCodeSize = 2;
				// System.out.printf("ORI%n", "");
				break;
			case 7:// yyy = 111
				ans = au.subtract(accumulator, immediateValue);
				// wrs.setReg(Reg.A, ans);
				opCodeSize = 2;
				// System.out.printf("CPI%n", "");
				break;
			}// switch(yyy)
			break;
		case 7:// zzz = 111
			int address = (yyy << 3) & 0XFFFF;
			opCode_Push(programCounter + 1);
			this.setProgramCounter(address);
			opCodeSize = 0;
			// System.out.printf("RST %d%n", yyy);
			break;
		default:
			badOpCode();

		}// switch(zzz)
		return opCodeSize;
	}// opCodePage03

	private void opCode_Jump() {
		int newAddress = mm.getWordReversed(programCounter + 1);

		if (!isValidMemoryLocation(newAddress)) {
			reportInvalidMemoryReference(newAddress);
		} else {
			programCounter = newAddress;
		}// if memory check
	}

	private void opCode_Return() {
		int stackPointer = wrs.getStackPointer();
		int oldPC = mm.popWord(stackPointer);
		if (!isValidMemoryLocation(oldPC)) {
			reportInvalidMemoryReference(oldPC);
			return; //
		}// if memory check
		programCounter = oldPC;
		wrs.setStackPointer(stackPointer + 2);
	}// opCode_Return

	private void opCode_Call() {
		int memoryLocation = mm.getWordReversed(programCounter + 1);
		if (!isValidMemoryLocation(memoryLocation)) {
			reportInvalidMemoryReference(memoryLocation + 1);
		} else {
			opCode_Push(programCounter + 3);
			this.setProgramCounter(memoryLocation);
			// opCodeSize = 0;
		}// if
	}// opCode_Call()

	private void opCode_Push(int value) {
		byte hiByte = (byte) (value >> 8);
		byte loByte = (byte) (value & 0X00FF);
		int stackLocation = wrs.getStackPointer();
		mm.pushWord(stackLocation, hiByte, loByte); // push the return
													// address
		wrs.setStackPointer(stackLocation - 2);
	}// opCode_Push

	private boolean opCodeConditionTrue(CodeConditional condition) {
		boolean ans = false;
		switch (condition) {
		case NZ:
			ans = ccr.isZeroFlagSet() ? false : true;
			break;
		case Z:
			ans = ccr.isZeroFlagSet() ? true : false;
			break;
		case NC:
			ans = ccr.isCarryFlagSet() ? false : true;
			break;
		case C:
			ans = ccr.isCarryFlagSet() ? true : false;
			break;
		case PO:
			ans = ccr.isParityFlagSet() ? false : true;
			break;
		case PE:
			ans = ccr.isParityFlagSet() ? true : false;
			break;
		case P:
			ans = ccr.isSignFlagSet() ? false : true;
			break;
		case M:
			ans = ccr.isSignFlagSet() ? true : false;
			break;
		default:

		}// switch
		return ans;
	}// conditionTrue

	private void badOpCode() {
		StringBuilder sb = new StringBuilder();
		String rawCode = valueToBinaryString(opCode, Byte.SIZE);
		sb.append(rawCode.substring(0, 2));
		sb.append(" ");
		sb.append(rawCode.substring(2, 5));
		sb.append(" ");
		sb.append(rawCode.substring(5, 8));

		String opCodeOctal = sb.toString();
		System.err
				.printf("Bad OpCode %02X: %s  - Page %02X with YYY = %02X with ZZZ = %02X%n",
						opCode, opCodeOctal, page, yyy, zzz);
		this.setRunning(false); // stop executing
		return;
	}//

	public static String valueToBinaryString(int value, int wordSize) {
		// int valuex = (int)value;
		String tempAns = Integer.toBinaryString(value);
		int len = tempAns.length();
		if (len < wordSize) {
			for (int i = len; i < Byte.SIZE; i++) {
				tempAns = "0" + tempAns;
			}
		} else if (len > wordSize) {
			tempAns = tempAns.substring(Integer.SIZE - wordSize, Integer.SIZE);
		} else {
			// tempAns is OK
		}// if
		return tempAns;

	}// valueToBinaryString
		// private byte getByteImmediate() {
	// return mm.getByte(programCounter++);
	// }// getImmediateByte
	//
	// private byte getByteIndirect(Reg reg) {
	// return mm.getByte(wrs.getDoubleReg(reg));
	// }// getByteIndirect
	//
	// private void badmemoryLocation(int badLocation) {
	// String message = String
	// .format("Attempted to access memory location (%04X) above curent max memory",
	// badLocation);
	// JOptionPane.showMessageDialog(null, message, "memory access error",
	// JOptionPane.ERROR_MESSAGE);
	// return; // exit gracefully
	// }// badmemoryLocation

	private void badmemoryOpCode(int badLocation) {
		String message = String.format(
				"Bad OpCode decode at memory location (%04X) ", badLocation);
		JOptionPane.showMessageDialog(null, message, "Bad OpCode",
				JOptionPane.ERROR_MESSAGE);
		return; // exit gracefully
	}// badmemoryOpCode

	/*
	 * displays error dialog and resets the running flag
	 */
	private void reportInvalidMemoryReference(int badLocation) {
		String message = String
				.format("Attempted to access memory location (%04X) above curent max memory",
						badLocation);
		JOptionPane.showMessageDialog(null, message, "memory access error",
				JOptionPane.ERROR_MESSAGE);
		this.setRunning(false);
		return; // exit gracefully
	}// badmemoryLocation

	private boolean isValidMemoryLocation(int thisLocation) {
		int maskedLocation = thisLocation & 0XFFFF;
		return maskedLocation <= memorySizeInBytes;
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////

	/*
	 * cycle thru all the byte codes
	 */
	public void beginTesting1() {
		// int itemsOnALine = 6;
		for (int code = 0; code < 256; code++) {
			opCode = (byte) ((code) & 0XFF);
			execute8080Instruction(opCode);
		}// for
		System.out.println();
		System.out.printf("beginTesting1 done%n");
	}// beginTesting1

	// private void identifyOpCode(ShowOpCode show) {// have not used yet
	// if (show == ShowOpCode.None) {
	// return;// do nothing
	// } else if (show == ShowOpCode.Brief) {
	// System.out.printf("opCode %02X: ", opCode);
	//
	// } else if (show == ShowOpCode.Extended) {
	// String opCodeOctal = Stuff.opCodeToOctal(opCode);
	// System.out
	// .printf("opCode %02X: %s  - Page %02X with YYY = %02X with ZZZ = %02X%n",
	// opCode, opCodeOctal, page, yyy, zzz);
	// }// if
	// return;
	// }// identifyOpCode

	// private enum ShowOpCode {
	// None, Brief, Extended
	// }

	@Override
	public void protectedMemoryAccess(MemoryEvent me) {
		reportInvalidMemoryReference(me.getLocation());
	}// protectedMemoryAccess

	@Override
	public void invalidMemoryAccess(MemoryEvent me) {
		// TODO Auto-generated method stub

	}

}// CentralProcessingUnit

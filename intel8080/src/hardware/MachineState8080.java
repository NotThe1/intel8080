package hardware;

import java.io.Serializable;
/*
 * this holds the values not already serialized 
 */

public class MachineState8080 implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int programCounter;
	private int memoryStart;
	private int memoryLength;
	
	public MachineState8080(){
		this.programCounter = 0;
		this.memoryStart = 0;
		this.memoryLength = 0;
	}//Constructor - MachineState8080(
	
	public MachineState8080(int programCounter, int memoryStart, int memoryLength){
		this.programCounter = programCounter;
		this.memoryStart = memoryStart;
		this.memoryLength = memoryLength;
	}//Constructor - MachineState8080(programCounter, memoryStart, memoryLength)
	
	public int getProgramCounter() {
		return programCounter;
	}//getProgramCounter
	public void setProgramCounter(short programCounter) {
		this.programCounter = programCounter;
	}//setProgramCounter
	public int getMemoryStart() {
		return memoryStart;
	}//getMemoryStart
	public void setMemoryStart(short memoryStart) {
		this.memoryStart = memoryStart;
	}//setMemoryStart
	public int getMemoryLength() {
		return memoryLength;
	}//getMemoryLength
	public void setMemoryLength(short memoryLength) {
		this.memoryLength = memoryLength;
	}//setMemoryLength
	
}//MachineState8080

package hardware;

import java.util.EventObject;

import hardware.Core.TRAP;

public class MemoryTrapEvent extends EventObject {
	
	private static final long serialVersionUID = 1L;
	private int location;
	private TRAP trap;

	public MemoryTrapEvent(Core source, int location,Core.TRAP trap) {
		super(source);
		this.location = location;
		this.trap = trap;				
	}//Constructor - MemoryTrapEvent

	public int getLocation(){
		return location;
	}//getLocation
	
	public TRAP getTrap(){
		return trap;
	}//getType
	
	public String getMessage(){
		return String.format("Error type: %s%n location: 0X%04X", trap.toString());
	}//getMessage
	
}//class MemoryEvent


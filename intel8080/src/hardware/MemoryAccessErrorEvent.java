package hardware;

import java.util.EventObject;

public class MemoryAccessErrorEvent extends EventObject {
	
	private static final long serialVersionUID = 1L;
	private int location;
	private String type;

	public MemoryAccessErrorEvent(Core source, int location,String type) {
		super(source);
		this.location = location;
		this.type = type;
	}//Constructor - MemoryErrorEvent

	public int getLocation(){
		return location;
	}//getLocation
	
	public String getType(){
		return type;
	}//getType
	
	public String getMessage(){
		return String.format("Error type: %s%n location: 0X%04X", type,location);
	}//getMessage
	
}//class MemoryEvent


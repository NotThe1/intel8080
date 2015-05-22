package hardware;

import java.util.EventObject;

public class MemoryEvent extends EventObject {
	
	private static final long serialVersionUID = 1L;
	private int location;

	public MemoryEvent(MainMemory source, int location) {
		super(source);
		this.location = location;
		// TODO Auto-generated constructor stub
	}//Constructor - MemoryEvent(evt)

	public int getLocation(){
		return location;
	}//getLocation
}//class MemoryEvent


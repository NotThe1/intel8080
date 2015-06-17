package hardware;

import java.util.EventListener;

public interface MemoryTrapListener extends EventListener {
	void memoryTrap(MemoryTrapEvent mte);
}//MemoryTrapListener

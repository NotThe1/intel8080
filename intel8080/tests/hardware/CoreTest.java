package hardware;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CoreTest {
	Core core;
	int size;
	int protectedBoundary;
	

//	@BeforeClass
//	public static void setUpBeforeClass() throws Exception {
//	}
//
//	@AfterClass
//	public static void tearDownAfterClass() throws Exception {
//	}

	@Before
	public void setUp() throws Exception {
		size = 16 * 1024;	//16K
		protectedBoundary = 0;	// nothing is protected
		core = new Core(size,protectedBoundary);
	}

	@After
	public void tearDown() throws Exception {
		core = null;
	}

	@Test
	public void bluntTest() {
		assertThat("Check size",core.getSize(),equalTo(size));
		assertThat("Check protected memory boundary",core.getProtectedBoundary(),equalTo(protectedBoundary));
		
		core.setTrapEnabled(true);
		assertThat("Check trap setting",core.isTrapEnabled(),equalTo(true));
		core.setTrapEnabled(false);
		assertThat("Check trap setting",core.isTrapEnabled(),equalTo(false));
	}//blunt Test
	
	@Test
	public void readWriteTestsSimple() {
		byte value = (byte) 0X55;
		int location = 0X100;
		core.write(location, value);
		assertThat("Simple write/read - 55",core.read(location),equalTo(value));
		
		value = (byte) 0XAA;
		core.write(location, value);
		assertThat("Simple write/read - AA",core.read(location),equalTo(value));
	
		value = (byte)0XFF;
		for (int i = 0; i < 0X100 ; i++){
			core.write(location + i, value++);			
		}//for - write
		
		value = (byte)0XFF;
		for (int i = 0; i < 0X100 ; i++){
			assertThat("Simple write/read consecutive location",core.read(location +i),equalTo(value++));				
		}//for - write
	}//readWriteTestsSimple
	
	@Test
	public void readWriteTestsRandom() {
		int bufferSize = 2 *1024;  // 2k buffer
		byte[] writeBuffer = new byte[bufferSize];
		byte[] readBuffer= new byte[bufferSize];
		
		int location = 0X0800;
		new Random().nextBytes(writeBuffer);
		
		for (int i = 0 ; i < bufferSize; i++){
			core.write(location + i, writeBuffer[i]);
		}// for - write
		
		for (int i = 0 ; i < bufferSize; i++){
				readBuffer[i] = core.read(location +i)	;	
		}// for - read
		
		assertThat("Random write/read consecutive location",readBuffer,equalTo(writeBuffer));	
	}//readWriteTestsRandom
	
	@Test
	public void readWriteDMATest() {
		int bufferSize = 512;  // typical sector size
		byte[] writeBuffer = new byte[bufferSize];
		byte[] readBuffer= new byte[bufferSize];
		
		new Random().nextBytes(writeBuffer);
		
		int location = 0X0A00;
		
		for (int i = 0 ; i < bufferSize; i++){
			core.write(location + i, writeBuffer[i]);
		}// for - write
				
		readBuffer = core.readDMA(location,bufferSize);
		assertThat("readDMA  data",readBuffer,equalTo(writeBuffer));
		
		new Random().nextBytes(writeBuffer);		// lets get some new data
		core.writeDMA(location, writeBuffer);
		assertThat("writeDMA and readDMA ",core.readDMA(location, bufferSize),equalTo(writeBuffer));


	}


		

}//class CoreTest

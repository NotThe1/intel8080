package hardware;

public class Stuff {

//	public static String byteToString(short operand1) {
//		return valueToBinaryString(operand1, Byte.SIZE);
//	}// byteToString

//	public static String dwordToString(short value) {
//		return valueToBinaryString(value, Short.SIZE);
//	}//dwordToString

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
	}//valueToBinaryString
	
	public static String opCodeToOctal(int value){
		String rawCode = valueToBinaryString(value,Byte.SIZE);
		String answer1 = rawCode.substring(0,2);
		String answer2 = rawCode.substring(2,5);
		String answer3 = rawCode.substring(5,8);
		return answer1 + " " + answer2 + " " + answer3;
	}
	


}// Stuff

package integrationcore;

public class IntStringPair implements Comparable<IntStringPair> {

	private int i;
	private String s;
	
	public IntStringPair(int i, String s){
		this.i = i;
		this.s = s;
	}
	
	public int compareTo(IntStringPair otherPair){
		return i-otherPair.getInt();
	}
	
	public int getInt() {
		return i;
	}
	public void setInt(int i) {
		this.i = i;
	}
	public String getString() {
		return s;
	}
	public void setString(String s) {
		this.s = s;
	}
	
}

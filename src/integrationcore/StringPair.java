package integrationcore;

public class StringPair {

	public String a;
	public String b;
	
	public StringPair(String a, String b) {
		this.a = a;
		this.b = b;
	}
	
	public boolean equals(Object obj){
		if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof StringPair))
            return false;
        StringPair rhs = (StringPair) obj;
        return( (a.equals(rhs.a) && b.equals(rhs.b)) || (a.equals(rhs.b) && b.equals(rhs.a)) );
	}
	
	public int hashCode() {
		int hash = 37;
        hash = 53 * hash + ( (a != null ? a.hashCode() : 0) + (b != null ? b.hashCode() : 0) );
        return hash;
    }
	
	public String toString(){
		return "["+a+"] <-> ["+b+"]";
	}

}

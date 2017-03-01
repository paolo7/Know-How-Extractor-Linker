package evaluation;

import integrationcore.StringPair;

public class Link {

	private String subject;
	private String linkType;
	private String object;
	
	public Link() {
	}
	
	public Link(String subject, String property, String object) {
		this.subject = subject;
		this.linkType = property;
		this.object = object;
	}
	
	public boolean equals(Object obj){
		if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Link))
            return false;
        Link rhs = (Link) obj;
        return( subject.equals(rhs.subject) && object.equals(rhs.object) && linkType.equals(rhs.linkType));
	}
	
	public int hashCode() {
		int hash = 37;
        hash = 53 * hash + ( (subject != null ? subject.hashCode() : 0) + (object != null ? object.hashCode() : 0) 
        		+ (linkType != null ? linkType.hashCode() : 0) );
        return hash;
    }

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
	}

	public String getLinkType() {
		return linkType;
	}

	public void setLinkType(String linkType) {
		this.linkType = linkType;
	}

}

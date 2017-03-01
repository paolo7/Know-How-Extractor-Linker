package comparison;

public class LinkResult implements Comparable<LinkResult> {

	private double score;
	private String entity1;
	private String entity2;
	private int typeOfLink;
	private String description;
	
	public LinkResult(double score, String entity1, String entity2, int typeOfLink){
		this.score = score;
		this.entity1 = entity1;
		this.entity2 = entity2;
		this.typeOfLink = typeOfLink;
	}
	public LinkResult(double score, String entity1, String entity2, int typeOfLink, String description){
		this.score = score;
		this.entity1 = entity1;
		this.entity2 = entity2;
		this.typeOfLink = typeOfLink;
		this.description = description;
	}
	
	public int compareTo(LinkResult otherPair){
		if(score-otherPair.getScore() > 0) return 1;
		else if(score-otherPair.getScore() < 0) return -1;
		else return 0;
	}

	private double getScore() {
		return score;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public String toString(){
		return description;
	}

	public String getEntity1() {
		return entity1;
	}

	/*public void setEntity1(String entity1) {
		this.entity1 = entity1;
	}*/

	public String getEntity2() {
		return entity2;
	}

	/*public void setEntity2(String entity2) {
		this.entity2 = entity2;
	}*/

	public int getTypeOfLink() {
		return typeOfLink;
	}

	/*public void setTypeOfLink(int typeOfLink) {
		this.typeOfLink = typeOfLink;
	}*/
	
	
}

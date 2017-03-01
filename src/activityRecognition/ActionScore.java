package activityRecognition;

public class ActionScore {

	private String uri;
	private double score;
	private String label;

	public ActionScore(String uri, double score) {
		this.uri = uri;
		this.setScore(score);
	}

	public String getUri() {
		return uri;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

}

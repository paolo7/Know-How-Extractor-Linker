package luceneIndexing;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;

public class KnowHowSimilarity extends Similarity{

	public KnowHowSimilarity() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public long computeNorm(FieldInvertState arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SimWeight computeWeight(float arg0, CollectionStatistics arg1,
			TermStatistics... arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SimScorer simScorer(SimWeight arg0, AtomicReaderContext arg1)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}

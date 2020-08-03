package org.apache.lucene.search.similarities;


import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SmallFloat;


public class CMPT456Similarity extends ClassicSimilarity {

  /** Sole constructor: parameter-free */
  public CMPT456Similarity() {}

  /** Implemented as <code>sqrt(freq)</code>. */
  @Override
  public float tf(float freq) {
    return (float)Math.sqrt(1 + freq);
  }

  /** Implemented as <code>log((docCount+1)/(docFreq+1)) + 1</code>. */
  @Override
  public float idf(long docFreq, long docCount) {
    return (float)(Math.log((docCount+2)/(double)(docFreq+2)) + 1.0);
  }

  @Override
  public String toString() {
    return "CMPT456Similarity";
  }
}

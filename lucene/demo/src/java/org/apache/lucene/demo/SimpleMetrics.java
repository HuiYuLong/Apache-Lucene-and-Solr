package org.apache.lucene.demo;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;


public class SimpleMetrics {

	public SimpleMetrics(String string, IndexReader reader) {

		try {

			Term term = new Term("contents", string.toLowerCase());
			System.out.println("Searching on the term: " + term.text());

			// Returns the number of documents containing the term.
			int docFreq = 0;
			docFreq = reader.docFreq(term);
			System.out.println("The number of documents containing the term: "+docFreq);

			// Returns the total number of occurrences of term across all documents (the sum of the freq() for each doc that has this term).
			long totalTermFreq = 0;
			totalTermFreq = reader.totalTermFreq(term);
			System.out.println("The total number of occurrences of term across all documents: "+totalTermFreq);
			
		} catch (Exception e) {
	      	System.out.println("Something went wrong.");
	      }
	}
}

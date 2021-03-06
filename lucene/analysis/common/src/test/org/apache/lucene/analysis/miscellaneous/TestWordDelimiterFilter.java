/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.analysis.miscellaneous;

import java.io.IOException;
import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.IOUtils;
import org.junit.Test;

import static org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter.*;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterIterator.DEFAULT_WORD_DELIM_TABLE;

/**
 * New WordDelimiterFilter tests... most of the tests are in ConvertedLegacyTest
 * TODO: should explicitly test things like protWords and not rely on
 * the factory tests in Solr.
 */
public class TestWordDelimiterFilter extends BaseTokenStreamTestCase {

  /*
  public void testPerformance() throws IOException {
    String s = "now is the time-for all good men to come to-the aid of their country.";
    Token tok = new Token();
    long start = System.currentTimeMillis();
    int ret=0;
    for (int i=0; i<1000000; i++) {
      StringReader r = new StringReader(s);
      TokenStream ts = new WhitespaceTokenizer(r);
      ts = new WordDelimiterFilter(ts, 1,1,1,1,0);

      while (ts.next(tok) != null) ret++;
    }

    System.out.println("ret="+ret+" time="+(System.currentTimeMillis()-start));
  }
  ***/

  @Test
  public void testOffsets() throws IOException {
    int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | CATENATE_ALL | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS | STEM_ENGLISH_POSSESSIVE;
    // test that subwords and catenated subwords have
    // the correct offsets.
    WordDelimiterFilter wdf = new WordDelimiterFilter(new CannedTokenStream(new Token("foo-bar", 5, 12)), DEFAULT_WORD_DELIM_TABLE, flags, null);

    assertTokenStreamContents(wdf, 
        new String[] { "foo", "foobar", "bar" },
        new int[] { 5, 5, 9 }, 
        new int[] { 8, 12, 12 });

    wdf = new WordDelimiterFilter(new CannedTokenStream(new Token("foo-bar", 5, 6)), DEFAULT_WORD_DELIM_TABLE, flags, null);
    
    assertTokenStreamContents(wdf,
        new String[] { "foo", "bar", "foobar" },
        new int[] { 5, 5, 5 },
        new int[] { 6, 6, 6 });
  }
  
  @Test
  public void testOffsetChange() throws Exception {
    int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | CATENATE_ALL | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS | STEM_ENGLISH_POSSESSIVE;
    WordDelimiterFilter wdf = new WordDelimiterFilter(new CannedTokenStream(new Token("??belkeit)", 7, 16)), DEFAULT_WORD_DELIM_TABLE, flags, null);
    
    assertTokenStreamContents(wdf,
        new String[] { "??belkeit" },
        new int[] { 7 },
        new int[] { 15 });
  }
  
  @Test
  public void testOffsetChange2() throws Exception {
    int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | CATENATE_ALL | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS | STEM_ENGLISH_POSSESSIVE;
    WordDelimiterFilter wdf = new WordDelimiterFilter(new CannedTokenStream(new Token("(??belkeit", 7, 17)), DEFAULT_WORD_DELIM_TABLE, flags, null);
    
    assertTokenStreamContents(wdf,
        new String[] { "??belkeit" },
        new int[] { 8 },
        new int[] { 17 });
  }
  
  @Test
  public void testOffsetChange3() throws Exception {
    int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | CATENATE_ALL | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS | STEM_ENGLISH_POSSESSIVE;
    WordDelimiterFilter wdf = new WordDelimiterFilter(new CannedTokenStream(new Token("(??belkeit", 7, 16)), DEFAULT_WORD_DELIM_TABLE, flags, null);
    
    assertTokenStreamContents(wdf,
        new String[] { "??belkeit" },
        new int[] { 8 },
        new int[] { 16 });
  }
  
  @Test
  public void testOffsetChange4() throws Exception {
    int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | CATENATE_ALL | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS | STEM_ENGLISH_POSSESSIVE;
    WordDelimiterFilter wdf = new WordDelimiterFilter(new CannedTokenStream(new Token("(foo,bar)", 7, 16)), DEFAULT_WORD_DELIM_TABLE, flags, null);
    
    assertTokenStreamContents(wdf,
        new String[] { "foo", "foobar", "bar"},
        new int[] { 8, 8, 12 },
        new int[] { 11, 15, 15 });
  }

  public void doSplit(final String input, String... output) throws Exception {
    int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS | STEM_ENGLISH_POSSESSIVE;
    WordDelimiterFilter wdf = new WordDelimiterFilter(keywordMockTokenizer(input),
        WordDelimiterIterator.DEFAULT_WORD_DELIM_TABLE, flags, null);
    
    assertTokenStreamContents(wdf, output);
  }

  @Test
  public void testSplits() throws Exception {
    doSplit("basic-split","basic","split");
    doSplit("camelCase","camel","Case");

    // non-space marking symbol shouldn't cause split
    // this is an example in Thai    
    doSplit("\u0e1a\u0e49\u0e32\u0e19","\u0e1a\u0e49\u0e32\u0e19");
    // possessive followed by delimiter
    doSplit("test's'", "test");

    // some russian upper and lowercase
    doSplit("????????????", "????????????");
    // now cause a split (russian camelCase)
    doSplit("????????????", "??????", "??????");

    // a composed titlecase character, don't split
    doSplit("a??ungla", "a??ungla");
    
    // a modifier letter, don't split
    doSplit("??????????????????????????????????????????", "??????????????????????????????????????????");
    
    // enclosing mark, don't split
    doSplit("test???", "test???");
    
    // combining spacing mark (the virama), don't split
    doSplit("??????????????????", "??????????????????");
    
    // don't split non-ascii digits
    doSplit("????????", "????????");
    
    // don't split supplementaries into unpaired surrogates
    doSplit("????????", "????????");
  }
  
  public void doSplitPossessive(int stemPossessive, final String input, final String... output) throws Exception {
    int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS;
    flags |= (stemPossessive == 1) ? STEM_ENGLISH_POSSESSIVE : 0;
    WordDelimiterFilter wdf = new WordDelimiterFilter(keywordMockTokenizer(input), flags, null);

    assertTokenStreamContents(wdf, output);
  }
  
  /*
   * Test option that allows disabling the special "'s" stemming, instead treating the single quote like other delimiters. 
   */
  @Test
  public void testPossessives() throws Exception {
    doSplitPossessive(1, "ra's", "ra");
    doSplitPossessive(0, "ra's", "ra", "s");
  }
  
  /*
   * Set a large position increment gap of 10 if the token is "largegap" or "/"
   */
  private static final class LargePosIncTokenFilter extends TokenFilter {
    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    
    protected LargePosIncTokenFilter(TokenStream input) {
      super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
      if (input.incrementToken()) {
        if (termAtt.toString().equals("largegap") || termAtt.toString().equals("/"))
          posIncAtt.setPositionIncrement(10);
        return true;
      } else {
        return false;
      }
    }  
  }
  
  @Test
  public void testPositionIncrements() throws Exception {
    final int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | CATENATE_ALL | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS | STEM_ENGLISH_POSSESSIVE;
    final CharArraySet protWords = new CharArraySet(new HashSet<>(Arrays.asList("NUTCH")), false);
    
    /* analyzer that uses whitespace + wdf */
    Analyzer a = new Analyzer() {
      @Override
      public TokenStreamComponents createComponents(String field) {
        Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, new WordDelimiterFilter(
            tokenizer,
            flags, protWords));
      }
    };

    /* in this case, works as expected. */
    assertAnalyzesTo(a, "LUCENE / SOLR", new String[] { "LUCENE", "SOLR" },
        new int[] { 0, 9 },
        new int[] { 6, 13 },
        null,
        new int[] { 1, 1 },
        null,
        false);
    
    /* only in this case, posInc of 2 ?! */
    assertAnalyzesTo(a, "LUCENE / solR", new String[] { "LUCENE", "sol", "solR", "R" },
        new int[] { 0, 9, 9, 12 },
        new int[] { 6, 12, 13, 13 },
        null,                     
        new int[] { 1, 1, 0, 1 },
        null,
        false);
    
    assertAnalyzesTo(a, "LUCENE / NUTCH SOLR", new String[] { "LUCENE", "NUTCH", "SOLR" },
        new int[] { 0, 9, 15 },
        new int[] { 6, 14, 19 },
        null,
        new int[] { 1, 1, 1 },
        null,
        false);
    
    /* analyzer that will consume tokens with large position increments */
    Analyzer a2 = new Analyzer() {
      @Override
      public TokenStreamComponents createComponents(String field) {
        Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, new WordDelimiterFilter(
            new LargePosIncTokenFilter(tokenizer),
            flags, protWords));
      }
    };
    
    /* increment of "largegap" is preserved */
    assertAnalyzesTo(a2, "LUCENE largegap SOLR", new String[] { "LUCENE", "largegap", "SOLR" },
        new int[] { 0, 7, 16 },
        new int[] { 6, 15, 20 },
        null,
        new int[] { 1, 10, 1 },
        null,
        false);
    
    /* the "/" had a position increment of 10, where did it go?!?!! */
    assertAnalyzesTo(a2, "LUCENE / SOLR", new String[] { "LUCENE", "SOLR" },
        new int[] { 0, 9 },
        new int[] { 6, 13 },
        null,
        new int[] { 1, 11 },
        null,
        false);
    
    /* in this case, the increment of 10 from the "/" is carried over */
    assertAnalyzesTo(a2, "LUCENE / solR", new String[] { "LUCENE", "sol", "solR", "R" },
        new int[] { 0, 9, 9, 12 },
        new int[] { 6, 12, 13, 13 },
        null,
        new int[] { 1, 11, 0, 1 },
        null,
        false);
    
    assertAnalyzesTo(a2, "LUCENE / NUTCH SOLR", new String[] { "LUCENE", "NUTCH", "SOLR" },
        new int[] { 0, 9, 15 },
        new int[] { 6, 14, 19 },
        null,
        new int[] { 1, 11, 1 },
        null,
        false);

    Analyzer a3 = new Analyzer() {
      @Override
      public TokenStreamComponents createComponents(String field) {
        Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        StopFilter filter = new StopFilter(tokenizer, StandardAnalyzer.STOP_WORDS_SET);
        return new TokenStreamComponents(tokenizer, new WordDelimiterFilter(filter, flags, protWords));
      }
    };

    assertAnalyzesTo(a3, "lucene.solr", 
        new String[] { "lucene", "lucenesolr", "solr" },
        new int[] { 0, 0, 7 },
        new int[] { 6, 11, 11 },
        null,
        new int[] { 1, 0, 1 },
        null,
        false);

    /* the stopword should add a gap here */
    assertAnalyzesTo(a3, "the lucene.solr", 
        new String[] { "lucene", "lucenesolr", "solr" }, 
        new int[] { 4, 4, 11 }, 
        new int[] { 10, 15, 15 },
        null,
        new int[] { 2, 0, 1 },
        null,
        false);

    IOUtils.close(a, a2, a3);
  }
  
  /** concat numbers + words + all */
  public void testLotsOfConcatenating() throws Exception {
    final int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | CATENATE_WORDS | CATENATE_NUMBERS | CATENATE_ALL | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS | STEM_ENGLISH_POSSESSIVE;    

    /* analyzer that uses whitespace + wdf */
    Analyzer a = new Analyzer() {
      @Override
      public TokenStreamComponents createComponents(String field) {
        Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, new WordDelimiterFilter(tokenizer, flags, null));
      }
    };
    
    assertAnalyzesTo(a, "abc-def-123-456", 
        new String[] { "abc", "abcdef", "abcdef123456", "def", "123", "123456", "456" }, 
        new int[] { 0, 0, 0, 4, 8, 8, 12 }, 
        new int[] { 3, 7, 15, 7, 11, 15, 15 },
        null,
        new int[] { 1, 0, 0, 1, 1, 0, 1 },
        null,
        false);
    a.close();
  }
  
  /** concat numbers + words + all + preserve original */
  public void testLotsOfConcatenating2() throws Exception {
    final int flags = PRESERVE_ORIGINAL | GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | CATENATE_WORDS | CATENATE_NUMBERS | CATENATE_ALL | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS | STEM_ENGLISH_POSSESSIVE;    

    /* analyzer that uses whitespace + wdf */
    Analyzer a = new Analyzer() {
      @Override
      public TokenStreamComponents createComponents(String field) {
        Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, new WordDelimiterFilter(tokenizer, flags, null));
      }
    };
    
    assertAnalyzesTo(a, "abc-def-123-456", 
        new String[] { "abc-def-123-456", "abc", "abcdef", "abcdef123456", "def", "123", "123456", "456" }, 
        new int[] { 0, 0, 0, 0, 4, 8, 8, 12 }, 
        new int[] { 15, 3, 7, 15, 7, 11, 15, 15 },
        null,
        new int[] { 1, 0, 0, 0, 1, 1, 0, 1 },
        null,
        false);
    a.close();
  }
  
  /** blast some random strings through the analyzer */
  public void testRandomStrings() throws Exception {
    int numIterations = atLeast(5);
    for (int i = 0; i < numIterations; i++) {
      final int flags = random().nextInt(512);
      final CharArraySet protectedWords;
      if (random().nextBoolean()) {
        protectedWords = new CharArraySet(new HashSet<>(Arrays.asList("a", "b", "cd")), false);
      } else {
        protectedWords = null;
      }
      
      Analyzer a = new Analyzer() {
        
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
          Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
          return new TokenStreamComponents(tokenizer, new WordDelimiterFilter(tokenizer, flags, protectedWords));
        }
      };
      // TODO: properly support positionLengthAttribute
      checkRandomData(random(), a, 200*RANDOM_MULTIPLIER, 20, false, false);
      a.close();
    }
  }
  
  /** blast some enormous random strings through the analyzer */
  public void testRandomHugeStrings() throws Exception {
    int numIterations = atLeast(5);
    for (int i = 0; i < numIterations; i++) {
      final int flags = random().nextInt(512);
      final CharArraySet protectedWords;
      if (random().nextBoolean()) {
        protectedWords = new CharArraySet(new HashSet<>(Arrays.asList("a", "b", "cd")), false);
      } else {
        protectedWords = null;
      }
      
      Analyzer a = new Analyzer() {
        
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
          Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
          return new TokenStreamComponents(tokenizer, new WordDelimiterFilter(tokenizer, flags, protectedWords));
        }
      };
      // TODO: properly support positionLengthAttribute
      checkRandomData(random(), a, 20*RANDOM_MULTIPLIER, 8192, false, false);
      a.close();
    }
  }
  
  public void testEmptyTerm() throws IOException {
    Random random = random();
    for (int i = 0; i < 512; i++) {
      final int flags = i;
      final CharArraySet protectedWords;
      if (random.nextBoolean()) {
        protectedWords = new CharArraySet(new HashSet<>(Arrays.asList("a", "b", "cd")), false);
      } else {
        protectedWords = null;
      }
    
      Analyzer a = new Analyzer() { 
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
          Tokenizer tokenizer = new KeywordTokenizer();
          return new TokenStreamComponents(tokenizer, new WordDelimiterFilter(tokenizer, flags, protectedWords));
        }
      };
      // depending upon options, this thing may or may not preserve the empty term
      checkAnalysisConsistency(random, a, random.nextBoolean(), "");
      a.close();
    }
  }

  /*
  public void testToDot() throws Exception {
    int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | CATENATE_ALL | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS | STEM_ENGLISH_POSSESSIVE | PRESERVE_ORIGINAL | CATENATE_WORDS | CATENATE_NUMBERS | STEM_ENGLISH_POSSESSIVE;
    String text = "PowerSystem2000-5-Shot's";
    WordDelimiterFilter wdf = new WordDelimiterFilter(new CannedTokenStream(new Token(text, 0, text.length())), DEFAULT_WORD_DELIM_TABLE, flags, null);
    //StringWriter sw = new StringWriter();
    // TokenStreamToDot toDot = new TokenStreamToDot(text, wdf, new PrintWriter(sw));
    PrintWriter pw = new PrintWriter("/x/tmp/before.dot");
    TokenStreamToDot toDot = new TokenStreamToDot(text, wdf, pw);
    toDot.toDot();
    pw.close();
    System.out.println("TEST DONE");
    //System.out.println("DOT:\n" + sw.toString());
  }
  */

  public void testOnlyNumbers() throws Exception {
    int flags = GENERATE_WORD_PARTS | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS;
    Analyzer a = new Analyzer() {
        
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, new WordDelimiterFilter(tokenizer, flags, null));
      }
    };

    assertAnalyzesTo(a, "7-586", 
                     new String[] {},
                     new int[] {},
                     new int[] {},
                     null,
                     new int[] {},
                     null,
                     false);
  }

  public void testNumberPunct() throws Exception {
    int flags = GENERATE_WORD_PARTS | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS;
    Analyzer a = new Analyzer() {
        
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, new WordDelimiterFilter(tokenizer, flags, null));
      }
    };

    assertAnalyzesTo(a, "6-", 
                     new String[] {"6"},
                     new int[] {0},
                     new int[] {1},
                     null,
                     new int[] {1},
                     null,
                     false);
  }

  private Analyzer getAnalyzer(final int flags) {
    return new Analyzer() {
        
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, new WordDelimiterFilter(tokenizer, flags, null));
      }
    };
  }
}

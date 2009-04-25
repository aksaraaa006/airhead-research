package edu.ucla.sspace.common;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class DocumentPreprocessorTest {

  @Test public void punktSpaceTest() {
    String[] wordList = {"how", "much", "wood", "would", "a", "woodchuck", "chuck", "if", ",", "?", ".", "as", "much"};
    String testDocument = "How much wood would a woodchuck chuck, if a woodchuck could chuck wood? As much wood as a woodchuck would, if a woodchuck could chuck wood.";
    String resultDocument = "how much wood would a woodchuck chuck , if a woodchuck could chuck wood ? as much wood as a woodchuck would , if a woodchuck could chuck wood .";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor(wordList);
    String result = testProcessor.process(testDocument);
    assertEquals(resultDocument, result);
  }

  @Test public void convertUrlEmailNumTest() {
    String[] wordList = {"word", "can", "a", "be", "a", "b", "c", "d", "e", "f", "g", "or"};
    String testDocument = "can a word :] be myusername@ucla.edu a b c d e f g or a 12345";
    String expectedResult = "can a word <emote> be <url> a b c d e f g or a <num>";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor(wordList);
    assertEquals(expectedResult, testProcessor.process(testDocument));
  }

  @Test public void stripHtmlTest() {
    String[] wordList = {"word", "can", "a", "be", "a", "b", "c", "d", "e", "f", "g", "or"};
    String testDocument = "can a word <b>be</b> <script> a b c d e f g or a </html>";
    String expectedResult = "can a word be a b c d e f g or a";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor(wordList);
    assertEquals(expectedResult, testProcessor.process(testDocument));
  }

  @Test public void stripLongWords() {
    String[] wordList = {"word", "can", "a", "be", "a", "b", "c", "d", "e", "f", "g", "or"};
    String testDocument = "can a word be a b c d e f g or a superfragalisticexpaliadouciousmagicake";
    String expectedResult = "can a word be a b c d e f g or a";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor(wordList);
    assertEquals(expectedResult, testProcessor.process(testDocument));
  }

  @Test public void ignoreJunkDocumentTest() {
    String[] wordList = {"word", "can", "a", "be", "a", "b", "c", "d", "e", "f"};
    // Just at the limit.
    String testDocument = "can a word be a b c d 1234 :]";
    String expectedResult = "can a word be a b c d <num> <emote>";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor(wordList);
    assertEquals(expectedResult, testProcessor.process(testDocument));
    // Just under the limit.
    testDocument = "can a word be a b c abc@car.com 1234 :]";
    assertEquals("", testProcessor.process(testDocument));
  }

  @Test public void convertDollarTest() {
    String[] wordList = {"word", "can", "a", "be", "a", "b", "c", "d", "e", "f", "g", "or"};
    String testDocument = "can a word be a b c d e f g or a $5";
    String expectedResult = "can a word be a b c d e f g or a 5 dollars";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor(wordList);
    assertEquals(expectedResult, testProcessor.process(testDocument));
    testDocument = "can a word be a b c d e f g or a $5notanumber";
    expectedResult = "can a word be a b c d e f g or a";
    assertEquals(expectedResult, testProcessor.process(testDocument));
  }

  @Test public void duplicateDocTest() {
    String[] wordList = {"word", "can", "a", "be", "a", "b", "c", "d", "e", "f", "g", "or"};
    String testDocument = "can a word be a b c d e f g or a superfragalisticexpaliadouciousmagicake";
    String expectedResult = "can a word be a b c d e f g or a";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor(wordList);
    assertEquals(expectedResult, testProcessor.process(testDocument));
    assertEquals("", testProcessor.process(testDocument));
  }

  @Test public void noArgChange() {
    String[] wordList = {"word", "can", "a", "be", "a", "b", "c", "d", "e", "f", "g", "or"};
    String testDocument = "can a word be a b c d e f g or a superfragalisticexpaliadouciousmagicake";
    String resultDocument = new String(testDocument);
    DocumentPreprocessor testProcessor = new DocumentPreprocessor(wordList);
    testProcessor.process(testDocument);
    assertEquals(resultDocument, testDocument);
  }

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(DocumentPreprocessorTest.class);
  }
}
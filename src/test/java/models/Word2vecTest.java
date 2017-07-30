package models;

import org.junit.Test;
import utils.VocabTest;

public class Word2vecTest {
    @Test
    public void fit() throws Exception {
        VocabTest vocabTest = new utils.VocabTest();
        vocabTest.createDummyFile();

        Word2vec w2v = new Word2vec(
                10,
                "test.txt",
                0.025,
                5,
                1e-5,
                3,
                1,
                false);
        w2v.fit();
        w2v.output();
        vocabTest.deleteDummyFile();
    }
}
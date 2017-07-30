package utils;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public class VocabTest {
    final private String fname = "test.txt";

    public void createDummyFile() throws IOException {
        PrintWriter writer = new PrintWriter(this.fname, "UTF-8");
        writer.println("w6 w6 w6 w6 w6 w6");
        writer.println("w4 w4 w4 w4 w0");
        writer.println("");
        writer.println("w1");
        writer.println("w3 w3 w3 w2 w2 ");
        writer.close();
    }

    public void deleteDummyFile(){
        File f = new File(this.fname);
        f.delete();
    }

    @Test
    public void testGetWord() throws Exception {
        this.createDummyFile();
        int minCount = 2;
        Vocab vocab = new Vocab(this.fname, minCount);
        int[] wordIds = {0, 1, 2, 3, 4};
        String[] preds = {"w6", "</s>", "w4", "w3", "w2"};
        String[] actual = new String[wordIds.length];

        for (int i = 0; i < wordIds.length; i++){
            actual[i] = vocab.getWord(wordIds[i]);
        }
        assertArrayEquals(actual, preds);
        assertEquals(null, vocab.getWord(-1));
        assertEquals(null, vocab.getWord(10));
        this.deleteDummyFile();
    }

    @Test
    public void testGetWordId() throws Exception {
        this.createDummyFile();
        int minCount = 2;
        Vocab vocab = new Vocab(this.fname, minCount);
        String[] tokens = {"w6", "</s>" ,"w4", "w3", "w2"};
        int[] pred = {0, 1, 2, 3, 4};
        int[] actual = new int[pred.length];

        for (int i = 0; i < tokens.length; i++){
            actual[i] = vocab.getWordId(tokens[i]);
        }
        assertArrayEquals(actual, pred);

        this.deleteDummyFile();
    }

    @Test
    public void testGetTrainWords() throws Exception {
        this.createDummyFile();
        int minCount = 1;
        Vocab vocab = new Vocab(this.fname, minCount);
        assertEquals(22, vocab.getTrainWords());

        minCount = 2;
        vocab = new Vocab(this.fname, minCount);
        assertEquals(20, vocab.getTrainWords());

        this.deleteDummyFile();
    }

    @Test
    public void testGetFreqNoRemovingMinCount() throws Exception {
        this.createDummyFile();
        int minCount = 1;
        Vocab vocab = new Vocab(this.fname, minCount);
        String[] tokens = {"w6", "</s>", "w4", "w3", "w2", "w1", "w0"};
        int[] pred = {6, 5, 4, 3, 2, 1, 1};
        int[] actual = new int[pred.length];

        for (int i = 0; i < tokens.length; i++){
            int wordId = vocab.getWordId(tokens[i]);
            actual[i] = vocab.getFreq(wordId);
        }
        assertArrayEquals(actual, pred);

        this.deleteDummyFile();
    }

    @Test
    public void testGetFreqRemovingMinCount() throws Exception {
        // remove words such that freq(w) < 2
        this.createDummyFile();

        int minCount = 2;
        Vocab vocab = new Vocab(this.fname, minCount);
        String[] tokens = {"w6", "</s>", "w4", "w3", "w2"};
        int[] pred = {6, 5, 4, 3, 2};
        int[] actual = new int[pred.length];

        for (int i = 0; i < tokens.length; i++){
            int wordId = vocab.getWordId(tokens[i]);
            actual[i] = vocab.getFreq(wordId);
        }
        assertArrayEquals(actual, pred);

        assertEquals(-1, vocab.getWordId("w1"));

        this.deleteDummyFile();
    }
}
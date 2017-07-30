package utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class WordTest {
    @Test
    public void incrementFreq() throws Exception {
        String token = "shiki";
        Word w = new Word(token);
        w.incrementFreq();

        assertEquals(2, w.getFreq());
    }

    @Test
    public void getWord() throws Exception {
        String token = "shiki";
        Word w = new Word(token);

        assertEquals(w.getWord(), token);
    }

    @Test
    public void getFreq() throws Exception {
        String token = "shiki";
        Word w = new Word(token);

        assertEquals(1, w.getFreq());
    }
}
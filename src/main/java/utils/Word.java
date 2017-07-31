package utils;

public class Word {
    private final String word;
    private int freq = 0;

    public Word(String word){
        this.word = word;
        this.freq++;
    }

    protected void incrementFreq(){
        this.freq++;
    }

    public String getWord(){
        return this.word;
    }

    public int getFreq(){
        return this.freq;
    }

}

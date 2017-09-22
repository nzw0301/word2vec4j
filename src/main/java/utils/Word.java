package utils;

public class Word {
    private final String word;
    private int freq = 0;

    public Word(String word){
        this.word = word;
        freq++;
    }

    protected void incrementFreq(){
        freq++;
    }

    public String getWord(){
        return word;
    }

    public int getFreq(){
        return freq;
    }

}

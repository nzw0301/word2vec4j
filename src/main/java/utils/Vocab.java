package utils;

import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Comparator;
import java.util.Collections;

public class Vocab {
    private final int MAX_SENTENCE_LENGTH = 1000;
    private Map<String, Integer> word2index = new HashMap<>();
    private List<Word> index2word = new ArrayList<>();
    private final int minCount;
    private int numTrainWords;
    private final int numVocab;
    private final String EOS = "</s>";
    private ArrayList<Double> discardTable = new ArrayList<>();


    public Vocab(String trainFileName, int minCount) throws IllegalArgumentException, IOException {
        this.minCount = minCount;
        if(this.minCount <= 0) {
            throw new IllegalArgumentException("Able to set only minCount positive value: " + this.minCount);
        }
        PushbackReader reader;
        try {
            reader = new PushbackReader(new FileReader(trainFileName));
        }catch (IOException e){
            throw new IllegalArgumentException("Unable to load " + trainFileName, e);
        }

        System.out.println("Loading words: ");
        StringBuilder word = new StringBuilder();
        while (readWord(reader, word)){
            this.addVocab(word.toString());
        }

        reader.close();
        this.sortVocab();
        this.numVocab = this.index2word.size();
        System.out.println("The number of vocab is " + this.numVocab);
        System.out.println("The number of words is " + this.numTrainWords);
    }

    public Vocab(String fname, int minCount, double sample) throws IllegalArgumentException, IOException {
        this(fname, minCount);
        this.initDiscardTable(sample);
    }

    public boolean readWord(PushbackReader reader, StringBuilder word) throws IOException {
        word.setLength(0);
        int character;

        while ((character = reader.read()) != -1) {
            if (character == '\t' || character  == ' ' || character == '\n'){
                if(word.length() == 0){
                    if (character == '\n'){
                        word.append(this.EOS);
                        return true;
                    }
                    continue;
                }else {
                    if (character == '\n'){
                        reader.unread('\n');
                    }
                    return true;
                }
            }
            word.append((char)character);
        }
        return (word.length() != 0);
    }


    public int readLine(PushbackReader reader, List<Integer> words, Random rand) throws IOException {
        words.clear();
        StringBuilder word = new StringBuilder();
        String token;
        int wordId;
        int numProcessedTokens = 0;

        while(this.readWord(reader, word)){
            token = word.toString();
            wordId = this.getWordId(token);
            if (wordId == -1) continue;
            numProcessedTokens++;
            if (this.discardTable.get(wordId) < rand.nextDouble()) continue; // skip sub-sampled word
            words.add(wordId);

            if (token.equals(this.EOS)) break;
            if (words.size() > this.MAX_SENTENCE_LENGTH) break;
        }

        return numProcessedTokens;
    }

    private void sortVocab(){
        List<Word> words = new ArrayList<>(this.index2word);
        Collections.sort(words, new Comparator<Word>() {
            @Override
            public int compare(Word o1, Word o2) {
                return Integer.compare(o2.getFreq(), o1.getFreq());
            }
        });

        this.index2word = words;
        for (int i = 0; i < this.index2word.size(); i++){
            if (this.minCount > this.index2word.get(i).getFreq()){
                for (int j = this.index2word.size()-1; i <= j; j--){
                    this.word2index.remove(this.index2word.get(j).getWord());
                    this.index2word.remove(j);
                }
                break;
            }else{
                this.word2index.replace(this.index2word.get(i).getWord(), i);
                this.numTrainWords += this.index2word.get(i).getFreq();
            }
        }
    }

    private void addVocab(String word){
        int wordId = this.word2index.getOrDefault(word, this.word2index.size());
        if (wordId == this.word2index.size()){
            this.index2word.add(new Word(word));
            this.word2index.put(word, wordId);
        }else {
            this.index2word.get(wordId).incrementFreq();
        }
    }

    private void initDiscardTable(double sample){
        double f;
        for(int wordId = 0; wordId < this.numVocab; wordId++){
            f = (double) this.getFreq(wordId) / this.numTrainWords;
            this.discardTable.add(
                    Math.sqrt(sample/f) + sample/f
            );
        }
    }

    public int getNumTrainWords(){ return this.numTrainWords; }

    public int getNumVocab(){ return this.numVocab; }

    public int getWordId(String word){
        return this.word2index.getOrDefault(word, -1);
    }

    public int getFreq(int wordId){
        return this.index2word.get(wordId).getFreq();
    }

    public String getWord(int wordId){
        if(wordId >= 0 && wordId < word2index.size()){
            return this.index2word.get(wordId).getWord();
        }else{
            return null;
        }
    }
}

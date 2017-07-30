package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ArrayNegativeSampler implements NegativeSampler{
    private final int NEGATIVE_TABLE_SIZE = 10000000;
    private final List<Integer> negativeTable;
    private final Random rand = new Random();

    public ArrayNegativeSampler(Vocab vocab, double power){
        this.negativeTable = new ArrayList<>(this.NEGATIVE_TABLE_SIZE);
        int numVocab = vocab.getVocabSize();

        double denom = 0.0;

        for (int i = 0; i < numVocab; i++){
            denom += Math.pow(vocab.getFreq(i), power);
        }

        double c;
        int index = 0;

        for (int i = 0; i < numVocab; i++){
            c = Math.pow(vocab.getFreq(i), power);
            for (int j = 0; j < c*this.NEGATIVE_TABLE_SIZE/denom; j++){
                this.negativeTable.add(index, i);
                index++;
            }
        }
    }

    public int sample(){
        return this.negativeTable.get(this.rand.nextInt(this.NEGATIVE_TABLE_SIZE));
    }
}


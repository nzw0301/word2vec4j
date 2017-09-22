package utils;

import java.util.ArrayList;
import java.util.List;


public class AliasNegativeSampler implements NegativeSampler{
    private AliasSampler aliasSampler;


    public AliasNegativeSampler(Vocab vocab, double power){
        int numVocab = vocab.getNumVocab();
        this.aliasSampler = new AliasSampler(numVocab);
        List<Double> prob = new ArrayList<>(numVocab);

        for (int wordId = 0; wordId < numVocab; wordId++){
            prob.add(Math.pow(vocab.getFreq(wordId), power));
        }

        aliasSampler.buildSamplingTable(prob);
    }

    public int sample(){
        return aliasSampler.sample();
    }
}

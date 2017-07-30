package utils;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class NegativeSamplerTest {
    private final double power = 0.75;

    private double[] createPrediction(int[] freqs){
        double[] pred = new double[freqs.length];

        double denom = 0.;
        for(int freq : freqs ){
            denom += Math.pow(freq, power);
        }

        for(int i = 0; i < freqs.length; i++){
            pred[i] = Math.pow(freqs[i], power)/denom;
        }
        return pred;
    }

    @Test
    public void aliasNegativeSample() throws Exception {
        utils.VocabTest t = new VocabTest();
        t.createDummyFile();

        int minCount = 1;
        Vocab vocab = new Vocab("test.txt", minCount);
        NegativeSampler sampler = new AliasNegativeSampler(vocab, power);

        int[] freqs = {6, 5, 4, 3, 2, 1, 1};
        double[] pred = createPrediction(freqs);

        Map<Integer, Integer> c = new HashMap<>();
        double[] act = new double[freqs.length];

        int numSample = 1000000;
        for(int i = 0; i < numSample; i ++){
            int w = sampler.sample();
            c.put(w, c.getOrDefault(w, 0)+1);
        }

        for(int k : c.keySet()){
            act[k] = (double)c.get(k)/numSample;
        }

        assertArrayEquals(act, pred, 1E-3);

        t.deleteDummyFile();
    }

    @Test
    public void arrayListNegativeSample() throws Exception {
        utils.VocabTest t = new VocabTest();
        t.createDummyFile();

        int minCount = 1;
        Vocab vocab = new Vocab("test.txt", minCount);
        NegativeSampler sampler = new ArrayNegativeSampler(vocab, power);

        int[] freqs = {6, 5, 4, 3, 2, 1, 1};
        double[] pred = createPrediction(freqs);


        Map<Integer, Integer> c = new HashMap<>();
        double[] act = new double[freqs.length];

        int numSample = 1000000;
        for(int i = 0; i < numSample; i ++){
            int w = sampler.sample();
            c.put(w, c.getOrDefault(w, 0)+1);
        }

        for(int k : c.keySet()){
            act[k] = (double)c.get(k)/numSample;
        }

        assertArrayEquals(act, pred, 1E-3);

        t.deleteDummyFile();
    }
}
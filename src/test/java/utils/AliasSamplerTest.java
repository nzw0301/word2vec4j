package utils;


import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class AliasSamplerTest {
    @Test
    public void buildSamplingTable() throws Exception {
        int K = 4;
        AliasSampler aliasSampler = new AliasSampler(K);
        List<Double> prob = Arrays.asList(
                0.5,
                0.25,
                0.15,
                0.1
        );

        aliasSampler.buildSamplingTable(prob);
    }

    @Test
    public void sample() throws Exception {
        //
    }

    @Test
    public void samplesNormalized() throws Exception {
        double[] pred = {0.5, 0.25, 0.15, 0.1};
        int K = pred.length;
        AliasSampler aliasSampler = new AliasSampler(K);

        List<Double> prob = new ArrayList<>(pred.length);
        for(double p: pred){
            prob.add(p);
        }

        aliasSampler.buildSamplingTable(prob);

        Map<Integer, Integer> c = new HashMap<>();
        double[] act = new double[pred.length];

        int numSample = 1000000;
        for(int w : aliasSampler.samples(numSample)){
            c.put(w, c.getOrDefault(w, 0)+1);
        }

        for(int k : c.keySet()){
            act[k] = (double)c.get(k)/numSample;
        }

        assertArrayEquals(act, pred, 1E-3);
    }

    @Test
    public void samplesUnNormalized() throws Exception {
        double[] data = {50., 25., 20., 5.};
        double[] pred = {0.5, 0.25, 0.2, 0.05};
        int K = data.length;
        AliasSampler aliasSampler = new AliasSampler(K);

        List<Double> prob = new ArrayList<>(data.length);
        for(double p: data){
            prob.add(p);
        }

        aliasSampler.buildSamplingTable(prob);

        Map<Integer, Integer> c = new HashMap<>();
        double[] act = new double[pred.length];

        int numSample = 1000000;
        for(int w : aliasSampler.samples(numSample)){
            c.put(w, c.getOrDefault(w, 0)+1);
        }

        for(int k : c.keySet()){
            act[k] = (double)c.get(k)/numSample;
        }

        assertArrayEquals(act, pred, 1E-3);
    }
}
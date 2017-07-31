package utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;


public class AliasSampler {
    private final int numSampledClass;
    private List<Double> S;
    private List<Integer> A;
    private final Random rand = new Random();

    public AliasSampler(int numSampledClass){
        this.numSampledClass = numSampledClass;
        rand.setSeed(7);
    }

    public void buildSamplingTable(List<Double> unNormalizedProb){
        int low, high;
        double v;

        this.S = new ArrayList<>(this.numSampledClass);
        this.A = Stream.generate(() -> 0).limit(this.numSampledClass).collect(toCollection(ArrayList::new));
        double denom = unNormalizedProb.stream().mapToDouble(f -> f.doubleValue()).sum();

        Queue<Integer> higherBin = new ArrayDeque<>();
        Queue<Integer> lowerBin = new ArrayDeque<>();

        for(int i = 0; i < this.numSampledClass; i++){
            v = this.numSampledClass * unNormalizedProb.get(i) / denom;
            this.S.add(v);
            if (v > 1.) {
                higherBin.add(i);
            }else{
                lowerBin.add(i);
            }
        }

        while(lowerBin.size() > 0 && higherBin.size() > 0){
            low = lowerBin.remove();
            high = higherBin.remove();
            this.A.set(low, high);
            this.S.set(high, this.S.get(high)-1.+this.S.get(low));
            if (this.S.get(high) < 1.) {
                lowerBin.add(high);
            }else{
                higherBin.add(high);
            }
        }
    }

    public int sample(){
        int k = this.rand.nextInt(this.numSampledClass);
        if (this.S.get(k) > this.rand.nextDouble()){
            return k;
        }else{
            return this.A.get(k);
        }
    }

    public List<Integer> samples(int numSamples){
        List<Integer> samples = new ArrayList<>(numSamples);
        for(int i = 0; i < numSamples; i++){
            samples.add(this.sample());
        }
        return samples;
    }
}

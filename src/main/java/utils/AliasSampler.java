package utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;


public class AliasSampler {
    private final int K;
    private List<Double> S;
    private List<Integer> A;
    private final Random ran = new Random();

    public AliasSampler(int K){
        this.K = K;
        ran.setSeed(7);
    }

    public void buildSamplingTable(List<Double> unNormalizedProb){
        this.S = new ArrayList<>(this.K);
        this.A = Stream.generate(() -> 0).limit(this.K).collect(toCollection(ArrayList::new));

        double denom = unNormalizedProb.stream().mapToDouble(f -> f.doubleValue()).sum();

        double v;
        int low, high;

        Queue<Integer> H = new ArrayDeque<>();
        Queue<Integer> L = new ArrayDeque<>();

        for(int i = 0; i < this.K; i++){
            v = this.K*unNormalizedProb.get(i)/denom;
            this.S.add(v);
            if(v > 1.){
                H.add(i);
            }else{
                L.add(i);
            }
        }

        while(L.size() > 0 && H.size() > 0){
            low = L.remove();
            high = H.remove();
            this.A.set(low, high);
            this.S.set(high, this.S.get(high)-1.+this.S.get(low));
            if (this.S.get(high) < 1.){
                L.add(high);
            }else{
                H.add(high);
            }
        }
    }

    public int sample(){
        int k = this.ran.nextInt(this.K);
        if (this.S.get(k) > this.ran.nextDouble()){
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

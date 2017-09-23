# Java Implementation of SkipGram/CBoW with Negative Sampling

## Parameters

- `dimEmbeddings`: the number of hidden vector dimension
- `trainFileName`: the path to the training corpus file
- `alpha`: initial learning rate
- `maxWindowSize`: window size
- `sample`: sum-sampling parameter
- `negative`: the number of negative sample
- `minCount`: the threshold of low frequency.
- `iter`: the number of iteration of SGD
- `useAlias4NS`: whether or not alias method is used for negative sampling.
  - `true`: [Alias method](https://en.wikipedia.org/wiki/Alias_method). It is good to save memory for large vocabulary.
  - `false`: Too long array. It is the original version.
- `shareHidden`: whether or not `in->hidden` vectors and  `hidden->out` vectors share

## Run 

```
gradle run
```

## TODO

- Support Java 9
- Multi thread training

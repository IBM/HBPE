# HBPE - Histogram Based Percentile Estimator

### Introduction

HBPE is a JVM library for efficient estimation of [percentiles](https://en.wikipedia.org/wiki/Percentile) and [percentile ranks](https://en.wikipedia.org/wiki/Percentile_rank) on a big stream of numbers.

The estimator inserts the population into bins of a predefined size instead of storing the entire population.
This is beneficial in case the size of the population is significantly higher than the value range.

The estimator is has a configurable precision scale. The precision scale affects the precision of the estimator, as a trade-of of required memory / run-time.

A naive implementation of percentile calculator would require `O(population size)` space (storing all numbers) and `O(population size)` time per query (iterating all numbers at worst case), whereas HBPE would take `O(number of bins)` for both space and time (bin count is determined by the desired precision and calculated as `value_range/bin_size`). 

HBPE is implemented in Kotlin and can be used from any JVM-based language.

A detailed explanation of how percentiles can be estimated using a Cumulative Frequency Graph can be found [here](http://courses.washington.edu/psy315/tutorials/Frequency_distribution_tutorial.pdf).

There are several [variations](https://en.wikipedia.org/wiki/Percentile#Second_variant) of percentile calculation. We made effort and included unit tests to ensure that 
HBPE conforms with the Microsoft Excel [PERCENTILE.INC](https://support.office.com/en-us/article/percentile-inc-function-680f9539-45eb-410b-9a5e-c1355e5fe2ed) function as much as possible.

### Usage

Add as a dependency to your `build.gradle`
```groovy
dependencies {
    implementation 'ibm:hbpe:1.0.0'
}
```
or to your `pom.xml`

```xml
<dependency>
  <groupId>ibm</groupId>
  <artifactId>hbpe</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```
    
### Build from Source

### Contributing

Pull requests are very welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

### License

[Apache v2](https://www.apache.org/licenses/LICENSE-2.0)

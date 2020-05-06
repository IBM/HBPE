# HBPE - Histogram Based Percentile Estimator for Java / JVM

### Introduction

HBPE is a JVM library for efficient estimation of [percentiles](https://en.wikipedia.org/wiki/Percentile) and [percentile ranks](https://en.wikipedia.org/wiki/Percentile_rank) on a big stream of numbers.

The estimator inserts the population into bins of a predefined size instead of keeping the entire population in memory.
This is beneficial if the population size is significantly higher than the value range, for example a use case of anomaly detection over latency values.

The estimator has a configurable precision scale. A higher precision scale will make the estimator more accurate, but will require more memory and increase query time.

A naive implementation of percentile calculator would require `O(population size)` space (storing all numbers) and `O(population size)` time per query (iterating all numbers at worst case), whereas HBPE would take `O(number of bins)` for both space and time (bin count is determined by the desired precision and calculated as `value_range/bin_size`). 

HBPE is implemented in Kotlin and can be used from Java or any other JVM-based language.

A detailed explanation of how percentiles can be estimated using a Cumulative Frequency Graph can be found [here](http://courses.washington.edu/psy315/tutorials/Frequency_distribution_tutorial.pdf).

There are several [variations](https://en.wikipedia.org/wiki/Percentile#Second_variant) of percentile calculation. We made effort and included unit tests to ensure that 
HBPE conforms with the Microsoft Excel [PERCENTILE.INC](https://support.office.com/en-us/article/percentile-inc-function-680f9539-45eb-410b-9a5e-c1355e5fe2ed) function as much as possible.

## Performance

Comparing runtime of HBPE to [DescriptiveStatistics](https://commons.apache.org/proper/commons-math/javadocs/api-3.3/org/apache/commons/math3/stat/descriptive/DescriptiveStatistics.html) (part of [apache.commons.math3](http://commons.apache.org/proper/commons-math/)) and to a naive implementation that stores all numbers in an array. 

Scenario ([source code](demo/src/main/java/hbpeDemo/BenchmarkGetPercentile.java)): 

  Repeat for 30000 times:
  1. Generate a random number between -100 .. 100
  1. Add the number to the population
  1. Calculate 75th percentile
  1. Calculate 10th percentile


```
*** Starting Naive benchmark
Naive took 3.767 ms
final 75 percentile is 50.33568575408211
final 10 percentile is -79.45891703602994

*** Starting Apache Math3 benchmark
Apache math3 took 11.549 ms
final 75 percentile is 50.33531065537358
final 10 percentile is -79.4619974157278

*** Starting HBPE benchmark
HBPE took 0.269 ms
final 75 percentile is 50.324999999999996
final 10 percentile is -79.41
```

### Usage

Create a new estimator. No need to define histogram bounds in advance, as they are expanded dynamically. 

The parameter value of `1` is the precision scale, which tradeoffs the accuracy of the estimator with its performance. It determines the size of each histogram's bin as `1/(10^precisionScale)`.

* 0 - for bin size of 1.0 
* 1 - for bin size of 0.1  (this is the default)  
* 2 - for bin size of 0.01
* and so on..

The accuracy of the estimator is better as the bin size gets smaller, but the required memory and calculation time grows (linear to the number of bins), so make sure to tune it by your expected range of values and accuracy requirements.

```java
HistogramBasedPercentileEstimator hbpe = new HistogramBasedPercentileEstimator(1);
```

Add some values (each call takes constant run-time):
```java
hbpe.addValue(30.0);
hbpe.addValue(40.0);
hbpe.addValue(20.0);
hbpe.addValue(10.0);
```

Calculate some percentiles (each call run-time is linear to the number of bins):
```java
System.out.println("p50: " + hbpe.getPercentile(50.0)); // p50: 25.05
System.out.println("p75: " + hbpe.getPercentile(75.0)); // p75: 32.525
System.out.println("p25: " + hbpe.getPercentile(25.0)); // p25: 17.575000000000003
```        

Calculate some percentile ranks (each call run-time is linear to the number of bins):
```java
System.out.println("PR of 38: " + hbpe.getPercentileRank(38.0)); // PR of 38: 75.0
System.out.println("PR of 25: " + hbpe.getPercentileRank(25.0)); // PR of 25: 50.0
```        


### Installation

HBPE is available as a package, hosted at the [jcenter](https://bintray.com/davidohana/hbpe/com.ibm.hbpe) repository.

#### For Maven:

```xml
<!-- Add jcenter as a repository for dependencies --> 
<repositories>
    <repository>
        <id>jcenter</id>
        <url>https://jcenter.bintray.com/</url>
    </repository>
</repositories>

<dependency>
  <groupId>com.ibm.hbpe</groupId>
  <artifactId>hbpe</artifactId>
  <version>1.0.3</version>
</dependency>
```

#### For Gradle:

```groovy

// Add jcenter as a repository for dependencies
repositories {
    jcenter()
}

dependencies {
    implementation 'com.ibm.hbpe:hbpe:1.0.3'
}
```

[ ![Download](https://api.bintray.com/packages/davidohana/hbpe/com.ibm.hbpe/images/download.svg?version=1.0.3) ](https://bintray.com/davidohana/hbpe/com.ibm.hbpe/1.0.3/link)

### Demo

3 sample programs are available under [demo/src/main/java/hbpeDemo](demo/src/main/java/hbpeDemo)

[HeightPercentileCalculator](demo/src/main/java/hbpeDemo/HeightPercentileCalculator.java) - Adds 10M random height values and then calculates the median and the percentile rank of 190cm.

[LatencyAnomalyDetection](demo/src/main/java/hbpeDemo/LatencyAnomalyDetection.java) - Perform a simple operation many times and measure the time it took. Identify anomalies in the measured time using if it is above the 99.99 percentile rank.

[BenchmarkGetPercentile](demo/src/main/java/hbpeDemo/BenchmarkGetPercentile.java) - Code of the benchmark comparison presented above: add a random number to the population and calculate the 10th & 75th percentiles. Repeat that for 30000 times.

You can run the demo apps by:

```bash
git clone https://github.com/IBM/HBPE.git

cd hbpe/demo

mvn exec:java -Dexec.mainClass="hbpeDemo.HeightPercentileCalculator"
mvn exec:java -Dexec.mainClass="hbpeDemo.LatencyAnomalyDetection"
mvn exec:java -Dexec.mainClass="hbpeDemo.BenchmarkGetPercentile"
```

### Build from Source

```bash

# clone this GitHub repository
git clone https://github.com/IBM/HBPE.git

cd hbpe

# compile and run tests with Maven (mvn tool must be in path)
mvn clean compile
```

### Contributing

Pull requests are very welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

[How to contribute](CONTRIBUTING.md)

### License

[Apache v2](https://www.apache.org/licenses/LICENSE-2.0)

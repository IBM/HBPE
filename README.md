# HBPE - Histogram Based Percentile Estimator for Java / JVM

### Introduction

HBPE is a JVM library for efficient estimation of [percentiles](https://en.wikipedia.org/wiki/Percentile) and [percentile ranks](https://en.wikipedia.org/wiki/Percentile_rank) on a big stream of numbers.

The estimator inserts the population into bins of a predefined size instead of storing the entire population.
This is beneficial in case the size of the population is significantly higher than the value range.

The estimator is has a configurable precision scale. The precision scale affects the precision of the estimator, as a trade-of of required memory / run-time.

A naive implementation of percentile calculator would require `O(population size)` space (storing all numbers) and `O(population size)` time per query (iterating all numbers at worst case), whereas HBPE would take `O(number of bins)` for both space and time (bin count is determined by the desired precision and calculated as `value_range/bin_size`). 

HBPE is implemented in Kotlin and can be used from Java or any other JVM-based language.

A detailed explanation of how percentiles can be estimated using a Cumulative Frequency Graph can be found [here](http://courses.washington.edu/psy315/tutorials/Frequency_distribution_tutorial.pdf).

There are several [variations](https://en.wikipedia.org/wiki/Percentile#Second_variant) of percentile calculation. We made effort and included unit tests to ensure that 
HBPE conforms with the Microsoft Excel [PERCENTILE.INC](https://support.office.com/en-us/article/percentile-inc-function-680f9539-45eb-410b-9a5e-c1355e5fe2ed) function as much as possible.

## Performance

### Usage



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
	<version>1.0.2</version>
</dependency>
```

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

#### For Gradle:

```groovy

// Add jcenter as a repository for dependencies
repositories {
    jcenter()
}

dependencies {
    implementation 'com.ibm.hbpe:hbpe:1.0.2'
}
```

[ ![Download](https://api.bintray.com/packages/davidohana/hbpe/com.ibm.hbpe/images/download.svg?version=1.0.2) ](https://bintray.com/davidohana/hbpe/com.ibm.hbpe/1.0.2/link)

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

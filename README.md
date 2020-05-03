# HBPE - Histogram Based Percentile Estimator

### Introduction

HBPE is a JVM library for efficient estimation of [percentiles](https://en.wikipedia.org/wiki/Percentile) and percentile ranks for a big stream of numbers.

HBPE is based on the assumption that population (all numbers that we will ever encounter) are distributed in a 
reasonable-sized range. The range of numbers does not have to be known in advance. For example - our population might be 
human heights, therefore we know in advance that range is approximately 0-300 cm.

A detailed explanation of how percentiles can be estimated using a Cumulative Frequency Graph can be found [here](http://courses.washington.edu/psy315/tutorials/Frequency_distribution_tutorial.pdf).

A naive implementation of percentile calculator over `N` numbers would require `o(N)` space (storing all numbers) and `o(N)` 
time per query (iterating all numbers at worst case), whereas HBPE would take `O(NB)` for both space and time where `NB` is 
the number of bins in the histogram, which is `range/bin_size`. 
`bin_size` is a configuration parameter that trades-of accuracy for performance. 
 
HBPE is implemented in Kotlin but can be used from any JVM-world language.

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

[MIT](https://choosealicense.com/licenses/mit/)

# AML-Compound
The Compound AML is a novel algorithm, inspired by AML's WordMatcher, 
which produces compound matches between three different ontologies. 
Cite as `Oliveira, D., & Pesquita, C. (2018). Improving the interoperability of biomedical ontologies with compound alignments. Journal of biomedical semantics, 9(1), 1. doi:10.1186/s13326-017-0171-8`       
  
## Choose the ontologies

Modify the aml.CompoundTest to match the desired thresholds, selection type, source, target 1 and target 2 ontologies.

## Building with Maven

Use the following command to build a shaded jar:

```mvn clean package```

## Running AML-Compound

After building the jar, type the following:

```java -jar target/CompoundAML-1.0-SNAPSHOT-shaded.jar```
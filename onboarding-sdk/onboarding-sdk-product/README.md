# Onboarding SDK Product

This library has been developed to provide a set of Java utility classes to simplify the work of handle **Selfcare Product** as string.

## Installation

To use this library in your projects, you can add the dependency to your pom.xml if you're using Maven:

```shell script
<dependency>
    <groupId>it.pagopa.selfcare</groupId>
    <artifactId>onboarding-sdk-product</artifactId>
    <version>0.0.1</version>
</dependency>
```

If you are using Gradle, you can add the dependency to your build.gradle file:

```shell script
dependencies {
    implementation 'it.pagopa.selfcare:onboarding-sdk-product:0.0.1'
}
```

## Usage

Here's an example of how to use ProductService for retrieving product:

```java script

public class Main {
    public static void main(String[] args) {
        final String productJsonString = ... ; // set a json string compliant to a List of Product Pojo
        final ProductService productService = new ProductServiceDefault(productJsonString);

        //Get product if it is valid
        final String productId = "prod-pagopa";
        final Product product = productService.getProductIsValid(productId);
    }
}
```
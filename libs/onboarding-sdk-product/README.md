# Onboarding SDK Product

This library has been developed to provide a set of Java utility classes to simplify the work of handle **Selfcare Product** as string.

Selfcare Products is a collection of PagoPA products available for use by institutions. Each product contains specific information, such as its status, admitted role, or a filepath template for building contract necessary for selfcare business logic. 

The Onboarding SDK Product offers a set of classes designed for managing this collection of records using a file.

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
## Product JSON Schema

Product string which are used by ProductService must follow a specific schema, look at src/main/schema folder.

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

Generally, you load product json string from an azure storage container, this example use onboading-sdk-azure-storage, and inject the product service in the context of Quarkus or Spring (replace @ApplicationScoped with @Bean). 

```java script
    @ApplicationScoped
    public ProductService productService(AzureStorageConfig azureStorageConfig){
       AzureBlobClient azureBlobClient = new AzureBlobClientDefault(azureStorageConfig.connectionStringProduct(), azureStorageConfig.containerProduct());
       String productJsonString = azureBlobClient.getFileAsText(azureStorageConfig.productFilepath());
        try {
            return new ProductServiceDefault(productJsonString, objectMapper());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Found an issue when trying to serialize product json string!!");
        }
    }
 ```
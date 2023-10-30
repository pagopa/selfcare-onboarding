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
## Product JSON Schema

Product string which are used by ProductService must follow a specific schema: 

```
{
  "type" : "record",
  "name" : "Product",
  "namespace" : "it.pagopa.selfcare.product.entity",
  "fields" : [ {
    "name" : "id",
    "type" : [ "string" ]
  }, {
    "name" : "logo",
    "type" : [ "string" ]
  }, {
    "name" : "depictImageUrl",
    "type" : [ "string" ]
  }, {
    "name" : "title",
    "type" : [ "string" ]
  }, {
    "name" : "logoBgColor",
    "type" : [ "string" ]
  }, {
    "name" : "description",
    "type" : [ "string" ]
  }, {
    "name" : "urlPublic",
    "type" : [ "string" ]
  }, {
    "name" : "urlBO",
    "type" : [ "string" ]
  }, {
    "name" : "createdAt",
    "type" : [ "string" ]
  }, {
    "name" : "createdBy",
    "type" : [ "string" ]
  }, {
    "name" : "modifiedAt",
    "type" : [ "string" ]
  }, {
    "name" : "modifiedBy",
    "type" : [ "string" ]
  }, {
    "name" : "roleManagementURL",
    "type" : [ "null", "string" ]
  }, {
    "name" : "contractTemplateUpdatedAt",
    "type" : [ "string" ]
  }, {
    "name" : "contractTemplatePath",
    "type" : [ "string" ]
  }, {
    "name" : "contractTemplateVersion",
    "type" : [ "string" ]
  }, {
    "name" : "institutionContractMappings",
    "type" : [ "null", {
      "type" : "map",
      "values" : {
        "type" : "record",
        "name" : "ContractStorage",
        "fields" : [ {
          "name" : "contractTemplateUpdatedAt",
          "type" : [ "null", "string" ]
        }, {
          "name" : "contractTemplatePath",
          "type" : [ "null", "string" ]
        }, {
          "name" : "contractTemplateVersion",
          "type" : [ "null", "string" ]
        } ]
      }
    } ]
  }, {
    "name" : "enabled",
    "type" : "boolean"
  }, {
    "name" : "delegable",
    "type" : ["null","boolean"]
  }, {
    "name" : "status",
    "type" : [ "string" ]
  }, {
    "name" : "parentId",
    "type" : [ "null", "string" ]
  }, {
    "name" : "identityTokenAudience",
    "type" : [ "string" ]
  }, {
    "name" : "backOfficeEnvironmentConfigurations",
    "type" : [ "null", {
      "type" : "map",
      "values" : {
        "type" : "record",
        "name" : "BackOfficeConfigurations",
        "fields" : [ {
          "name" : "url",
          "type" : [ "null", "string" ]
        }, {
          "name" : "identityTokenAudience",
          "type" : [ "null", "string" ]
        } ]
      }
    } ]
  }, {
    "name" : "parent",
    "type" : [ "null", "Product" ]
  } ]
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
# Onboarding SDK Crypto

This module contains utilities to perform cryptographic operation, such digital signatures.

See [Confluence page](https://pagopa.atlassian.net/wiki/spaces/SCP/pages/616857618/Firma+digitale+per+mezzo+dei+servizi+di+Aruba)
for integration and documentation details


### Hash signature sources

It is possible to configure different hash signature sources.

The sources available inside this repository are:

* Pkcs7HashSignService

## Pkcs7HashSignService

It will use the provided private key and certificate, you must set these env variables.

| ENV                  | Description                                                                     | Default |
|----------------------|---------------------------------------------------------------------------------|---------|
| CRYPTO_PRIVATE_KEY   | The private key (PEM) used when the pkcs7 hash signature source is <i>local</i> |         |
| CRYPTO_CERT          | The certificate (PEM) used when the pkcs7 hash signature source is <i>local</i> |         |


## Installation

To use this library in your projects, you can add the dependency to your pom.xml if you're using Maven:

```shell script
<dependency>
    <groupId>it.pagopa.selfcare</groupId>
    <artifactId>onboarding-sdk-crypto</artifactId>
    <version>0.1.0</version>
</dependency>
```
If you are using Gradle, you can add the dependency to your build.gradle file:

```shell script
dependencies {
    implementation 'it.pagopa.selfcare:onboarding-sdk-crypto:0.1.0'
}
```

## Usage

You can inject the service in the context of Quarkus or Spring (replace @ApplicationScoped with @Bean).

```java script
    @ApplicationScoped
    public Pkcs7HashSignService pkcs7HashSignService(){
       return new Pkcs7HashSignServiceImpl();
    }
    
    @ApplicationScoped
    public PadesSignService padesSignService(Pkcs7HashSignService pkcs7HashSignService){
        return new PadesSignServiceImpl(pkcs7HashSignService);
    }
 ```
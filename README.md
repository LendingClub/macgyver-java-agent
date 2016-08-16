# macgyver-java-agent

[![CircleCI](https://circleci.com/gh/LendingClub/macgyver-java-agent.svg?style=svg)](https://circleci.com/gh/LendingClub/macgyver-java-agent)
 [ ![Download](https://api.bintray.com/packages/robschoening/io-macgyver/macgyver-java-agent/images/download.svg) ](https://bintray.com/robschoening/io-macgyver/macgyver-java-agent/_latestVersion)

This provides service discovery capability to JVM based apps.  The core class ```MacGyverAgent``` will report
application status and thread dumps to the MacGyver service.


## Usage

The following will create an agent that reports status to the MacGyver server:

```java
new MacGyverAgent()
    .withSender(
        new HttpAgentSender()
            .withBaseUrl("https://macgyver.example.com"))
    .start();
```

To provide app-specific metatdata, implement the ```AppMetadataProvider``` interface and pass it to the agent:

```java
AppMetadataProvider myProvider = ....;

new MacGyverAgent()
    .withAppMetadataProvider(myProvider)
    .withSender(
        new HttpAgentSender()
            .withBaseUrl("https://macgyver.example.com"))
    .start();
```



## Transports

### HTTP  Transport

This transport will transmit information to MacGyver using ```HTTP POST```.


### SNS Transport

This transport will communicate with the MacGyver server via SNS.  The SNS topic will need to be configured to deliver messages to an SQS queue.
The MacGyver server will then consume the messages from the SQS queue.

To configure using SNS:

```java

// Configure your Amazon SNS client
AmazonSNSClient client = new AmazonSNSClient(...);

// Add an SnsAgentSender to your agent
new MacGyverAgent()
    .withSender(
        new SnsAgentSender()
            .withAmazonSNSClient(myClient)
            .withTopicArn("arn:aws:sns:us-east-1:000000000000:my-topic")))
    .start();
```

## Decorators

Decorators may be used to customize the data sent to the mothership.
# SDIS-FEUP


### Description
Java messaging app with focus on concurrent multiple servers response and load distributed across server by implementing a distributed hash table using the [Chord Algorithm](https://en.wikipedia.org/wiki/Chord_(peer-to-peer)).

#### Run a server
```
java -Djavax.net.ssl.keyStore=server.keys -Djavax.net.ssl.keyStorePassword=123456 -Djavax.net.ssl.trustStore=truststore -Djavax.net.ssl.trustStorePassword=123456 Server.Server localhost 4445
```
#### Run a client
```
java -Djavax.net.ssl.keyStore=client.keys -Djavax.net.ssl.keyStorePassword=123456 -Djavax.net.ssl.trustStore=truststore -Djavax.net.ssl.trustStorePassword=123456 Client.Client localhost 4445
```

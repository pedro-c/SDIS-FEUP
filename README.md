# SDIS-FEUP

java -Djavax.net.ssl.keyStore=server.keys -Djavax.net.ssl.keyStorePassword=123456 -Djavax.net.ssl.trustStore=truststore -Djavax.net.ssl.trustStorePassword=123456 Server.Server localhost 4445

java -Djavax.net.ssl.keyStore=client.keys -Djavax.net.ssl.keyStorePassword=123456 -Djavax.net.ssl.trustStore=truststore -Djavax.net.ssl.trustStorePassword=123456 Client.Client localhost 4445

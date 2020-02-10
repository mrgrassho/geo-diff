package com.geodiff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebProperties {

    private final String protocol;

    private final String serverHost;

    private final String serverPort;

    @Autowired
    public WebProperties(@Value("${server.protocol}") String protocol,
                         @Value("${server.host}") String serverHost,
                         @Value("${server.port}") String serverPort) {
        checkThatProtocolIsValid(protocol);

        this.protocol = protocol.toLowerCase();
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    private void checkThatProtocolIsValid(String protocol) {
        if (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https")) {
            throw new IllegalArgumentException(String.format(
                    "Protocol: %s is not allowed. Allowed protocols are: http and https.",
                    protocol
            ));
        }
    }

    public String getProtocol() {
        return protocol;
    }

    public String getServerHost() {
        return serverHost;
    }

    public String getServerPort() {
        return serverPort;
    }

    public String getUrl(){
        return this.protocol  + "://" + this.serverHost + ":" + this.serverPort;
    }
}
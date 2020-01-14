package com.sematext.spm.client.jmx;

import java.io.IOException;

import java.rmi.server.RMISocketFactory;
import java.net.Socket;
import java.net.ServerSocket;

class HostOverrideClientSocketFactory extends RMISocketFactory {
    private String host;
    public HostOverrideClientSocketFactory(String host) {
        this.host = host;
    }
    public ServerSocket createServerSocket(int port) throws IOException {
        return RMISocketFactory.getDefaultSocketFactory()
                .createServerSocket(port);
    }
    public Socket createSocket(String host, int port) throws IOException {
        return RMISocketFactory.getDefaultSocketFactory()
                .createSocket(this.host, port);
    }
}
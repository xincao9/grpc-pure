package com.xincao9.grpc.pure.discovery;


public abstract class ServerRegister {

    protected String appName = "Default";
    protected Integer port = 9999;

    public ServerRegister(String appName, Integer port) {
        this.appName = appName;
        this.port = port;
    }

    abstract public void start();

    abstract public void stop();

    abstract public Boolean register();
}

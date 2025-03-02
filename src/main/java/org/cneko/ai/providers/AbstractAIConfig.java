package org.cneko.ai.providers;

import org.cneko.ai.core.NetworkingProxy;

public abstract class AbstractAIConfig {
    protected String apiKey;
    protected String model;
    protected double temperature;
    protected String host;
    protected int port;
    protected NetworkingProxy proxy;
    protected boolean tls = true;

    public AbstractAIConfig(String apiKey, String model, double temperature, String host, int port,boolean tls) {
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.host = host;
        this.port = port;
        this.tls = tls;
    }

    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public double getTemperature() { return temperature; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public NetworkingProxy getProxy() { return proxy; }

    public void setModel(String model) { this.model = model; }
    public void setTemperature(double temperature) {
        if (temperature >= 0 && temperature <= 2) {
            this.temperature = temperature;
        }
    }
    public void setHost(String host) { this.host = host; }
    public void setPort(int port) { this.port = port; }
    public void setProxy(NetworkingProxy proxy) { this.proxy = proxy; }
    public void setTls(boolean tls) { this.tls = tls; }
    public boolean isTls() { return tls; }
}

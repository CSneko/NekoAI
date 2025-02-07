package org.cneko.ai.core;


public class NetworkingProxy {
    private String ip;
    private int port;
    private String username;
    private String password;

    // 构造函数，用于创建没有用户名和密码的代理
    public NetworkingProxy(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.username = null;
        this.password = null;
    }

    // 构造函数，用于创建带有用户名和密码的代理
    public NetworkingProxy(String ip, int port, String username, String password) {
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    // Getter 和 Setter 方法
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // 检查代理是否有用户名和密码
    public boolean hasAuth() {
        return username != null && password != null;
    }

    @Override
    public String toString() {
        if (hasAuth()) {
            return "Proxy [ip=" + ip + ", port=" + port + ", username=" + username + ", password=" + password + "]";
        } else {
            return "Proxy [ip=" + ip + ", port=" + port + "]";
        }
    }



}
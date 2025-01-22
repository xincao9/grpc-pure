package fun.golinks.grpc.pure.discovery;

/**
 * 服务注册器
 */
public abstract class ServerRegister {

    protected String appName = "Default";
    protected Integer port = 9999;

    /**
     * 构造服务注册器
     *
     * @param appName
     *            应用名
     * @param port
     *            监听端口
     */
    public ServerRegister(String appName, Integer port) {
        this.appName = appName;
        this.port = port;
    }

    /**
     * 启动
     */
    abstract public void start();

    /**
     * 停止
     */
    abstract public void stop();

    /**
     * 注册服务
     */
    abstract public Boolean register();
}

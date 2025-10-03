package common.server;

/**
 * 通用服务生命周期接口
 * - 所有子项目的 Server 必须实现该接口
 */
public interface LifecycleServer {
    /**
     * 启动服务
     */
    void start(int port, boolean sslEnabled, int sessionTimeout) throws Exception;

    /**
     * 停止服务
     */
    void stop() throws Exception;

    /**
     * 获取模块名称
     */
    String getName();
}

package console;

public class SubClass implements IModule {

    @Override
    public String getDescription() {
        return "这是一个使用 Mockito 进行单元测试演示的子类模块。";
    }

    @Override
    public void run(String args) {
        System.out.println("SubClass 正在运行...");
        System.out.println("接收到参数: " + args);
    }

}

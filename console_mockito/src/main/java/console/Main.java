package console;

import org.powermock.api.mockito.PowerMockito;

public class Main {
    private IModule module;

    // "vmArgs": "--add-opens java.base/java.lang=ALL-UNNAMED --add-opens
    // java.base/java.util=ALL-UNNAMED --add-opens
    // java.base/java.lang.reflect=ALL-UNNAMED --add-opens
    // java.base/java.util.concurrent=ALL-UNNAMED"
    public static void main(String[] args) {
        try {
            new Main().before().doing();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Main before() throws Exception {
        SubClass subClass = PowerMockito.spy(new SubClass());
        PowerMockito.when(subClass, "getDescription").thenReturn("Hello from spy!");
        this.module = subClass;
        return this;
    }

    private void doing() {
        this.module.run(this.module.getDescription());
    }
}

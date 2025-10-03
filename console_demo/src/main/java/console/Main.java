package console;

import common.annotations.ReflectConsoleArgs;

public class Main {

    @ReflectConsoleArgs({"core-demo"})
    public static void main(String[] args) {
        String message = "Hello from console_demo!";
        if (args != null && args.length > 0) {
            message += " <" + args[0] + ">";
        }
        System.out.println(message);
    }
}

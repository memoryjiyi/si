import java.util.Scanner;

/**
 * B2 SafeHome 控制面板 ControlPanel 状态机
 *
 * 状态：待机(IDLE)、读取键入字符(READING)、锁定(LOCKED)、系统功能选择(MENU)
 *
 * 行为说明：
 *  - 户主敲击键盘按键输入密码时进入"读取键入字符"状态；
 *  - 每输入一个字符即执行与设置的密码长度(4位)比较；
 *  - 达到4位则验证密码，密码正确 -> 系统功能选择状态；
 *  - 密码错误累计3次 -> 锁定控制面板；
 *  - 锁定超过120秒 -> 重新允许输入；
 *  - 在系统功能选择状态完成操作后可退回待机。
 */
public class ControlPanelStateMachine {

    enum State {
        IDLE("待机"),
        READING("读取键入字符"),
        LOCKED("锁定"),
        MENU("系统功能选择");

        private final String name;
        State(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    private static final int PASSWORD_LENGTH = 4;   // 密码长度4位
    private static final int MAX_ATTEMPTS = 3;      // 错误上限3次
    private static final long LOCK_DURATION_MS = 120_000; // 锁定120秒

    private State currentState = State.IDLE;
    private final String password = "1234";         // 预设密码
    private StringBuilder input = new StringBuilder();
    private int wrongAttempts = 0;                  // 错误次数
    private long lockStartTime = 0;                 // 锁定开始时间

    /** 输入一个字符事件：每输入一个字符执行长度比较 */
    public void inputChar(char c) {
        System.out.printf("输入字符 '%c' | 当前状态: %s", c, currentState);
        switch (currentState) {
            case IDLE:
                // 事件：户主敲击键盘，开始输入密码
                input = new StringBuilder();
                input.append(c);
                transition(State.READING);
                checkInput();
                break;

            case READING:
                input.append(c);
                checkInput();
                break;

            case LOCKED:
                long elapsed = System.currentTimeMillis() - lockStartTime;
                if (elapsed < LOCK_DURATION_MS) {
                    System.out.printf("  [拒绝] 仍处于锁定状态(已锁%d秒/120秒)，输入无效%n",
                            elapsed / 1000);
                } else {
                    // 转换：[锁定超过120秒] -> 重新允许输入
                    transition(State.READING);
                    input = new StringBuilder();
                    input.append(c);
                    checkInput();
                }
                break;

            case MENU:
                System.out.println("  (在功能选择状态，字符忽略)");
                break;
        }
    }

    /** 对已输入字符执行比较与验证 */
    private void checkInput() {
        if (input.length() < PASSWORD_LENGTH) {
            // 自转换：[未达到密码长度] 继续读取键入字符
            System.out.printf("  [比较] 已输入%d位/4位，继续读取...%n", input.length());
        } else {
            verify();
        }
    }

    /** 验证密码（达到4位后执行） */
    private void verify() {
        String entered = input.toString();
        System.out.printf("  [验证] 输入的密码 = %s，", entered);
        if (entered.equals(password)) {
            wrongAttempts = 0;
            // 转换：[密码正确] -> 系统功能选择
            transition(State.MENU);
        } else {
            wrongAttempts++;
            System.out.printf("密码错误(第%d次)%n", wrongAttempts);
            if (wrongAttempts >= MAX_ATTEMPTS) {
                // 转换：[密码错误3次] -> 锁定
                transition(State.LOCKED);
            } else {
                // 转换：[密码错误且未达3次] -> 重新进入读取键入字符
                input = new StringBuilder();
                System.out.println("  [动作] 清空已输入字符，允许重新输入");
            }
        }
    }

    /** 进入锁定状态：开启120秒定时解除 */
    private void startLockTimer() {
        lockStartTime = System.currentTimeMillis();
        Thread t = new Thread(() -> {
            try { Thread.sleep(LOCK_DURATION_MS); } catch (InterruptedException ignored) {}
            System.out.println("\n[定时事件] 锁定超过120秒，解除锁定，允许户主输入");
        });
        t.setDaemon(true);  // 不阻塞虚拟机退出
        t.start();
    }

    /** 退出系统功能选择，回到待机 */
    public void logout() {
        if (currentState == State.MENU) {
            transition(State.IDLE);
        }
    }

    private void transition(State next) {
        currentState = next;
        System.out.println("  => 迁移到 [" + next + "]");
        if (next == State.LOCKED) {
            input = new StringBuilder();
            wrongAttempts = 0;
            System.out.println("  [动作] 锁定控制面板，120秒内禁止输入");
            startLockTimer();
        } else if (next == State.IDLE) {
            input = new StringBuilder();
        }
    }

    // ===== 演示 =====
    public static void main(String[] args) {
        System.out.println("===== SafeHome 控制面板 ControlPanel 状态机演示 =====");
        ControlPanelStateMachine cp = new ControlPanelStateMachine();

        System.out.println("\n--- 场景1：密码正确，进入系统功能选择 ---");
        for (char c : "1234".toCharArray()) cp.inputChar(c);

        System.out.println("\n--- 场景2：完成操作，退出回到待机 ---");
        cp.logout();

        System.out.println("\n--- 场景3：密码错误3次，面板锁定120秒 ---");
        for (char c : "1111".toCharArray()) cp.inputChar(c); // 第1次错误
        for (char c : "2222".toCharArray()) cp.inputChar(c); // 第2次错误
        for (char c : "3333".toCharArray()) cp.inputChar(c); // 第3次错误 -> 锁定
        cp.inputChar('9'); // 锁定期间输入被拒绝

        System.out.println("===== 演示结束 =====");
    }
}

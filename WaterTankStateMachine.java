import java.util.Timer;
import java.util.TimerTask;

/**
 * B1 恒温水箱状态机 (20°~100°)
 *
 * 状态：待机(IDLE)、加热(HEATING)、保温恒温(KEEPING)、休眠(SLEEPING)、维修(REPAIRING)
 * 状态图三要素：状态(State)、事件/转换(Transition)、动作(Action)/监护条件(Guard)
 */
public class WaterTankStateMachine {

    // ===== 状态枚举 =====
    enum State {
        IDLE("待机检测"),        // 检测温度与水量
        HEATING("加热烧水"),     // 继电器闭合，烧水中
        KEEPING("恒温保温"),     // 温度达到100°断电，处于恒温区间
        SLEEPING("休眠"),        // 晚上23点进入休眠
        REPAIRING("维修中");     // 水箱烧坏，维修

        private final String name;
        State(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    // ===== 事件枚举 =====
    enum Event {
        TEMP_BELOW_20,   // 温度传感器检测到温度低于20°
        TEMP_REACH_100,  // 温度传感器检测到温度达到100°
        NO_WATER,        // 水量传感器检测到无水
        HAS_WATER,       // 水量传感器检测到有水
        TANK_BROKEN,     // 水箱烧坏
        REPAIR_DONE,     // 维修完成
        TIME_23_OCLOCK,  // 晚上11点整
        TIME_7_OCLOCK;   // 早上7点整
    }

    // ===== 上下文数据（监护条件用） =====
    private State currentState = State.SLEEPING;
    private boolean waterPresent = true;   // 水量传感器读数
    private double temperature = 15.0;     // 温度传感器读数
    private boolean broken = false;
    private Timer timer;

    public WaterTankStateMachine() {
        // 模拟时钟：整点触发时间事件
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            int hour = 8; // 从早上8点开始模拟
            @Override public void run() {
                hour = (hour + 1) % 24;
                if (hour == 23) dispatch(Event.TIME_23_OCLOCK);
                else if (hour == 7) dispatch(Event.TIME_7_OCLOCK);
                else simulateSensor(hour);
            }
        }, 0, 1000);
    }

    // 模拟传感器读数与水箱状态变化
    private void simulateSensor(int hour) {
        if (currentState == State.HEATING) {
            temperature += 18.5;             // 加热升温
            if (temperature >= 100) {
                temperature = 100;
                dispatch(Event.TEMP_REACH_100);
            }
        } else if (currentState == State.KEEPING || currentState == State.IDLE) {
            temperature -= 2.5;              // 自然降温
            if (temperature < 20) dispatch(Event.TEMP_BELOW_20);
        }
    }

    /** 事件分发：状态机核心（事件驱动 + 状态迁移 + 动作） */
    public synchronized void dispatch(Event event) {
        System.out.printf("[事件] %-14s 当前状态: %-8s", event, currentState);
        switch (currentState) {
            case SLEEPING:
                if (event == Event.TIME_7_OCLOCK) {
                    transition(State.IDLE);          // 早上7点，进入恒温烧水过程
                }
                break;

            case IDLE:
                if (event == Event.TEMP_BELOW_20 && waterPresent && !broken) {
                    System.out.print("  <监护: 温度<20 且 有水>");
                    transition(State.HEATING);       // action: 打开继电器电源
                } else if (event == Event.TEMP_BELOW_20 && !waterPresent) {
                    System.out.print("  <监护: 温度<20 且 无水>");
                    System.out.println("  [动作] 不打开电源，继续待机");
                } else if (event == Event.TANK_BROKEN) {
                    transition(State.REPAIRING);
                } else if (event == Event.TIME_23_OCLOCK) {
                    transition(State.SLEEPING);      // action: 断开电源，休眠
                }
                break;

            case HEATING:
                if (event == Event.TEMP_REACH_100) {
                    System.out.print("  <温度达到100°>");
                    transition(State.KEEPING);       // action: 断开电源
                } else if (event == Event.TANK_BROKEN) {
                    transition(State.REPAIRING);
                } else if (event == Event.TIME_23_OCLOCK) {
                    transition(State.SLEEPING);
                }
                break;

            case KEEPING:
                if (event == Event.TEMP_BELOW_20 && waterPresent) {
                    transition(State.HEATING);       // 恒温循环：温度低于20°重新烧水
                } else if (event == Event.NO_WATER) {
                    transition(State.IDLE);
                } else if (event == Event.TANK_BROKEN) {
                    transition(State.REPAIRING);
                } else if (event == Event.TIME_23_OCLOCK) {
                    transition(State.SLEEPING);
                }
                break;

            case REPAIRING:
                if (event == Event.REPAIR_DONE) {
                    broken = false;
                    transition(State.IDLE);
                }
                break;
        }
    }

    /** 状态迁移 + 入口动作 */
    private void transition(State next) {
        currentState = next;
        System.out.println("  => 迁移到 [" + next + "]");
        switch (next) { // entry / 动作
            case HEATING:
                System.out.println("  [动作] 打开继电器电源，开始烧水");
                break;
            case KEEPING:
                System.out.println("  [动作] 断开电源，进入恒温保温");
                break;
            case SLEEPING:
                System.out.println("  [动作] 断开电源，进入休眠");
                break;
            case REPAIRING:
                broken = true;
                System.out.println("  [动作] 水箱烧坏，送修...");
                // 模拟3秒后维修完成
                new Timer(true).schedule(new TimerTask() {
                    @Override public void run() { dispatch(Event.REPAIR_DONE); }
                }, 3000);
                break;
            default:
                break;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("===== 恒温水箱状态机 (20°~100°) 演示 =====");
        System.out.println("(演示：1秒模拟1小时，早上7点自动进入恒温烧水过程)");
        WaterTankStateMachine sm = new WaterTankStateMachine();
        sm.dispatch(Event.TIME_7_OCLOCK);   // 早上7点，启动恒温烧水过程
        sm.waterPresent = true;             // 水量传感器：水箱有水
        Thread.sleep(16000);                // 模拟运行：加热→恒温→低于20°再加热→23点休眠
        sm.timer.cancel();
        System.out.println("===== 演示结束 =====");
    }
}

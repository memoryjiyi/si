import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * B3 字符串提取状态机
 *
 * 从源代码文件中提取所有字符串（双引号之间的文本）。
 * 反斜杠转义下一个字符，例如 \" 输出为 "，\\ 输出为 \。
 *
 * 状态机三要素：
 *  - 状态：OUTSIDE(字符串外)、IN_STRING(字符串内)、ESCAPE(转义状态)
 *  - 事件：读入的每个字符
 *  - 动作/转换：根据当前状态与输入字符决定迁移及输出动作
 */
public class StringExtractorStateMachine {

    /** 状态定义 */
    enum State {
        OUTSIDE("字符串外(普通代码)"),
        IN_STRING("字符串内"),
        ESCAPE("转义中(字符串内)");

        private final String name;
        State(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    private State currentState = State.OUTSIDE;   // 初始状态：字符串外
    private final StringBuilder buffer = new StringBuilder(); // 当前字符串缓冲区
    private final List<String> result = new ArrayList<>();    // 提取结果

    /** 状态机核心：逐字符驱动 */
    public void processChar(char c) {
        switch (currentState) {
            case OUTSIDE:
                if (c == '"') {
                    buffer.setLength(0);
                    currentState = State.IN_STRING;   // 进入字符串内
                }
                break;

            case IN_STRING:
                if (c == '\\') {
                    currentState = State.ESCAPE;      // 进入转义状态
                } else if (c == '"') {
                    result.add(buffer.toString());    // 动作：字符串结束，收集
                    currentState = State.OUTSIDE;
                } else {
                    buffer.append(c);                 // 动作：普通字符入缓冲区
                }
                break;

            case ESCAPE:
                // 转义：无论下一个字符是什么都直接加入缓冲区（如 \" -> "、\\ -> \）
                buffer.append(c);
                currentState = State.IN_STRING;
                break;
        }
    }

    /** 从文件内容中提取所有字符串 */
    public List<String> extract(String content) {
        result.clear();
        currentState = State.OUTSIDE;
        for (char c : content.toCharArray()) {
            processChar(c);
        }
        return new ArrayList<>(result);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("===== 字符串提取状态机演示 =====");

        // 1. 内置示例：源代码中的字符串字面量（含首尾双引号，内含 \" 转义）
        // 源代码原文: "一般学习一门编程语言的时候，基本上第一个程序是输出\"Hello World!\""
        String sample = "\"一般学习一门编程语言的时候，基本上第一个程序是输出\\\"Hello World!\\\"\"";
        System.out.println("输入: " + sample);
        StringExtractorStateMachine sm = new StringExtractorStateMachine();
        List<String> r1 = sm.extract(sample);
        System.out.println("输出: " + (r1.isEmpty() ? "(未提取到)" : r1.get(0)));
        System.out.println();

        // 2. 对源代码文件进行提取（演示模式：以自身源文件为输入）
        String file = args.length > 0 ? args[0] : "src/StringExtractorStateMachine.java";
        String content = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
        List<String> strings = sm.extract(content);
        System.out.println("从文件 [" + file + "] 中共提取到 " + strings.size() + " 个字符串：");
        for (int i = 0; i < strings.size(); i++) {
            System.out.printf("  %3d: %s%n", i + 1, strings.get(i));
        }
    }
}

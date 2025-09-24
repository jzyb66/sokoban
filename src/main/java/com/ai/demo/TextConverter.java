package com.ai.demo;

import java.util.Scanner;

/**
 * 这是一个可多次执行的文本转换工具。
 * 1. 程序会持续运行，等待用户输入。
 * 2. 接收用户输入的多行文本。
 * 3. 将“上/下/左/右”转换为“U/D/L/R”。
 * 4. 按“10字母一组，10组一行”的格式输出。
 * 5. 用户可以输入 'exit' 来随时退出程序。
 */
public class TextConverter {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 程序主循环，使其可以多次执行
        while (true) {
            System.out.println("\n──────────────────────────────────────────────────");
            System.out.println("请输入文本(可粘贴多行)，完成后输入一个空行以开始转换。");
            System.out.println("若要退出程序，请输入 'exit' 并按回车。");
            System.out.print("=> ");

            // 读取第一行输入，用于判断是否要退出
            String firstLine = scanner.nextLine();

            // 检查退出指令
            if (firstLine.equalsIgnoreCase("exit")) {
                break; // 跳出主循环
            }

            // 如果第一行是空的，用户可能只是想跳过，重新显示提示
            if (firstLine.isEmpty()) {
                continue; // 进入下一次循环
            }

            // 准备收集一个完整的、多行的转换任务
            StringBuilder multiLineInput = new StringBuilder(firstLine);

            // 继续读取后续行，直到遇到空行
            while (scanner.hasNextLine()) {
                String subsequentLine = scanner.nextLine();
                if (subsequentLine.isEmpty()) {
                    break; // 当前任务的输入结束
                }
                multiLineInput.append(subsequentLine);
            }

            // 执行转换和格式化
            System.out.println("\n[INFO] 输入结束，正在处理...");
            String fullInput = multiLineInput.toString();
            String result = convertAndFormat(fullInput);

            // 打印结果
            System.out.println("转换后的输出:");
            System.out.println(result);
        }

        // 循环结束后，关闭程序
        System.out.println("程序已退出。");
        scanner.close();
    }

    /**
     * 将输入字符串中的“上/下/左/右”替换为“U/D/L/R”，并按规则格式化。
     * (此方法无需修改)
     * @param text 用户的原始输入字符串
     * @return 经过转换和格式化后的字符串
     */
    public static String convertAndFormat(String text) {
        // 第一步：执行字符转换
        StringBuilder convertedText = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '上': convertedText.append('U'); break;
                case '下': convertedText.append('D'); break;
                case '左': convertedText.append('L'); break;
                case '右': convertedText.append('R'); break;
                default: break;
            }
        }
        String convertedString = convertedText.toString();

        // 第二步：对转换后的字母串进行格式化
        StringBuilder formattedOutput = new StringBuilder();
        for (int i = 0; i < convertedString.length(); i++) {
            formattedOutput.append(convertedString.charAt(i));
            int charCount = i + 1;

            if (charCount == convertedString.length()) {
                continue;
            }

            if (charCount % 100 == 0) {
                formattedOutput.append("\n");
            } else if (charCount % 10 == 0) {
                formattedOutput.append(" ");
            }
        }
        return formattedOutput.toString();
    }
}
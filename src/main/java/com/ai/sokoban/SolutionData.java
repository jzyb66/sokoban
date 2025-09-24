package com.ai.sokoban;

import javafx.scene.input.KeyCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SolutionData {

    // 使用简洁的字符串来存储答案：U (上), D (下), L (左), R (右)
    // 答案已根据您提供的中文指令逐一精确校对。
    private static final List<String> SOLUTIONS_STR = Arrays.asList(
            // 关卡 1: 下上左左右上上下右右 (10步)
            "DULLRU UDRR",
            // 关卡 2:
            "RRDDDDRDDL LURDRULUUU UULLDRURDD DDRRRDRUUD LLLDDLLURD RULUUUULLD RURDDDRRRD RULLLDDLLU RDRULURRR",
            // 关卡 3:
            "RRRRRDDRRU ULUULLLLDD DUUURRRRDD LLLRRRUULL LLDDLDRURD URRDDRURUL LLLRRRUULL LLDDLDRURR RRRRULDLLL LLUURRRRDR DLLLLRRRUU LLLLDD",
            // 关卡 4:
            "RDRDRDDLLR RUULDLDRUU ULDDUUUURD DDD",
            // 关卡 5:
            "RDRRDDRDDL LULLDLURRR DLLRRRRUUL UULLDDUURR DDRDDLLULL",
            // 关卡 6:
            "LLLLLDLLLL UURRRURUUL URRLDDDLDL LLULUUUURR RURRRRDDLL DLLDLUUDRR RDRDDULLDL LLDDRRRRUR LDLLLLUUUL UUUURRDDDR DRDLLRURRR DDLDLLLLUU DDRRRRURRR DRRULLLL",
            // 关卡 7
            "DDDLULLULL DDDUUURRDR RDDDLLULDL LRRRRRUUUL LULLDDDRDL UUUURRDLDD LDRUUURRRR UULDRDLLLU LDDDUUULDD D",
            // 关卡 8
            "LLLDLLLRUD RRULLLRRRR RUULLLDLDD RRULLRRRRD LLLL",
            // 关卡 9
            "LULULLDDRU LURDRRDRRU LLLULLDDLL LURUUURURR RURRDDDDUU UULLDRLLLL DLDDDRDRUD RULURDRRDR RULLLRRUUU LUURDDDDUU\n" +
                    "ULLLLLDLDD DDRRRUUDRR DRRULLLRRU UUULLLLLDL DDDDRRRULL LUUURUULDD DDUUURRRRR RDDDDLLLDL LLLURRDRU",
            // 关卡 10
            "DRRRURRUUL LDDLDLLURR RUURRDLULD ULDD",
            // 关卡 11
            "ULLLDLLLUR RRLLUULDLD RRDRRURRDR RULLLRRRRU ULDRDLLLRR UULDRDL",
            // 关卡 12
            "DLLDLLURRU RRDDLLDDRR UDLLUUULLD R",
            // 关卡 13
            "LUURRUUDLL DDDRRUUDDR RULDLLLUUR RURDLLLDDR RUURULDDDR UULLUU",
            // 关卡 14
            "DRLUURDRRU LRURRDLLDD DRRULULUUR RDLDLLLLDD RULURRLLUU RDLDR",
            // 关卡 15
            "null",
            // 关卡 16
            "LLUULUURLD DRULUUURRD LULDDDUURR RRRDDLLLLL LRRDLLRRDD RRUURULLLL UURRDLULDD RDLRDDRRRU ULULLULDUU RRRRRDDLLL\n" +
                    "L\n",
            // 关卡 17
            "LUULUURRRD DRDLUUULLD RURDDRDDRR ULDLUDLUDL LURRLLULUR URRDDUULLD RURD",
            // 关卡 18
            "RUULUURUUL DLLULLDRRR RURDLDDDRD DLULLUUUDD DRRUUUULLU LLDRLDRURR RDDDDLLURD RUUUDDLLUU LURDLLLDDR RRRDRUU",
            // 关卡 19
            "RUURUDLDDR RURRUULLRR DDLLDLLULL UDRRULUURU RRDLULLDDD DLLURDRUUU DDLLURDRUD RRUURULDDD LDDRRURRUU LLRRDDLLDL\n" +
                    "LUURUUDRRR DDLURULLRD DLDLLUURU",
            // 关卡 20
            "RUULURUULL LDLDDRUULD DDDRUUURRU DDRUUDDDDL ULUDLDLUUU RRDRUDDRUU"
    );

    // 缓存已解析的答案，避免重复转换
    private static final List<List<KeyCode>> PARSED_SOLUTIONS = new ArrayList<>();

    // 静态代码块，在类加载时自动解析所有字符串答案
    static {
        for (String s : SOLUTIONS_STR) {
            if (s == null) {
                PARSED_SOLUTIONS.add(null);
            } else {
                PARSED_SOLUTIONS.add(parseSolution(s));
            }
        }
    }

    /**
     * 将代表答案的字符串转换为KeyCode列表。
     * @param solutionString 包含 U, D, L, R 和空格的字符串
     * @return KeyCode指令列表
     */
    private static List<KeyCode> parseSolution(String solutionString) {
        // 移除所有非UDLR的字符（如空格），然后将字符串分割成单个字符
        return Arrays.stream(solutionString.replaceAll("[^UDLR]", "").split(""))
                .map(ch -> {
                    switch (ch) {
                        case "U": return KeyCode.UP;
                        case "D": return KeyCode.DOWN;
                        case "L": return KeyCode.LEFT;
                        case "R": return KeyCode.RIGHT;
                        default: return null; // 理论上不会发生
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 根据关卡索引获取预设的解法。
     * @param levelIndex 关卡的索引 (从0开始)
     * @return 如果存在解法，则返回 KeyCode 列表；否则返回 null。
     */
    public static List<KeyCode> getSolution(int levelIndex) {
        if (levelIndex >= 0 && levelIndex < PARSED_SOLUTIONS.size()) {
            return PARSED_SOLUTIONS.get(levelIndex);
        }
        return null;
    }
}
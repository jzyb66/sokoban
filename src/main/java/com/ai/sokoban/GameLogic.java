package com.ai.sokoban;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 游戏核心逻辑处理类。
 * 【职责】: 负责所有与游戏规则、状态管理和流程控制相关的逻辑，完全与UI界面实现分离。
 * 1. 管理当前关卡数据（地图、布局）。
 * 2. 处理玩家移动、推箱子逻辑。
 * 3. 记录游戏历史（用于撤销）。
 * 4. 检查游戏胜利条件。
 * 5. 管理游戏计时器。
 * 6. 控制答案的播放、暂停和单步执行。
 */
public class GameLogic {

    private final UIManager uiManager; // UI管理器，用于在逻辑变化后通知界面更新
    private final List<int[][]> levels; // 从LevelData加载的所有关卡数据

    // --- 游戏状态变量 ---
    private int currentLevelIndex = 0;
    private int moveCount = 0;
    private boolean isLevelComplete = false;
    private int[][] currentMap; // 动态地图，存储玩家和箱子的位置
    private int[][] currentLevelLayout; // 静态布局，存储墙和目标点的位置
    private List<int[][]> moveHistory; // 用于存储手动操作的历史记录

    // --- 计时器相关 ---
    private Timeline timer;
    private int timeSeconds;

    // --- 答案动画相关 ---
    private SequentialTransition solutionAnimation;
    private List<KeyCode> solution;
    private int solutionStep = 0;

    /**
     * GameLogic的构造函数。
     * @param uiManager UI管理器实例，用于解耦逻辑和视图。
     */
    public GameLogic(UIManager uiManager) {
        this.uiManager = uiManager;
        this.levels = LevelData.getLevels(); // 加载所有关卡
        setupTimer();
        // 初始化关卡选择框，并设置回调
        uiManager.setupLevelChoiceBox(levels.size(), (newLevelIndex) -> {
            if (newLevelIndex != currentLevelIndex) {
                loadLevel(newLevelIndex);
            }
        });
    }

    /**
     * 返回UI管理器的实例，以便外部（如Controller）可以访问它。
     * @return UIManager实例。
     */
    public UIManager getUiManager() {
        return this.uiManager;
    }

    /**
     * 设置并初始化游戏计时器。
     */
    private void setupTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            timeSeconds++;
            uiManager.updateTimeLabel(timeSeconds); // 每秒更新一次时间标签
        }));
        timer.setCycleCount(Timeline.INDEFINITE); // 无限循环
    }

    /**
     * 加载指定索引的关卡，并重置所有相关状态。
     * @param levelIndex 要加载的关卡的索引 (从0开始)。
     */
    public void loadLevel(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= levels.size()) return;

        // --- 重置游戏状态 ---
        isLevelComplete = false;
        currentLevelIndex = levelIndex;
        moveCount = 0;
        timeSeconds = 0;
        moveHistory = new ArrayList<>(); // 清空历史记录

        // --- 更新UI显示 ---
        uiManager.updateLevelLabel(currentLevelIndex + 1);
        uiManager.updateMovesLabel(moveCount);
        uiManager.updateTimeLabel(timeSeconds);
        uiManager.selectLevelInChoiceBox(levelIndex);

        if (timer != null) timer.playFromStart(); // 重启计时器
        stopSolutionAnimation(); // 如果正在播放答案，则停止

        resetMapToInitialState(); // 根据关卡数据重置地图
        uiManager.drawMap(currentMap, currentLevelLayout); // 绘制新关卡的地图
        uiManager.setControlsForManualPlay(e -> undoMove()); // 设置UI为手动模式
        uiManager.requestFocusOnRoot(); // 让游戏窗口获得焦点以响应键盘
    }

    /**
     * 重置当前关卡到初始状态。
     */
    public void resetCurrentLevel() {
        if (timer != null) timer.stop();
        loadLevel(currentLevelIndex);
    }

    /**
     * **【逻辑修正处】**
     * 根据LevelData中的原始数据，正确地初始化当前关卡的地图(currentMap)和布局(currentLevelLayout)。
     * 这个方法将原始地图中的一个数字（如代表“玩家”的2）拆分到两个数组中：
     * - `currentLevelLayout`: 只存放静态背景（墙、目标点、空地）。
     * - `currentMap`: 只存放动态物体（玩家、箱子）。
     */
    private void resetMapToInitialState() {
        int[][] originalLevel = levels.get(currentLevelIndex);
        int numRows = originalLevel.length;
        int maxWidth = Arrays.stream(originalLevel).mapToInt(row -> row.length).max().orElse(0);
        currentLevelLayout = new int[numRows][maxWidth];
        currentMap = new int[numRows][maxWidth];

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < Math.min(maxWidth, originalLevel[i].length); j++) {
                int tile = originalLevel[i][j];
                switch (tile) {
                    case 0: // 空地
                        currentLevelLayout[i][j] = 0;
                        currentMap[i][j] = 0;
                        break;
                    case 1: // 墙
                        currentLevelLayout[i][j] = 1;
                        currentMap[i][j] = 0;
                        break;
                        case 2: // 目标点
                        currentLevelLayout[i][j] = 4;
                        currentMap[i][j] = 0;
                        break;
                    case 3: // 箱子
                        currentLevelLayout[i][j] = 0; // 箱子在空地上
                        currentMap[i][j] = 3;
                        break;

                    case 4: // 玩家
                        currentLevelLayout[i][j] = 0; // 玩家站在空地上
                        currentMap[i][j] = 2;
                        break;
                    case 5: // 箱子在目标点上
                        currentLevelLayout[i][j] = 4; // 背景是目标点
                        currentMap[i][j] = 3;         // 上面是箱子
                        break;
                    default: // 未知情况按空地处理
                        currentLevelLayout[i][j] = 0;
                        currentMap[i][j] = 0;
                        break;
                }
            }
        }
    }


    /**
     * 处理玩家的移动请求。
     * @param code 按下的方向键 (UP, DOWN, LEFT, RIGHT)。
     */
    public void handlePlayerMove(KeyCode code) {
        if (isLevelComplete) return;

        // 如果之前暂停了答案，现在手动操作，则恢复为手动模式
        if (solutionAnimation != null && solutionAnimation.getStatus() == Animation.Status.PAUSED) {
            stopSolutionAnimation();
            uiManager.setControlsForManualPlay(e -> undoMove());
        }

        if (!Arrays.asList(KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT).contains(code)) return;

        int[] playerPos = findPlayer();
        if (playerPos == null) return;

        // 在移动前保存当前状态，用于撤销
        moveHistory.add(deepCopy(currentMap));

        if (movePlayer(playerPos[0], playerPos[1], code)) {
            moveCount++;
            uiManager.updateMovesLabel(moveCount);
            uiManager.drawMap(currentMap, currentLevelLayout);
            checkWinCondition();
        } else {
            // 如果移动无效，则移除刚刚添加的历史记录
            moveHistory.remove(moveHistory.size() - 1);
        }
    }

    /**
     * 核心移动逻辑：根据方向判断玩家或箱子是否能移动。
     * @param playerRow 玩家当前行。
     * @param playerCol 玩家当前列。
     * @param code 移动方向。
     * @return 如果移动成功，返回true，否则返回false。
     */
    private boolean movePlayer(int playerRow, int playerCol, KeyCode code) {
        int dRow = 0, dCol = 0;
        switch (code) {
            case UP:    dRow = -1; break;
            case DOWN:  dRow = 1;  break;
            case LEFT:  dCol = -1; break;
            case RIGHT: dCol = 1;  break;
        }
        uiManager.updatePlayerImage(code); // 通知UI更新玩家朝向

        int targetRow = playerRow + dRow;
        int targetCol = playerCol + dCol;

        // 边界或墙壁检查
        if (!isValid(targetRow, targetCol) || currentLevelLayout[targetRow][targetCol] == 1) return false;

        int targetObject = currentMap[targetRow][targetCol];
        if (targetObject == 0) { // 目标是空地
            move(playerRow, playerCol, targetRow, targetCol, 2);
            return true;
        } else if (targetObject == 3) { // 目标是箱子
            int boxTargetRow = targetRow + dRow;
            int boxTargetCol = targetCol + dCol;
            // 检查箱子前方是否是空地且不是墙
            if (isValid(boxTargetRow, boxTargetCol) && currentLevelLayout[boxTargetRow][boxTargetCol] != 1 && currentMap[boxTargetRow][boxTargetCol] == 0) {
                move(targetRow, targetCol, boxTargetRow, boxTargetCol, 3); // 移动箱子
                move(playerRow, playerCol, targetRow, targetCol, 2);       // 移动玩家
                return true;
            }
        }
        return false;
    }

    /**
     * 在地图上移动一个对象（玩家或箱子）。
     */
    private void move(int oldRow, int oldCol, int newRow, int newCol, int objectId) {
        currentMap[oldRow][oldCol] = 0; // 原位置清空
        currentMap[newRow][newCol] = objectId; // 新位置放置对象
    }

    /**
     * 检查是否所有箱子都已在目标点上，即是否胜利。
     */
    private void checkWinCondition() {
        for (int i = 0; i < currentLevelLayout.length; i++) {
            for (int j = 0; j < currentLevelLayout[i].length; j++) {
                // 只要有一个目标点(4)上没有箱子(3)，就说明未过关
                if (currentLevelLayout[i][j] == 4 && currentMap[i][j] != 3) {
                    return;
                }
            }
        }

        // --- 游戏胜利 ---
        isLevelComplete = true;
        timer.stop();
        uiManager.disableKeyboardInput(); // 禁止操作

        if (currentLevelIndex < levels.size() - 1) {
            showAlertAndThen("恭喜过关!", "你完成了第 " + (currentLevelIndex + 1) + " 关！", () -> {
                loadLevel(currentLevelIndex + 1); // 自动加载下一关
            });
        } else {
            showAlertAndThen("恭喜!", "你已经完成了所有关卡！", () -> {
                loadLevel(0); // 所有关卡完成后，回到第一关
            });
        }
    }

    /**
     * 撤销上一步操作。
     */
    public void undoMove() {
        if (!moveHistory.isEmpty()) {
            // 恢复到上一个状态
            currentMap = moveHistory.remove(moveHistory.size() - 1);
            moveCount--;
            uiManager.updateMovesLabel(moveCount);
            uiManager.drawMap(currentMap, currentLevelLayout);
        }
    }

    /**
     * 开始播放当前关卡的答案动画。
     */
    public void solveLevel() {
        resetCurrentLevel(); // 播放答案前先重置
        timer.stop();

        solution = SolutionData.getSolution(currentLevelIndex);
        solutionStep = 0;

        if (solution == null) {
            showAlertAndThen("提示", "此关卡没有可用答案。", null);
        } else {
            uiManager.updateMovesLabelText("开始播放解法...");
            animateSolution(solution, 0);
        }
    }

    /**
     * 创建并播放答案动画。
     * @param moves 答案的移动步骤列表。
     * @param startingStep 动画开始的步骤索引。
     */
    private void animateSolution(List<KeyCode> moves, int startingStep) {
        solutionAnimation = new SequentialTransition();
        uiManager.setControlsForSolving(); // 切换UI到答案播放模式

        for (int i = 0; i < moves.size(); i++) {
            final int currentMoveIndex = i;
            // 创建一个关键帧，延迟100毫秒执行一步移动
            KeyFrame kf = new KeyFrame(Duration.millis(100), e -> {
                solutionStep = startingStep + currentMoveIndex + 1;
                KeyCode move = moves.get(currentMoveIndex);
                int[] playerPos = findPlayer();
                if (playerPos != null) {
                    movePlayer(playerPos[0], playerPos[1], move);
                    uiManager.updateMovesLabel(solutionStep);
                    uiManager.drawMap(currentMap, currentLevelLayout);
                }
            });
            solutionAnimation.getChildren().add(new Timeline(kf));
        }

        // 动画播放完成后
        solutionAnimation.setOnFinished(e -> {
            stopSolutionAnimation();
            uiManager.setControlsForManualPlay(evt -> undoMove()); // 恢复UI到手动模式
            checkWinCondition(); // 检查是否过关
        });

        solutionAnimation.play();
    }

    /**
     * 切换答案动画的播放/暂停状态。
     */
    public void toggleSolutionAnimation() {
        if (solutionAnimation == null) return;

        if (solutionAnimation.getStatus() == Animation.Status.RUNNING) {
            solutionAnimation.pause();
            // 切换UI到暂停模式，并设置“上一步/下一步”按钮的回调
            uiManager.setControlsForPausedSolution(
                    e -> prevSolutionStep(),
                    e -> nextSolutionStep()
            );
        } else if (solutionAnimation.getStatus() == Animation.Status.PAUSED) {
            // 从当前步骤继续播放
            if (solution != null && solutionStep < solution.size()) {
                List<KeyCode> remainingMoves = solution.subList(solutionStep, solution.size());
                animateSolution(remainingMoves, solutionStep);
            }
        }
    }

    /**
     * 完全停止答案动画。
     */
    private void stopSolutionAnimation() {
        if (solutionAnimation != null) {
            solutionAnimation.stop();
            solutionAnimation = null;
        }
        uiManager.hidePauseButton();
    }

    /**
     * 答案演示上一步。
     */
    private void prevSolutionStep() {
        if (solutionStep > 0) {
            solutionStep--;
            applySolutionStep(solutionStep);
        }
    }

    /**
     * 答案演示下一步。
     */
    private void nextSolutionStep() {
        if (solution != null && solutionStep < solution.size()) {
            solutionStep++;
            applySolutionStep(solutionStep);
        }
    }

    /**
     * 将游戏状态直接应用到指定的答案步骤。
     * @param step 要应用的步骤编号。
     */
    private void applySolutionStep(int step) {
        resetMapToInitialState(); // 从头开始
        // 模拟从第0步到第step步
        for (int i = 0; i < step; i++) {
            int[] playerPos = findPlayer();
            if (playerPos != null) {
                movePlayer(playerPos[0], playerPos[1], solution.get(i));
            }
        }
        uiManager.updateMovesLabel(step);
        uiManager.drawMap(currentMap, currentLevelLayout);
    }

    // --- 辅助工具方法 ---

    private int[] findPlayer() {
        for (int i = 0; i < currentMap.length; i++) {
            for (int j = 0; j < currentMap[i].length; j++) {
                if (currentMap[i][j] == 2) return new int[]{i, j};
            }
        }
        return null;
    }

    private boolean isValid(int row, int col) {
        return row >= 0 && row < currentMap.length && col >= 0 && col < currentMap[row].length;
    }

    private int[][] deepCopy(int[][] original) {
        if (original == null) return null;
        return Arrays.stream(original).map(int[]::clone).toArray(int[][]::new);
    }

    private void showAlertAndThen(String title, String message, Runnable onOk) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
            if (onOk != null) onOk.run();
        });
    }
}
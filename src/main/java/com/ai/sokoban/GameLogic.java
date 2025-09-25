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
 */
public class GameLogic {

    private final UIManager uiManager;
    private final List<int[][]> levels;

    private int currentLevelIndex = 0;
    private int moveCount = 0;
    private boolean isLevelComplete = false;
    private int[][] currentMap;
    private int[][] currentLevelLayout;
    private List<int[][]> moveHistory;

    private Timeline timer;
    private int timeSeconds;

    private SequentialTransition solutionAnimation;
    private List<KeyCode> solution;
    private int solutionStep = 0;

    /**
     * GameLogic的构造函数。
     * @param uiManager UI管理器实例，用于解耦逻辑和视图。
     */
    public GameLogic(UIManager uiManager) {
        this.uiManager = uiManager;
        this.levels = LevelData.getLevels();
        setupTimer();
        uiManager.setupLevelChoiceBox(levels.size(), (newLevelIndex) -> {
            if (newLevelIndex != currentLevelIndex) {
                loadLevel(newLevelIndex);
            }
        });
    }

    /**
     * 返回UI管理器的实例。
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
            uiManager.updateTimeLabel(timeSeconds);
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
    }

    /**
     * 加载指定索引的关卡，并重置所有相关状态。
     * @param levelIndex 要加载的关卡的索引 (从0开始)。
     */
    public void loadLevel(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= levels.size()) return;

        isLevelComplete = false;
        currentLevelIndex = levelIndex;
        moveCount = 0;
        timeSeconds = 0;
        moveHistory = new ArrayList<>();

        uiManager.updateLevelLabel(currentLevelIndex + 1);
        uiManager.updateMovesLabel(moveCount);
        uiManager.updateTimeLabel(timeSeconds);
        uiManager.selectLevelInChoiceBox(levelIndex);

        if (timer != null) timer.playFromStart();
        stopSolutionAnimation();

        resetMapToInitialState();
        uiManager.drawMap(currentMap, currentLevelLayout);
        uiManager.setControlsForManualPlay(e -> undoMove());
        uiManager.requestFocusOnRoot();
    }

    /**
     * 重置当前关卡。
     */
    public void resetCurrentLevel() {
        if (timer != null) timer.stop();
        loadLevel(currentLevelIndex);
    }

    /**
     * **【逻辑已彻底修正】**
     * 根据LevelData的原始数据，正确分离静态布局（墙、目标）和动态对象（玩家、箱子）。
     * 此逻辑现在与您最初在HelloController中的实现完全一致，不会再混淆人物和目标。
     */
    private void resetMapToInitialState() {
        int[][] originalLevel = levels.get(currentLevelIndex);
        int numRows = originalLevel.length;
        int maxWidth = Arrays.stream(originalLevel).mapToInt(row -> row.length).max().orElse(0);
        currentLevelLayout = new int[numRows][maxWidth];
        currentMap = new int[numRows][maxWidth];

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < maxWidth; j++) {
                if (j < originalLevel[i].length) {
                    int tile = originalLevel[i][j];
                    if (tile == 5) { // 箱子在目标点上
                        currentLevelLayout[i][j] = 2; // 静态背景是目标
                        currentMap[i][j] = 3;         // 动态对象是箱子
                    } else if (tile == 4) { // 玩家
                        currentLevelLayout[i][j] = 0; // 静态背景是空地
                        currentMap[i][j] = 2;         // 动态对象是玩家
                    } else if (tile == 2) { // 目标点
                        currentLevelLayout[i][j] = 4; // 静态背景是目标
                        currentMap[i][j] = 0;         // 没有动态对象
                    } else if (tile == 3) { // 仅箱子
                        currentLevelLayout[i][j] = 0; // 静态背景是空地
                        currentMap[i][j] = 3;         // 动态对象是箱子
                    } else { // 墙(1)或空地(0)
                        currentLevelLayout[i][j] = tile; // 它们是静态背景
                        currentMap[i][j] = 0;            // 没有动态对象
                    }
                } else {
                    currentLevelLayout[i][j] = 0;
                    currentMap[i][j] = 0;
                }
            }
        }
    }

    /**
     * 处理玩家的移动请求。
     */
    public void handlePlayerMove(KeyCode code) {
        if (isLevelComplete) return;

        if (solutionAnimation != null && solutionAnimation.getStatus() == Animation.Status.PAUSED) {
            stopSolutionAnimation();
            uiManager.setControlsForManualPlay(e -> undoMove());
        }

        if (!Arrays.asList(KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT).contains(code)) return;

        int[] playerPos = findPlayer();
        if (playerPos == null) return;

        moveHistory.add(deepCopy(currentMap));

        if (movePlayer(playerPos[0], playerPos[1], code)) {
            moveCount++;
            uiManager.updateMovesLabel(moveCount);
            uiManager.drawMap(currentMap, currentLevelLayout);
            checkWinCondition();
        } else {
            moveHistory.remove(moveHistory.size() - 1);
        }
    }

    /**
     * 核心移动逻辑。
     */
    private boolean movePlayer(int playerRow, int playerCol, KeyCode code) {
        int dRow = 0, dCol = 0;
        switch (code) {
            case UP:    dRow = -1; break;
            case DOWN:  dRow = 1;  break;
            case LEFT:  dCol = -1; break;
            case RIGHT: dCol = 1;  break;
        }
        uiManager.updatePlayerImage(code);

        int targetRow = playerRow + dRow;
        int targetCol = playerCol + dCol;

        if (!isValid(targetRow, targetCol) || currentLevelLayout[targetRow][targetCol] == 1) return false;

        int targetObject = currentMap[targetRow][targetCol];
        if (targetObject == 0) {
            move(playerRow, playerCol, targetRow, targetCol, 2);
            return true;
        } else if (targetObject == 3) {
            int boxTargetRow = targetRow + dRow;
            int boxTargetCol = targetCol + dCol;
            if (isValid(boxTargetRow, boxTargetCol) && currentLevelLayout[boxTargetRow][boxTargetCol] != 1 && currentMap[boxTargetRow][boxTargetCol] == 0) {
                move(targetRow, targetCol, boxTargetRow, boxTargetCol, 3);
                move(playerRow, playerCol, targetRow, targetCol, 2);
                return true;
            }
        }
        return false;
    }

    private void move(int oldRow, int oldCol, int newRow, int newCol, int objectId) {
        currentMap[oldRow][oldCol] = 0;
        currentMap[newRow][newCol] = objectId;
    }

    private void checkWinCondition() {
        for (int i = 0; i < currentLevelLayout.length; i++) {
            for (int j = 0; j < currentLevelLayout[i].length; j++) {
                if (currentLevelLayout[i][j] == 4 && currentMap[i][j] != 3) {
                    return;
                }
            }
        }
        isLevelComplete = true;
        timer.stop();
        uiManager.disableKeyboardInput();
        if (currentLevelIndex < levels.size() - 1) {
            showAlertAndThen("恭喜过关!", "你完成了第 " + (currentLevelIndex + 1) + " 关！", () -> {
                loadLevel(currentLevelIndex + 1);
            });
        } else {
            showAlertAndThen("恭喜!", "你已经完成了所有关卡！", () -> {
                loadLevel(0);
            });
        }
    }

    public void undoMove() {
        if (!moveHistory.isEmpty()) {
            currentMap = moveHistory.remove(moveHistory.size() - 1);
            moveCount--;
            uiManager.updateMovesLabel(moveCount);
            uiManager.drawMap(currentMap, currentLevelLayout);
        }
    }

    public void solveLevel() {
        resetCurrentLevel();
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

    private void animateSolution(List<KeyCode> moves, int startingStep) {
        solutionAnimation = new SequentialTransition();
        uiManager.setControlsForSolving();
        for (int i = 0; i < moves.size(); i++) {
            final int currentMoveIndex = i;
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
        solutionAnimation.setOnFinished(e -> {
            stopSolutionAnimation();
            uiManager.setControlsForManualPlay(evt -> undoMove());
            checkWinCondition();
        });
        solutionAnimation.play();
    }

    public void toggleSolutionAnimation() {
        if (solutionAnimation == null) return;
        if (solutionAnimation.getStatus() == Animation.Status.RUNNING) {
            solutionAnimation.pause();
            uiManager.setControlsForPausedSolution(e -> prevSolutionStep(), e -> nextSolutionStep());
        } else if (solutionAnimation.getStatus() == Animation.Status.PAUSED) {
            if (solution != null && solutionStep < solution.size()) {
                List<KeyCode> remainingMoves = solution.subList(solutionStep, solution.size());
                animateSolution(remainingMoves, solutionStep);
            }
        }
    }

    private void stopSolutionAnimation() {
        if (solutionAnimation != null) {
            solutionAnimation.stop();
            solutionAnimation = null;
        }
        uiManager.hidePauseButton();
    }

    private void prevSolutionStep() {
        if (solutionStep > 0) {
            solutionStep--;
            applySolutionStep(solutionStep);
        }
    }

    private void nextSolutionStep() {
        if (solution != null && solutionStep < solution.size()) {
            solutionStep++;
            applySolutionStep(solutionStep);
        }
    }

    private void applySolutionStep(int step) {
        resetMapToInitialState();
        for (int i = 0; i < step; i++) {
            int[] playerPos = findPlayer();
            if (playerPos != null) {
                movePlayer(playerPos[0], playerPos[1], solution.get(i));
            }
        }
        uiManager.updateMovesLabel(step);
        uiManager.drawMap(currentMap, currentLevelLayout);
    }

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
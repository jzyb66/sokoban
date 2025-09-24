package com.ai.sokoban;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HelloController {

    @FXML private StackPane rootPane;
    @FXML private GridPane gameGrid;
    @FXML private Label levelLabel;
    @FXML private Label movesLabel;
    @FXML private Label timeLabel;
    @FXML private Button solveButton;
    @FXML private Button resetButton;
    @FXML private ChoiceBox<Integer> levelChoiceBox;
    @FXML private Button pauseButton;
    @FXML private Button prevStepButton;
    @FXML private Button nextStepButton;

    private List<int[][]> levels = new ArrayList<>();
    private int currentLevelIndex = 0;
    private int moveCount = 0;
    private boolean isLevelComplete = false;

    private int[][] currentMap;
    private int[][] currentLevelLayout;

    // 用于存储手动操作的历史记录
    private List<int[][]> moveHistory;

    private Image wallImage, boxImage, goalImage, groundImage, boxOnGoalImage;
    private Image playerUpImage, playerDownImage, playerLeftImage, playerRightImage;
    private Image currentPlayerImage;

    private Timeline timer;
    private int timeSeconds;
    private SequentialTransition solutionAnimation;
    private List<KeyCode> solution;
    private int solutionStep = 0;


    @FXML
    public void initialize() {
        loadImages();
        createLevels();
        setupTimer();
        setupLevelChoiceBox();
        loadLevel(currentLevelIndex);
    }

    private int[][] deepCopy(int[][] original) {
        if (original == null) return null;
        final int[][] result = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = Arrays.copyOf(original[i], original[i].length);
        }
        return result;
    }

    private void setupTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            timeSeconds++;
            timeLabel.setText("时间: " + timeSeconds + "s");
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
    }

    private void loadImages() {
        try {
            wallImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/wall.png")));
            boxImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/box.png")));
            goalImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/goal.png")));
            groundImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/ground.png")));
            boxOnGoalImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/box_on_goal.png")));
            playerUpImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/player_up.png")));
            playerDownImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/player_down.png")));
            playerLeftImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/player_left.png")));
            playerRightImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/player_right.png")));
        } catch (Exception e) {
            e.printStackTrace();
            showAlertAndThen("错误", "图片加载失败！请检查images文件夹下的图片文件。", null);
        }
    }

    private void createLevels() {
        levels = LevelData.getLevels();
    }

    private void setupLevelChoiceBox() {
        levelChoiceBox.getItems().addAll(
                IntStream.rangeClosed(1, levels.size()).boxed().collect(Collectors.toList())
        );
        levelChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal - 1 != currentLevelIndex) {
                loadLevel(newVal - 1);
            }
        });
    }

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
                    if (tile == 5) {
                        currentLevelLayout[i][j] = 4;
                        currentMap[i][j] = 3;
                    } else if (tile == 4) {
                        currentLevelLayout[i][j] = 0;
                        currentMap[i][j] = 2;
                    } else if (tile == 2) {
                        currentLevelLayout[i][j] = 4;
                        currentMap[i][j] = 0;
                    } else if (tile == 3) {
                        currentLevelLayout[i][j] = 0;
                        currentMap[i][j] = 3;
                    } else {
                        currentLevelLayout[i][j] = tile;
                        currentMap[i][j] = 0;
                    }
                } else {
                    currentLevelLayout[i][j] = 0;
                    currentMap[i][j] = 0;
                }
            }
        }
    }


    private void loadLevel(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= levels.size()) return;

        isLevelComplete = false;
        currentLevelIndex = levelIndex;
        if (levelChoiceBox.getSelectionModel().getSelectedIndex() != levelIndex) {
            levelChoiceBox.getSelectionModel().select(levelIndex);
        }
        levelLabel.setText("关卡: " + (currentLevelIndex + 1));
        moveCount = 0;
        movesLabel.setText("步数: 0");
        timeSeconds = 0;
        timeLabel.setText("时间: 0s");

        // 初始化操作历史
        moveHistory = new ArrayList<>();

        if (timer != null) timer.playFromStart();

        stopSolutionAnimation();

        currentPlayerImage = playerDownImage;
        resetMapToInitialState(); // 使用新方法重置地图

        drawMap();
        setControlsForManualPlay(true);
        if (rootPane != null) {
            rootPane.requestFocus();
        }
    }

    @FXML
    public void resetGame() {
        timer.stop();
        loadLevel(currentLevelIndex);
    }

    private void setControlsForManualPlay(boolean enabled) {
        solveButton.setDisable(!enabled);
        resetButton.setDisable(!enabled);
        levelChoiceBox.setDisable(!enabled);

        // 在手动模式下，“上一步”是撤销功能
        prevStepButton.setVisible(enabled);
        prevStepButton.setText("上一步");
        prevStepButton.setOnAction(e -> undoMove());

        // 隐藏不相关的按钮
        nextStepButton.setVisible(false);
        pauseButton.setVisible(false);

        rootPane.setOnKeyPressed(enabled ? (event -> handleKeyPress(event.getCode())) : null);
    }

    private void setControlsForSolving() {
        solveButton.setDisable(true);
        resetButton.setDisable(true);
        levelChoiceBox.setDisable(true);

        // 隐藏手动模式的按钮
        prevStepButton.setVisible(false);
        nextStepButton.setVisible(false);

        // 显示“暂停”按钮
        pauseButton.setVisible(true);
        pauseButton.setDisable(false);
        pauseButton.setText("暂停");

        rootPane.setOnKeyPressed(null);
    }

    private void setControlsForPausedSolution() {
        resetButton.setDisable(false); // 允许在暂停时重置
        pauseButton.setText("继续");

        // 显示答案控制按钮
        prevStepButton.setVisible(true);
        prevStepButton.setText("答案上一步");
        prevStepButton.setOnAction(e -> prevSolutionStep());

        nextStepButton.setVisible(true);
        nextStepButton.setText("答案下一步");
        nextStepButton.setOnAction(e -> nextSolutionStep());
    }

    private void drawMap() {
        gameGrid.getChildren().clear();
        for (int row = 0; row < currentMap.length; row++) {
            for (int col = 0; col < currentMap[row].length; col++) {
                ImageView groundView = new ImageView(groundImage);
                groundView.setFitWidth(40);
                groundView.setFitHeight(40);
                gameGrid.add(groundView, col, row);

                int layoutTile = currentLevelLayout[row][col];
                if (layoutTile == 4) {
                    ImageView goalView = new ImageView(goalImage);
                    goalView.setFitWidth(40);
                    goalView.setFitHeight(40);
                    gameGrid.add(goalView, col, row);
                } else if (layoutTile == 1) {
                    ImageView wallView = new ImageView(wallImage);
                    wallView.setFitWidth(40);
                    wallView.setFitHeight(40);
                    gameGrid.add(wallView, col, row);
                }

                int objectTile = currentMap[row][col];
                if (objectTile == 2) {
                    ImageView playerView = new ImageView(currentPlayerImage);
                    playerView.setFitWidth(40);
                    playerView.setFitHeight(40);
                    gameGrid.add(playerView, col, row);
                } else if (objectTile == 3) {
                    ImageView boxView = new ImageView(currentLevelLayout[row][col] == 4 ? boxOnGoalImage : boxImage);
                    boxView.setFitWidth(40);
                    boxView.setFitHeight(40);
                    gameGrid.add(boxView, col, row);
                }
            }
        }
        if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
            rootPane.getScene().getWindow().sizeToScene();
        }
    }

    public void handleKeyPress(KeyCode code) {
        if (isLevelComplete) return;

        if (solutionAnimation != null && solutionAnimation.getStatus() == Animation.Status.PAUSED) {
            stopSolutionAnimation();
            setControlsForManualPlay(true);
        }

        if (!Arrays.asList(KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT).contains(code)) return;

        int[] playerPos = findPlayer();
        if (playerPos == null) return;

        // 在移动前保存当前状态
        moveHistory.add(deepCopy(currentMap));

        if (movePlayer(playerPos[0], playerPos[1], code)) {
            moveCount++;
            movesLabel.setText("步数: " + moveCount);
            drawMap();
            checkWinCondition();
        } else {
            // 如果移动无效，则移除刚刚添加的历史记录
            moveHistory.remove(moveHistory.size() - 1);
        }
    }

    private boolean movePlayer(int playerRow, int playerCol, KeyCode code) {
        int dRow = 0, dCol = 0;
        switch (code) {
            case UP:
                dRow = -1;
                currentPlayerImage = playerUpImage;
                break;
            case DOWN:
                dRow = 1;
                currentPlayerImage = playerDownImage;
                break;
            case LEFT:
                dCol = -1;
                currentPlayerImage = playerLeftImage;
                break;
            case RIGHT:
                dCol = 1;
                currentPlayerImage = playerRightImage;
                break;
        }

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
        rootPane.setOnKeyPressed(null);

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

    private void showAlertAndThen(String title, String message, Runnable onOk) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();

            if (onOk != null) {
                onOk.run();
            }
        });
    }

    @FXML
    private void solveLevel() {
        resetGame();
        timer.stop();

        solution = SolutionData.getSolution(currentLevelIndex);
        solutionStep = 0;

        if (solution == null) {
            showAlertAndThen("提示", "此关卡没有可用答案。", null);
        } else {
            movesLabel.setText("开始播放解法...");
            animateSolution(solution, 0);
        }
    }

    private void animateSolution(List<KeyCode> moves, int startingStep) {
        solutionAnimation = new SequentialTransition();
        setControlsForSolving();

        for (int i = 0; i < moves.size(); i++) {
            final int currentMoveIndex = i;
            KeyFrame kf = new KeyFrame(Duration.millis(100), e -> {
                solutionStep = startingStep + currentMoveIndex + 1;
                KeyCode move = moves.get(currentMoveIndex);
                int[] playerPos = findPlayer();
                if (playerPos != null) {
                    movePlayer(playerPos[0], playerPos[1], move);
                    movesLabel.setText("步数: " + solutionStep);
                    drawMap();
                }
            });
            solutionAnimation.getChildren().add(new Timeline(kf));
        }

        solutionAnimation.setOnFinished(e -> {
            stopSolutionAnimation();
            setControlsForManualPlay(true);
            checkWinCondition();
        });

        solutionAnimation.play();
    }

    @FXML
    private void pauseSolveAnimation() {
        if (solutionAnimation == null) return;

        if (solutionAnimation.getStatus() == Animation.Status.RUNNING) {
            solutionAnimation.pause();
            setControlsForPausedSolution();
        } else if (solutionAnimation.getStatus() == Animation.Status.PAUSED) {
            // **FIXED LOGIC HERE**
            // Re-create and play the animation for the remaining steps
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
        pauseButton.setVisible(false);
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

        movesLabel.setText("步数: " + step);
        drawMap();
    }

    @FXML
    private void undoMove() {
        if (!moveHistory.isEmpty()) {
            currentMap = moveHistory.remove(moveHistory.size() - 1);
            moveCount--;
            movesLabel.setText("步数: " + moveCount);
            drawMap();
        }
    }
}
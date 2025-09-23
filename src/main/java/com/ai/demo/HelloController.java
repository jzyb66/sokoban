package com.ai.demo;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class HelloController {

    @FXML private BorderPane rootPane;
    @FXML private GridPane gameGrid;
    @FXML private Label levelLabel;
    @FXML private Label movesLabel;
    @FXML private Label timeLabel;
    @FXML private Button solveButton;
    @FXML private Button resetButton;
    @FXML private Button skipButton;
    @FXML private Button pauseButton;

    private List<int[][]> levels = new ArrayList<>();
    private int currentLevelIndex = 0;
    private int moveCount = 0;

    private int[][] currentMap;
    private int[][] currentLevelLayout;

    private Image wallImage, boxImage, goalImage, groundImage, boxOnGoalImage;
    private Image playerUpImage, playerDownImage, playerLeftImage, playerRightImage;
    private Image currentPlayerImage;

    private Timeline timer;
    private int timeSeconds;
    private SequentialTransition solutionAnimation;
    private Task<List<KeyCode>> solverTask;

    @FXML
    public void initialize() {
        loadImages();
        createLevels();
        setupTimer();
        loadLevel(currentLevelIndex);
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
            showAlert("错误", "图片加载失败！请检查images文件夹下的图片文件。");
        }
    }

    private void createLevels() {
        levels = LevelData.getLevels();
    }

    private void loadLevel(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= levels.size()) return;
        currentLevelIndex = levelIndex;
        levelLabel.setText("关卡: " + (currentLevelIndex + 1));
        moveCount = 0;
        movesLabel.setText("步数: 0");
        timeSeconds = 0;
        timeLabel.setText("时间: 0s");
        if (timer != null) timer.playFromStart();

        stopSolverAndAnimation();

        currentPlayerImage = playerDownImage;
        int[][] originalLevel = levels.get(levelIndex);
        int numRows = originalLevel.length;
        int maxWidth = Arrays.stream(originalLevel).mapToInt(row -> row.length).max().orElse(0);
        currentLevelLayout = new int[numRows][maxWidth];
        currentMap = new int[numRows][maxWidth];

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < maxWidth; j++) {
                if (j < originalLevel[i].length) {
                    int tile = originalLevel[i][j];
                    if (tile == 5) {
                        currentLevelLayout[i][j] = 4; currentMap[i][j] = 3;
                    } else if (tile == 4) {
                        currentLevelLayout[i][j] = 0; currentMap[i][j] = 2;
                    } else if (tile == 2) {
                        currentLevelLayout[i][j] = 4; currentMap[i][j] = 0;
                    } else if (tile == 3) {
                        currentLevelLayout[i][j] = 0; currentMap[i][j] = 3;
                    } else {
                        currentLevelLayout[i][j] = tile; currentMap[i][j] = 0;
                    }
                } else {
                    currentLevelLayout[i][j] = 0; currentMap[i][j] = 0;
                }
            }
        }
        drawMap();
        setControlsForManualPlay(true);
        if (rootPane != null) rootPane.requestFocus();
    }

    @FXML
    public void resetGame() {
        timer.stop();
        loadLevel(currentLevelIndex);
    }

    @FXML
    public void skipLevel() {
        timer.stop();
        if (currentLevelIndex < levels.size() - 1) {
            loadLevel(currentLevelIndex + 1);
        } else {
            showAlert("提示", "已经是最后一关了！将返回第一关。");
            loadLevel(0);
        }
    }

    private void setControlsForManualPlay(boolean enabled) {
        solveButton.setDisable(!enabled);
        resetButton.setDisable(!enabled);
        skipButton.setDisable(!enabled);
        pauseButton.setDisable(true);
        rootPane.setOnKeyPressed(enabled ? (event -> handleKeyPress(event.getCode())) : null);
    }

    private void setControlsForSolving() {
        solveButton.setDisable(true);
        resetButton.setDisable(true);
        skipButton.setDisable(true);
        pauseButton.setDisable(false);
        rootPane.setOnKeyPressed(null);
    }

    private void drawMap() {
        gameGrid.getChildren().clear();
        for (int row = 0; row < currentMap.length; row++) {
            for (int col = 0; col < currentMap[row].length; col++) {
                ImageView groundView = new ImageView(groundImage);
                groundView.setFitWidth(40); groundView.setFitHeight(40);
                gameGrid.add(groundView, col, row);

                int layoutTile = currentLevelLayout[row][col];
                if (layoutTile == 4) {
                    ImageView goalView = new ImageView(goalImage);
                    goalView.setFitWidth(40); goalView.setFitHeight(40);
                    gameGrid.add(goalView, col, row);
                } else if (layoutTile == 1) {
                    ImageView wallView = new ImageView(wallImage);
                    wallView.setFitWidth(40); wallView.setFitHeight(40);
                    gameGrid.add(wallView, col, row);
                }

                int objectTile = currentMap[row][col];
                if (objectTile == 2) {
                    ImageView playerView = new ImageView(currentPlayerImage);
                    playerView.setFitWidth(40); playerView.setFitHeight(40);
                    gameGrid.add(playerView, col, row);
                } else if (objectTile == 3) {
                    ImageView boxView = new ImageView(currentLevelLayout[row][col] == 4 ? boxOnGoalImage : boxImage);
                    boxView.setFitWidth(40); boxView.setFitHeight(40);
                    gameGrid.add(boxView, col, row);
                }
            }
        }
    }

    public void handleKeyPress(KeyCode code) {
        if (solutionAnimation != null && solutionAnimation.getStatus() == Animation.Status.PAUSED) {
            stopSolverAndAnimation();
            setControlsForManualPlay(true);
        }

        if (!Arrays.asList(KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT).contains(code)) return;

        int[] playerPos = findPlayer();
        if (playerPos == null) return;

        if (movePlayer(playerPos[0], playerPos[1], code)) {
            moveCount++;
            movesLabel.setText("步数: " + moveCount);
            drawMap();
            checkWinCondition();
        }
    }

    private boolean movePlayer(int playerRow, int playerCol, KeyCode code) {
        int dRow = 0, dCol = 0;
        switch (code) {
            case UP: dRow = -1; currentPlayerImage = playerUpImage; break;
            case DOWN: dRow = 1; currentPlayerImage = playerDownImage; break;
            case LEFT: dCol = -1; currentPlayerImage = playerLeftImage; break;
            case RIGHT: dCol = 1; currentPlayerImage = playerRightImage; break;
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

        timer.stop();
        if (currentLevelIndex < levels.size() - 1) {
            showAlert("恭喜过关!", "你完成了第 " + (currentLevelIndex + 1) + " 关！");
            loadLevel(currentLevelIndex + 1);
        } else {
            showAlert("恭喜!", "你已经完成了所有关卡！");
            loadLevel(0);
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @FXML
    private void solveLevel() {
        setControlsForSolving();
        timer.stop();

        solverTask = new Task<>() {
            @Override
            protected List<KeyCode> call() {
                SokobanSolver solver = new SokobanSolver(currentLevelLayout);
                return solver.solve(currentMap, visitedCount -> {
                    // 更新UI线程上的消息
                    Platform.runLater(() -> movesLabel.setText("已检查: " + visitedCount + " 状态"));
                });
            }
        };

        solverTask.setOnSucceeded(event -> {
            movesLabel.textProperty().unbind(); // 解除绑定
            List<KeyCode> solution = solverTask.getValue();
            if (solution == null) {
                showAlert("求解失败", "此关卡状态太复杂或无解。");
                setControlsForManualPlay(true);
                movesLabel.setText("步数: " + moveCount);
                timer.play();
            } else {
                movesLabel.setText("找到解法，共 " + solution.size() + " 步");
                animateSolution(solution);
            }
        });

        solverTask.setOnFailed(event -> {
            movesLabel.textProperty().unbind();
            showAlert("错误", "求解过程中发生错误。");
            setControlsForManualPlay(true);
            movesLabel.setText("步数: " + moveCount);
            timer.play();
        });

        solverTask.setOnCancelled(event -> {
            movesLabel.textProperty().unbind();
            setControlsForManualPlay(true);
            movesLabel.setText("步数: " + moveCount);
        });

        new Thread(solverTask).start();
    }

    private void animateSolution(List<KeyCode> solution) {
        solutionAnimation = new SequentialTransition();
        setControlsForSolving();

        for (KeyCode move : solution) {
            KeyFrame kf = new KeyFrame(Duration.millis(200), e -> {
                int[] playerPos = findPlayer();
                if (playerPos != null) {
                    movePlayer(playerPos[0], playerPos[1], move);
                    drawMap();
                }
            });
            solutionAnimation.getChildren().add(new Timeline(kf));
        }

        solutionAnimation.setOnFinished(e -> {
            stopSolverAndAnimation();
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
            pauseButton.setText("继续");
            resetButton.setDisable(false);
            rootPane.setOnKeyPressed(event -> handleKeyPress(event.getCode()));

        } else if (solutionAnimation.getStatus() == Animation.Status.PAUSED) {
            solutionAnimation.play();
            pauseButton.setText("暂停解关");
            resetButton.setDisable(true);
            rootPane.setOnKeyPressed(null);
        }
    }

    private void stopSolverAndAnimation() {
        if (solutionAnimation != null) {
            solutionAnimation.stop();
            solutionAnimation = null;
        }
        if (solverTask != null && solverTask.isRunning()) {
            solverTask.cancel(true);
            solverTask = null;
        }
        pauseButton.setText("暂停解关");
        pauseButton.setDisable(true);
    }
}
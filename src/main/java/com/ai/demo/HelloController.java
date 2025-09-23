package com.ai.demo;

import javafx.animation.KeyFrame;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
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

import java.util.*;

public class HelloController {

    @FXML private BorderPane rootPane;
    @FXML private GridPane gameGrid;
    @FXML private Label levelLabel;
    @FXML private Label movesLabel;
    @FXML private Label timeLabel;
    @FXML private Button solveButton;
    @FXML private Button resetButton;
    @FXML private Button skipButton;

    private List<int[][]> levels = new ArrayList<>();
    private int currentLevelIndex = 0;
    private int moveCount = 0;

    private int[][] currentMap;
    private int[][] currentLevelLayout;

    private Image wallImage, boxImage, goalImage, groundImage, boxOnGoalImage;
    private Image playerUpImage, playerDownImage, playerLeftImage, playerRightImage;
    private Image currentPlayerImage; // 当前角色应该显示的图像

    private Timeline timer;
    private int timeSeconds;
    private SequentialTransition solutionAnimation;

    // --- 内部类定义 (GameState, Point) ---
    private record GameState(int playerRow, int playerCol, List<Point> boxes, GameState parent, KeyCode move) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GameState gameState = (GameState) o;
            return playerRow == gameState.playerRow && playerCol == gameState.playerCol && boxes.equals(gameState.boxes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerRow, playerCol, boxes);
        }
    }

    private record Point(int row, int col) {}

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

            // 加载4个方向的小人图像
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
        // 从 LevelData 类加载关卡数据
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
        timer.playFromStart();

        if (solutionAnimation != null) solutionAnimation.stop();

        // 初始时，角色朝向下方
        currentPlayerImage = playerDownImage;

        int[][] originalLevel = levels.get(levelIndex);
        int numRows = originalLevel.length;
        int maxWidth = 0;
        for (int[] row : originalLevel) {
            if (row.length > maxWidth) {
                maxWidth = row.length;
            }
        }

        currentLevelLayout = new int[numRows][maxWidth];
        currentMap = new int[numRows][maxWidth];

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < maxWidth; j++) {
                if (j < originalLevel[i].length) {
                    int tile = originalLevel[i][j];
                    if (tile == 5) { // 箱子在目标点上
                        currentLevelLayout[i][j] = 4; // 布局是目标点
                        currentMap[i][j] = 3;         // 地图上是箱子
                    } else if (tile == 4) { // 玩家
                        currentLevelLayout[i][j] = 0; // 布局是地面
                        currentMap[i][j] = 2;         // 地图上是玩家
                    } else if (tile == 2) { // 目标点
                        currentLevelLayout[i][j] = 4; // 布局是目标点
                        currentMap[i][j] = 0;
                    } else if (tile == 3) { // 箱子
                        currentLevelLayout[i][j] = 0;
                        currentMap[i][j] = 3;
                    } else { // 墙或地面
                        currentLevelLayout[i][j] = tile;
                        currentMap[i][j] = 0;
                    }
                } else {
                    currentLevelLayout[i][j] = 0;
                    currentMap[i][j] = 0;
                }
            }
        }
        drawMap();
        setControlsDisabled(false);
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

    private void setControlsDisabled(boolean disabled) {
        if (solveButton != null) solveButton.setDisable(disabled);
        if (resetButton != null) resetButton.setDisable(disabled);
        if (skipButton != null) skipButton.setDisable(disabled);
        if (rootPane != null) {
            rootPane.setOnKeyPressed(disabled ? null : (event -> handleKeyPress(event.getCode())));
        }
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
                if (objectTile == 2) { // 玩家
                    ImageView playerView = new ImageView(currentPlayerImage);
                    playerView.setFitWidth(40);
                    playerView.setFitHeight(40);
                    gameGrid.add(playerView, col, row);
                } else if (objectTile == 3) { // 箱子
                    ImageView boxView = new ImageView(currentLevelLayout[row][col] == 4 ? boxOnGoalImage : boxImage);
                    boxView.setFitWidth(40);
                    boxView.setFitHeight(40);
                    gameGrid.add(boxView, col, row);
                }
            }
        }
    }

    public void handleKeyPress(KeyCode code) {
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
        if (targetObject == 0) { // 前方是空地
            move(playerRow, playerCol, targetRow, targetCol, 2);
            return true;
        } else if (targetObject == 3) { // 前方是箱子
            int boxTargetRow = targetRow + dRow;
            int boxTargetCol = targetCol + dCol;
            if (isValid(boxTargetRow, boxTargetCol) && currentLevelLayout[boxTargetRow][boxTargetCol] != 1 && currentMap[boxTargetRow][boxTargetCol] == 0) {
                move(targetRow, targetCol, boxTargetRow, boxTargetCol, 3); // 移动箱子
                move(playerRow, playerCol, targetRow, targetCol, 2); // 移动玩家
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
                    return; // 只要有一个目标点上没箱子，就没赢
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
        setControlsDisabled(true);
        timer.stop();
        movesLabel.setText("步数: 正在计算...");

        new Thread(() -> {
            List<KeyCode> solution = findSolution();
            Platform.runLater(() -> {
                if (solution == null) {
                    showAlert("求解失败", "此关卡太复杂，无法在短时间内找到解法。");
                    setControlsDisabled(false);
                    timer.play();
                } else {
                    movesLabel.setText("步数: 找到解法，共 " + solution.size() + " 步");
                    animateSolution(solution);
                }
            });
        }).start();
    }

    private List<KeyCode> findSolution() {
        int[] playerPos = findPlayer();
        if(playerPos == null) return null;

        List<Point> initialBoxes = new ArrayList<>();
        for (int i = 0; i < currentMap.length; i++) {
            for (int j = 0; j < currentMap[i].length; j++) {
                if (currentMap[i][j] == 3) {
                    initialBoxes.add(new Point(i, j));
                }
            }
        }
        initialBoxes.sort(Comparator.comparingInt(p -> p.row * 100 + p.col));

        GameState initialState = new GameState(playerPos[0], playerPos[1], initialBoxes, null, null);
        Queue<GameState> queue = new LinkedList<>();
        Set<GameState> visited = new HashSet<>();

        queue.add(initialState);
        visited.add(initialState);

        while (!queue.isEmpty()) {
            GameState currentState = queue.poll();

            if (isWinState(currentState.boxes)) {
                List<KeyCode> path = new ArrayList<>();
                GameState state = currentState;
                while (state.parent != null) {
                    path.add(state.move);
                    state = state.parent;
                }
                Collections.reverse(path);
                return path;
            }

            for (KeyCode move : Arrays.asList(KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT)) {
                int dRow = 0, dCol = 0;
                switch (move) {
                    case UP: dRow = -1; break;
                    case DOWN: dRow = 1; break;
                    case LEFT: dCol = -1; break;
                    case RIGHT: dCol = 1; break;
                }

                int nextPlayerRow = currentState.playerRow + dRow;
                int nextPlayerCol = currentState.playerCol + dCol;

                if (!isValid(nextPlayerRow, nextPlayerCol) || currentLevelLayout[nextPlayerRow][nextPlayerCol] == 1) continue;

                Point nextPlayerPoint = new Point(nextPlayerRow, nextPlayerCol);
                if (currentState.boxes.contains(nextPlayerPoint)) {
                    int nextBoxRow = nextPlayerRow + dRow;
                    int nextBoxCol = nextPlayerCol + dCol;
                    Point nextBoxPoint = new Point(nextBoxRow, nextBoxCol);
                    if (isValid(nextBoxRow, nextBoxCol) && currentLevelLayout[nextBoxRow][nextBoxCol] != 1 && !currentState.boxes.contains(nextBoxPoint)) {
                        List<Point> nextBoxes = new ArrayList<>(currentState.boxes);
                        nextBoxes.remove(nextPlayerPoint);
                        nextBoxes.add(nextBoxPoint);
                        nextBoxes.sort(Comparator.comparingInt(p -> p.row * 100 + p.col));

                        GameState nextState = new GameState(nextPlayerRow, nextPlayerCol, nextBoxes, currentState, move);
                        if (!visited.contains(nextState)) {
                            queue.add(nextState);
                            visited.add(nextState);
                        }
                    }
                } else {
                    GameState nextState = new GameState(nextPlayerRow, nextPlayerCol, new ArrayList<>(currentState.boxes), currentState, move);
                    if (!visited.contains(nextState)) {
                        queue.add(nextState);
                        visited.add(nextState);
                    }
                }
            }
        }
        return null;
    }

    private boolean isWinState(List<Point> boxes) {
        for (int i = 0; i < currentLevelLayout.length; i++) {
            for (int j = 0; j < currentLevelLayout[i].length; j++) {
                if (currentLevelLayout[i][j] == 4 && !boxes.contains(new Point(i, j))) {
                    return false;
                }
            }
        }
        return true;
    }

    private void animateSolution(List<KeyCode> solution) {
        solutionAnimation = new SequentialTransition();

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
            checkWinCondition();
        });

        solutionAnimation.play();
    }
}
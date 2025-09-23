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

    private final List<int[][]> levels = new ArrayList<>();
    private int currentLevelIndex = 0;
    private int moveCount = 0;

    private int[][] currentMap;
    private int[][] currentLevelLayout;

    private Image wallImage, playerImage, boxImage, goalImage, groundImage, boxOnGoalImage;

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
            // 比较玩家位置和箱子位置列表
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
            playerImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/player.png")));
            boxImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/box.png")));
            goalImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/goal.png")));
            groundImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/ground.png")));
            boxOnGoalImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/box_on_goal.png")));
        } catch (Exception e) {
            showAlert("错误", "图片加载失败！");
        }
    }

    private void createLevels() {
        // ... (关卡数据和之前一样，为节省篇幅省略，实际代码中应包含所有关卡)
        levels.add(new int[][]{
                {0,0,0,0,0,0,1,1,1,0,0,0},{0,0,0,0,0,0,1,4,1,0,0,0},{0,0,0,0,0,0,1,0,1,1,1,1},{0,0,0,0,1,1,1,3,0,3,4,1},{0,0,0,0,1,4,0,3,2,1,1,1},{0,0,0,0,1,1,1,1,3,1,0,0},{0,0,0,0,0,0,0,1,4,1,0,0},{0,0,0,0,0,0,0,1,1,1,0,0}
        });
        levels.add(new int[][]{
                {0,0,0,0,1,1,1,1,1,0},{0,0,0,0,1,2,0,0,1,0},{0,0,0,0,1,0,3,3,1,0},{0,0,0,0,1,0,3,0,1,0},{0,0,0,0,1,1,1,0,1,1},{0,0,0,0,0,1,1,0,0,0},{0,0,0,0,0,1,0,0,0,1},{0,0,0,0,0,1,0,0,0,1},{0,0,0,0,0,1,1,1,1,1}
        });
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

        int[][] originalLevel = levels.get(levelIndex);
        int maxWidth = Arrays.stream(originalLevel).mapToInt(row -> row.length).max().orElse(0);

        currentLevelLayout = new int[originalLevel.length][maxWidth];
        currentMap = new int[originalLevel.length][maxWidth];

        for (int i = 0; i < originalLevel.length; i++) {
            for (int j = 0; j < originalLevel[i].length; j++) {
                int tile = originalLevel[i][j];
                if (tile == 5) {
                    currentLevelLayout[i][j] = 4;
                    currentMap[i][j] = 3;
                } else if (tile == 2 || tile == 3) {
                    currentLevelLayout[i][j] = 0;
                    currentMap[i][j] = tile;
                } else {
                    currentLevelLayout[i][j] = tile;
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
        // *** 关键修复：添加空值检查 ***
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
                if (objectTile == 2 || objectTile == 3) {
                    ImageView topView = new ImageView();
                    topView.setFitWidth(40);
                    topView.setFitHeight(40);
                    if (objectTile == 2) {
                        topView.setImage(playerImage);
                    } else {
                        topView.setImage(currentLevelLayout[row][col] == 4 ? boxOnGoalImage : boxImage);
                    }
                    gameGrid.add(topView, col, row);
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
            case UP: dRow = -1; break;
            case DOWN: dRow = 1; break;
            case LEFT: dCol = -1; break;
            case RIGHT: dCol = 1; break;
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
        // 对箱子排序，确保GameState的哈希值唯一性
        initialBoxes.sort(Comparator.comparingInt(p -> p.row * 100 + p.col));

        GameState initialState = new GameState(playerPos[0], playerPos[1], initialBoxes, null, null);
        Queue<GameState> queue = new LinkedList<>();
        Set<GameState> visited = new HashSet<>();

        queue.add(initialState);
        visited.add(initialState);

        while (!queue.isEmpty()) {
            GameState currentState = queue.poll();

            // 检查胜利
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

            // 尝试移动
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
                if (currentState.boxes.contains(nextPlayerPoint)) { // 推箱子
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
                } else { // 仅移动玩家
                    GameState nextState = new GameState(nextPlayerRow, nextPlayerCol, new ArrayList<>(currentState.boxes), currentState, move);
                    // 注意：只移动玩家时，箱子位置不变，可能产生大量重复状态，但为了逻辑完整性保留
                    if (!visited.contains(nextState)) {
                        queue.add(nextState);
                        visited.add(nextState);
                    }
                }
            }
        }
        return null; // 未找到解法
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
            // 动画结束后，状态可能不是胜利状态（如果求解器不完美），所以重新检查
            checkWinCondition();
        });

        solutionAnimation.play();
    }
}
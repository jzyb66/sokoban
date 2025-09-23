package com.ai.demo;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HelloController {

    @FXML private BorderPane rootPane;
    @FXML private GridPane gameGrid;
    @FXML private Label levelLabel;
    @FXML private Button resetButton;

    // 数据映射: 0:空地, 1:墙, 2:玩家, 3:箱子, 4:目标点, 5:箱子在目标点上(仅用于初始布局)
    private final List<int[][]> levels = new ArrayList<>();
    private int currentLevelIndex = 0;

    private int[][] currentMap;         // 保存游戏当前状态
    private int[][] currentLevelLayout; // 仅保存关卡的墙体和目标点布局

    // 使用原始图片资源
    private Image wallImage, playerImage, boxImage, goalImage, groundImage, boxOnGoalImage;

    @FXML
    public void initialize() {
        loadImages();
        createLevels();
        loadLevel(currentLevelIndex);
    }

    private void loadImages() {
        try {
            // *** 使用您项目最开始的图片命名 ***
            wallImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/wall.png")));
            playerImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/player.png")));
            boxImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/box.png")));
            goalImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/goal.png")));
            groundImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/ground.png")));
            boxOnGoalImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/box_on_goal.png")));
        } catch (NullPointerException e) {
            showAlert("严重错误", "一个或多个图片资源加载失败！\n请确保 `resources/images` 文件夹存在，并且包含以下文件:\nwall.png, player.png, box.png, goal.png, ground.png, box_on_goal.png");
            e.printStackTrace();
        }
    }

    private void createLevels() {
        // 使用 shunyue1320/sokoban 项目的关卡数据
        // 0:空地, 1:墙, 2:玩家(原4), 3:箱子, 4:目标点(原2), 5:箱子在目标点上
        levels.add(new int[][]{
                {0,0,0,0,0,0,1,1,1,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,1,4,1,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,1,0,1,1,1,1,0,0,0,0},
                {0,0,0,0,1,1,1,3,0,3,4,1,0,0,0,0},
                {0,0,0,0,1,4,0,3,2,1,1,1,0,0,0,0},
                {0,0,0,0,1,1,1,1,3,1,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,1,4,1,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,1,1,1,0,0,0,0,0,0}
        });

        levels.add(new int[][]{
                {0,0,0,0,1,1,1,1,1,0,0,0,0,0,0,0},
                {0,0,0,0,1,2,0,0,1,0,0,0,0,0,0,0}, // JS版这里是4(人物)，但目标点是2，这里改为2
                {0,0,0,0,1,0,3,3,1,0,1,1,1,0,0,0},
                {0,0,0,0,1,0,3,0,1,0,1,4,1,0,0,0},
                {0,0,0,0,1,1,1,0,1,1,1,4,1,0,0,0},
                {0,0,0,0,0,1,1,0,0,0,0,4,1,0,0,0},
                {0,0,0,0,0,1,0,0,0,1,0,0,1,0,0,0},
                {0,0,0,0,0,1,0,0,0,1,1,1,1,0,0,0},
                {0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,0}
        });
    }

    private void loadLevel(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= levels.size()) return;
        currentLevelIndex = levelIndex;
        levelLabel.setText("关卡: " + (currentLevelIndex + 1));

        int[][] originalLevel = levels.get(levelIndex);
        currentLevelLayout = new int[originalLevel.length][];
        currentMap = new int[originalLevel.length][];

        for (int i = 0; i < originalLevel.length; i++) {
            int[] row = originalLevel[i];
            currentLevelLayout[i] = new int[row.length];
            currentMap[i] = new int[row.length];
            for (int j = 0; j < row.length; j++) {
                int tile = row[j];
                // 关键处理：分离布局和物体
                if (tile == 5) { // 箱子在目标点上
                    currentLevelLayout[i][j] = 4; // 布局层是目标点
                    currentMap[i][j] = 3;         // 游戏状态层是箱子
                } else if (tile == 2 || tile == 3) { // 玩家或箱子
                    currentLevelLayout[i][j] = 0; // 布局层是空地
                    currentMap[i][j] = tile;      // 游戏状态层是物体
                } else { // 墙、目标点、空地
                    currentLevelLayout[i][j] = tile;
                    currentMap[i][j] = 0; // 游戏状态层默认是空地
                }
            }
        }
        drawMap();

        if (rootPane != null) {
            rootPane.requestFocus();
        }
    }

    @FXML
    public void resetGame() {
        loadLevel(currentLevelIndex);
    }

    private void drawMap() {
        gameGrid.getChildren().clear();
        for (int row = 0; row < currentMap.length; row++) {
            for (int col = 0; col < currentMap[row].length; col++) {
                // 1. 画地砖
                ImageView groundView = new ImageView(groundImage);
                groundView.setFitWidth(40);
                groundView.setFitHeight(40);
                gameGrid.add(groundView, col, row);

                // 2. 在地砖上画目标点或墙
                int layoutTile = currentLevelLayout[row][col];
                if (layoutTile == 4) { // 目标点
                    ImageView goalView = new ImageView(goalImage);
                    goalView.setFitWidth(40);
                    goalView.setFitHeight(40);
                    gameGrid.add(goalView, col, row);
                } else if (layoutTile == 1) { // 墙
                    ImageView wallView = new ImageView(wallImage);
                    wallView.setFitWidth(40);
                    wallView.setFitHeight(40);
                    gameGrid.add(wallView, col, row);
                }

                // 3. 画最上层的动态物体 (玩家或箱子)
                int objectTile = currentMap[row][col];
                if (objectTile == 2 || objectTile == 3) {
                    ImageView topView = new ImageView();
                    topView.setFitWidth(40);
                    topView.setFitHeight(40);
                    if (objectTile == 2) {
                        topView.setImage(playerImage);
                    } else { // objectTile == 3
                        topView.setImage(currentLevelLayout[row][col] == 4 ? boxOnGoalImage : boxImage);
                    }
                    gameGrid.add(topView, col, row);
                }
            }
        }
    }

    public void handleKeyPress(KeyCode code) {
        int dRow = 0, dCol = 0;
        switch (code) {
            case UP: dRow = -1; break;
            case DOWN: dRow = 1; break;
            case LEFT: dCol = -1; break;
            case RIGHT: dCol = 1; break;
            default: return;
        }

        int[] playerPos = findPlayer();
        if (playerPos == null) return;
        int playerRow = playerPos[0];
        int playerCol = playerPos[1];

        int targetRow = playerRow + dRow;
        int targetCol = playerCol + dCol;

        if (!isValid(targetRow, targetCol)) return;
        if (currentLevelLayout[targetRow][targetCol] == 1) return; // 撞墙

        // 查看目标格子上的物体
        int targetObject = currentMap[targetRow][targetCol];
        if (targetObject == 0) { // 目标是空地
            move(playerRow, playerCol, targetRow, targetCol, 2);
        } else if (targetObject == 3) { // 目标是箱子
            int boxTargetRow = targetRow + dRow;
            int boxTargetCol = targetCol + dCol;
            if (isValid(boxTargetRow, boxTargetCol) && currentLevelLayout[boxTargetRow][boxTargetCol] != 1 && currentMap[boxTargetRow][boxTargetCol] == 0) {
                move(targetRow, targetCol, boxTargetRow, boxTargetCol, 3); // 移动箱子
                move(playerRow, playerCol, targetRow, targetCol, 2);   // 移动玩家
            }
        }

        drawMap();
        checkWinCondition();
    }

    private void move(int oldRow, int oldCol, int newRow, int newCol, int objectId) {
        currentMap[oldRow][oldCol] = 0; // 原位置变为空物体
        currentMap[newRow][newCol] = objectId; // 新位置变为移动的物体
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

        if (currentLevelIndex < levels.size() - 1) {
            showAlert("恭喜过关!", "你完成了第 " + (currentLevelIndex + 1) + " 关！即将进入下一关。");
            loadLevel(currentLevelIndex + 1);
        } else {
            showAlert("恭喜!", "你已经完成了所有关卡！游戏将从第一关重新开始。");
            loadLevel(0);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
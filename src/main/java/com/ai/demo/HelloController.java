package com.ai.demo;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;

public class HelloController {

    @FXML
    private GridPane gameGrid;

    // 原始地图布局，用于重置游戏
    private final int[][] levelMap = {
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {1, 0, 2, 0, 0, 0, 0, 0, 1, 1},
            {1, 0, 0, 3, 0, 1, 4, 0, 0, 1},
            {1, 1, 1, 0, 3, 0, 0, 4, 0, 1},
            {1, 0, 0, 0, 1, 1, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
    };

    // 当前游戏状态的地图
    private int[][] currentMap;

    private Image wallImage;
    private Image playerImage;
    private Image boxImage;
    private Image goalImage;
    private Image groundImage;
    private Image boxOnGoalImage; // 箱子在目标点上的图片

    @FXML
    public void initialize() {
        try {
            wallImage = new Image(getClass().getResourceAsStream("/images/wall.png"));
            playerImage = new Image(getClass().getResourceAsStream("/images/player.png"));
            boxImage = new Image(getClass().getResourceAsStream("/images/box.png"));
            goalImage = new Image(getClass().getResourceAsStream("/images/goal.png"));
            groundImage = new Image(getClass().getResourceAsStream("/images/ground.png"));
            boxOnGoalImage = new Image(getClass().getResourceAsStream("/images/box_on_goal.png")); // 假设有这张图
        } catch (Exception e) {
            System.err.println("图片加载失败。请确保images文件夹中有 wall.png, player.png, box.png, goal.png, ground.png, 和 box_on_goal.png。");
            e.printStackTrace();
        }

        resetGame();
    }

    private void resetGame() {
        currentMap = new int[levelMap.length][];
        for (int i = 0; i < levelMap.length; i++) {
            currentMap[i] = new int[levelMap[i].length];
            System.arraycopy(levelMap[i], 0, currentMap[i], 0, levelMap[i].length);
        }
        drawMap();
    }

    private void drawMap() {
        gameGrid.getChildren().clear();
        for (int row = 0; row < currentMap.length; row++) {
            for (int col = 0; col < currentMap[row].length; col++) {
                ImageView groundView = new ImageView(groundImage);
                groundView.setFitWidth(40);
                groundView.setFitHeight(40);
                gameGrid.add(groundView, col, row);

                ImageView imageView = new ImageView();
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);

                // 如果原始地图是目标点，先画目标点
                if (levelMap[row][col] == 4) {
                    ImageView goalView = new ImageView(goalImage);
                    goalView.setFitWidth(40);
                    goalView.setFitHeight(40);
                    gameGrid.add(goalView, col, row);
                }

                switch (currentMap[row][col]) {
                    case 1:
                        imageView.setImage(wallImage);
                        break;
                    case 2:
                        imageView.setImage(playerImage);
                        break;
                    case 3: // 判断箱子是否在目标点上
                        if (levelMap[row][col] == 4) {
                            imageView.setImage(boxOnGoalImage);
                        } else {
                            imageView.setImage(boxImage);
                        }
                        break;
                    // 目标点已经预先画好，所以这里不需要再画
                    case 4:
                    default:
                        continue;
                }
                gameGrid.add(imageView, col, row);
            }
        }
    }

    public void handleKeyPress(KeyCode code) {
        if (code != KeyCode.UP && code != KeyCode.DOWN && code != KeyCode.LEFT && code != KeyCode.RIGHT) {
            return;
        }

        // 1. 找到玩家当前位置
        int playerRow = -1, playerCol = -1;
        for (int i = 0; i < currentMap.length; i++) {
            for (int j = 0; j < currentMap[i].length; j++) {
                if (currentMap[i][j] == 2) {
                    playerRow = i;
                    playerCol = j;
                    break;
                }
            }
        }

        // 2. 计算目标位置
        int targetRow = playerRow;
        int targetCol = playerCol;
        switch (code) {
            case UP:    targetRow--; break;
            case DOWN:  targetRow++; break;
            case LEFT:  targetCol--; break;
            case RIGHT: targetCol++; break;
        }

        // 3. 移动逻辑判断
        // 如果目标是墙，则不动
        if (currentMap[targetRow][targetCol] == 1) {
            return;
        }
        // 如果目标是空地或目标点
        else if (currentMap[targetRow][targetCol] == 0 || currentMap[targetRow][targetCol] == 4) {
            currentMap[playerRow][playerCol] = 0;
            currentMap[targetRow][targetCol] = 2;
        }
        // 如果目标是箱子
        else if (currentMap[targetRow][targetCol] == 3) {
            int boxTargetRow = targetRow + (targetRow - playerRow);
            int boxTargetCol = targetCol + (targetCol - playerCol);

            // 判断箱子的目标位置是否可达
            if (currentMap[boxTargetRow][boxTargetCol] == 0 || currentMap[boxTargetRow][boxTargetCol] == 4) {
                currentMap[targetRow][targetCol] = 0; // 原箱子位置变为空地
                currentMap[boxTargetRow][boxTargetCol] = 3; // 新位置变为箱子
                currentMap[playerRow][playerCol] = 0; // 原玩家位置变为空地
                currentMap[targetRow][targetCol] = 2; // 新玩家位置
            }
        }

        // 4. 重新绘制地图并检查胜利条件
        drawMap();
        checkWinCondition();
    }

    private void checkWinCondition() {
        for (int i = 0; i < levelMap.length; i++) {
            for (int j = 0; j < levelMap[i].length; j++) {
                // 如果一个目标点上没有箱子，则游戏未结束
                if (levelMap[i][j] == 4 && currentMap[i][j] != 3) {
                    return;
                }
            }
        }

        // 如果所有目标点都有箱子，则游戏胜利
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("恭喜!");
        alert.setHeaderText(null);
        alert.setContentText("你赢了！游戏将重置。");
        alert.showAndWait();
        resetGame();
    }
}
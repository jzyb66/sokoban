package com.ai.sokoban;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * UI界面管理器。
 * 【职责】: 专门负责所有与JavaFX界面相关的操作，实现视图和逻辑的彻底分离。
 * 1. 持有所有需要操作的UI组件的引用。
 * 2. 加载和管理游戏所需的图片资源。
 * 3. 根据地图数据在GridPane上绘制游戏场景。
 * 4. 更新界面上的所有标签（关卡、步数、时间）。
 * 5. 根据游戏模式（手动、播放、暂停）切换按钮的可见性和状态。
 * 6. 控制键盘事件的启用和禁用。
 */
public class UIManager {

    // --- UI组件的引用 ---
    private final StackPane localRootPane;      // FXML内部的rootPane
    private StackPane externalRootPane;         // 从Application传入的，真正带有Scene的rootPane
    private final GridPane gameGrid;
    private final Label levelLabel;
    private final Label movesLabel;
    private final Label timeLabel;
    private final Button solveButton;
    private final Button resetButton;
    private final ChoiceBox<Integer> levelChoiceBox;
    private final Button pauseButton;
    private final Button prevStepButton;
    private final Button nextStepButton;

    // --- 游戏元素的图片资源 ---
    private final Image wallImage, boxImage, goalImage, groundImage, boxOnGoalImage;
    private final Image playerUpImage, playerDownImage, playerLeftImage, playerRightImage;
    private Image currentPlayerImage;

    /**
     * UIManager的构造函数。
     */
    public UIManager(StackPane rootPane, GridPane gameGrid, Label levelLabel, Label movesLabel, Label timeLabel,
                     Button solveButton, Button resetButton, ChoiceBox<Integer> levelChoiceBox,
                     Button pauseButton, Button prevStepButton, Button nextStepButton) {
        this.localRootPane = rootPane;
        this.gameGrid = gameGrid;
        this.levelLabel = levelLabel;
        this.movesLabel = movesLabel;
        this.timeLabel = timeLabel;
        this.solveButton = solveButton;
        this.resetButton = resetButton;
        this.levelChoiceBox = levelChoiceBox;
        this.pauseButton = pauseButton;
        this.prevStepButton = prevStepButton;
        this.nextStepButton = nextStepButton;

        // 加载所有图片资源
        this.wallImage = loadImage("/images/wall.png");
        this.boxImage = loadImage("/images/box.png");
        this.goalImage = loadImage("/images/goal.png");
        this.groundImage = loadImage("/images/ground.png");
        this.boxOnGoalImage = loadImage("/images/box_on_goal.png");
        this.playerUpImage = loadImage("/images/player_up.png");
        this.playerDownImage = loadImage("/images/player_down.png");
        this.playerLeftImage = loadImage("/images/player_left.png");
        this.playerRightImage = loadImage("/images/player_right.png");

        this.currentPlayerImage = playerDownImage; // 默认玩家朝下
    }

    /**
     * 接收来自外部（HelloApplication）的根节点引用。
     * 这个引用是必需的，因为只有它才关联了Scene，才能正确地控制键盘事件。
     * @param rootPane 应用程序的根StackPane。
     */
    public void setExternalRootPane(StackPane rootPane) {
        this.externalRootPane = rootPane;
    }

    /**
     * 从资源路径加载图片的辅助方法。
     * @param path 图片在resources下的路径。
     * @return 加载后的Image对象。
     */
    private Image loadImage(String path) {
        try {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
        } catch (Exception e) {
            System.err.println("图片加载失败: " + path);
            e.printStackTrace();
            return null; // 在实际应用中可以返回一个默认的错误图片
        }
    }

    /**
     * 根据游戏地图和布局数据在GridPane上绘制游戏场景。
     * @param map 包含动态对象（玩家、箱子）的地图。
     * @param layout 包含静态元素（墙、目标点）的布局。
     */
    public void drawMap(int[][] map, int[][] layout) {
        gameGrid.getChildren().clear(); // 绘制前先清空旧的地图
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                // 每个格子先绘制一个地砖作为背景
                ImageView groundView = new ImageView(groundImage);
                groundView.setFitWidth(40);
                groundView.setFitHeight(40);
                gameGrid.add(groundView, col, row);

                // 在地砖上绘制静态布局（墙、目标点）
                int layoutTile = layout[row][col];
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

                // 在最上层绘制动态对象（玩家、箱子）
                int objectTile = map[row][col];
                if (objectTile == 2) { // 玩家
                    ImageView playerView = new ImageView(currentPlayerImage);
                    playerView.setFitWidth(40);
                    playerView.setFitHeight(40);
                    gameGrid.add(playerView, col, row);
                } else if (objectTile == 3) { // 箱子
                    // 如果箱子在目标点上，显示“箱子在目标点”的特殊图片
                    ImageView boxView = new ImageView(layout[row][col] == 4 ? boxOnGoalImage : boxImage);
                    boxView.setFitWidth(40);
                    boxView.setFitHeight(40);
                    gameGrid.add(boxView, col, row);
                }
            }
        }
        // 自动调整窗口大小以适应地图
        if (localRootPane.getScene() != null && localRootPane.getScene().getWindow() != null) {
            localRootPane.getScene().getWindow().sizeToScene();
        }
    }

    /**
     * 根据玩家移动方向，更新当前要显示的玩家图片。
     * @param direction 玩家移动的方向 (KeyCode)。
     */
    public void updatePlayerImage(KeyCode direction) {
        switch (direction) {
            case UP:    currentPlayerImage = playerUpImage;    break;
            case DOWN:  currentPlayerImage = playerDownImage;  break;
            case LEFT:  currentPlayerImage = playerLeftImage;  break;
            case RIGHT: currentPlayerImage = playerRightImage; break;
        }
    }

    /**
     * 初始化关卡选择下拉框。
     * @param numLevels 总关卡数。
     * @param onLevelSelected 当用户选择新关卡时的回调函数。
     */
    public void setupLevelChoiceBox(int numLevels, Consumer<Integer> onLevelSelected) {
        levelChoiceBox.getItems().addAll(
                IntStream.rangeClosed(1, numLevels).boxed().collect(Collectors.toList())
        );
        levelChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                onLevelSelected.accept(newVal - 1); // 将选择的关卡号(从1开始)转换为索引(从0开始)
            }
        });
    }

    // --- UI组件更新方法 ---
    public void updateLevelLabel(int level) { levelLabel.setText("关卡: " + level); }
    public void updateMovesLabel(int moves) { movesLabel.setText("步数: " + moves); }
    public void updateMovesLabelText(String text) { movesLabel.setText(text); }
    public void updateTimeLabel(int seconds) { timeLabel.setText("时间: " + seconds + "s"); }
    public void selectLevelInChoiceBox(int levelIndex) {
        if (levelChoiceBox.getSelectionModel().getSelectedIndex() != levelIndex) {
            levelChoiceBox.getSelectionModel().select(levelIndex);
        }
    }

    /**
     * 设置UI控件以适应手动游戏模式。
     * @param onUndoAction “上一步”按钮的点击事件处理器。
     */
    public void setControlsForManualPlay(EventHandler<ActionEvent> onUndoAction) {
        solveButton.setDisable(false);
        resetButton.setDisable(false);
        levelChoiceBox.setDisable(false);
        prevStepButton.setVisible(true);
        prevStepButton.setText("上一步");
        prevStepButton.setOnAction(onUndoAction);
        nextStepButton.setVisible(false);
        pauseButton.setVisible(false);
        enableKeyboardInput();
    }

    /**
     * 设置UI控件以适应答案播放模式。
     */
    public void setControlsForSolving() {
        solveButton.setDisable(true);
        resetButton.setDisable(true);
        levelChoiceBox.setDisable(true);
        prevStepButton.setVisible(false);
        nextStepButton.setVisible(false);
        pauseButton.setVisible(true);
        pauseButton.setDisable(false);
        pauseButton.setText("暂停");
        disableKeyboardInput();
    }

    /**
     * 设置UI控件以适应答案暂停模式。
     * @param onPrevAction “答案上一步”按钮的事件处理器。
     * @param onNextAction “答案下一步”按钮的事件处理器。
     */
    public void setControlsForPausedSolution(EventHandler<ActionEvent> onPrevAction, EventHandler<ActionEvent> onNextAction) {
        resetButton.setDisable(false);
        pauseButton.setText("继续");
        prevStepButton.setVisible(true);
        prevStepButton.setText("答案上一步");
        prevStepButton.setOnAction(onPrevAction);
        nextStepButton.setVisible(true);
        nextStepButton.setText("答案下一步");
        nextStepButton.setOnAction(onNextAction);
    }

    public void hidePauseButton() { pauseButton.setVisible(false); }

    /**
     * 禁用键盘输入。通过将根节点的事件处理器设为null实现。
     */
    public void disableKeyboardInput() {
        if (externalRootPane != null) {
            externalRootPane.setOnKeyPressed(null);
        }
    }

    /**
     * 启用键盘输入。它重新设置了在 HelloApplication 中定义的原始事件处理器。
     */
    public void enableKeyboardInput() {
        if (externalRootPane != null && externalRootPane.getScene() != null) {
            externalRootPane.setOnKeyPressed(externalRootPane.getScene().getOnKeyPressed());
        }
    }

    public void requestFocusOnRoot() {
        if (localRootPane != null) {
            localRootPane.requestFocus();
        }
    }
}
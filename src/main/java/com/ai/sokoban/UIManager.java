package com.ai.sokoban;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * UI界面管理器。
 * 【职责】: 专门负责所有与JavaFX界面相关的操作。
 */
public class UIManager {

    private final StackPane localRootPane;
    private StackPane externalRootPane;
    private EventHandler<KeyEvent> originalKeyEventHandler;

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

        this.wallImage = loadImage("/images/wall.png");
        this.boxImage = loadImage("/images/box.png");
        this.goalImage = loadImage("/images/goal.png");
        this.groundImage = loadImage("/images/ground.png");
        this.boxOnGoalImage = loadImage("/images/box_on_goal.png");
        this.playerUpImage = loadImage("/images/player_up.png");
        this.playerDownImage = loadImage("/images/player_down.png");
        this.playerLeftImage = loadImage("/images/player_left.png");
        this.playerRightImage = loadImage("/images/player_right.png");

        this.currentPlayerImage = playerDownImage;
    }

    /**
     * 接收并保存外部的根节点和键盘处理器。
     */
    public void setExternalRootPaneAndHandler(StackPane rootPane, EventHandler<KeyEvent> keyEventHandler) {
        this.externalRootPane = rootPane;
        this.originalKeyEventHandler = keyEventHandler;
    }

    private Image loadImage(String path) {
        try {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
        } catch (Exception e) {
            System.err.println("图片加载失败: " + path);
            e.printStackTrace();
            return null;
        }
    }

    public void drawMap(int[][] map, int[][] layout) {
        gameGrid.getChildren().clear();
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                ImageView groundView = new ImageView(groundImage);
                groundView.setFitWidth(40);
                groundView.setFitHeight(40);
                gameGrid.add(groundView, col, row);

                int layoutTile = layout[row][col];
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

                int objectTile = map[row][col];
                if (objectTile == 2) {
                    ImageView playerView = new ImageView(currentPlayerImage);
                    playerView.setFitWidth(40);
                    playerView.setFitHeight(40);
                    gameGrid.add(playerView, col, row);
                } else if (objectTile == 3) {
                    ImageView boxView = new ImageView(layout[row][col] == 4 ? boxOnGoalImage : boxImage);
                    boxView.setFitWidth(40);
                    boxView.setFitHeight(40);
                    gameGrid.add(boxView, col, row);
                }
            }
        }
        if (localRootPane.getScene() != null && localRootPane.getScene().getWindow() != null) {
            localRootPane.getScene().getWindow().sizeToScene();
        }
    }

    public void updatePlayerImage(KeyCode direction) {
        switch (direction) {
            case UP:    currentPlayerImage = playerUpImage;    break;
            case DOWN:  currentPlayerImage = playerDownImage;  break;
            case LEFT:  currentPlayerImage = playerLeftImage;  break;
            case RIGHT: currentPlayerImage = playerRightImage; break;
        }
    }

    public void setupLevelChoiceBox(int numLevels, Consumer<Integer> onLevelSelected) {
        levelChoiceBox.getItems().addAll(
                IntStream.rangeClosed(1, numLevels).boxed().collect(Collectors.toList())
        );
        levelChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                onLevelSelected.accept(newVal - 1);
            }
        });
    }

    public void updateLevelLabel(int level) { levelLabel.setText("关卡: " + level); }
    public void updateMovesLabel(int moves) { movesLabel.setText("步数: " + moves); }
    public void updateMovesLabelText(String text) { movesLabel.setText(text); }
    public void updateTimeLabel(int seconds) { timeLabel.setText("时间: " + seconds + "s"); }
    public void selectLevelInChoiceBox(int levelIndex) {
        if (levelChoiceBox.getSelectionModel().getSelectedIndex() != levelIndex) {
            levelChoiceBox.getSelectionModel().select(levelIndex);
        }
    }

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

    public void disableKeyboardInput() {
        if (externalRootPane != null) {
            externalRootPane.setOnKeyPressed(null);
        }
    }

    public void enableKeyboardInput() {
        if (externalRootPane != null) {
            externalRootPane.setOnKeyPressed(originalKeyEventHandler);
        }
    }

    public void requestFocusOnRoot() {
        if (localRootPane != null) {
            localRootPane.requestFocus();
        }
    }
}
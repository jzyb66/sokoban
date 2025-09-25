package com.ai.sokoban;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

/**
 * 主视图(hello-view.fxml)的控制器。
 * 【职责】: 充当视图(View)和业务逻辑(Logic)之间的桥梁。
 * 1. 负责初始化UIManager和GameLogic。
 * 2. 将FXML中的UI组件引用传递给UIManager。
 * 3. 接收用户的输入事件（如点击按钮、按键），并调用GameLogic中对应的方法来处理。
 */
public class HelloController {

    // @FXML注解：将代码中的变量与FXML文件中对应fx:id的UI组件关联起来
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

    private GameLogic gameLogic; // 游戏核心逻辑处理器

    /**
     * 初始化方法，在FXML文件加载完成后由JavaFX平台自动调用。
     */
    @FXML
    public void initialize() {
        // 1. 创建UI管理器，并将所有需要操控的UI组件作为参数传入
        UIManager uiManager = new UIManager(
                rootPane, gameGrid, levelLabel, movesLabel, timeLabel,
                solveButton, resetButton, levelChoiceBox, pauseButton,
                prevStepButton, nextStepButton
        );

        // 2. 创建游戏逻辑处理器，并将UI管理器传入，以便逻辑处理器在需要时可以更新UI
        this.gameLogic = new GameLogic(uiManager);

        // 3. 控制器调用游戏逻辑处理器，开始加载第一个关卡
        gameLogic.loadLevel(0);
    }

    /**
     * 由HelloApplication调用，用于将最顶层的根节点(Root Pane)的引用传递进来。
     * 这是为了让UIManager能够安全地控制键盘事件的监听。
     * @param rootPane 应用程序的根StackPane。
     */
    public void setRootPaneForUIManager(StackPane rootPane) {
        if (gameLogic != null && gameLogic.getUiManager() != null) {
            gameLogic.getUiManager().setExternalRootPane(rootPane);
        }
    }

    /**
     * 处理键盘按键事件。
     * 当用户在界面上按下键盘时，此方法由HelloApplication中的事件监听器调用。
     * @param code 被按下的键的键码 (KeyCode)。
     */
    public void handleKeyPress(KeyCode code) {
        // 将按键事件直接转发给游戏逻辑处理器
        if (gameLogic != null) {
            gameLogic.handlePlayerMove(code);
        }
    }

    /**
     * 响应“重置本关”按钮的点击事件 (onAction="#resetGame")。
     */
    @FXML
    public void resetGame() {
        gameLogic.resetCurrentLevel();
    }

    /**
     * 响应“答案”按钮的点击事件 (onAction="#solveLevel")。
     */
    @FXML
    private void solveLevel() {
        gameLogic.solveLevel();
    }

    /**
     * 响应“暂停/继续”按钮的点击事件 (onAction="#pauseSolveAnimation")。
     */
    @FXML
    private void pauseSolveAnimation() {
        gameLogic.toggleSolutionAnimation();
    }
}
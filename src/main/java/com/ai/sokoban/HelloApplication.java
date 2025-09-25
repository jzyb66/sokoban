package com.ai.sokoban;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 游戏主程序入口类。
 * 【职责】: 启动应用、加载主视图(FXML)并进行初始化设置。
 */
public class HelloApplication extends Application {

    /**
     * JavaFX应用的启动方法，是程序的主入口点。
     * @param stage 主舞台对象，由JavaFX平台自动创建和传入。
     * @throws IOException 当加载hello-view.fxml文件失败时抛出。
     */
    @Override
    public void start(Stage stage) throws IOException {
        // 1. 创建FXMLLoader实例，用于加载FXML布局文件
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));

        // 2. 加载FXML定义的根节点 (StackPane) 并获取其控制器
        StackPane root = fxmlLoader.load();
        HelloController controller = fxmlLoader.getController();

        // 3. 创建场景(Scene)
        Scene scene = new Scene(root);

        // 4. 定义键盘事件处理器。这是一个lambda表达式，它会在按键时调用控制器的方法
        EventHandler<KeyEvent> keyEventHandler = event -> {
            controller.handleKeyPress(event.getCode());
        };

        // 5. 将该处理器设置到根节点上，使其能够监听全局按键
        root.setOnKeyPressed(keyEventHandler);

        // 6. **关键步骤**: 将根节点和键盘处理器本身一起传递给控制器。
        //    这样做可以确保UIManager能随时恢复或禁用正确的键盘监听，解决了切换关卡后无法控制的问题。
        controller.setupUIManager(root, keyEventHandler);

        // 7. 完成舞台（窗口）的设置
        stage.setTitle("推箱子 (Sokoban)");
        stage.setScene(scene);
        stage.sizeToScene(); // 窗口大小自适应内容
        stage.setResizable(false); // 禁止调整窗口大小
        stage.show(); // 显示窗口

        // 8. 让根节点获得焦点，以便立即开始接收键盘事件
        root.requestFocus();
    }

    /**
     * Java程序的main方法。
     * @param args 命令行参数（本游戏中未使用）。
     */
    public static void main(String[] args) {
        // 调用launch()方法来启动JavaFX应用
        launch();
    }
}
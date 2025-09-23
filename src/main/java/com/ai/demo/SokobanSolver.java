package com.ai.demo;

import javafx.scene.input.KeyCode;
import java.util.*;
import java.util.function.LongConsumer;

public class SokobanSolver {

    record Point(int row, int col) {}

    static class GameState {
        final int playerRow;
        final int playerCol;
        final List<Point> boxes;
        final GameState parent;
        final KeyCode move;
        final int g;
        int h;

        GameState(int playerRow, int playerCol, List<Point> boxes, GameState parent, KeyCode move, int g) {
            this.playerRow = playerRow;
            this.playerCol = playerCol;
            this.boxes = boxes;
            this.parent = parent;
            this.move = move;
            this.g = g;
        }

        public int getF() {
            return g + h;
        }

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

    private final int[][] levelLayout;
    private final List<Point> goals = new ArrayList<>();
    private final boolean[][] deadSquares;

    public SokobanSolver(int[][] levelLayout) {
        this.levelLayout = levelLayout;
        for (int i = 0; i < levelLayout.length; i++) {
            for (int j = 0; j < levelLayout[i].length; j++) {
                if (levelLayout[i][j] == 4) { // 4 is a goal
                    goals.add(new Point(i, j));
                }
            }
        }
        // 预先计算所有死点位置
        this.deadSquares = precomputeDeadlocks();
    }

    // 预计算死锁位置 (高级功能)
    private boolean[][] precomputeDeadlocks() {
        int rows = levelLayout.length;
        int cols = Arrays.stream(levelLayout).mapToInt(r -> r.length).max().orElse(0);
        boolean[][] dead = new boolean[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (levelLayout[r][c] == 1) continue; // 墙不是死点
                if (goals.contains(new Point(r, c))) continue; // 目标点不是死点

                // 检查角落死锁
                boolean upWall = (r == 0) || levelLayout[r - 1][c] == 1;
                boolean downWall = (r == rows - 1) || levelLayout[r + 1][c] == 1;
                boolean leftWall = (c == 0) || levelLayout[r][c - 1] == 1;
                boolean rightWall = (c == cols - 1) || levelLayout[r][c + 1] == 1;
                if ((upWall || downWall) && (leftWall || rightWall)) {
                    dead[r][c] = true;
                }
            }
        }

        // 检查墙边死锁 (更复杂的情况)
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!dead[r][c]) {
                    // 从每个非死角点开始，看是否能把箱子推到所有目标点
                    if (isWallDeadlock(r, c, dead)) {
                        dead[r][c] = true;
                    }
                }
            }
        }
        return dead;
    }

    private boolean isWallDeadlock(int row, int col, boolean[][] dead) {
        // 检查水平墙边
        boolean stuckHorizontal = true;
        for (int c = 0; c < dead[0].length; c++) {
            if (!dead[row][c] && !goals.contains(new Point(row, c))) {
                stuckHorizontal = false;
                break;
            }
        }
        if (stuckHorizontal) return true;

        // 检查垂直墙边
        boolean stuckVertical = true;
        for (int r = 0; r < dead.length; r++) {
            if (!dead[r][col] && !goals.contains(new Point(r, col))) {
                stuckVertical = false;
                break;
            }
        }
        return stuckVertical;
    }


    // A* 算法，现在使用 progressUpdater 来报告进度
    public List<KeyCode> solve(int[][] initialMap, LongConsumer progressUpdater) {
        int[] playerPos = findPlayer(initialMap);
        if (playerPos == null) return null;

        List<Point> initialBoxes = findBoxes(initialMap);
        initialBoxes.sort(Comparator.comparingInt(p -> p.row * 1000 + p.col));

        GameState initialState = new GameState(playerPos[0], playerPos[1], initialBoxes, null, null, 0);
        initialState.h = calculateHeuristic(initialBoxes);

        PriorityQueue<GameState> openSet = new PriorityQueue<>(Comparator.comparingInt(GameState::getF));
        Set<GameState> closedSet = new HashSet<>();
        openSet.add(initialState);

        while (!openSet.isEmpty()) {
            GameState currentState = openSet.poll();

            if (closedSet.size() % 2000 == 0) { // 每处理2000个状态，更新一次UI
                progressUpdater.accept((long)closedSet.size());
            }

            if (isWinState(currentState.boxes)) {
                return reconstructPath(currentState);
            }

            closedSet.add(currentState);

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

                if (!isValid(nextPlayerRow, nextPlayerCol) || levelLayout[nextPlayerRow][nextPlayerCol] == 1) continue;

                Point nextPlayerPoint = new Point(nextPlayerRow, nextPlayerCol);
                GameState nextState;

                if (currentState.boxes.contains(nextPlayerPoint)) {
                    int nextBoxRow = nextPlayerRow + dRow;
                    int nextBoxCol = nextPlayerCol + dCol;

                    if (isValid(nextBoxRow, nextBoxCol) && levelLayout[nextBoxRow][nextBoxCol] != 1 && !currentState.boxes.contains(new Point(nextBoxRow, nextBoxCol))) {
                        // 使用预计算的死锁信息
                        if (deadSquares[nextBoxRow][nextBoxCol]) {
                            continue;
                        }

                        List<Point> nextBoxes = new ArrayList<>(currentState.boxes);
                        nextBoxes.remove(nextPlayerPoint);
                        nextBoxes.add(new Point(nextBoxRow, nextBoxCol));
                        nextBoxes.sort(Comparator.comparingInt(p -> p.row * 1000 + p.col));

                        nextState = new GameState(nextPlayerRow, nextPlayerCol, nextBoxes, currentState, move, currentState.g + 1);
                        nextState.h = calculateHeuristic(nextBoxes);

                        if (!closedSet.contains(nextState) && !openSet.contains(nextState)) {
                            openSet.add(nextState);
                        }
                    }
                } else {
                    nextState = new GameState(nextPlayerRow, nextPlayerCol, new ArrayList<>(currentState.boxes), currentState, move, currentState.g + 1);
                    nextState.h = currentState.h;
                    if (!closedSet.contains(nextState) && !openSet.contains(nextState)) {
                        openSet.add(nextState);
                    }
                }
            }
        }
        return null;
    }

    private int calculateHeuristic(List<Point> boxes) {
        int totalDistance = 0;
        for (Point box : boxes) {
            int minDistance = Integer.MAX_VALUE;
            for (Point goal : goals) {
                minDistance = Math.min(minDistance, Math.abs(box.row - goal.row) + Math.abs(box.col - goal.col));
            }
            totalDistance += minDistance;
        }
        return totalDistance;
    }

    private boolean isWinState(List<Point> boxes) {
        return new HashSet<>(boxes).equals(new HashSet<>(goals));
    }

    private List<KeyCode> reconstructPath(GameState state) {
        List<KeyCode> path = new ArrayList<>();
        while (state.parent != null) {
            path.add(state.move);
            state = state.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private int[] findPlayer(int[][] map) {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == 2) return new int[]{i, j};
            }
        }
        return null;
    }

    private List<Point> findBoxes(int[][] map) {
        List<Point> boxes = new ArrayList<>();
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == 3) {
                    boxes.add(new Point(i, j));
                }
            }
        }
        return boxes;
    }

    private boolean isValid(int row, int col) {
        return row >= 0 && row < levelLayout.length && col >= 0 && col < levelLayout[row].length;
    }
}
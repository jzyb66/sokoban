package com.ai.demo;

import javafx.scene.input.KeyCode;
import java.util.*;
import java.util.function.LongConsumer;

public class SokobanSolver {

    record Point(int row, int col) implements Comparable<Point> {
        @Override
        public int compareTo(Point other) {
            int rowCmp = Integer.compare(row, other.row);
            return rowCmp != 0 ? rowCmp : Integer.compare(col, other.col);
        }
    }

    static class GameState {
        final Point playerPosition;
        final TreeSet<Point> boxes;
        final GameState parent;
        final List<KeyCode> pathToThisState;
        final int g;
        int h;

        GameState(Point playerPosition, TreeSet<Point> boxes, GameState parent, List<KeyCode> pathToThisState, int g) {
            this.playerPosition = playerPosition;
            this.boxes = boxes;
            this.parent = parent;
            this.pathToThisState = pathToThisState;
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
            return playerPosition.equals(gameState.playerPosition) && boxes.equals(gameState.boxes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerPosition, boxes);
        }
    }

    private final int[][] levelLayout;
    private final List<Point> goals;
    private final boolean[][] deadSquares;
    private final int rows;
    private final int cols;

    public SokobanSolver(int[][] levelLayout) {
        this.levelLayout = levelLayout;
        this.rows = levelLayout.length;
        this.cols = Arrays.stream(levelLayout).mapToInt(r -> r.length).max().orElse(0);
        this.goals = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (levelLayout[i][j] == 4) goals.add(new Point(i, j));
            }
        }
        this.deadSquares = precomputeSimpleDeadlocks();
    }

    private boolean[][] precomputeSimpleDeadlocks() {
        boolean[][] dead = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (goals.contains(new Point(r, c))) continue;
                if (levelLayout[r][c] == 1) continue;

                boolean upWall = (r == 0) || levelLayout[r - 1][c] == 1;
                boolean downWall = (r == rows - 1) || levelLayout[r + 1][c] == 1;
                boolean leftWall = (c == 0) || levelLayout[r][c - 1] == 1;
                boolean rightWall = (c == cols - 1) || levelLayout[r][c + 1] == 1;
                if ((upWall || downWall) && (leftWall || rightWall)) {
                    dead[r][c] = true;
                }
            }
        }
        return dead;
    }

    /**
     * The final and most effective deadlock detection.
     * It checks if a box pushed to a non-goal square is now permanently stuck.
     */
    private boolean isDeadlocked(Point pushedBox, TreeSet<Point> boxes) {
        // If the box is on a goal, it's never a deadlock.
        if (goals.contains(pushedBox)) {
            return false;
        }

        // Check simple corner deadlocks first (very fast)
        if (deadSquares[pushedBox.row][pushedBox.col]) {
            return true;
        }

        // The ultimate check: A box is deadlocked if it cannot move vertically AND cannot move horizontally.
        int r = pushedBox.row;
        int c = pushedBox.col;

        // Check vertical mobility
        boolean canMoveUp = (r > 0) && (levelLayout[r - 1][c] != 1) && !boxes.contains(new Point(r - 1, c));
        boolean canMoveDown = (r < rows - 1) && (levelLayout[r + 1][c] != 1) && !boxes.contains(new Point(r + 1, c));
        boolean verticallyStuck = !canMoveUp && !canMoveDown;

        // Check horizontal mobility
        boolean canMoveLeft = (c > 0) && (levelLayout[r][c - 1] != 1) && !boxes.contains(new Point(r, c - 1));
        boolean canMoveRight = (c < cols - 1) && (levelLayout[r][c + 1] != 1) && !boxes.contains(new Point(r, c + 1));
        boolean horizontallyStuck = !canMoveLeft && !canMoveRight;

        // If it's stuck in both directions, it's a deadlock.
        return verticallyStuck && horizontallyStuck;
    }


    public List<KeyCode> solve(int[][] initialMap, LongConsumer progressUpdater) {
        Point playerPos = findPlayer(initialMap);
        TreeSet<Point> initialBoxes = findBoxes(initialMap);

        GameState initialState = new GameState(playerPos, initialBoxes, null, new ArrayList<>(), 0);
        initialState.h = calculateHeuristic(initialBoxes);

        PriorityQueue<GameState> openSet = new PriorityQueue<>(Comparator.comparingInt(GameState::getF));
        Set<GameState> closedSet = new HashSet<>();
        openSet.add(initialState);

        while (!openSet.isEmpty()) {
            GameState currentState = openSet.poll();

            if (closedSet.size() % 1000 == 0) progressUpdater.accept((long) closedSet.size());
            if (isWinState(currentState.boxes)) return reconstructPath(currentState);
            if (!closedSet.add(currentState)) continue;

            for (Point box : currentState.boxes) {
                for (KeyCode move : new KeyCode[]{KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT}) {
                    int dRow = 0, dCol = 0;
                    if (move == KeyCode.UP) dRow = -1; else if (move == KeyCode.DOWN) dRow = 1;
                    else if (move == KeyCode.LEFT) dCol = -1; else if (move == KeyCode.RIGHT) dCol = 1;

                    Point newBoxPos = new Point(box.row + dRow, box.col + dCol);
                    Point playerPushPos = new Point(box.row - dRow, box.col - dCol);

                    if (!isValid(newBoxPos.row, newBoxPos.col) || levelLayout[newBoxPos.row][newBoxPos.col] == 1 || currentState.boxes.contains(newBoxPos)) {
                        continue;
                    }

                    List<KeyCode> playerPath = getPlayerPath(currentState.playerPosition, playerPushPos, currentState.boxes);
                    if (playerPath != null) {
                        TreeSet<Point> nextBoxes = new TreeSet<>(currentState.boxes);
                        nextBoxes.remove(box);
                        nextBoxes.add(newBoxPos);

                        if (isDeadlocked(newBoxPos, nextBoxes)) {
                            continue;
                        }

                        List<KeyCode> fullPathToNext = new ArrayList<>(playerPath);
                        fullPathToNext.add(move);

                        GameState nextState = new GameState(box, nextBoxes, currentState, fullPathToNext, currentState.g + 1);
                        nextState.h = calculateHeuristic(nextBoxes);
                        if (!closedSet.contains(nextState)) {
                            openSet.add(nextState);
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<KeyCode> getPlayerPath(Point start, Point end, Set<Point> boxes) {
        if (!isValid(end.row, end.col) || levelLayout[end.row][end.col] == 1) return null;
        if (start.equals(end)) return new ArrayList<>();

        Queue<List<Point>> queue = new LinkedList<>();
        queue.add(Collections.singletonList(start));
        Set<Point> visited = new HashSet<>();
        visited.add(start);

        while (!queue.isEmpty()) {
            List<Point> path = queue.poll();
            Point current = path.get(path.size() - 1);
            if (current.equals(end)) return convertPathToKeyCodes(path);

            for (int[] dir : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                Point next = new Point(current.row + dir[0], current.col + dir[1]);
                if (isValid(next.row, next.col) && levelLayout[next.row][next.col] != 1 && !boxes.contains(next) && !visited.contains(next)) {
                    visited.add(next);
                    List<Point> newPath = new ArrayList<>(path);
                    newPath.add(next);
                    queue.add(newPath);
                }
            }
        }
        return null;
    }

    private List<KeyCode> convertPathToKeyCodes(List<Point> path) {
        List<KeyCode> moves = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            Point from = path.get(i);
            Point to = path.get(i + 1);
            if (to.row < from.row) moves.add(KeyCode.UP);
            else if (to.row > from.row) moves.add(KeyCode.DOWN);
            else if (to.col < from.col) moves.add(KeyCode.LEFT);
            else if (to.col > from.col) moves.add(KeyCode.RIGHT);
        }
        return moves;
    }

    private List<KeyCode> reconstructPath(GameState finalState) {
        LinkedList<KeyCode> fullPath = new LinkedList<>();
        GameState current = finalState;
        while (current != null && current.parent != null) {
            fullPath.addAll(0, current.pathToThisState);
            current = current.parent;
        }
        return fullPath;
    }

    private int calculateHeuristic(TreeSet<Point> boxes) {
        int totalDistance = 0;
        List<Point> unmatchedGoals = new ArrayList<>(goals);
        for (Point box : boxes) {
            int minDistance = Integer.MAX_VALUE;
            Point bestGoal = null;
            for (Point goal : unmatchedGoals) {
                int dist = Math.abs(box.row - goal.row) + Math.abs(box.col - goal.col);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestGoal = goal;
                }
            }
            if (bestGoal != null) {
                totalDistance += minDistance;
                unmatchedGoals.remove(bestGoal);
            }
        }
        return totalDistance;
    }

    private boolean isWinState(Set<Point> boxes) {
        return new HashSet<>(boxes).equals(new HashSet<>(goals));
    }

    private Point findPlayer(int[][] map) {
        for (int i = 0; i < map.length; i++) for (int j = 0; j < map[i].length; j++) if (map[i][j] == 2) return new Point(i, j);
        return null;
    }

    private TreeSet<Point> findBoxes(int[][] map) {
        TreeSet<Point> boxes = new TreeSet<>();
        for (int i = 0; i < map.length; i++) for (int j = 0; j < map[i].length; j++) if (map[i][j] == 3) boxes.add(new Point(i, j));
        return boxes;
    }

    private boolean isValid(int row, int col) {
        return row >= 0 && row < this.rows && col >= 0 && col < this.cols;
    }
}
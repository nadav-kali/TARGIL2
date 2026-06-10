import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MazeSolver {

    public static List<Point> findShortestPath(int startY, int startX, int rows, int cols, boolean[][] mazeMatrix) {
        boolean[][] visited = new boolean[rows][cols];
        Point[][] parentMap = new Point[rows][cols];

        Queue<Point> queue = new LinkedList<>();
        Point start = new Point(startX, startY);
        queue.add(start);
        visited[startY][startX] = true;

        int[] dRow = {-1, 1, 0, 0};
        int[] dCol = {0, 0, -1, 1};

        boolean found = false;
        Point endPoint = null;

        while (!queue.isEmpty()) {
            Point curr = queue.poll();

            // הגעה מדויקת לנקודת הסיום (width-1, height-1)
            if (curr.y == rows - 1 && curr.x == cols - 1) {
                found = true;
                endPoint = curr;
                break;
            }

            for (int i = 0; i < 4; i++) {
                int nextRow = curr.y + dRow[i];
                int nextCol = curr.x + dCol[i];

                if (nextRow >= 0 && nextRow < rows && nextCol >= 0 && nextCol < cols) {
                    if (mazeMatrix[nextRow][nextCol] && !visited[nextRow][nextCol]) {
                        visited[nextRow][nextCol] = true;
                        parentMap[nextRow][nextCol] = curr;
                        queue.add(new Point(nextCol, nextRow));
                    }
                }
            }
        }

        if (!found) return null;

        List<Point> actualPath = new ArrayList<>();
        Point curr = endPoint;
        while (curr != null) {
            actualPath.add(0, curr);
            curr = parentMap[curr.y][curr.x];
        }
        return actualPath;
    }
}
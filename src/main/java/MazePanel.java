import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MazePanel extends JPanel {
    private boolean[][] mazeMatrix = null;
    private List<Point> animatedPath = null;

    // הגדרות עיצוב מהשרת
    private Color wallCellColor = Color.BLACK;
    private Color pathColor = Color.GREEN;
    private boolean drawGrid = false;
    private Color gridColor = Color.LIGHT_GRAY;

    public MazePanel() {
        setBackground(Color.DARK_GRAY);
        setPreferredSize(new Dimension(480, 480));
    }

    public void setAnimatedPath(List<Point> animatedPath) {
        this.animatedPath = animatedPath;
    }

    public void setConfig(Color wall, Color path, boolean drawG, Color grid) {
        this.wallCellColor = wall;
        this.pathColor = path;
        this.drawGrid = drawG;
        this.gridColor = grid;
    }

    public void setMazeData(boolean[][] matrix) {
        this.mazeMatrix = matrix;
    }

    public void clearPanel() {
        this.mazeMatrix = null;
        repaint();
    }

    public void updatePanelSize() {
        this.setPreferredSize(new Dimension(480, 480));
        this.revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (mazeMatrix == null) return;

        Graphics2D g2d = (Graphics2D) g.create();
        int rows = mazeMatrix.length;
        int cols = mazeMatrix[0].length;

        double cellW = (double) getWidth() / cols;
        double cellH = (double) getHeight() / rows;

        // ציור המבוך - קירות ומעברים ידנית לפי הדרישה!
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (mazeMatrix[y][x]) {
                    g2d.setColor(Color.WHITE); // מעבר - תמיד לבן
                } else {
                    g2d.setColor(wallCellColor); // קיר - צבע מהשרת
                }
                g2d.fillRect((int)(x * cellW), (int)(y * cellH), (int)Math.ceil(cellW), (int)Math.ceil(cellH));
            }
        }

        // ציור רשת אם נדרש
        if (drawGrid) {
            g2d.setColor(gridColor);
            for (int y = 0; y <= rows; y++) {
                g2d.drawLine(0, (int)(y * cellH), getWidth(), (int)(y * cellH));
            }
            for (int x = 0; x <= cols; x++) {
                g2d.drawLine((int)(x * cellW), 0, (int)(x * cellW), getHeight());
            }
        }

        // ציור פתרון האנימציה (משבצת אחר משבצת)
        if (animatedPath != null && !animatedPath.isEmpty()) {
            g2d.setColor(pathColor);
            for (Point p : animatedPath) {
                // מצייר משבצת מלאה עבור כל צעד בפתרון
                g2d.fillRect((int)(p.x * cellW), (int)(p.y * cellH), (int)Math.ceil(cellW), (int)Math.ceil(cellH));
            }
        }

        g2d.dispose();
    }
}
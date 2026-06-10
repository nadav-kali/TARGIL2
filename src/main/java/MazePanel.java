import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class MazePanel extends JPanel {

    private BufferedImage originalMazeImage = null;
    private boolean[][] mazeMatrix = null;
    private List<Point> animatedPath = new ArrayList<>();
    private Color pathColor = Color.GREEN;

    public MazePanel() {
        setBackground(Color.DARK_GRAY);
        updatePanelSize();
    }

    public void updatePanelSize() {
        Dimension size = new Dimension(480, 480);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        revalidate();
    }

    public void setMazeData(BufferedImage originalMazeImage, boolean[][] mazeMatrix) {
        this.originalMazeImage = originalMazeImage;
        this.mazeMatrix = mazeMatrix;
    }

    public void setAnimatedPath(List<Point> animatedPath) {
        this.animatedPath = animatedPath;
    }

    public void setPathColor(Color pathColor) {
        this.pathColor = pathColor;
    }

    public void clearPanel() {
        this.originalMazeImage = null;
        this.mazeMatrix = null;
        this.animatedPath.clear();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (originalMazeImage == null) return;

        // 1. ציור המבוך המקורי בגודל הקבוע והנקי שלו
        g.drawImage(originalMazeImage, 0, 0, 480, 480, null);

        // 2. ציור מסלול הפתרון תוך נעילה למרכז השביל
        if (!animatedPath.isEmpty() && mazeMatrix != null) {
            g.setColor(new Color(pathColor.getRed(), pathColor.getGreen(), pathColor.getBlue(), 180));

            int cellSize = 16; // הגודל המדויק של כל משבצת מבוך (480 חלקי 30)

            // חישוב הרזולוציה האמיתית של התמונה מהשרת כדי לדעת כמה פיקסלים יש בכל בלוק
            double imgCellWidth = (double) originalMazeImage.getWidth() / 30;
            double imgCellHeight = (double) originalMazeImage.getHeight() / 30;

            for (Point p : animatedPath) {
                // המרה של פיקסל ה-BFS חזרה לאינדקס המשבצת הלוגי (0 עד 29)
                int cellX = (int) (p.x / imgCellWidth);
                int cellY = (int) (p.y / imgCellHeight);

                // חישוב מיקום הציור המושלם על המסך לפי הבלוקים של ה-16 פיקסלים
                int drawX = cellX * cellSize;
                int drawY = cellY * cellSize;

                // ציור הריבוע שממלא את השביל הלבן בדיוק מתמטי
                g.fillRect(drawX, drawY, cellSize, cellSize);
            }
        }
    }
}
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MazeApp extends JFrame {

    // אזורי ממשק משתמש (UI)
    private JLabel lblWallColor, lblPathColor, lblDrawGrid, lblGridColor, lblAnimDelay;
    private JTextField txtWidth, txtHeight;
    private JButton btnRefreshConfig, btnGetMaze, btnCheckSolution;
    private MazePanel mazePanel;
    private JScrollPane scrollPane;

    // נתוני קונפיגורציה מהשרת
    private Color wallCellColor = Color.BLACK;
    private Color pathColor = Color.GREEN;
    private boolean drawGrid = false;
    private Color gridColor = Color.LIGHT_GRAY;
    private int animationDelayMs = 50;

    // נתוני המבוך
    private int mazeWidth = 30;
    private int mazeHeight = 30;
    private boolean[][] mazeMatrix; // true = מעבר (לבן), false = קיר
    private BufferedImage originalMazeImage = null; // שמירת התמונה המקורית מהשרת

    // ניהול האנימציה והפתרון
    private List<Point> solutionPath = new ArrayList<>();
    private List<Point> animatedPath = new ArrayList<>();
    private Timer animationTimer;
    private boolean isAnimating = false;

    public MazeApp() {
        super("מערכת יצירת מבוך ויזואלי מ-API");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // מחק או שים בהערה את השורה הבאה שמגדירה גודל קשיח ענקי:
        // setSize(950, 750);

        setLayout(new BorderLayout(10, 10));

        initControlPanel();

        mazePanel = new MazePanel();

        // עטיפת ה-mazePanel בתוך JPanel מרכזי כדי למנוע מתיחה של ה-BorderLayout
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerWrapper.setBackground(Color.DARK_GRAY);
        centerWrapper.add(mazePanel);

        scrollPane = new JScrollPane(centerWrapper);
        // נבטל את הגבולות של ה-scroll pane כדי שלא יתפסו פיקסלים מיותרים
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(scrollPane, BorderLayout.CENTER);

        refreshRenderConfig();

        // פקודה קריטית: מתאימה את גודל החלון בדיוק לרכיבים שבתוכו (המבוך והתפריטים)
        pack();
        setLocationRelativeTo(null);
    }

    private void initControlPanel() {
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("הגדרות ציור ומימדים מהשרת"));

        JPanel configDisplayPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        lblWallColor = new JLabel("צבע קיר: --");
        lblPathColor = new JLabel("צבע נתיב: --");
        lblDrawGrid = new JLabel("הצג רשת: --");
        lblGridColor = new JLabel("צבע רשת: --");
        lblAnimDelay = new JLabel("זמן אנימציה: --");

        configDisplayPanel.add(lblWallColor);
        configDisplayPanel.add(new JLabel(" | "));
        configDisplayPanel.add(lblPathColor);
        configDisplayPanel.add(new JLabel(" | "));
        configDisplayPanel.add(lblDrawGrid);
        configDisplayPanel.add(new JLabel(" | "));
        configDisplayPanel.add(gridColor != null ? lblGridColor : new JLabel());
        configDisplayPanel.add(new JLabel(" | "));
        configDisplayPanel.add(lblAnimDelay);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        btnRefreshConfig = new JButton("Refresh Config");
        btnRefreshConfig.addActionListener(e -> refreshRenderConfig());

        txtWidth = new JTextField("30", 5);
        txtHeight = new JTextField("30", 5);

        btnGetMaze = new JButton("GET MAZE");
        btnGetMaze.addActionListener(e -> fetchMazeImage());

        actionPanel.add(btnRefreshConfig);
        actionPanel.add(new JLabel("  |  רוחב (width):"));
        actionPanel.add(txtWidth);
        actionPanel.add(new JLabel("גובה (height):"));
        actionPanel.add(txtHeight);
        actionPanel.add(btnGetMaze);

        topPanel.add(configDisplayPanel);
        topPanel.add(actionPanel);
        add(topPanel, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnCheckSolution = new JButton("Check Solution");
        btnCheckSolution.setEnabled(false);
        btnCheckSolution.setFont(new Font("Arial", Font.BOLD, 14));
        btnCheckSolution.addActionListener(e -> checkAndAnimateSolution());
        bottomPanel.add(btnCheckSolution);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void refreshRenderConfig() {
        SwingUtilities.invokeLater(() -> {
            try {
                String urlStr = "https://backend-qcf9.onrender.com/fm1/get-render-config";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject json = new JSONObject(response.toString());

                    String wallHex = json.getString("wallCellColor");
                    String pathHex = json.getString("pathColor");
                    drawGrid = json.getBoolean("drawGrid");
                    String gridHex = json.has("gridColor") ? json.getString("gridColor") : "#000000";
                    animationDelayMs = json.getInt("animationDelayMs");

                    wallCellColor = Color.decode(wallHex);
                    pathColor = Color.decode(pathHex);
                    gridColor = Color.decode(gridHex);

                    lblWallColor.setText("צבע קיר: " + wallHex);
                    lblPathColor.setText("צבע נתיב: " + pathHex);
                    lblDrawGrid.setText("הצג רשת: " + (drawGrid ? "כן" : "לא"));
                    lblGridColor.setText("צבע רשת: " + gridHex);
                    lblAnimDelay.setText("זמן אנימציה: " + animationDelayMs + "ms");

                    mazePanel.repaint();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void fetchMazeImage() {
        mazeWidth = parseInputSize(txtWidth.getText());
        mazeHeight = parseInputSize(txtHeight.getText());

        txtWidth.setText(String.valueOf(mazeWidth));
        txtHeight.setText(String.valueOf(mazeHeight));

        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        isAnimating = false;
        solutionPath.clear();
        animatedPath.clear();
        btnCheckSolution.setEnabled(false);

        new Thread(() -> {
            try {
                String mazeUrlStr = String.format("https://backend-qcf9.onrender.com/fm1/get-maze-image?width=%d&height=%d", mazeWidth, mazeHeight);
                URL url = new URL(mazeUrlStr);

                BufferedImage img = ImageIO.read(url);
                if (img != null) {
                    originalMazeImage = img;
                    int imgWidth = img.getWidth();
                    int imgHeight = img.getHeight();

                    mazeMatrix = new boolean[imgHeight][imgWidth];

                    for (int y = 0; y < imgHeight; y++) {
                        for (int x = 0; x < imgWidth; x++) {
                            int rgb = img.getRGB(x, y);
                            int red = (rgb >> 16) & 0xFF;
                            int green = (rgb >> 8) & 0xFF;
                            int blue = rgb & 0xFF;

                            if (red == 255 && green == 255 && blue == 255) {
                                mazeMatrix[y][x] = true;
                            } else {
                                mazeMatrix[y][x] = false;
                            }
                        }
                    }

                    SwingUtilities.invokeLater(() -> {
                        mazePanel.updatePanelSize();
                        btnCheckSolution.setEnabled(true);
                        mazePanel.repaint();
                        pack();
                    });

                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "לא ניתן היה לקרוא את התמונה מהשרת."));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "שגיאה בטעינת המבוך: " + ex.getMessage()));
            }
        }).start();
    }

    private int parseInputSize(String text) {
        try {
            int val = Integer.parseInt(text.trim());
            if (val < 5 || val > 100) {
                return 30;
            }
            return val;
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    private void checkAndAnimateSolution() {
        if (mazeMatrix == null || isAnimating) return;

        int rows = mazeMatrix.length;
        int cols = mazeMatrix[0].length;

        int startX = 0, startY = 0;
        while (startX < cols && !mazeMatrix[0][startX]) startX++;
        if (startX == cols) startX = 0;

        if (!mazeMatrix[startY][startX]) {
            JOptionPane.showMessageDialog(this, "נקודת ההתחלה של המבוך חסומה בתמונה!");
            return;
        }

        List<Point> path = findShortestPath(startY, startX, rows, cols);

        if (path == null || path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No solution found");
            return;
        }

        solutionPath = path;
        animatedPath.clear();
        isAnimating = true;
        btnCheckSolution.setEnabled(false);
        btnRefreshConfig.setEnabled(false);
        btnGetMaze.setEnabled(false);

        // שליטה על קצב ההתקדמות בהתאם לגודל הקובץ כדי שהאנימציה תזרום חלק
        int skipRatio = Math.max(1, solutionPath.size() / 150);

        animationTimer = new Timer(animationDelayMs, new ActionListener() {
            private int currentIndex = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentIndex < solutionPath.size()) {
                    for (int i = 0; i < skipRatio && currentIndex < solutionPath.size(); i++) {
                        animatedPath.add(solutionPath.get(currentIndex));
                        currentIndex++;
                    }
                    mazePanel.repaint();
                } else {
                    animationTimer.stop();
                    isAnimating = false;
                    btnCheckSolution.setEnabled(true);
                    btnRefreshConfig.setEnabled(true);
                    btnGetMaze.setEnabled(true);
                }
            }
        });

        animationTimer.start();
    }

    private List<Point> findShortestPath(int startY, int startX, int rows, int cols) {
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

            if (curr.y == rows - 1 || curr.x == cols - 1) {
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

    private class MazePanel extends JPanel {

        public MazePanel() {
            setBackground(Color.DARK_GRAY);
            Dimension size = new Dimension(480, 480);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
        }

        public void updatePanelSize() {
            Dimension size = new Dimension(480, 480);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            revalidate();
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MazeApp app = new MazeApp();
            app.setVisible(true);
        });
    }
}
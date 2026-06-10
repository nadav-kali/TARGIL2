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
import java.util.List;

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
    private boolean[][] mazeMatrix;

    // ניהול האנימציה והפתרון
    private List<Point> solutionPath = new ArrayList<>();
    private List<Point> animatedPath = new ArrayList<>();
    private Timer animationTimer;
    private boolean isAnimating = false;

    public MazeApp() {
        super("מערכת יצירת מבוך ויזואלי מ-API");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        initControlPanel();

        mazePanel = new MazePanel();
        mazePanel.setAnimatedPath(animatedPath); // קישור הרשימה הדינמית לפאנל

        // עטיפת ה-mazePanel בתוך JPanel מרכזי
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerWrapper.setBackground(Color.DARK_GRAY);
        centerWrapper.add(mazePanel);

        scrollPane = new JScrollPane(centerWrapper);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        refreshRenderConfig();

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
        configDisplayPanel.add(lblGridColor);
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

                    // עדכון הגדרת הצבע ישירות לרכיב הפאנל
                    mazePanel.setPathColor(pathColor);
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
        mazePanel.clearPanel();
        btnCheckSolution.setEnabled(false);

        new Thread(() -> {
            try {
                String mazeUrlStr = String.format("https://backend-qcf9.onrender.com/fm1/get-maze-image?width=%d&height=%d", mazeWidth, mazeHeight);
                URL url = new URL(mazeUrlStr);

                BufferedImage img = ImageIO.read(url);
                if (img != null) {
                    int imgWidth = img.getWidth();
                    int imgHeight = img.getHeight();

                    mazeMatrix = new boolean[imgHeight][imgWidth];

                    for (int y = 0; y < imgHeight; y++) {
                        for (int x = 0; x < imgWidth; x++) {
                            int rgb = img.getRGB(x, y);
                            int red = (rgb >> 16) & 0xFF;
                            int green = (rgb >> 8) & 0xFF;
                            int blue = rgb & 0xFF;

                            // לבן = מעבר (true), אחרת = קיר (false)
                            mazeMatrix[y][x] = (red == 255 && green == 255 && blue == 255);
                        }
                    }

                    SwingUtilities.invokeLater(() -> {
                        mazePanel.setMazeData(img, mazeMatrix);
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
            if (val < 5 || val > 100) return 30;
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

        // שימוש במחלקה המופרדת לפתרון המבוך
        List<Point> path = MazeSolver.findShortestPath(startY, startX, rows, cols, mazeMatrix);

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MazeApp app = new MazeApp();
            app.setVisible(true);
        });
    }
}
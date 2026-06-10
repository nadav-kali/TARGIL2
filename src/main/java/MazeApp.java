import org.json.JSONObject;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MazeApp extends JFrame {

    private JLabel lblWallColor, lblPathColor, lblDrawGrid, lblGridColor, lblAnimDelay;
    private JTextField txtWidth, txtHeight;
    private JButton btnRefreshConfig, btnGetMaze, btnCheckSolution;
    private MazePanel mazePanel;
    private JScrollPane scrollPane;

    private Color wallCellColor = Color.BLACK;
    private Color pathColor = Color.GREEN;
    private boolean drawGrid = false;
    private Color gridColor = Color.LIGHT_GRAY;
    private int animationDelayMs = 50;

    private int mazeWidth = 30;
    private int mazeHeight = 30;
    private boolean[][] mazeMatrix;

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
        mazePanel.setAnimatedPath(animatedPath);

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
        actionPanel.add(new JLabel("  |  width:"));
        actionPanel.add(txtWidth);
        actionPanel.add(new JLabel("height:"));
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

                    // מעדכן את הפאנל בנתוני העיצוב מהשרת
                    mazePanel.setConfig(wallCellColor, pathColor, drawGrid, gridColor);
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

                    mazeMatrix = new boolean[mazeHeight][mazeWidth];
                    double cellW = (double) imgWidth / mazeWidth;
                    double cellH = (double) imgHeight / mazeHeight;

                    for (int y = 0; y < mazeHeight; y++) {
                        for (int x = 0; x < mazeWidth; x++) {
                            int pixelX = (int) (x * cellW + cellW / 2);
                            int pixelY = (int) (y * cellH + cellH / 2);

                            pixelX = Math.min(pixelX, imgWidth - 1);
                            pixelY = Math.min(pixelY, imgHeight - 1);

                            int rgb = img.getRGB(pixelX, pixelY);
                            int red = (rgb >> 16) & 0xFF;
                            int green = (rgb >> 8) & 0xFF;
                            int blue = rgb & 0xFF;

                            // לבן = true (מעבר), כל צבע אחר = false (קיר)
                            mazeMatrix[y][x] = (red > 200 && green > 200 && blue > 200);
                        }
                    }

                    SwingUtilities.invokeLater(() -> {
                        mazePanel.setMazeData(mazeMatrix);
                        mazePanel.updatePanelSize();
                        btnCheckSolution.setEnabled(true);
                        mazePanel.repaint();
                        pack();
                    });
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
            if (val < 5 || val > 100) return 30; // ברירת מחדל אם הערך חורג
            return val;
        } catch (NumberFormatException e) {
            return 30; // ברירת מחדל אם הקלט אינו מספר
        }
    }

    private void checkAndAnimateSolution() {
        if (mazeMatrix == null || isAnimating) return;

        int rows = mazeMatrix.length;
        int cols = mazeMatrix[0].length;

        // בדיקה: אם ההתחלה (0,0) או הסיום (width-1, height-1) הם קירות
        if (!mazeMatrix[0][0] || !mazeMatrix[rows - 1][cols - 1]) {
            JOptionPane.showMessageDialog(this, "No solution found");
            return;
        }

        List<Point> path = MazeSolver.findShortestPath(0, 0, rows, cols, mazeMatrix);

        if (path == null || path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No solution found");
            return;
        }

        solutionPath = path;
        animatedPath.clear();
        mazePanel.repaint(); // מנקה מסלול קודם אם היה

        isAnimating = true;
        btnCheckSolution.setEnabled(false);
        btnRefreshConfig.setEnabled(false);
        btnGetMaze.setEnabled(false);

        animationTimer = new Timer(animationDelayMs, e -> {
            if (animatedPath.size() < solutionPath.size()) {
                animatedPath.add(solutionPath.get(animatedPath.size()));
                mazePanel.repaint();
            } else {
                animationTimer.stop();
                isAnimating = false;
                btnCheckSolution.setEnabled(true);
                btnRefreshConfig.setEnabled(true);
                btnGetMaze.setEnabled(true);
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
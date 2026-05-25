import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.Timer;

public class Tetris extends JFrame {
    private JLabel statusbar;

    public Tetris() {
        initUI();
    }

    private void initUI() {
        statusbar = new JLabel(" 0");
        add(statusbar, BorderLayout.SOUTH);

        Board board = new Board(this);
        add(board);
        board.start();

        setTitle("Java Tetris");
        setSize(400, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
    }

    public JLabel getStatusBar() {
        return statusbar;
    }

    public static void main(String[] args) {
        LoginWindow login = new LoginWindow();
        login.setVisible(true);
    }
}

class LoginWindow extends JFrame {
    
    private JTextField userField;
    private JPasswordField passField;

    public LoginWindow() {
        setTitle("Login");
        
        setSize(400, 250); 
        
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        setLayout(new GridLayout(3, 1)); 

        Font labelFont = new Font("Arial", Font.BOLD, 24);
        Font inputFont = new Font("Arial", Font.PLAIN, 18);

        JPanel panelUser = new JPanel();
        
        JLabel lblAccount = new JLabel("Account: ");
        lblAccount.setFont(labelFont);
        panelUser.add(lblAccount);
        
        userField = new JTextField(10); 
        userField.setFont(inputFont);
        panelUser.add(userField);
        add(panelUser);

        JPanel panelPass = new JPanel();
        
        JLabel lblPass = new JLabel("Password: ");
        lblPass.setFont(labelFont); 
        panelPass.add(lblPass);
        
        passField = new JPasswordField(10); 
        passField.setFont(inputFont);
        panelPass.add(passField);
        add(panelPass);

        JPanel panelBtn = new JPanel();
        JButton btnLogin = new JButton("Login");
        btnLogin.setFont(new Font("Arial", Font.BOLD, 20)); // 按鈕字也稍微加大
        panelBtn.add(btnLogin);
        add(panelBtn);

        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkLogin();
            }
        });
        this.getRootPane().setDefaultButton(btnLogin);
    }
    private void checkLogin() {
        String user = userField.getText();
        String pass = new String(passField.getPassword()); 

        if ("user".equals(user) && "123".equals(pass)) {
            this.dispose(); 
            Tetris game = new Tetris(); 
            game.setVisible(true); 
        } else {
            JOptionPane.showMessageDialog(this, "Login Failed! Try again.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

enum Tetrominoe { 
    NoShape, ZShape, SShape, LineShape, 
    TShape, SquareShape, LShape, JShape 
}

class Board extends JPanel implements ActionListener {

    private final int BOARD_WIDTH = 10;
    private final int BOARD_HEIGHT = 22;
    private Timer timer;
    private boolean isFallingFinished = false;
    private boolean isStarted = false;
    private boolean isPaused = false;
    private int numLinesRemoved = 0;
    private int curX = 0;
    private int curY = 0;
    private Tetrominoe curPiece;
    private Tetrominoe[] board;
    
    private JButton restartBtn; 
    private JButton resumeBtn; 

    private final int[][][] coordsTable = new int[][][] {
        { { 0, 0 },   { 0, 0 },   { 0, 0 },   { 0, 0 } },
        { { 0, -1 },  { 0, 0 },   { -1, 0 },  { -1, 1 } },
        { { 0, -1 },  { 0, 0 },   { 1, 0 },   { 1, 1 } },
        { { 0, -1 },  { 0, 0 },   { 0, 1 },   { 0, 2 } },
        { { -1, 0 },  { 0, 0 },   { 1, 0 },   { 0, 1 } },
        { { 0, 0 },   { 1, 0 },   { 0, 1 },   { 1, 1 } },
        { { -1, -1 }, { 0, -1 },  { 0, 0 },   { 0, 1 } },
        { { 1, -1 },  { 0, -1 },  { 0, 0 },   { 0, 1 } }
    };

    private Tetris parent;
    private JLabel statusbar;
    private int curPieceCoords[][] = new int[4][2];

    public Board(Tetris parent) {
        initBoard(parent);
    }

    private void initBoard(Tetris parent) {
        setFocusable(true);
        setLayout(new GridBagLayout()); 

        curPiece = Tetrominoe.NoShape;
        timer = new Timer(400, this);
        this.parent = parent;
        statusbar = parent.getStatusBar();
        board = new Tetrominoe[BOARD_WIDTH * BOARD_HEIGHT];
        
        restartBtn = new JButton("Restart Game");
        restartBtn.setFont(new Font("Arial", Font.BOLD, 30)); 
        restartBtn.setPreferredSize(new Dimension(250, 60)); 
        restartBtn.setFocusable(false);
        restartBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                start();
            }
        });
        restartBtn.setVisible(false);
        add(restartBtn);

        resumeBtn = new JButton("Start"); 
        resumeBtn.setFont(new Font("Arial", Font.BOLD, 30)); 
        resumeBtn.setPreferredSize(new Dimension(250, 60)); 
        resumeBtn.setFocusable(false); 
        resumeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pause(); 
            }
        });
        resumeBtn.setVisible(false); 
        add(resumeBtn);

        clearBoard();
        addKeyListener(new TAdapter());
    }

    public void start() {
        isStarted = true;
        isPaused = false;
        numLinesRemoved = 0;
        clearBoard();
        
        restartBtn.setVisible(false);
        resumeBtn.setVisible(false);
        statusbar.setText("0");
        
        newPiece();
        timer.start();
    }

    private void pause() {
        if (!isStarted) return;

        isPaused = !isPaused; 
        
        if (isPaused) {
            timer.stop();
            statusbar.setText("paused");
            resumeBtn.setVisible(true); 
        } else {
            timer.start();
            statusbar.setText(String.valueOf(numLinesRemoved));
            resumeBtn.setVisible(false); 
        }
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }

    private void doDrawing(Graphics g) {
        Dimension size = getSize();
        int boardTop = (int) size.getHeight() - BOARD_HEIGHT * squareHeight();

        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                Tetrominoe shape = shapeAt(j, BOARD_HEIGHT - i - 1);
                if (shape != Tetrominoe.NoShape) {
                    drawSquare(g, j * squareWidth(), boardTop + i * squareHeight(), shape);
                }
            }
        }

        if (curPiece != Tetrominoe.NoShape) {
            for (int i = 0; i < 4; i++) {
                int x = curX + curPieceCoords[i][0];
                int y = curY - curPieceCoords[i][1];
                drawSquare(g, x * squareWidth(), boardTop + (BOARD_HEIGHT - y - 1) * squareHeight(), curPiece);
            }
        }
    }

    private void dropDown() {
        int newY = curY;
        while (newY > 0) {
            if (!tryMove(curPiece, curX, newY - 1)) break;
            newY--;
        }
        pieceDropped();
    }

    private void oneLineDown() {
        if (!tryMove(curPiece, curX, curY - 1)) {
            pieceDropped();
        }
    }

    private void clearBoard() {
        for (int i = 0; i < BOARD_HEIGHT * BOARD_WIDTH; i++) {
            board[i] = Tetrominoe.NoShape;
        }
    }

    private void pieceDropped() {
        for (int i = 0; i < 4; i++) {
            int x = curX + curPieceCoords[i][0];
            int y = curY - curPieceCoords[i][1];
            board[(y * BOARD_WIDTH) + x] = curPiece;
        }

        removeFullLines();

        if (!isFallingFinished) {
            newPiece();
        }
    }

    private void newPiece() {
        setRandomShape();
        curX = BOARD_WIDTH / 2 + 1;
        curY = BOARD_HEIGHT - 1 + minY();

        if (!tryMove(curPiece, curX, curY)) {
            curPiece = Tetrominoe.NoShape;
            timer.stop();
            isStarted = false;
            statusbar.setText("Game Over");
            
            restartBtn.setVisible(true);
            resumeBtn.setVisible(false);
        }
    }

    private void setRandomShape() {
        int x = (int) (Math.random() * 7 + 1);
        Tetrominoe[] values = Tetrominoe.values();
        setShape(values[x]);
    }

    private void setShape(Tetrominoe shape) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 2; j++) {
                curPieceCoords[i][j] = coordsTable[shape.ordinal()][i][j];
            }
        }
        curPiece = shape;
    }

    private boolean tryMove(Tetrominoe newPiece, int newX, int newY) {
        for (int i = 0; i < 4; i++) {
            int x = newX + curPieceCoords[i][0];
            int y = newY - curPieceCoords[i][1];
            if (x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT) return false;
            if (shapeAt(x, y) != Tetrominoe.NoShape) return false;
        }

        curPiece = newPiece;
        curX = newX;
        curY = newY;
        repaint();
        return true;
    }

    private void removeFullLines() {
        int numFullLines = 0;

        for (int i = BOARD_HEIGHT - 1; i >= 0; i--) {
            boolean lineIsFull = true;
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (shapeAt(j, i) == Tetrominoe.NoShape) {
                    lineIsFull = false;
                    break;
                }
            }

            if (lineIsFull) {
                numFullLines++;
                for (int k = i; k < BOARD_HEIGHT - 1; k++) {
                    for (int j = 0; j < BOARD_WIDTH; j++) {
                        board[(k * BOARD_WIDTH) + j] = shapeAt(j, k + 1);
                    }
                }
            }
        }

        if (numFullLines > 0) {
            numLinesRemoved += numFullLines;
            statusbar.setText(String.valueOf(numLinesRemoved));
            isFallingFinished = true;
            curPiece = Tetrominoe.NoShape;
            repaint();
        }
    }

    private void drawSquare(Graphics g, int x, int y, Tetrominoe shape) {
        Color colors[] = { 
            new Color(0, 0, 0), new Color(204, 102, 102), 
            new Color(102, 204, 102), new Color(102, 102, 204), 
            new Color(204, 204, 102), new Color(204, 102, 204), 
            new Color(102, 204, 204), new Color(218, 170, 0) 
        };

        Color color = colors[shape.ordinal()];
        g.setColor(color);
        g.fillRect(x + 1, y + 1, squareWidth() - 2, squareHeight() - 2);

        g.setColor(color.brighter());
        g.drawLine(x, y + squareHeight() - 1, x, y);
        g.drawLine(x, y, x + squareWidth() - 1, y);

        g.setColor(color.darker());
        g.drawLine(x + 1, y + squareHeight() - 1, x + squareWidth() - 1, y + squareHeight() - 1);
        g.drawLine(x + squareWidth() - 1, y + squareHeight() - 1, x + squareWidth() - 1, y + 1);
    }
    
    private void rotateLeft() {
        if (curPiece == Tetrominoe.SquareShape) return;

        int[][] result = new int[4][2];
        for (int i = 0; i < 4; ++i) {
            result[i][0] = curPieceCoords[i][1];
            result[i][1] = -curPieceCoords[i][0];
        }
        
        int[][] temp = new int[4][2];
        for(int i=0; i<4; i++) {
             temp[i] = curPieceCoords[i];
             curPieceCoords[i] = result[i];
        }
        
        if (!tryMove(curPiece, curX, curY)) {
             for(int i=0; i<4; i++) {
                 curPieceCoords[i] = temp[i];
             }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isFallingFinished) {
            isFallingFinished = false;
            newPiece();
        } else {
            oneLineDown();
        }
    }

    private int squareWidth() { return (int) getSize().getWidth() / BOARD_WIDTH; }
    private int squareHeight() { return (int) getSize().getHeight() / BOARD_HEIGHT; }
    private Tetrominoe shapeAt(int x, int y) { return board[(y * BOARD_WIDTH) + x]; }
    private int minY() {
        int m = curPieceCoords[0][1];
        for (int i=0; i<4; i++) m = Math.min(m, curPieceCoords[i][1]);
        return m;
    }

    class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (!isStarted || curPiece == Tetrominoe.NoShape) return;

            int keycode = e.getKeyCode();
            
            if (keycode == KeyEvent.VK_P) {
                pause();
                return;
            }
            if (isPaused) return;

            switch (keycode) {
                case KeyEvent.VK_LEFT: tryMove(curPiece, curX - 1, curY); break;
                case KeyEvent.VK_RIGHT: tryMove(curPiece, curX + 1, curY); break;
                case KeyEvent.VK_DOWN: dropDown(); break; 
                case KeyEvent.VK_UP: rotateLeft(); break;
                case KeyEvent.VK_SPACE: dropDown(); break;
                case KeyEvent.VK_D: oneLineDown(); break;
            }
        }
    }
}

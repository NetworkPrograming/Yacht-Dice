import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;

public class GameGUI extends JFrame {

    private Image backgroundImage = new ImageIcon(getClass().getResource("/resources/background.jpg")).getImage();
    private Image scoreBoard = new ImageIcon(getClass().getResource("/resources/scoreBoard.jpeg")).getImage();

    JButton giveUpButton = new JButton("Give Up");
    JTextArea textArea = new JTextArea(10, 20);
    private JTextArea chatDisplay; // 채팅 메시지 표시 영역
    private JTextField t_input;  // 메시지 입력 필드
    private JButton b_send;    // 메시지 보내기 버튼
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public GameGUI(ObjectInputStream in,ObjectOutputStream out) {
        super("Yacht-Dice");
        this.in = in;
        this.out = out;

        //setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1600, 770));
        pack();
        setResizable(false);
        setLocationRelativeTo(null);

        buildGUI();
        AddDiceComponents();
        //setVisible(true);
    }

    private void buildGUI() {
        // 레이어드 페인(JLayeredPane) 생성
        JLayeredPane layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER); // JLayeredPane을 프레임에 추가

        // 배경 패널을 레이어 0에 추가
        JPanel backgroundPanel = BackgroundPanel();
        backgroundPanel.setBounds(0, 0, getWidth(), getHeight());
        layeredPane.add(backgroundPanel, Integer.valueOf(0));  // 레이어 0에 배경 추가

        // 점수 패널을 레이어 1에 추가
        JPanel scorePanel = ScorePanel();
        scorePanel.setBounds(0, 0, 360, 750); // 점수판 크기 설정
        layeredPane.add(scorePanel, Integer.valueOf(1)); // 레이어 1에 점수판 추가

        // 채팅 패널을 레이어 2에 추가
        JPanel chatPanel = ChatPanel();
        chatPanel.setBounds(1150, 0, 320, 760); // 채팅판 크기 설정
        layeredPane.add(chatPanel, Integer.valueOf(100)); // 레이어 2에 채팅판 추가

        // 주사위 패널을 레이어 200에 추가
        JPanel dicePanel = AddDiceComponents();
        dicePanel.setBounds(330, 0, 830,750);
        layeredPane.add(dicePanel, Integer.valueOf(200));
    }



    private JPanel BackgroundPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 330, 0, 1200, 750, this);
            }
        };
        panel.setLayout(null);

        return panel;
    }

    private JPanel ScorePanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(scoreBoard, 0, 0, 360, 750, this);
            }
        };
        panel.setLayout(null);
        return panel;
    }


    private JPanel ChatPanel() { // 채팅 패널
        JPanel panel = new JPanel();
        //panel.setOpaque(false); // 패널을 투명하게 설정
        panel.setLayout(null);
        // JTextArea 추가하여 메시지 표시
        textArea.setEditable(false);
        textArea.setBounds(10, 10, 300, 650);
        textArea.setFont(new Font("Arial", Font.PLAIN, 14)); // 폰트 설정
        panel.add(textArea);

        // 메시지 입력 필드
        t_input = new JTextField();
        t_input.setBounds(10,700,250,40);
        panel.add(t_input);

        // 메시지 보내기 버튼
        b_send = new JButton("Send");
        b_send.setBounds(260,700,50,40);
        panel.add(b_send);
        
        // 엔터와 버튼 클릭으로 메시지 전송
        b_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = t_input.getText();
                textArea.append(message + "\n");
                t_input.setText("");
            }
        });

        t_input.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = t_input.getText();
                textArea.append(message + "\n");
                t_input.setText("");
            }
        });

        setVisible(true);

        return panel;
    }

    private void sendMessageToServer() {
        String message = t_input.getText();
        if (message != null && !message.isEmpty()) {
            try {
                out.writeObject(message + "\n");
                out.flush();
                t_input.setText("");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void receiveMessageFromServer() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                textArea.append(message + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private JPanel AddDiceComponents() {
        JPanel jPanel = new JPanel();
        jPanel.setOpaque(false); // 패널을 투명하게 설정
        jPanel.setLayout(null);

        // 주사위 그리기
        JButton diceOneImg = createDiceButton("resources/dice1.png", 200, 200); // 1번 주사위
        jPanel.add(diceOneImg);

        JButton diceTwoImg = createDiceButton("resources/dice2.png", 295, 200); // 2번 주사위
        jPanel.add(diceTwoImg);

        JButton diceThreeImg = createDiceButton("resources/dice3.png", 385, 200); // 3번 주사위
        jPanel.add(diceThreeImg);

        JButton diceFourImg = createDiceButton("resources/dice4.png", 475, 200); // 4번 주사위
        jPanel.add(diceFourImg);

        JButton diceFiveImg = createDiceButton("resources/dice5.png", 565, 200); // 5번 주사위
        jPanel.add(diceFiveImg);

        // 굴리기 버튼
        Random rand = new Random();
        JButton b_roll = new JButton("Roll!");
        b_roll.setBounds(360, 300, 100, 100);
        b_roll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                b_roll.setEnabled(false);

                playSound("/resources/dice_roll.wav");

                // 3초동안 굴리기
                long startTime = System.currentTimeMillis();
                Thread rollThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long endTime = System.currentTimeMillis();
                        try {
                            while ((endTime - startTime) / 1000F < 2) {
                                // roll dice
                                int diceOne = rand.nextInt(1, 7);
                                int diceTwo = rand.nextInt(1, 7);
                                int diceThree = rand.nextInt(1, 7);
                                int diceFour = rand.nextInt(1, 7);
                                int diceFive = rand.nextInt(1, 7);

                                // 주사위 이미지 업데이트 (이동된 주사위는 굴리지 않음)
                                if (!(boolean) diceOneImg.getClientProperty("isMoved")) {
                                    ImgService.updateImage(diceOneImg, "resources/dice" + diceOne + ".png");
                                }
                                if (!(boolean) diceTwoImg.getClientProperty("isMoved")) {
                                    ImgService.updateImage(diceTwoImg, "resources/dice" + diceTwo + ".png");
                                }
                                if (!(boolean) diceThreeImg.getClientProperty("isMoved")) {
                                    ImgService.updateImage(diceThreeImg, "resources/dice" + diceThree + ".png");
                                }
                                if (!(boolean) diceFourImg.getClientProperty("isMoved")) {
                                    ImgService.updateImage(diceFourImg, "resources/dice" + diceFour + ".png");
                                }
                                if (!(boolean) diceFiveImg.getClientProperty("isMoved")) {
                                    ImgService.updateImage(diceFiveImg, "resources/dice" + diceFive + ".png");
                                }

                                repaint();
                                revalidate();

                                // sleep thread
                                Thread.sleep(60);

                                endTime = System.currentTimeMillis();
                            }

                            b_roll.setEnabled(true);
                        } catch (InterruptedException e) {
                            System.out.println("Threading Error: " + e);
                        }
                    }
                });
                rollThread.start();
            }
        });
        jPanel.add(b_roll);

        return jPanel;
    }

    private JButton createDiceButton(String imagePath, int x, int y) {
        JButton b_dice = ImgService.loadImage(imagePath);
        b_dice.setBounds(x, y, 75, 75);

        // "isMoved" 상태를 버튼에 저장 (기본값은 false)
        b_dice.putClientProperty("isMoved", false);

        // 클릭 시 특정 위치로 이동하도록 이벤트 추가
        b_dice.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean isMoved = (boolean) b_dice.getClientProperty("isMoved");

                if (!isMoved) {
                    // 이동
                    b_dice.setLocation(x, y - 130); // 예시: 아래쪽으로 130px 이동
                    b_dice.putClientProperty("isMoved", true);  // 이동 상태 업데이트
                } else {
                    // 원래 위치로 돌아감
                    b_dice.setLocation(x, y);
                    b_dice.putClientProperty("isMoved", false); // 이동 상태 업데이트
                }
                repaint();
            }
        });

        return b_dice;
    }

    private void playSound(String soundFile) {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(getClass().getResource(soundFile));
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error playing sound: " + e.getMessage());
        }
    }
}


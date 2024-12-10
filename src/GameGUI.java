import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Arrays;
import java.util.Random;

public class GameGUI extends JFrame {

    private YachtDiceClient client;

    private Image backgroundImage = new ImageIcon(getClass().getResource("/resources/background.jpg")).getImage();
    private Image scoreBoard = new ImageIcon(getClass().getResource("/resources/scoreBoard.jpeg")).getImage();
    private Image sendButton = new ImageIcon(getClass().getResource("/resources/send_button.png")).getImage();

    JButton giveUpButton = new JButton("Give Up");
    JButton b_roll;
    JTextArea textArea = new JTextArea(10, 20);
    private JTextArea chatDisplay; // 채팅 메시지 표시 영역
    private JTextField t_input;  // 메시지 입력 필드
    private JButton b_send;    // 메시지 보내기 버튼
    private ObjectOutputStream out;
    private ObjectInputStream in;

    //
    final static int DICE_SIZE = 5;
    final static int MAX_DICE_NUM = 6;

    int[] dices;
    int[] counts;

    Random rand;


    public GameGUI(YachtDiceClient client, ObjectInputStream in,ObjectOutputStream out) {
        super("Yacht-Dice");
        this.in = in;
        this.out = out;

        dices = new int[DICE_SIZE];
        counts = new int[MAX_DICE_NUM];

        rand = new Random();

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.setVisible(true);
                dispose();
            }
        });

        setPreferredSize(new Dimension(1600, 720));
        pack();
        setResizable(false);
        setLocationRelativeTo(null);

        buildGUI();
        AddDiceComponents();
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
        scorePanel.setBounds(0, 0, 300, 750); // 점수판 크기 설정
        layeredPane.add(scorePanel, Integer.valueOf(1)); // 레이어 1에 점수판 추가

        // 채팅 패널을 레이어 2에 추가
        JPanel chatPanel = ChatPanel();
        chatPanel.setBounds(1000, 0, 320, 760); // 채팅판 크기 설정
        layeredPane.add(chatPanel, Integer.valueOf(300)); // 레이어 2에 채팅판 추가

        // 주사위 패널을 레이어 200에 추가
        JPanel dicePanel = AddDiceComponents();
        dicePanel.setBounds(330, 0, 700, 750);
        layeredPane.add(dicePanel, Integer.valueOf(10));
    }

    private JPanel BackgroundPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 300, 0, 1200, 750, this);
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
               g.drawImage(scoreBoard, 0, 0, 300, 750, this);
            }
        };
        panel.setLayout(null);
        return panel;

//        JPanel panel = new JPanel();
//        GridBagConstraints gbc = new GridBagConstraints();
//        gbc.fill = GridBagConstraints.BOTH;
//        panel.setLayout(new GridLayout(16, 5));
//
//        String[] rowLabels = {
//                " ", "에이스","듀얼","트리플","쿼드","펜타","헥사","보너스점수","상단항목 합계",
//                "초이스","포커","풀하우스","스몰스트레이트","라지스트레이트","요트","합계"
//        };
//
//        // 버튼 생성 및 추가
//        JButton[][] buttons = new JButton[16][5];
//        // 15x4 점수판 + 1행/1열 레이블 생성
//        for (int row = 0; row < 16; row++) { // 15 + 1
//            for (int col = 0; col < 5; col++) {// 4 + 1
//                gbc.gridx = col;
//                gbc.gridy = row;
//
//                if (row == 0 && col == 0) {
//                    // 좌측 상단 모서리 (빈칸)
//                    panel.add(new JLabel(""));
//                } else if (row == 0) {
//                    // 1행: 열 헤더
//                    JLabel header = new JLabel("User " + col, SwingConstants.CENTER); // 후에 참가자 아이디로 변경
//                    header.setFont(new Font("Arial", Font.BOLD, 14));
//                    panel.add(header);
//                } else if (col == 0) {
//                    // 1열: 행 헤더
//                    JLabel header = new JLabel(rowLabels[row], SwingConstants.CENTER); // rowLabels 배열 사용
//                    header.setFont(new Font("Arial", Font.BOLD, 14));
//                    if(row == 7 || row == 8 || row == 15){
//                        header.setForeground(Color.WHITE);
//                        header.setBackground(Color.LIGHT_GRAY);
//                        header.setOpaque(true);
//                    }
//                    panel.add(header, gbc);
////                } else if (row == 7){
////                    JButton button = new JButton("0/63");
////                    button.setForeground(Color.WHITE);
////                    button.setBackground(Color.LIGHT_GRAY);
////                    button.setOpaque(true);
//                } else {
//                    // 나머지 버튼 생성
//                    JButton button = new JButton(""); // 초기값 "0"
//                    button.setFont(new Font("Arial", Font.BOLD, 16));
//                    panel.add(button);
//
//                    // 버튼 클릭 이벤트 처리
//                    int finalRow = row - 1; // 실제 데이터는 0부터 시작
//                    int finalCol = col - 1;
//                    button.addActionListener(new ActionListener() {
//                        @Override
//                        public void actionPerformed(ActionEvent e) {
//                            // 버튼 클릭 시 동작 (값 증가)
//                        }
//
//                    });
//                }
//            }
//            // 줄 삽입 (가로선)
////            if (row==7) {
////                gbc.gridy = row;
////                gbc.gridx = 0;
////                gbc.gridwidth = 5; // 전체 열에 적용
////                JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
////                panel.add(separator, gbc);
////            }
//        }
//        return panel;
    }

    private JPanel ChatPanel() { // 채팅 패널
        JPanel panel = new JPanel();
        panel.setOpaque(false); // 패널을 투명하게 설정
        panel.setLayout(null);
        // JTextArea 추가하여 메시지 표시
        textArea.setEditable(false);
        //textArea.setOpaque(false);
        Border border = BorderFactory.createLineBorder(Color.darkGray, 2);
        textArea.setBorder(border);
        textArea.setBounds(15, 10, 250, 625);
        textArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14)); // 폰트 설정
        panel.add(textArea);

        // 메시지 입력 필드
        t_input = new JTextField();
        //t_input.setOpaque(false);
        t_input.setBorder(border);
        t_input.setBounds(15, 640, 220, 30);
        panel.add(t_input);

        // 메시지 보내기 버튼
        b_send = new JButton();
        String sendImage = "/resources/send_button.png";

        // 이미지 로드 및 크기 조정
        ImageIcon originalIcon = new ImageIcon(getClass().getResource(sendImage));
        Image scaledImage = originalIcon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        // 버튼에 스케일된 이미지 설정
        b_send.setIcon(scaledIcon);

        // 버튼 크기 및 위치 설정
        b_send.setBounds(235, 640, 30, 30);
        b_send.setContentAreaFilled(false); // 배경 제거


        // 패널에 추가
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
        JButton[] b_dices = new JButton[DICE_SIZE];
        jPanel.setOpaque(false); // 패널을 투명하게 설정
        jPanel.setLayout(null);
        Arrays.fill(dices, 1);

        b_dices[0] = createDiceButton("resources/dice" + 1 + ".png" , 110,210);
        b_dices[1] = createDiceButton("resources/dice" + 2 + ".png" , 200,210);
        b_dices[2] = createDiceButton("resources/dice" + 3 + ".png" , 295,210);
        b_dices[3] = createDiceButton("resources/dice" + 4 + ".png" , 390,210);
        b_dices[4] = createDiceButton("resources/dice" + 5 + ".png" , 490,210);

        for(int i=0;i<DICE_SIZE;i++) { //주사위 버튼 만들기
            jPanel.add(b_dices[i]);
        }

        // 굴리기 버튼
        b_roll = new JButton();
        String rollImage = "/resources/roll_button.png";

        // 이미지 로드 및 크기 조정
        ImageIcon originalIcon = new ImageIcon(getClass().getResource(rollImage));
        Image scaledImage = originalIcon.getImage().getScaledInstance(200, 110, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        b_roll.setIcon(scaledIcon);
        b_roll.setContentAreaFilled(false); // 배경 제거
        b_roll.setBorderPainted(false);
        b_roll.setBounds(230, 400, 200, 110);
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
                                // 최종 값 결정 (2초 후)

                                for(int i=0;i<DICE_SIZE;i++) { // 킵하지 않은게 있으면 랜덤값 생성 후 이미지 그림
                                    if(!(boolean)b_dices[i].getClientProperty("isSaved")){
                                        dices[i] = rand.nextInt(6) + 1;
                                        ImgService.updateImage(b_dices[i],"resources/dice" + dices[i] + ".png");
                                    }
                                }

                                jPanel.repaint();
                                jPanel.revalidate();

                                Thread.sleep(60);

                                endTime = System.currentTimeMillis();
                            }
                            b_roll.setEnabled(true);

                            for(int i=0;i<DICE_SIZE;i++) { // isSaved 값 출력
                                System.out.println(dices[i] + " " + b_dices[i].getClientProperty("isSaved"));
                            }

                            System.out.println(available()); // 족보 출력

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


    private String available() {
        countDices();
        String results = "[";

        boolean hasResults = false;

        // 초이스 선택 X일 시 초이스도 포함
        if (checkNo1()) {
            results += "포커 ";
            hasResults = true;
        }
        if (checkNo2()) {
            results += "풀하우스 ";
            hasResults = true;
        }
        if (checkNo3()) {
            results += "스몰 스트레이트 ";
            hasResults = true;
        }
        if (checkNo4()) {
            results += "라지 스트레이트 ";
            hasResults = true;
        }
        if (checkNo5()) {
            results += "요트 ";
            hasResults = true;
        }

        // 어디에도 해당 안되면 -> 1~6 숫자 각각 더하기 칸
        if (!hasResults) {
            results += "해당 없음";
        }

        results += "]";
        return results;
    }

    private void countDices() { //굴린 주사위 수 저장
        Arrays.fill(counts, 0); //count 0으로 초기화

        for (int i = 0; i < DICE_SIZE; i++) {
            counts[dices[i] - 1]++;
        }
    }

    private boolean checkNo1() { //포커 (같은 숫자 4개)
        boolean check = false;
        for (int count : counts) {
            if (count >= 4)
                check = true;
        }
        return check;
    }

    private boolean checkNo2() { //풀하우스 체크
        boolean check2 = false;
        boolean check3 = false;
        for (int count : counts) {
            if (count == 2) {
                check2 = true;
            } else if (count == 3) {
                check3 = true;
            }
        }
        return check2 && check3;
    }

    private boolean checkNo3() { //스몰스트레이트
        boolean check = false;
        if (counts[2] >= 1 && counts[3] >= 1) { //스몰스트레이트 시 3,4 는 무조건 포함
            if (counts[0] >= 1 && counts[1] >= 1 || counts[1] >= 1 && counts[4] >= 1 || counts[4] >= 1 && counts[5] >= 1) { // 1234 , 2345, 3456
                check = true;
            }
        }
        return check;
    }

    private boolean checkNo4() { //라지스트레이트
        boolean check = false;
        if (counts[1] >= 1 && counts[2] >= 1 && counts[3] >= 1 && counts[4] >= 1) { //라지스트레이트 시 2,3,4,5는 무조건 포함
            if (counts[0] >= 1 || counts[5] >= 1) {
                check = true;
            }
        }
        return check;
    }

    private boolean checkNo5() { //요트 (같은 숫자 5개)
        boolean check = false;
        for (int count : counts) {
            if (count == 5)
                check = true;
        }
        return check;
    }

    private JButton createDiceButton(String imagePath, int x, int y) {
        JButton b_dice = ImgService.loadImage(imagePath);
        b_dice.setBounds(x, y, 75, 75);

        // "isSaved" 상태를 버튼에 저장 (기본값은 false)
        b_dice.putClientProperty("isSaved", false);

        // 클릭 시 특정 위치로 이동하도록 이벤트 추가
        b_dice.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean isSaved = (boolean) b_dice.getClientProperty("isSaved");

                if (!isSaved) { //false일 때
                    // 이동
                    b_dice.setLocation(b_dice.getX(), b_dice.getY() - 130); // 예시: 아래쪽으로 130px 이동
                    b_dice.putClientProperty("isSaved", true);  // 이동 상태 업데이트
                    repaint();
                }
                if (isSaved) {
                    // 원래 위치로 돌아감
                    b_dice.setLocation(b_dice.getX(), b_dice.getY() + 130);
                    b_dice.putClientProperty("isSaved", false); // 이동 상태 업데이트
                    repaint();
                }
                repaint();
            }
        });

        return b_dice;
    }

//    private JButton getDiceButton(int index) {
//        switch (index) {
//            case 0: return diceOneImg;
//            case 1: return diceTwoImg;
//            case 2: return diceThreeImg;
//            case 3: return diceFourImg;
//            case 4: return diceFiveImg;
//            default: return null;  // 잘못된 인덱스일 경우 null 반환
//        }
//    }

//    private void printDice() {
//        System.out.println(dices[0] + " " + diceOneImg.getClientProperty("isSaved"));
//
//        System.out.println(dices[1]+ " " + diceTwoImg.getClientProperty("isSaved"));
//
//        System.out.println(dices[2]+ " " + diceThreeImg.getClientProperty("isSaved"));
//
//        System.out.println(dices[3]+ " " + diceFourImg.getClientProperty("isSaved"));
//
//        System.out.println(dices[4]+ " " + diceFiveImg.getClientProperty("isSaved"));
//
//
//        //System.out.println(Arrays.toString(dices));
//    }

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

/////테스트용main////////

    public static void main(String[] args) {
        GameGUI g = new GameGUI(null,null,null);
    }
}


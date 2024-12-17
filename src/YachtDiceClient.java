import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class YachtDiceClient extends JFrame {
    private String serverAddress;
    private int serverPort;
    private JTextField t_userID;
    private JTextField t_IP;

    private JButton b_connect;
    private JButton b_disconnect;
    private JButton b_exit;

    private JButton b_createRoom;

    private Socket socket;

    private String uid;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    private Thread receiveThread = null;

    private JPanel roomPanel;

    String roomTitle;
    String password;

    private JTextArea t_display;
    private JTextField t_input;
    private JButton b_send;

    String roomTitle_copy = "";

    // 위에는 클라이언트에 필요한 변수
    /// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 밑에는 게임창에 필요한 변수

    private String[] User_Array_client = {null, null, null, null};
    private String[] User_Array_client_copy = {null, null, null, null};

    private Image backgroundImage = new ImageIcon(getClass().getResource("/resources/background.jpg")).getImage();
    private Image scoreBoard = new ImageIcon(getClass().getResource("/resources/scoreBoard.jpg")).getImage();
    private Image sendButton = new ImageIcon(getClass().getResource("/resources/send_button.png")).getImage();

    JTextArea textArea = new JTextArea(10, 20);
    private JTextArea chatDisplay; // 채팅 메시지 표시 영역
    private JTextField t_input_GAME;  // 메시지 입력 필드
    private JButton b_send_GAME;    // 메시지 보내기 버튼
    JButton b_giveup;

    boolean allTrue;

    final static int DICE_SIZE = 5;
    final static int MAX_DICE_NUM = 6;

    int checkRoll = 0; // 굴린 횟수 세기

    int[] dices;
    int[] counts;
    int turnIndex;
    int turn; // 전체 턴

    boolean[][] isScored; //점수등록여부

    int[] userList; //유저리스트
    int[][] userScore; //유저 점수
    int userNum;
    String[] userName;
    int turnUser;

    int[] totalScore; //총점
    int[] middleScore; //에이스 ~ 헥사까지 점수
    int bonusScore = 35;

    JLabel[][] scoreLabels;
    JLabel[] userLabels;
    JLabel l_turn;
    JLabel l_nowTurn; // 현재 누구 턴인지 알려주는 라벨

    Random rand;
    int user1 = 0;
    private String currentTurn;

    int check_game_start = 0; // 게임 시작을 하기 위한 인원 체크용 변수
    JPanel dice_panel = new JPanel();
    JButton[] b_dices = new JButton[DICE_SIZE];
    // 굴리기 버튼 생성
    JButton b_roll;
    JButton b_game_start;

    int userCount;
    int check_giveup = 0;
    private int count = 0;
    private int count_giveup = 0;
    private int totalScore_copy[] = new int[4];

    JFrame newFrame;

    public void GameGUI() {

        dices = new int[DICE_SIZE];
        counts = new int[MAX_DICE_NUM];
        userList = new int[4];
        userScore = new int[4][15];
        isScored = new boolean[4][15];
        totalScore = new int[4];
        middleScore = new int[4];
        turnUser = 0;

        userCount = 0;
        turn = 1;
        count = 0;

        rand = new Random();

        userName = new String[4];

        System.setProperty("sun.java2d.uiScale", "1.0"); // DPI 스케일링 고정

        newFrame = new JFrame(roomTitle_copy);
        newFrame.setSize(1450, 770);
        newFrame.setLocationRelativeTo(null); // 화면 중앙에 위치
        //newFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 닫기 버튼 클릭 시 창만 닫힘
        newFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // 창이 닫힐 때 아무 동작도 하지 않도록 설정
        newFrame.setResizable(false); // 창 크기 조절 비활성화

        // 레이어드 페인(JLayeredPane) 생성
        JLayeredPane layeredPane = new JLayeredPane();
        newFrame.add(layeredPane); // JLayeredPane을 프레임에 추가

        // 배경 패널을 레이어 0에 추가
        JPanel backgroundPanel = BackgroundPanel();
        backgroundPanel.setBounds(50, 0, newFrame.getWidth(), newFrame.getHeight());
        layeredPane.add(backgroundPanel, Integer.valueOf(0)); // 레이어 0에 배경 추가

        // 점수 패널을 레이어 1에 추가
        JPanel scorePanel = ScorePanel();
        scorePanel.setBounds(0, 0, 400, 750); // 점수판 크기 설정
        layeredPane.add(scorePanel, Integer.valueOf(1)); // 레이어 1에 점수판 추가

        // 채팅 패널을 레이어 2에 추가
        JPanel chatPanel = ChatPanel();
        chatPanel.setBounds(1120, 0, 320, 760); // 채팅판 크기 설정
        layeredPane.add(chatPanel, Integer.valueOf(2)); // 레이어 2에 채팅판 추가

        // 주사위 패널을 레이어 10에 추가
        JPanel dicePanel = AddDiceComponents();
        dicePanel.setBounds(430, 0, 750, 750);
        layeredPane.add(dicePanel, Integer.valueOf(10)); // 레이어 10에 주사위 패널 추가

        newFrame.setVisible(true); // 창을 보이도록 설정
        newFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                YachtDiceClient.this.setVisible(true); // 기존 창 다시 보이기
                quit_room(roomTitle_copy);
                printDisplay(roomTitle_copy + " 게임 방에서 퇴장하였습니다.");
                checkRoll = 0;
            }
        });
        send_message_room_first(roomTitle_copy, uid + "님이 입장했습니다.");
    }

    private JPanel BackgroundPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 350, 0, 1200, 750, this);
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
                g.drawImage(scoreBoard, 0, 0, 400, 750, this);
            }
        };
        panel.add(SetScorePanel());
        panel.setLayout(null);
        return panel;
    }

    private JPanel SetScorePanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setBounds(0, 0, 400, 750);
        panel.setLayout(null);
        int user, i;

        //턴 라벨 관리
        l_turn = new JLabel("1");
        l_turn.setBounds(38, 70, 100, 100);
        l_turn.setForeground(Color.YELLOW);
        l_turn.setFont(new Font("맑은 고딕", Font.BOLD, 25));
        l_turn.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(l_turn);


        // 유저별 점수칸 관리 (4명의 유저, 각 15개의 점수칸)
        scoreLabels = new JLabel[4][15];
        userLabels = new JLabel[4];

        for (user = 0; user < 4; user++) {
            userLabels[user] = new JLabel();
            userLabels[user].setBounds(141 + (user * 65), 50, 60, 60);
            userLabels[user].setFont(new Font("맑은 고딕", Font.BOLD, 13));
            userLabels[user].setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(userLabels[user]);
        }

        for (user = 0; user < 4; user++) { //라벨기본세팅
            for (i = 0; i < 15; i++) {
                scoreLabels[user][i] = new JLabel();
                scoreLabels[user][i].setFont(new Font("맑은 고딕", Font.BOLD, 17));


                //클릭 이벤트 리스너 추가
                addLabelClickListener(scoreLabels[user][i], user, i);
            }
        }

        // 각 유저의 점수칸 레이블 초기화
        for (user = 0; user < 4; user++) { //// 에이스 ~ 헥사
            for (i = 0; i < 6; i++) {
                //scoreLabels[user][i] = new JLabel();
                // 점수칸 위치 설정
                scoreLabels[user][i].setBounds(140 + (user * 65), 137 + (i * 37), 65, 38);
                scoreLabels[user][i].setHorizontalAlignment(SwingConstants.CENTER);
                panel.add(scoreLabels[user][i]);

                //클릭 이벤트 리스너 추가
                addLabelClickListener(scoreLabels[user][i], user, i);
            }
        }

        for (user = 0; user < 4; user++) { //// 윗부분 점수 합
            //scoreLabels[user][6] = new JLabel();
            scoreLabels[user][6].setBounds(133 + (user * 65), 357, 65, 35);
            scoreLabels[user][6].setHorizontalAlignment(SwingConstants.CENTER);
            scoreLabels[user][6].setForeground(Color.WHITE);
            panel.add(scoreLabels[user][6]);

            //클릭 이벤트 리스너 추가
            addLabelClickListener(scoreLabels[user][6], user, 6);
        }

        for (user = 0; user < 4; user++) { //0 또는 35
            //scoreLabels[user][7] = new JLabel();
            scoreLabels[user][7].setBounds(140 + (user * 65), 390, 65, 35);
            scoreLabels[user][7].setHorizontalAlignment(SwingConstants.CENTER);
            scoreLabels[user][7].setForeground(Color.WHITE);
            panel.add(scoreLabels[user][7]);

            //클릭 이벤트 리스너 추가
            addLabelClickListener(scoreLabels[user][7], user, 7);
        }

        for (user = 0; user < 4; user++) { //초이스
            //scoreLabels[user][8] = new JLabel();
            scoreLabels[user][8].setBounds(140 + (user * 65), 448, 65, 35);
            scoreLabels[user][8].setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(scoreLabels[user][8]);

            //클릭 이벤트 리스너 추가
            addLabelClickListener(scoreLabels[user][8], user, 8);
        }

        for (user = 0; user < 4; user++) { ////포커 ~ 요트
            for (i = 9; i < 14; i++) {
                //scoreLabels[user][i] = new JLabel();
                // 점수칸 위치 설정
                scoreLabels[user][i].setBounds(140 + (user * 65), 493 + ((i - 9) * 36), 65, 35);
                scoreLabels[user][i].setHorizontalAlignment(SwingConstants.CENTER);
                panel.add(scoreLabels[user][i]);

                //클릭 이벤트 리스너 추가
                addLabelClickListener(scoreLabels[user][i], user, i);
            }
        }

        for (user = 0; user < 4; user++) { //총점
            //scoreLabels[user][14] = new JLabel();
            scoreLabels[user][14].setBounds(140 + (user * 65), 680, 65, 56);
            scoreLabels[user][14].setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(scoreLabels[user][14]);

        }

        return panel;
    }

    private void addLabelClickListener(JLabel label, int user, int scoreIndex) {
        // user와 scoreIndex를 final로 선언
        final int finalUser = user;
        final int finalScoreIndex = scoreIndex;

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                if (!isScored[finalUser][finalScoreIndex]) { //한번 클릭된 점수는 클릭 불가
                    // 클릭된 라벨을 가져오기
                    String text = label.getText().trim(); // 공백 제거

                    System.out.println("Label Text: '" + text + "'");
                    //printDisplay2("현재 턴 : " + currentTurn);

                    try {
                        // 텍스트가 비어 있거나 숫자가 아니면 예외 발생
                        if (!Objects.equals(currentTurn, uid)) {
                            printDisplay2("현재 " + uid + " 님의 턴이 아니므로 클릭할 수 없습니다.");
                            throw new NumberFormatException("Text is empty.");
                        }

                        // 정수로 변환
                        int score = Integer.parseInt(text);

                        //에이스 ~ 헥사 클릭 시
                        if (finalScoreIndex == 0 | finalScoreIndex == 1 | finalScoreIndex == 2 | finalScoreIndex == 3 || finalScoreIndex == 4 | finalScoreIndex == 5) {
                            middleScore[finalUser] += score; //중간점수 배열에 점수 추가
                            scoreLabels[finalUser][6].setText(String.valueOf(middleScore[finalUser])); //라벨에 표시
                            isScored[finalUser][6] = true; // 저장 상태 업데이트
                        }

                        //에이스 ~ 헥사 전부 값 들어있을 때
                        if (middleScore[finalUser] >= 63) {
                            userScore[finalUser][7] = bonusScore; // 35점 추가
                            totalScore[finalUser] += bonusScore;
                            totalScore_copy[finalUser] += bonusScore;
                            isScored[finalUser][7] = true; // 저장 상태 업데이트
                            scoreLabels[finalUser][7].setText(String.valueOf(bonusScore)); //라벨에도 반영
                            scoreLabels[finalUser][14].setText(String.valueOf(totalScore[finalUser]));
                        } else {
                            userScore[finalUser][7] = 0;
                            isScored[finalUser][7] = true; // 저장 상태 업데이트
                            scoreLabels[finalUser][7].setText("0"); //라벨에도 반영
                        }

                        // 점수 저장
                        userScore[finalUser][finalScoreIndex] = score;
                        int bonus = userScore[finalUser][7];
                        isScored[finalUser][finalScoreIndex] = true; // 저장 상태 업데이트

                        // 라벨 색상 변경 및 총점 업데이트
                        label.setForeground(Color.BLACK);
                        totalScore[finalUser] += userScore[finalUser][finalScoreIndex];
                        totalScore_copy[finalUser] += userScore[finalUser][finalScoreIndex];
                        scoreLabels[finalUser][14].setText(String.valueOf(totalScore[finalUser])); // 총점 표시
                        sendScoreToServer(uid, finalUser, finalScoreIndex, score, totalScore[finalUser], middleScore[finalUser], bonus);

                        for (int i = 0; i < 15; i++) {//해당 유저 점수 중 등록안된 점수 전부 초기화
                            if (!isScored[finalUser][i]) {
                                scoreLabels[user][i].setText("");
                            }
                        }


                    } catch (NumberFormatException ex) {
                    }
                }
            }
        });
    }

    private JPanel ChatPanel() { // 채팅 패널
        JPanel panel = new JPanel();
        panel.setOpaque(false); // 패널을 투명하게 설정
        panel.setLayout(null);

        // JTextArea 추가하여 메시지 표시
        textArea.setEditable(false);
        //textArea.setOpaque(false);
        Border border = BorderFactory.createLineBorder(Color.darkGray, 2);
        //textArea.setBorder(border);
        textArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14)); // 폰트 설정
        textArea.setText("");

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBounds(15, 10, 300, 660); // 스크롤 패널 위치 및 크기 설정
        scrollPane.setOpaque(false); // 스크롤 패널을 투명하게 설정

        panel.add(scrollPane);


        // 메시지 입력 필드
        t_input_GAME = new JTextField();
        //t_input.setOpaque(false);
        t_input_GAME.setBorder(border);
        t_input_GAME.setBounds(14, 670, 275, 27);
        panel.add(t_input_GAME);

        // 메시지 보내기 버튼
        b_send_GAME = new JButton();
        b_send_GAME.setBorder(border);

        // 이미지 로드 및 크기 조정
        ImageIcon originalIcon = new ImageIcon(getClass().getResource("/resources/send_button.png"));
        Image scaledImage = originalIcon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        // 버튼에 스케일된 이미지 설정
        b_send_GAME.setIcon(scaledIcon);

        // 버튼 크기 및 위치 설정
        b_send_GAME.setBounds(285, 670, 29, 27);
        b_send_GAME.setContentAreaFilled(false); // 배경 제거

        // 패널에 추가
        panel.add(b_send_GAME);

        // 엔터와 버튼 클릭으로 메시지 전송
        b_send_GAME.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                send_message_room(roomTitle_copy);
            }
        });

        t_input_GAME.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                send_message_room(roomTitle_copy);
            }
        });

        return panel;
    }

    private void printDisplay2(String msg) {
        textArea.append(msg + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    private void setUser() { // 유저리스트
        for (int i = 0; i < 4; i++) {
            userLabels[i].setText(User_Array_client[i]);
        }
    }

    private JPanel AddDiceComponents() {
        dice_panel = new JPanel();
        b_dices = new JButton[DICE_SIZE];
        // 굴리기 버튼 생성
        b_roll = new JButton();
        b_game_start = new JButton();
        b_giveup = new JButton();

        dice_panel.setOpaque(false); // 패널을 투명하게 설정
        dice_panel.setLayout(null);

        //턴 라벨 관리
        l_nowTurn = new JLabel();
        l_nowTurn.setBounds(185, 0, 300, 50);
        l_nowTurn.setForeground(Color.BLACK);
        l_nowTurn.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        l_nowTurn.setHorizontalAlignment(SwingConstants.CENTER);
        dice_panel.add(l_nowTurn);


        // 주사위 초기값 설정
        Arrays.fill(dices, 1);

        // 굴리기 버튼 생성
        b_roll = createRollButton();
        b_roll.addActionListener(e -> setupRollButton(b_roll, b_dices, dice_panel)); // 버튼 액션 연결

        b_giveup = createGiveupButton();
        b_giveup.addActionListener(e -> setGiveupButton());

        b_game_start = game_start_Button();
        check_game_start = 0;
        b_game_start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (uid.equals(User_Array_client[0])) { // 플레이어 1만 게임시작 버튼을 누를 수 있음.
                    how_many_people();
                    try {
                        // 0.1초 슬립. how_many_people 함수에서 변수 처리하는 속도가 너무 느림.
                        // 슬립을 해서 변수 대입할 시간을 벌어줌
                        Thread.sleep(100);
                    } catch (InterruptedException e22) {
                        e22.printStackTrace();
                    }
                    if (check_game_start == 1) {
                        send_game_start();
                    } else {
                        JOptionPane.showMessageDialog(dice_panel, "2명 이상이 방에 입장해 있어야 합니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(dice_panel, "게임은 방장만 시작 할 수 있습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        dice_panel.add(b_game_start);
        return dice_panel;
    }

    private void UI_update(JPanel jPanel, JButton b_game_start, JButton b_roll, JButton[] b_dices, JButton b_giveup) {
        jPanel.remove(b_game_start);

        jPanel.add(b_giveup);
        jPanel.add(b_roll);

        // 주사위 버튼 생성 및 추가
        for (int i = 0; i < DICE_SIZE; i++) {
            int xPosition = 110 + (i * 90); // x 좌표 동적 설정
            b_dices[i] = createDiceButton("resources/dice" + (i + 1) + ".png", xPosition, 210, i, b_dices);
            //printDisplay2(String.valueOf(i));
            b_dices[i].putClientProperty("isSaved", false); // 초기값 설정
            jPanel.add(b_dices[i]);
            b_dices[i].setVisible(true);
        }
        turnIndex = 0;
        currentTurn = User_Array_client[0];
        for (String s : User_Array_client) {
            if (s != "") { // null이 아닌 유저만 카운트
                userCount++;
            }
        }
        jPanel.repaint();
        jPanel.revalidate();
    }

    private void how_many_people() {
        // 게임 시작
        send(new Yacht(t_userID.getText(), Yacht.MODE_HOW_MANY_PEOPLE, roomTitle_copy));
    }

    private void send_game_start() {
        // 게임 시작
        send(new Yacht(t_userID.getText(), Yacht.MODE_GAME_START, roomTitle_copy, "1"));
    }

    private JButton createDiceButton(String imagePath, int x, int y, int i, JButton[] b_dices) {
        b_dices[i] = ImgService.loadImage(imagePath);
        char dice_number = imagePath.charAt(14);
        b_dices[i].setBounds(x, y, 80, 80);
        //b_dices[i].setContentAreaFilled(false); // 배경 제거
        //b_dices[i].setBorderPainted(false);    // 테두리 제거
        b_dices[i].putClientProperty("isSaved", false); // 저장 여부 초기화

        b_dices[i].addActionListener(e -> {
            if (checkRoll != 0) {
                if (Objects.equals(currentTurn, uid)) {
                    boolean isSaved = !(boolean) b_dices[i].getClientProperty("isSaved");
                    b_dices[i].putClientProperty("isSaved", isSaved); // 상태 토글
                    if (isSaved) {
                        b_dices[i].setLocation(b_dices[i].getX(), b_dices[i].getY() - 130); // 저장 시 위치 변경
                        String temp = b_dices[i].getX() + "!=!" + b_dices[i].getY() + "!=!" + (i + 1);
                        send_move_dice(roomTitle_copy, temp);
                    } else {
                        b_dices[i].setLocation(b_dices[i].getX(), b_dices[i].getY() + 130); // 해제 시 원위치
                        String temp = b_dices[i].getX() + "!=!" + b_dices[i].getY() + "!=!" + (i + 1);
                        send_move_dice(roomTitle_copy, temp);
                    }
                }
            }
        });
        return b_dices[i];
    }

    private JButton createRollButton() {
        JButton button = new JButton();
        ImageIcon originalIcon = new ImageIcon(getClass().getResource("/resources/roll_button.png"));
        Image scaledImage = originalIcon.getImage().getScaledInstance(200, 110, Image.SCALE_SMOOTH);
        button.setIcon(new ImageIcon(scaledImage));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusable(false); //포커스테두리 없애기
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); //갖다대면 손가락모양 표시
        button.setBounds(230, 550, 200, 110);

        return button;
    }

    private JButton createGiveupButton() {
        JButton button = new JButton();
        ImageIcon originalIcon = new ImageIcon(getClass().getResource("/resources/giveup_button.png"));
        Image scaledImage = originalIcon.getImage().getScaledInstance(120, 66, Image.SCALE_SMOOTH);
        button.setIcon(new ImageIcon(scaledImage));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBounds(270, 660, 120, 66);

        return button;
    }

    private JButton game_start_Button() {
        JButton button = new JButton();
        ImageIcon originalIcon = new ImageIcon(getClass().getResource("/resources/game_start.png"));
        Image scaledImage = originalIcon.getImage().getScaledInstance(200, 110, Image.SCALE_SMOOTH);
        button.setIcon(new ImageIcon(scaledImage));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBounds(230, 550, 200, 110);

        return button;
    }

    private void setupRollButton(JButton b_roll, JButton[] b_dices, JPanel jPanel) {
        boolean allSaved = checkIfAllSaved(b_dices); // 주사위 상태 확인

        if (Objects.equals(currentTurn, uid)) {
            // 모든 주사위가 저장되지 않은 경우, 현재 턴인 유저에만 리스너 추가
            handleRollAction(b_roll, b_dices, jPanel);
        } else {
            b_roll.setEnabled(false); // 모든 주사위가 저장된 경우 버튼 비활성화
            for (int i = 0; i < DICE_SIZE; i++) {
                b_dices[i].setEnabled(false);
            }
        }
    }

    private void setGiveupButton() {
        //항복 다이얼로그 띄우기
        int choice = JOptionPane.showConfirmDialog(
                null,
                "항복하시겠습니까?",
                "항복하기",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            surrender(roomTitle_copy);
            if (uid.equals(User_Array_client[turnIndex])) {
                sendScoreToServer(uid, 10000, 10000, 10000, 10000, 10000, 10000);
            }
            try {
                // 스레드를 0.3초 동안 일시 중지
                Thread.sleep(300);
            } catch (InterruptedException e) {
                System.out.println("스레드가 중단되었습니다.");
            }
            checkRoll = 0;
            //남아있는사람 1 명일 때 게임 종료
            check_giveup = 0;
            for (int i = 0; i < 4; i++) {
                if (User_Array_client[i].length() < 1 || User_Array_client[i] == null) {
                    check_giveup++;
                }
            }
            if (check_giveup == 3) {
                sendScoreToServer(uid, 20000, 20000, 20000, 20000, 20000, 20000);
                return;
            }
        }
    }

    private void surrender(String roomtitle) {
        send(new Yacht(t_userID.getText(), Yacht.MODE_SURRENDER, roomtitle, "아무거나"));
    }

    private void handleRollAction(JButton b_roll, JButton[] b_dices, JPanel jPanel) {
        b_roll.setEnabled(false); // 굴리기 버튼 비활성화
        checkRoll++;

        // 주사위 버튼 비활성화
        for (int i = 0; i < DICE_SIZE; i++) {
            b_dices[i].setEnabled(false); // 주사위 버튼 비활성화
        }

        // 점수판 클릭 불가
        for (int user = 0; user < 4; user++) {
            for (int i = 0; i < 15; i++) {
                scoreLabels[user][i].setEnabled(false);
            }
        }

        Timer timer = new Timer(60, null); // 60ms 간격으로 주사위 변경
        long startTime = System.currentTimeMillis();

        timer.addActionListener(e -> {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000; // 초 단위 경과 시간
            if (elapsed >= 2) { // 2초 후 굴리기 종료
                timer.stop();
                finalizeRoll(b_roll, b_dices, jPanel);
                available(userNum);

            } else {
                for (int i = 0; i < DICE_SIZE; i++) {
                    if (!(boolean) b_dices[i].getClientProperty("isSaved")) {
                        dices[i] = rand.nextInt(6) + 1; // 1~6 랜덤 값
                        send_rolling_dice(roomTitle_copy, "resources/dice" + dices[i] + ".png" + i);
                    }

                }
            }
        });

        playSound("/resources/dice_roll.wav"); // 주사위 소리 재생
        timer.start();
    }

    private void send_rolling_dice(String roomTitle, String message) {
        send(new Yacht(t_userID.getText(), Yacht.MODE_SHOW_ROLLING_DICE, roomTitle, message));
    }

    private void send_move_dice(String roomTitle, String message) {
        send(new Yacht(t_userID.getText(), Yacht.MODE_MOVE_DICE, roomTitle, message));
    }

    private boolean checkIfAllSaved(JButton[] b_dices) {
        for (JButton diceButton : b_dices) {
            if (!(boolean) diceButton.getClientProperty("isSaved")) {
                return false; // 저장되지 않은 주사위가 있다면 false 반환
            }
        }
        return true; // 모든 주사위가 저장된 경우 true 반환
    }

    private void finalizeRoll(JButton b_roll, JButton[] b_dices, JPanel jPanel) {
        b_roll.setEnabled(true); // 굴리기 버튼 활성화
        boolean allSaved = true;
        for (int i = 0; i < DICE_SIZE; i++) {
            b_dices[i].setEnabled(true);
        }
        for (int i = 0; i < DICE_SIZE; i++) {
            if (!(boolean) b_dices[i].getClientProperty("isSaved")) {
                allSaved = false;
            }
        }

        if (checkRoll == 3 || allSaved) {
            b_roll.setEnabled(false); // 굴리기 버튼 비활성화
            Timer timer = new Timer(1000, e -> {
                for (int i = 0; i < DICE_SIZE; i++) {
                    if (!(boolean) b_dices[i].getClientProperty("isSaved")) {
                        b_dices[i].putClientProperty("isSaved", true); // 모든 주사위 저장
                        b_dices[i].setLocation(b_dices[i].getX(), b_dices[i].getY() - 130); // 주사위 위치 이동
                        String temp = b_dices[i].getX() + "!=!" + (b_dices[i].getY()) + "!=!" + (i + 1);
                        send_move_dice(roomTitle_copy, temp);
                    }
                    b_dices[i].setEnabled(false); // 버튼 비활성화
                }
            });
            timer.setRepeats(false); // 1회만 실행되도록 설정
            timer.start(); // 타이머 시작
        }

        for (int i = 0; i < DICE_SIZE; i++) { // isSaved 값 출력
            System.out.println(dices[i] + " " + b_dices[i].getClientProperty("isSaved"));
        }

        // UI 갱신
        SwingUtilities.invokeLater(() -> {
            jPanel.repaint();
            jPanel.revalidate();
        });
    }

    private void available(int userNum) {
        Arrays.fill(counts, 0); //counts 0으로 초기화
        for (int user = 0; user < 4; user++) { //라벨 초기화 (점수확정X인것만)
            for (int i = 0; i < scoreLabels[user].length; i++) { //라벨 초기화
                scoreLabels[user][i].setEnabled(true);
                if (!isScored[userNum][i]) {
                    scoreLabels[userNum][i].setText("");
                }
            }
        }
        countDices(userNum);
        checkScore(userNum);

    }

    private void checkScore(int userNum) {
        boolean hasResults = false;

        // 에이스 ~ 헥사까지 체크
        Map<Integer, Integer> scores = calcScore();
        for (Map.Entry<Integer, Integer> entry : scores.entrySet()) {
            int diceValue = entry.getKey();
            int score = entry.getValue();
            if (score > 0) {
                if (!isScored[userNum][diceValue - 1]) {
                    scoreLabels[userNum][diceValue - 1].setText(String.valueOf(score));
                    scoreLabels[userNum][diceValue - 1].setForeground(Color.GRAY);
                    scoreLabels[userNum][diceValue - 1].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    hasResults = true;
                }
            }
        }

        String results = "[";

        //족보 체크
        if (checkNo1() && !isScored[userNum][9]) { // 포커 체크
            scoreLabels[userNum][9].setText(String.valueOf(totalScore()));
            scoreLabels[userNum][9].setForeground(Color.GRAY);
            scoreLabels[userNum][9].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            results += "포커 ";
            hasResults = true;
        }

        if (checkNo2() && !isScored[userNum][10]) { //풀하우스 체크
            scoreLabels[userNum][10].setText(String.valueOf(totalScore()));
            scoreLabels[userNum][10].setForeground(Color.GRAY);
            scoreLabels[userNum][10].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            results += "풀하우스 ";
            hasResults = true;
        }

        if (checkNo3() && !isScored[userNum][11]) { //스몰 스트레이트 체크
            int score = 15;
            scoreLabels[userNum][11].setText(String.valueOf(score));
            scoreLabels[userNum][11].setForeground(Color.GRAY);
            scoreLabels[userNum][11].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            results += "스몰 스트레이트 ";
            hasResults = true;
        }

        if (checkNo4() && !isScored[userNum][12]) {
            int score = 30;
            scoreLabels[userNum][12].setText(String.valueOf(score));
            scoreLabels[userNum][12].setForeground(Color.GRAY);
            scoreLabels[userNum][12].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            results += "라지 스트레이트 ";
            hasResults = true;
        }

        if (checkNo5() && !isScored[userNum][13]) {
            int score = 50;
            scoreLabels[userNum][13].setText(String.valueOf(score));
            scoreLabels[userNum][13].setForeground(Color.GRAY);
            scoreLabels[userNum][13].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            results += "요트 ";
            hasResults = true;
        }

        if (!isScored[userNum][8]) {
            scoreLabels[userNum][8].setText(String.valueOf(totalScore()));
            scoreLabels[userNum][8].setForeground(Color.GRAY);
            scoreLabels[userNum][8].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            results += "초이스 ";
            hasResults = true;
        }
        if (!hasResults) { //어디에도 쓸게 없으면 빈 공간에 0을 써야함
            for (int i = 0; i < 15; i++) {
                if (!isScored[userNum][i]) { //점수 없는 공간 발견하면
                    scoreLabels[userNum][i].setText("0"); // 0점 가능
                    scoreLabels[userNum][i].setForeground(Color.GRAY);
                    scoreLabels[userNum][i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }
            results += "빈공간에 0점 쓰기";
        }
        results += "]";

        scoreLabels[userNum][14].setText(String.valueOf(totalScore[userNum]));

        System.out.println(results + checkRoll);

    }

    private void countDices(int userNum) { //굴린 주사위 수 저장
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

    private Map<Integer, Integer> calcScore() {
        Map<Integer, Integer> basicScore = new HashMap<>();
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) { // 주사위 값이 하나 이상 나온 경우
                int diceValue = i + 1; // 주사위 값 (1 ~ 6)
                int score = counts[i] * diceValue; // 점수
                basicScore.put(diceValue, score); // Map에 주사위 값, 점수 저장
            }
        }
        return basicScore;
    }

    private int totalScore() {
        int total = 0;
        total = Arrays.stream(dices).sum(); // 나온 값 전부 더함
        return total;
    }

    public void nextTurn() {
        turnIndex = (turnIndex + 1) % userCount;
        count++;
        while (User_Array_client[turnIndex].isEmpty()) {
            turnIndex++;
            turnIndex = (turnIndex + 1) % userCount;
            count++;
        }
        checkRoll = 0; // 주사위 굴린 횟수 초기화 후 UI 재설정
        b_roll.setEnabled(true); // 굴리기 버튼 활성화

        for (int i = 0; i < DICE_SIZE; i++) {
            // 주사위 저장 상태 해제
            b_dices[i].putClientProperty("isSaved", false);

            // 주사위 원래 위치로 복귀
            b_dices[i].setLocation(b_dices[i].getX(), 210);
            String temp = b_dices[i].getX() + "!=!" + 210 + "!=!" + (i + 1);

            // 버튼 활성화
            b_dices[i].setEnabled(true);
        }
        turn = count / userCount + 1;
        currentTurn = User_Array_client[turnIndex];
        if (turn == 13) { //턴 12가 끝나면
            sendScoreToServer(uid, 30000, 30000, 30000, 30000, 30000, 30000);
            return;
        }
        l_nowTurn.setText("Turn " + turn + ": " + currentTurn + "의 차례입니다");
        l_turn.setText(String.valueOf(turn));
    }

    // 클라이언트에서 점수 업데이트 요청 보내기
    public void sendScoreUpdate(int userNum, int scoreIndex, int score, DataOutputStream out) {
        try {
            String message = "UPDATE_SCORE," + userNum + "," + scoreIndex + "," + score;
            out.writeUTF(message); // 서버로 메시지 전송
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 점수 갱신 메서드
    public void sendScoreToServer(String userID, int userNum, int scoreIndex, int score, int totalScore, int middleScore, int bonus) {
        try {
            // 전송할 데이터 객체 생성
            Yacht data = new Yacht(userID, Yacht.MODE_TX_STRING_SCORE, "UserNum: " + userNum + ", ScoreIndex: " + scoreIndex + ", Score: " + score + ", TotalScore: " + totalScore + ", MiddleScore: " + middleScore + ", Bonus: " + bonus, roomTitle_copy);

            out.writeObject(data);
            out.flush();

            System.out.println("데이터가 서버로 전송되었습니다.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 점수 갱신 함수
    public void updateScore(int userNum, int scoreIndex, int score, int totalScore, int middleScore, int bonus) {
        scoreLabels[userNum][scoreIndex].setText(String.valueOf(score));
        scoreLabels[userNum][14].setText(String.valueOf(totalScore));
        scoreLabels[userNum][6].setText(String.valueOf(middleScore));
        scoreLabels[userNum][7].setText(String.valueOf(bonus));
    }

    private int setUserNum(String userId) {
        for (int i = 0; i < 4; i++) {
            if (userId.equals(User_Array_client[i])) {
                userNum = i;
                userLabels[userNum].setForeground(new Color(86, 145, 255));
                break; // 유저를 찾으면 더 이상 탐색하지 않음
            }
        }
        //printDisplay2(uid + "입장번호 : " + userNum);
        return userNum;
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

    private void openRoomWindow(String roomTitle) {
        this.setVisible(false);  // 클라이언트 창 숨기기
        GameGUI();
    }

    // 주석 사이에는 GameGUI 코드

    /// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void send_message_room(String roomTitle) {
        String message_temp = t_input_GAME.getText().trim();

        if (message_temp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "공백으로 이루어진 채팅을 칠 수 없습니다.", "경고", JOptionPane.WARNING_MESSAGE);
            t_input_GAME.setText("");
        } else {
            send(new Yacht(t_userID.getText(), Yacht.MODE_TX_STRING_ROOM, roomTitle, message_temp));
            t_input_GAME.setText("");
        }

    }

    private void send_message_room_first(String roomTitle, String message) {
        send(new Yacht(t_userID.getText(), Yacht.MODE_TX_STRING_ROOM_FIRST, roomTitle, message));
    }

    public YachtDiceClient(String serverAddress, int serverPort) {
        super("Yacht Game Client");
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        buildGUI();

        setMinimumSize(new Dimension(700, 700)); // 최소 크기 설정
        setSize(700, 700);
        setLocation(0, 0);
        setResizable(false); // 창 크기 조절 비활성화
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void displayRoomList(String[] roomEntries) {
        roomPanel.removeAll(); // 기존 방 버튼 제거

        for (String entry : roomEntries) {
            String[] parts = entry.split(":"); // 방 제목과 비밀번호 존재 여부를 ":"로 분리
            String title = parts[0]; // 방 제목
            boolean hasPassword = parts.length > 1 && !parts[1].equals("0"); // 비밀번호 존재 여부 체크
            String password_temp = "d";
            if (title == null || title.trim().isEmpty()) {
                // 방 이름이 없을때 아무일도 일어나지 않아야함.
            } else {
                ImageIcon secretRoomIcon = new ImageIcon(getClass().getResource("/resources/secret_room.png"));
                Image scaledImage = secretRoomIcon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);

                JButton roomButton;
                if (hasPassword) {
                    password_temp = parts[1];
                    roomButton = new JButton(title, scaledIcon);
                } else {
                    password_temp = null;
                    roomButton = new JButton(title);
                }

                roomButton.setPreferredSize(new Dimension(roomPanel.getParent().getWidth(), 70)); // 버튼 가로 크기 설정
                roomButton.setMaximumSize(new Dimension(roomPanel.getParent().getWidth(), 70)); // 버튼 최대 크기 설정

                String finalPassword_temp = password_temp;
                roomButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        roomTitle_copy = title;
                        enterRoom(t_userID.getText(), title, 1, finalPassword_temp);
                    }
                });
                roomPanel.add(roomButton); // 방 버튼 추가
                roomPanel.add(Box.createVerticalStrut(50)); // 버튼 아래쪽 여백 추가
            }
        }
        roomPanel.revalidate(); // 패널 업데이트
        roomPanel.repaint(); // 화면 다시 그리기
    }

    private void enterRoom(String userIDID, String roomTitle, int flag, String password) {
        roomTitle_copy = roomTitle;
        if (password != null) {
            String inputPassword = null;

            while (true) {
                if (flag == 1) {
                    inputPassword = JOptionPane.showInputDialog(this, "비밀번호를 입력하세요:", "비밀번호 확인", JOptionPane.PLAIN_MESSAGE);

                    // 비밀번호 입력창에서 취소 버튼을 눌렀을 경우
                    if (inputPassword == null) {
                        return;
                    }

                    // 비밀번호가 틀린 경우
                    if (!inputPassword.equals(password)) {
                        JOptionPane.showMessageDialog(this, "비밀번호가 틀렸습니다. 다시 입력하세요.", "비밀번호 확인", JOptionPane.WARNING_MESSAGE);
                        continue; // 다시 입력 요청
                    }

                    // 비밀번호가 맞는 경우
                    send(new Yacht(uid, Yacht.MODE_ENTER_ROOM, roomTitle_copy));
                    break;
                } else if (flag == 0) {
                    send(new Yacht(uid, Yacht.MODE_ENTER_ROOM, roomTitle_copy));
                    break;
                }
            }
        } else {
            // 비밀번호가 필요 없는 경우
            send(new Yacht(uid, Yacht.MODE_ENTER_ROOM, roomTitle_copy));
        }
    }

    private void quit_room(String roomTitle) {
        String roomtitle_temp = roomTitle;
        send(new Yacht(t_userID.getText(), Yacht.MODE_QUIT_ROOM, roomtitle_temp));
        send_message_room_first(roomTitle, uid + "님이 퇴장했습니다.");
    }

    private void connectToServer() throws UnknownHostException, IOException {
        socket = new Socket();
        SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        socket.connect(sa, 3000);

//        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        receiveThread = new Thread(new Runnable() {

            private ObjectInputStream in;

            private void receiveMessage() {
                try {
                    Yacht inMsg = (Yacht) in.readObject();
                    if (inMsg == null) {
                        disconnect();
                        printDisplay("서버 연결 끊김");
                        return;
                    }
                    switch (inMsg.mode) {
                        case Yacht.MODE_TX_STRING:
                            printDisplay(inMsg.userID + ": " + inMsg.message);
                            break;
                        case Yacht.MODE_LOGIN:
                            if (inMsg.message.equals("이미 사용중인 이름 입니다.")) {
                                JOptionPane.showMessageDialog(YachtDiceClient.this, inMsg.message, "경고", JOptionPane.WARNING_MESSAGE);
                                disconnect();
                            } else {
                                printDisplay(inMsg.message);
                            }
                            break;
                        case Yacht.MODE_LOGOUT:
                            printDisplay(inMsg.message);
                            break;
                        case Yacht.MODE_TX_IMAGE:
                            printDisplay(inMsg.userID + ": " + inMsg.message);
                            break;
                        case Yacht.MODE_CREATE_NORMAL_ROOM:
                            if (inMsg.message.equals("일반 방이 이미 존재합니다.")) {
                                if (Objects.equals(inMsg.userID, t_userID.getText())) {
                                    JOptionPane.showMessageDialog(YachtDiceClient.this, inMsg.message, "경고", JOptionPane.WARNING_MESSAGE);
                                }
                                break;
                            } else {
                                if (Objects.equals(inMsg.userID, t_userID.getText())) {
                                    enterRoom(t_userID.getText(), inMsg.roomTitle, 0, inMsg.passWord);
                                }
                            }
                            break;
                        case Yacht.MODE_CREATE_SECRET_ROOM:
                            if (inMsg.message.equals("비밀 방이 이미 존재합니다.")) {
                                if (Objects.equals(inMsg.userID, t_userID.getText())) {
                                    JOptionPane.showMessageDialog(YachtDiceClient.this, inMsg.message, "경고", JOptionPane.WARNING_MESSAGE);
                                }
                                break;
                            } else {
                                if (Objects.equals(inMsg.userID, t_userID.getText())) {
                                    enterRoom(t_userID.getText(), inMsg.roomTitle, 0, inMsg.passWord);
                                }
                            }
                            break;
                        case Yacht.MODE_ROOM_LIST:
                            String roomTitlesString = inMsg.message; // 쉼표로 구분된 방 제목 문자열 수신
                            String[] roomTitlesArray = roomTitlesString.split(","); // 쉼표로 분리하여 배열로 변환

                            displayRoomList(roomTitlesArray); // 방 목록 출력
                            break;
                        case Yacht.MODE_ENTER_ROOM:
                            if (inMsg.message.equals("(" + roomTitle_copy + ") 방이 가득 차서 입장할 수 없습니다.")) {
                                JOptionPane.showMessageDialog(YachtDiceClient.this, inMsg.message, "경고", JOptionPane.WARNING_MESSAGE);
                            } else if (inMsg.message.equals("(" + roomTitle_copy + ")" + " 방이 게임을 시작 해 입장할 수 없습니다.")) {
                                JOptionPane.showMessageDialog(YachtDiceClient.this, inMsg.message, "경고", JOptionPane.WARNING_MESSAGE);
                            } else {
                                if (Objects.equals(inMsg.userID, t_userID.getText())) {
                                    //System.out.println(inMsg.userID + "님이 " + inMsg.message + " 방에 접속");
                                    openRoomWindow(inMsg.message);
                                }
                            }
                            break;
                        case Yacht.MODE_QUIT_ROOM:
                            break;
                        case Yacht.MODE_TX_STRING_ROOM:
                            if (roomTitle_copy.equals(inMsg.roomTitle)) { // 입장한 방과 채팅을 친 방이 같다면
                                // 만약 입장을 하지 않았으면 방 이름은 없을것이고 다른 방에 입장했다면 조건에 만족하지 않을 것임
                                printDisplay2(inMsg.userID + ": " + inMsg.message);
                            }
                            break;
                        case Yacht.MODE_TX_STRING_ROOM_FIRST:
                            if (roomTitle_copy.equals(inMsg.roomTitle)) {
                                // 방에 입장과 퇴장 , 항복할 때 채팅 뿌리기
                                String temp_message = inMsg.message;
                                String[] parts = temp_message.split("!=!");

                                for (int i = 1; i < parts.length; i++) {
                                    User_Array_client[i - 1] = parts[i];
                                    User_Array_client_copy[i - 1] = parts[i];
                                }
                                for (int i = parts.length; i < 5; i++) {
                                    User_Array_client[i - 1] = "";
                                    User_Array_client_copy[i - 1] = "";
                                }

                                inMsg.message = parts[0];
                                if (inMsg.message.equals("게임 시작!")) {
                                    printDisplay2("\n" + inMsg.message);
                                    UI_update(dice_panel, b_game_start, b_roll, b_dices, b_giveup);
                                    setUserNum(uid);
                                    for (int i = 0; i < 4; i++) {
                                        if (User_Array_client[i].length() < 1 || User_Array_client[i] == null) {
                                        } else {
                                            printDisplay2("순서 : " + (i + 1) + " - " + User_Array_client[i]);
                                        }
                                    }
                                    printDisplay2("------------------------------------");
                                    l_nowTurn.setText("Turn " + turn + ": " + currentTurn + "의 차례입니다");
//                                    for (int i = 0; i < 4; i++) { // 유저 이름 채팅창에 출력
//                                        printDisplay2(User_Array_client[i]);
//                                    }
                                } else {
                                    printDisplay2(inMsg.message);
                                }
                                setUser();
                            }
                            break;
                        case Yacht.MODE_TX_STRING_SCORE: // 점수 받기
                            if (roomTitle_copy.equals(inMsg.roomTitle)) {
                                String message = inMsg.message;
                                //printDisplay2("서버로부터 받은 점수 정보: " + message);

                                // 메시지에서 점수 정보 추출
                                String[] parts = message.split(", ");
                                String userNumStr = parts[0].split(": ")[1];
                                String scoreIndexStr = parts[1].split(": ")[1];
                                String scoreStr = parts[2].split(": ")[1];
                                String totalScoreStr = parts[3].split(": ")[1];
                                String middleScoreStr = parts[4].split(": ")[1];
                                String bonusStr = parts[5].split(": ")[1];

                                int userNum = Integer.parseInt(userNumStr);
                                int scoreIndex = Integer.parseInt(scoreIndexStr);
                                int score = Integer.parseInt(scoreStr);
                                int totalScore = Integer.parseInt(totalScoreStr);
                                int middleScore = Integer.parseInt(middleScoreStr);
                                int bonus = Integer.parseInt(bonusStr);
                                if (userNum == 10000 && scoreIndex == 10000 && score == 10000 && totalScore == 10000) {
                                    nextTurn();
                                } else if (userNum == 20000 && scoreIndex == 20000 && score == 20000 && totalScore == 20000) {
                                    printDisplay2("------------------------------------");
                                    printDisplay2("게임 종료!");
                                    printDisplay2("결과 발표\n");
                                    int check_winner = 0;
                                    int count_winner = 0;
                                    for (int i = 0; i < 4; i++) {
                                        if (User_Array_client[i].length() > 1) {
                                            count_winner++;
                                            check_winner = i;
                                        }
                                    }
                                    if (count_winner == 1) { // 나머지가 다 항복하고 한명만 남았을 경우
                                        printDisplay2("** 1등 : " + User_Array_client[check_winner] + " **");
                                        for (int i = 0; i < check_winner; i++) {
                                            if (User_Array_client_copy[i].length() < 1 || User_Array_client_copy[i] == null) {
                                            } else {
                                                printDisplay2("항복 : " + User_Array_client_copy[i]);
                                            }
                                        }
                                        for (int i = check_winner + 1; i < 4; i++) {
                                            if (User_Array_client_copy[i].length() < 1 || User_Array_client_copy[i] == null) {
                                            } else {
                                                printDisplay2("항복 : " + User_Array_client_copy[i]);
                                            }
                                        }
                                    }
                                    newFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                } else if (userNum == 30000 && scoreIndex == 30000 && score == 30000 && totalScore == 30000) {
                                    if (uid.equals(inMsg.userID)) {
                                        printDisplay2("------------------------------------");
                                        printDisplay2("게임 종료!");
                                        printDisplay2("결과 발표\n");

                                        int LAST_SORT[] = new int[4];
                                        int LAST_TEMP[] = new int[4];
                                        String LAST_STRING[] = new String[4];
                                        String LAST_STRING_COPY[] = new String[4];

                                        for (int i = 0; i < 4; i++) {
                                            if (User_Array_client_copy[i].length() < 1 || User_Array_client_copy[i] == null) {
                                                LAST_SORT[i] = 1000000;
                                                LAST_STRING[i] = "";
                                            } else {
                                                if (scoreLabels[i][14].getText() == null || scoreLabels[i][14].getText().equals("")) {
                                                    LAST_SORT[i] = 1000000;
                                                    LAST_STRING[i] = "";
                                                } else {
                                                    LAST_SORT[i] = Integer.parseInt(scoreLabels[i][14].getText());
                                                    LAST_STRING[i] = User_Array_client_copy[i] + "  점수 : ";
                                                }
                                                if (!User_Array_client_copy[i].equals(User_Array_client[i])) {
                                                    LAST_SORT[i] = 20000;
                                                    LAST_STRING[i] = "항복 : " + User_Array_client_copy[i];
                                                }
                                            }
                                        }

                                        for (int i = 0; i < 3; i++) {
                                            for (int j = i + 1; j < 4; j++) {
                                                if (LAST_SORT[i] < LAST_SORT[j]) {
                                                    int temp = LAST_SORT[i];
                                                    LAST_SORT[i] = LAST_SORT[j];
                                                    LAST_SORT[j] = temp;
                                                    String temp_string = LAST_STRING[i];
                                                    LAST_STRING[i] = LAST_STRING[j];
                                                    LAST_STRING[j] = temp_string;
                                                }
                                            }
                                        }
                                        int winner = 1;
                                        int duplication = 0; // 공동순위 숫자
                                        for (int i = 0; i < 4; i++) {
                                            if (LAST_SORT[i] == 1000000 || LAST_STRING.equals("") || LAST_SORT[i] == 20000) {
                                            } else {
                                                if (winner == 1) { // 1등이면
                                                    printDisplay2("** " + winner + "등 : " + LAST_STRING[i] + LAST_SORT[i] + " **");
                                                    winner++;
                                                } else { // 1등이 아니면
                                                    if (LAST_SORT[i] == LAST_SORT[i - 1]) { // 그 전 순위와 점수가 같다면
                                                        winner--; // 공동 등수로 처리
                                                        if (winner == 1) {
                                                            printDisplay2("** " + winner + "등 : " + LAST_STRING[i] + LAST_SORT[i] + " **");
                                                            winner++;
                                                        } else {
                                                            printDisplay2(winner - 1 + "등 : " + LAST_STRING[i] + LAST_SORT[i]);
                                                            winner++;
                                                        }
                                                        duplication++;
                                                    } else {
                                                        if (duplication == 0) {
                                                            printDisplay2(winner + "등 : " + LAST_STRING[i] + LAST_SORT[i]);
                                                            winner++;
                                                        } else {
                                                            printDisplay2(winner + duplication + "등 : " + LAST_STRING[i] + LAST_SORT[i]);
                                                            winner = winner + 1 + duplication;
                                                            duplication = 0;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        for (int i = 0; i < 4; i++) {
                                            if (LAST_SORT[i] == 20000) {
                                                printDisplay2(LAST_STRING[i]);
                                            }
                                        }
                                    }
                                    newFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                } else {
                                    // 점수판에 반영하는 로직
                                    updateScore(userNum, scoreIndex, score, totalScore, middleScore, bonus);
                                    nextTurn();
                                }
                            }
                            break;
                        case Yacht.MODE_GAME_START:
                            send_message_room_first(roomTitle_copy, "게임 시작!");
                            break;
                        case Yacht.MODE_HOW_MANY_PEOPLE:
                            if (inMsg.message.equals("게임이 시작되려면 2명 이상이 방에 입장해 있어야 합니다.")) {
                                check_game_start = 0;
                            } else {
                                check_game_start = 1;
                            }
                            break;
                        case Yacht.MODE_SHOW_ROLLING_DICE:
                            if (roomTitle_copy.equals(inMsg.roomTitle)) {
                                // resources.dice*.png* 문자열이 도착하는데 앞에있는 *은 temp변수에, 뒤에있는 *은 temp_number 변수에 넣는다
                                int temp_number = Character.getNumericValue(inMsg.message.charAt(19)); // 몇번 주사위
                                char temp = inMsg.message.charAt(14);
                                ImgService.updateImage(b_dices[temp_number], "resources/dice" + temp + ".png");
                            }
                            break;
                        case Yacht.MODE_MOVE_DICE:
                            if (roomTitle_copy.equals(inMsg.roomTitle)) {
                                // printDisplay2(inMsg.message);
                                // x좌표!=!y좌표!=!주사위번호+1 형식으로 문자열이 반환됨
                                String[] parts = inMsg.message.split("!=!");
                                int X_location = Integer.parseInt(parts[0]);
                                int Y_location = Integer.parseInt(parts[1]);
                                int dice_number = Integer.parseInt(parts[2]);
                                b_dices[dice_number - 1].setLocation(X_location, Y_location); // 저장 시 위치 변경
                            }
                            break;
                        case Yacht.MODE_SURRENDER:
                            if (roomTitle_copy.equals(inMsg.roomTitle)) {
                                printDisplay2(inMsg.userID + "님이 항복했습니다.");
                                for (int i = 0; i < 4; i++) {
                                    if (User_Array_client[i].equals(inMsg.userID)) {
                                        User_Array_client[i] = "";
                                        break;
                                    }
                                }
                                setUser();
                            }
                            break;
                    }
                } catch (IOException e) {
                    printDisplay("연결을 종료했습니다.");
                } catch (ClassNotFoundException e) {
                    printDisplay("잘못된 객체가 전달되었습니다.");
                }
            }

            @Override
            public void run() {
                try {
                    in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                } catch (IOException e) {
                    System.out.println("입력 스트림이 열리지 않음");
                }
                while (receiveThread == Thread.currentThread()) {
                    receiveMessage();
                }
            }
        });
        receiveThread.start();
    }

    private void refresh() {
        roomPanel.revalidate(); // 패널 갱신
        roomPanel.repaint(); // 패널 다시 그리기
    }

    private void createRoomDialog() {
        // 방 만들기 팝업창
        JDialog dialog = new JDialog(this, "방 만들기", true);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false); // 창 크기 조절 비활성화

        // 방 이름 설정하는 패널
        JPanel roomNamePanel = new JPanel();
        roomNamePanel.setLayout(new GridLayout(1, 2));
        roomNamePanel.add(createCenteredLabel("방 이름 :"));
        JTextField roomTitleField = new JTextField();
        roomNamePanel.add(roomTitleField);

        // 비밀 방 체크박스 패널
        JPanel checkPanel = new JPanel();
        checkPanel.setLayout(new FlowLayout());
        JCheckBox secretRoomCheckBox = new JCheckBox("비밀 방");
        checkPanel.add(secretRoomCheckBox);

        // 비밀번호 입력 패널
        JPanel passwordPanel = new JPanel();
        passwordPanel.setLayout(new GridLayout(1, 2));
        passwordPanel.add(createCenteredLabel("비밀번호 :"));
        JPasswordField passwordField = new JPasswordField();
        passwordField.setEnabled(false); // 비밀번호 입력란 비활성화
        passwordPanel.add(passwordField);

        // 비밀 방 체크 여부에 따라 입력 필드 활성화/비활성화
        secretRoomCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordField.setEnabled(secretRoomCheckBox.isSelected());
            }
        });

        // 방 만들기 버튼
        JButton createButton = new JButton("방 만들기");
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                roomTitle = roomTitleField.getText();
                password = secretRoomCheckBox.isSelected() ? new String(passwordField.getPassword()) : null;

                if (check_space(roomTitle)) {
                    JOptionPane.showMessageDialog(YachtDiceClient.this, "방 이름은 공백을 포함할 수 없습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (password != null) {
                    if (password.length() < 4) {
                        JOptionPane.showMessageDialog(YachtDiceClient.this, "비밀번호는 4자리 이상이여야 합니다.", "경고", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (check_space(password)) {
                        JOptionPane.showMessageDialog(YachtDiceClient.this, "비밀번호는 공백을 포함할 수 없습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }

                if (roomTitle.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "방 이름을 입력하세요.", "방 이름 확인", JOptionPane.WARNING_MESSAGE);
                    roomTitleField.setText("");
                    return; // 방 이름이 없다면 방 생성하지 않음
                }
                // 비밀번호가 필요한 비밀방일 경우 체크
                if (secretRoomCheckBox.isSelected() && (password.isEmpty())) {
                    JOptionPane.showMessageDialog(dialog, "비밀번호를 입력하세요.", "비밀번호 확인", JOptionPane.WARNING_MESSAGE);
                    passwordField.setText("");
                    return; // 비밀번호가 없다면 방 생성하지 않음
                }

                if (password == null) {
                    check_new_Normal_room();
                } else {
                    check_new_Secret_room();
                }
                dialog.dispose();
            }
        });

        // 팝업의 하단을 제외한 나머지 부분들에 여백 추가
        JPanel westPanel = new JPanel();
        westPanel.setPreferredSize(new Dimension(50, 50));
        dialog.add(westPanel, BorderLayout.WEST);

        JPanel eastPanel = new JPanel();
        eastPanel.setPreferredSize(new Dimension(50, 50));
        dialog.add(eastPanel, BorderLayout.EAST);

        JPanel northPanel = new JPanel();
        northPanel.setPreferredSize(new Dimension(20, 20));
        dialog.add(northPanel, BorderLayout.NORTH);

        // 위에서 생성했던 모든 패널들을 합치는 패널
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(4, 1));

        centerPanel.add(roomNamePanel); // 방 이름 입력 패널
        centerPanel.add(checkPanel); // 비밀 방 체크박스
        centerPanel.add(passwordPanel); // 비밀번호 입력 패널

        dialog.add(centerPanel, BorderLayout.CENTER);
        dialog.add(createButton, BorderLayout.SOUTH);

        // 팝업 크기 및 위치 설정
        dialog.setMinimumSize(new Dimension(300, 250));
        dialog.setSize(300, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void check_new_Normal_room() {
        String roomtitle_temp = roomTitle;
        send(new Yacht(uid, Yacht.MODE_CREATE_NORMAL_ROOM, roomtitle_temp));
    }

    private void check_new_Secret_room() {
        String roomtitle_temp = roomTitle;
        String password_temp = password;
        send(new Yacht(uid, Yacht.MODE_CREATE_SECRET_ROOM, roomtitle_temp, password_temp));
    }

    private void buildGUI() {
        // 상위 패널 생성
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 왼쪽 패널
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(createDisplayPanel(), BorderLayout.CENTER);
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.add(createInfoPanel());
        p.add(createControlPanel());
        leftPanel.add(p, BorderLayout.SOUTH);
        mainPanel.add(leftPanel, BorderLayout.CENTER); // 왼쪽에 추가

        // 오른쪽 패널
        JPanel rightPanel = new JPanel(new BorderLayout()); // 예시 패널 생성 메서드 호출
        rightPanel.add(textPanel(), BorderLayout.CENTER);
        rightPanel.add(createInputPanel(), BorderLayout.SOUTH);
        mainPanel.add(rightPanel, BorderLayout.EAST); // 오른쪽에 추가

        add(mainPanel, BorderLayout.CENTER);
    }

    private boolean check_space(String text) {
        for (char ch : text.toCharArray()) {
            if (ch == ' ') {
                // 공백 발견
                return true;
            }
        }
        return false;
    }

    private JPanel createControlPanel() {
        JPanel p = new JPanel(new GridLayout(0, 3));

        b_connect = new JButton("접속 하기");
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (t_IP.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(YachtDiceClient.this, "IP를 입력해주세요.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                YachtDiceClient.this.serverAddress = t_IP.getText();

                try {
                    String text = t_userID.getText();
                    if (check_space(text)) {
                        JOptionPane.showMessageDialog(YachtDiceClient.this, "공백을 포함할 수 없습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    } else if (text.isEmpty()) {
                        JOptionPane.showMessageDialog(YachtDiceClient.this, "아이디를 입력해주세요.", "경고", JOptionPane.WARNING_MESSAGE);
                    } else {
                        connectToServer();
                        sendUserID();

                        b_connect.setEnabled(false);
                        b_disconnect.setEnabled(true);
                        b_exit.setEnabled(false);

                        t_userID.setEditable(false);
                        t_input.setEnabled(true);
                        b_send.setEnabled(true);

                        b_createRoom.setEnabled(true);
                    }
                } catch (UnknownHostException e1) {
                    printDisplay("서버 주소를 확인하세요");
                    return;
                } catch (IOException e1) {
                    printDisplay("서버와의 연결 오류 : " + e1.getMessage());
                    return;
                }
            }
        });

        b_disconnect = new JButton("접속 끊기");
        b_disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();

                b_connect.setEnabled(true);
                b_disconnect.setEnabled(false);
                b_exit.setEnabled(true);

                t_userID.setEditable(true);

                t_input.setEnabled(false);
                b_send.setEnabled(false);
                t_input.setText("");

                b_createRoom.setEnabled(false);
            }
        });

        b_exit = new JButton("종료 하기");
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        p.add(b_connect);
        p.add(b_disconnect);
        p.add(b_exit);

        b_disconnect.setEnabled(false);

        return p;
    }

    private JPanel createInputPanel() {
        JPanel p = new JPanel(new BorderLayout());

        t_input = new JTextField(20);
        t_input.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        b_send = new JButton("보내기");
        b_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        p.add(t_input, BorderLayout.CENTER);
        p.add(b_send, BorderLayout.EAST);

        t_input.setEnabled(false);
        b_send.setEnabled(false);

        return p;
    }

    private JPanel textPanel() {
        JPanel p = new JPanel(new BorderLayout());

        t_display = new JTextArea();
        t_display.setEditable(false);


        JScrollPane scrollPane = new JScrollPane(t_display);
        scrollPane.setPreferredSize(new Dimension(350, 200)); // 스크롤 패널 크기 설정


        p.add(scrollPane, BorderLayout.CENTER);

        return p;
    }

    private JPanel createDisplayPanel() {
        JPanel p = new JPanel(new BorderLayout());

        b_createRoom = new JButton("방 만들기");
        b_createRoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createRoomDialog();
            }
        });
        b_createRoom.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        b_createRoom.setEnabled(false);

        // 버튼과 여백을 포함하는 패널 생성
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.add(b_createRoom);
        buttonPanel.add(Box.createVerticalStrut(50)); // 버튼 아래에 50픽셀 여백 추가

        // 버튼 패널을 상단에 추가
        p.add(buttonPanel, BorderLayout.NORTH);

        // 여백을 유지하기 위한 빈 패널 추가
        JPanel westPanel = new JPanel();
        westPanel.setPreferredSize(new Dimension(50, 50));
        p.add(westPanel, BorderLayout.WEST);

        JPanel southPanel = new JPanel();
        southPanel.setPreferredSize(new Dimension(50, 50));
        p.add(southPanel, BorderLayout.SOUTH);

        JPanel eastPanel = new JPanel();
        eastPanel.setPreferredSize(new Dimension(50, 50));
        p.add(eastPanel, BorderLayout.EAST);

        // 방 목록이 표시되는 패널
        roomPanel = new JPanel();
        roomPanel.setLayout(new BoxLayout(roomPanel, BoxLayout.Y_AXIS));

        // 방 목록에 스크롤 가능한 패널 추가
        JScrollPane roomScrollPane = new JScrollPane(roomPanel);
        roomScrollPane.setPreferredSize(new Dimension(200, 200));
        //roomScrollPane.setBorder(null); // 테두리 제거

        // 스크롤바 숨기기
        //roomScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); // Y축 숨기기
        roomScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // X축 숨기기

        p.add(buttonPanel, BorderLayout.NORTH);
        p.add(roomScrollPane, BorderLayout.CENTER);

        return p;
    }

    private JPanel createInfoPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));

        t_IP = new JTextField(10);

        t_IP.setText(getLocalAddr());
        t_IP.setHorizontalAlignment(JTextField.CENTER);

        t_userID = new JTextField(12);
        Random random = new Random();
        t_userID.setText("도전자" + (random.nextInt(100) + 1));
        t_userID.setHorizontalAlignment(JTextField.CENTER);

        p.add(new JLabel("IP:"));
        p.add(t_IP);

        p.add(new JLabel("아이디:"));
        p.add(t_userID);

        return p;
    }

    private void printDisplay(String msg) {
        t_display.append(msg + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());

    }

    private String getLocalAddr() {
        InetAddress local = null;
        String addr = "";
        try {
            local = InetAddress.getLocalHost();
            addr = local.getHostAddress();
            System.out.println(addr);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return addr;
    }

    private JLabel createCenteredLabel(String text) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.CENTER); // 가운데 정렬
        return label;
    }

    private void disconnect() {
        b_connect.setEnabled(true);
        b_disconnect.setEnabled(false);
        b_exit.setEnabled(true);

        t_userID.setEditable(true);

        t_input.setEnabled(false);
        b_send.setEnabled(false);
        t_input.setText("");

        b_createRoom.setEnabled(false);

        roomPanel.removeAll();
        refresh();

        send(new Yacht(uid, Yacht.MODE_LOGOUT, ""));

        try {
            receiveThread = null;
            socket.close();
        } catch (IOException e) {
            System.err.println("클라이언트 닫기 오류 > " + e.getMessage());
            System.exit(-1);
        }
    }

    private void send(Yacht msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("클라이언트 일반 전송 오류 > " + e.getMessage());
        }
    }

    private void sendMessage() {
        String message = t_input.getText().trim();
        if (message.isEmpty()) {
            JOptionPane.showMessageDialog(YachtDiceClient.this, "공백으로 이루어진 채팅을 칠 수 없습니다.", "경고", JOptionPane.WARNING_MESSAGE);
            t_input.setText("");
        } else {
            send(new Yacht(uid, Yacht.MODE_TX_STRING, message));
            t_input.setText("");
        }
    }

    private void sendUserID() {
        uid = t_userID.getText();
        send(new Yacht(uid, Yacht.MODE_LOGIN, ""));
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;

        new YachtDiceClient(serverAddress, serverPort);
    }
}
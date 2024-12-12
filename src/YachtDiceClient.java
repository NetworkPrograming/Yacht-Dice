import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

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
    private JTextArea t_display_test;
    private JTextField t_input;
    private JTextField t_input_test;
    private JButton b_send;

    String roomTitle_copy = "";

    // 위에는 클라이언트에 필요한 변수
    /// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 밑에는 게임창에 필요한 변수

    private Image backgroundImage = new ImageIcon(getClass().getResource("/resources/background.jpg")).getImage();
    private Image scoreBoard = new ImageIcon(getClass().getResource("/resources/scoreBoard.jpg")).getImage();
    private Image sendButton = new ImageIcon(getClass().getResource("/resources/send_button.png")).getImage();

    JButton giveUpButton = new JButton("Give Up");
    JTextArea textArea = new JTextArea(10, 20);
    private JTextArea chatDisplay; // 채팅 메시지 표시 영역
    private JTextField t_input_GAME;  // 메시지 입력 필드
    private JButton b_send_GAME;    // 메시지 보내기 버튼
    JButton b_dice;

    final static int DICE_SIZE = 5;
    final static int MAX_DICE_NUM = 6;

    int checkRoll = 0; // 굴린 횟수 세기

    int[] dices;
    int[] counts;

    Random rand;

    public void GameGUI() {
        dices = new int[DICE_SIZE];
        counts = new int[MAX_DICE_NUM];

        rand = new Random();
        System.setProperty("sun.java2d.uiScale", "1.0"); // DPI 스케일링 고정

        JFrame newFrame = new JFrame(roomTitle_copy);
        newFrame.setSize(1400, 770);
        newFrame.setLocationRelativeTo(null); // 화면 중앙에 위치
        newFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 닫기 버튼 클릭 시 창만 닫힘

        // 레이어드 페인(JLayeredPane) 생성
        JLayeredPane layeredPane = new JLayeredPane();
        newFrame.add(layeredPane); // JLayeredPane을 프레임에 추가

        // 배경 패널을 레이어 0에 추가
        JPanel backgroundPanel = BackgroundPanel();
        backgroundPanel.setBounds(50, 0, newFrame.getWidth(), newFrame.getHeight());
        layeredPane.add(backgroundPanel, Integer.valueOf(0)); // 레이어 0에 배경 추가

        // 점수 패널을 레이어 1에 추가
        JPanel scorePanel = ScorePanel();
        scorePanel.setBounds(0, 0, 350, 750); // 점수판 크기 설정
        layeredPane.add(scorePanel, Integer.valueOf(1)); // 레이어 1에 점수판 추가

        // 채팅 패널을 레이어 2에 추가
        JPanel chatPanel = ChatPanel();
        chatPanel.setBounds(1100, 0, 320, 760); // 채팅판 크기 설정
        layeredPane.add(chatPanel, Integer.valueOf(2)); // 레이어 2에 채팅판 추가

        // 주사위 패널을 레이어 10에 추가
        JPanel dicePanel = AddDiceComponents();
        dicePanel.setBounds(380, 0, 750, 750);
        layeredPane.add(dicePanel, Integer.valueOf(10)); // 레이어 10에 주사위 패널 추가

        AddDiceComponents();

        newFrame.setVisible(true); // 창을 보이도록 설정
        newFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                YachtDiceClient.this.setVisible(true); // 기존 창 다시 보이기
                quit_room(roomTitle);
                printDisplay(roomTitle + " 게임 방에서 퇴장하였습니다.");
            }
        });
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
                g.drawImage(scoreBoard, 0, 0, 350, 750, this);
            }
        };
        panel.setLayout(null);
        return panel;
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
        textArea.setBounds(15, 10, 250, 660);
        textArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14)); // 폰트 설정
        panel.add(textArea);

        // 메시지 입력 필드
        t_input_GAME = new JTextField();
        //t_input.setOpaque(false);
        //t_input_GAME.setBorder(border);
        t_input_GAME.setBounds(15, 670, 220, 30);
        panel.add(t_input_GAME);

        // 메시지 보내기 버튼
        b_send_GAME = new JButton();
        String sendImage = "/resources/send_button.png";

        // 이미지 로드 및 크기 조정
        ImageIcon originalIcon = new ImageIcon(getClass().getResource(sendImage));
        Image scaledImage = originalIcon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        // 버튼에 스케일된 이미지 설정
        b_send_GAME.setIcon(scaledIcon);

        // 버튼 크기 및 위치 설정
        b_send_GAME.setBounds(235, 670, 30, 30);
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

    private void printDisplay2(String msg){
        textArea.append(msg+"\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    private JPanel AddDiceComponents() {
        JPanel jPanel = new JPanel();

        JButton[] b_dices = new JButton[DICE_SIZE];
        jPanel.setOpaque(false); // 패널을 투명하게 설정
        jPanel.setLayout(null);
        Arrays.fill(dices, 1);

        b_dices[0] = createDiceButton("resources/dice" + 1 + ".png", 110, 210);
        b_dices[1] = createDiceButton("resources/dice" + 2 + ".png", 200, 210);
        b_dices[2] = createDiceButton("resources/dice" + 3 + ".png", 295, 210);
        b_dices[3] = createDiceButton("resources/dice" + 4 + ".png", 390, 210);
        b_dices[4] = createDiceButton("resources/dice" + 5 + ".png", 490, 210);

        for (int i = 0; i < DICE_SIZE; i++) { //주사위 버튼 만들기
            jPanel.add(b_dices[i]);
        }

        // 굴리기 버튼
        JButton b_roll = new JButton();
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
                checkRoll++;
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

                                for (int i = 0; i < DICE_SIZE; i++) { // 킵하지 않은게 있으면 랜덤값 생성 후 이미지 그림
                                    if (!(boolean) b_dices[i].getClientProperty("isSaved")) {
                                        dices[i] = rand.nextInt(6) + 1;
                                        // UI 업데이트 요청
                                        ImgService.updateImage(b_dices[i], "resources/dice" + dices[i] + ".png");
                                        b_dices[i].setEnabled(false); // 굴리는 동안 주사위 클릭 금지
                                    }
                                }
                                // UI 갱신 및 딜레이
                                SwingUtilities.invokeLater(() -> {
                                    jPanel.repaint();
                                    jPanel.revalidate();
                                });

                                Thread.sleep(60);
                                endTime = System.currentTimeMillis();
                            }
                            // 주사위 굴림 종료 후 버튼 활성화
                            SwingUtilities.invokeLater(() -> {
                                b_roll.setEnabled(true);
                                for (JButton diceButton : b_dices) {
                                    diceButton.setEnabled(true);
                                }
                            });

                            for (int i = 0; i < DICE_SIZE; i++) { // isSaved 값 출력
                                System.out.println(dices[i] + " " + b_dices[i].getClientProperty("isSaved"));
                            }

                            System.out.println(available() + checkRoll); // 족보 출력
                            Thread.sleep(1000); //1초 대기


                            ////
                            boolean allTrue = true; // 모든 값이 true인지 확인하는 로직 -> 주사위 클릭했을 때로 이동
                            for (int i = 0; i < DICE_SIZE; i++) {
                                if (!(boolean) b_dices[i].getClientProperty("isSaved")) {
                                    allTrue = false;
                                    break;
                                }
                            }
                            if (allTrue) { // 모두 isSaved 상태 되면 주사위,롤버튼 클릭 X
                                b_roll.setEnabled(false);
                                for (int i = 0; i < DICE_SIZE; i++){
                                    b_dices[i].setEnabled(false);
                                }
                            }
                            if(checkRoll==3){ // 3번 다 굴리면 isSaved 아닌 것들 다 isSaved로 바꾸고 위로 올림
                                b_roll.setEnabled(false);
                                for (int i = 0; i < DICE_SIZE; i++) {
                                    b_dices[i].setEnabled(false);
                                    if (!(boolean) b_dices[i].getClientProperty("isSaved")) {
                                        b_dices[i].setLocation(b_dices[i].getX(), b_dices[i].getY() - 130);
                                        b_dices[i].putClientProperty("isSaved", true);  // 이동 상태 업데이트
                                        repaint();
                                    }
                                }
                            }


                            ////

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

//    private void startTurn(){ // 턴 시작
//        b_roll.setEnabled(true);
//        for (int i = 0; i < DICE_SIZE; i++){
//            b_dices[i].setEnabled(true);
//        }
//    }

//    private boolean isTurnEnded(){ //턴 끝났는지 확인
//        boolean allTrue = true; // 모든 값이 true인지 확인하는 로직
//        for (int i = 0; i < DICE_SIZE; i++) {
//            if (!(boolean) b_dices[i].getClientProperty("isSaved")) {
//                allTrue = false;
//                break;
//            }
//        }
//        if (allTrue) { // 모두 isSaved 상태 되면 주사위,롤버튼 클릭 X
//            b_roll.setEnabled(false);
//            for (int i = 0; i < DICE_SIZE; i++){
//                b_dices[i].setEnabled(false);
//            }
//            return true;
//        }
//          if(checkRoll==3){ // 3번 다 굴리면 isSaved 아닌 것들 다 isSaved로 바꾸고 위로 올림
//            b_roll.setEnabled(false);
//            for (int i = 0; i < DICE_SIZE; i++) {
//                    b_dices[i].setEnabled(false);
//                if (!(boolean) b_dices[i].getClientProperty("isSaved")) {
//                    b_dices[i].setLocation(b_dices[i].getX(), b_dices[i].getY() - 130);
//                    b_dices[i].putClientProperty("isSaved", true);  // 이동 상태 업데이트
//                    repaint();
//                }
//            }
//            return true; // 턴 종료
//        }
//        else return false;
//        return false;
//    }



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
                //모두 isSaved인지 검사하고 맞으면 턴 종료
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

    private void openRoomWindow(String roomTitle) {
        this.setVisible(false);  // 클라이언트 창 숨기기
        GameGUI();
    }

    // 주석 사이에는 GameGUI 코드

    /// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void send_message_room(String roomTitle) {
        String roomtitle_temp = roomTitle;
        send(new Yacht(t_userID.getText(), Yacht.MODE_TX_STRING_ROOM, roomtitle_temp, t_input_GAME.getText()));
        t_input_GAME.setText("");
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
        String roomtitle_temp;
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
                    roomtitle_temp = roomTitle;
                    send(new Yacht(uid, Yacht.MODE_ENTER_ROOM, roomtitle_temp));
                    break;
                } else if (flag == 0) {
                    roomtitle_temp = roomTitle;
                    send(new Yacht(uid, Yacht.MODE_ENTER_ROOM, roomtitle_temp));
                    break;
                }
            }
        } else {
            // 비밀번호가 필요 없는 경우
            roomtitle_temp = roomTitle;
            send(new Yacht(uid, Yacht.MODE_ENTER_ROOM, roomtitle_temp));
        }
    }

    private void quit_room(String roomTitle) {
        String roomtitle_temp = roomTitle;
        send(new Yacht(t_userID.getText(), Yacht.MODE_QUIT_ROOM, roomtitle_temp));
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
                            if (inMsg.message.equals(roomTitle_copy + " 방이 가득 차서 입장할 수 없습니다.")) {
                                printDisplay(inMsg.message);
                            } else {
                                if (Objects.equals(inMsg.userID, t_userID.getText())) {
                                    System.out.println(inMsg.userID + "님이 " + inMsg.message + " 방에 접속");
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

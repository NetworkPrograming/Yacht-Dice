import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
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
    private JTextField t_input;
    private JButton b_send;

    String roomTitle_copy;

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

    /// ///////////////// ///////////////// ///////////////// ///////////////// /////////////////

    // 여기 이벤트 리스너에서 일반방과 비밀방 각각 버튼을 눌렀을때 접속 이벤트를 구현해야함.
    // 원래 enterroom 함수가 있었지만 코드를 갈아엎는 과정에서 아직 가져오지 못함.
    // 비번방은 비밀번호 입력이 아직 안뜸.
    // 현재는 바로 게임 화면이 뜸
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
                //System.out.println(title);
                ImageIcon secretRoomIcon = new ImageIcon(getClass().getResource("/resources/secret_room.png"));
                Image scaledImage = secretRoomIcon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);

                JButton roomButton;
                if (hasPassword) {
                    password_temp = parts[1];
                    roomButton = new JButton(title, scaledIcon);
                    //printDisplay(title + "?");
                } else {
                    password_temp = null;
                    roomButton = new JButton(title);
                    //printDisplay(title + "!");
                }

                roomButton.setPreferredSize(new Dimension(209, 70)); // 버튼 가로 크기 설정
                roomButton.setMaximumSize(new Dimension(209, 70)); // 버튼 최대 크기 설정하여 가로 중앙 정렬 유지

                String finalPassword_temp = password_temp;
                String finaltitle_temp = title;
                //System.out.println("방 이름" + title);
                roomButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // 접속할 수 있는 환경을 추가
                        //System.out.println("방 이름" + title);
                        //System.out.println("방 이름" + finaltitle_temp);
                        enterRoom(t_userID.getText(), finaltitle_temp, 1, finalPassword_temp);
                        roomTitle_copy = finaltitle_temp;
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
        //System.out.println("????" + roomTitle + "????");
        System.out.println(userIDID + "????");
        String roomtitle_temp;
/// //////////////////////////////////////////////////////////////////////// /////////////////////////////////////////////////////////////////////
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
                    //printDisplay(roomTitle + " 비밀 방에 접속했습니다. 비밀번호 : " + password);
                    roomtitle_temp = roomTitle;
                    //System.out.println(roomtitle_temp + 1);
                    send(new Yacht(uid, Yacht.MODE_ENTER_ROOM, roomtitle_temp));
                    break;
                } else if (flag == 0) {
                    //printDisplay(roomTitle + " 비밀 방에 접속했습니다. 비밀번호 : " + password);
                    roomtitle_temp = roomTitle;
                    //System.out.println(roomtitle_temp + 2);
                    send(new Yacht(uid, Yacht.MODE_ENTER_ROOM, roomtitle_temp));
                    break;
                }
            }
        } else {
            // 비밀번호가 필요 없는 경우
            //printDisplay(roomTitle + "일반 방에 접속했습니다.");
            roomtitle_temp = roomTitle;
            //System.out.println(roomtitle_temp + 3);
            send(new Yacht(uid, Yacht.MODE_ENTER_ROOM, roomtitle_temp));
        }
/// //////////////////////////////////////////////////////////////////////// /////////////////////////////////////////////////////////////////////
    }

    private void openRoomWindow(String roomTitle) {
        // 방 접속 후 바로 GameGUI 창을 띄운다
        //System.out.println(roomTitle + " 방에 접속");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                GameGUI gameGUI = new GameGUI(in, out);
                gameGUI.setVisible(true);  // GameGUI 창 보이기
                setVisible(false);
            }
        });  // GameGUI 창 바로 실행
    }

    /// ///////////////// ///////////////// ///////////////// ///////////////// /////////////////

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
                                //requestRoomList();
                            }
                            break;
                        case Yacht.MODE_LOGOUT:
                            printDisplay(inMsg.message);
                            break;
                        case Yacht.MODE_TX_IMAGE:
                            printDisplay(inMsg.userID + ": " + inMsg.message);
                            //printDisplay(inMsg.image);
                            break;
                        case Yacht.MODE_CREATE_NORMAL_ROOM:
                            if (inMsg.message.equals("일반 방이 이미 존재합니다.")) {
                                if (Objects.equals(inMsg.userID, t_userID.getText())) {
                                    JOptionPane.showMessageDialog(YachtDiceClient.this, inMsg.message, "경고", JOptionPane.WARNING_MESSAGE);
                                }
                                break;
                            } else {
                                // 존재하는 모든 방 다시 출력
                                //requestRoomList();
                                if (Objects.equals(inMsg.userID, t_userID.getText())) {
                                    enterRoom(t_userID.getText(), inMsg.roomTitle, 0, inMsg.passWord);
                                }
                                //enterRoom(t_userID.getText(), inMsg.roomTitle, 0, inMsg.passWord);
                            }
                            break;
                        case Yacht.MODE_CREATE_SECRET_ROOM:
                            if (inMsg.message.equals("비밀 방이 이미 존재합니다.")) {
                                if (Objects.equals(inMsg.userID, t_userID.getText())) {
                                    JOptionPane.showMessageDialog(YachtDiceClient.this, inMsg.message, "경고", JOptionPane.WARNING_MESSAGE);
                                }
                                break;
                            } else {
                                // 존재하는 모든 방 다시 출력
                                //requestRoomList();
                                if (Objects.equals(inMsg.userID, t_userID.getText())) {
                                    enterRoom(t_userID.getText(), inMsg.roomTitle, 0, inMsg.passWord);
                                }
                                //enterRoom(t_userID.getText(), inMsg.roomTitle, 0, inMsg.passWord);
                            }
                            break;
                        case Yacht.MODE_ROOM_LIST:
                            String roomTitlesString = inMsg.message; // 쉼표로 구분된 방 제목 문자열 수신
                            String[] roomTitlesArray = roomTitlesString.split(","); // 쉼표로 분리하여 배열로 변환

                            displayRoomList(roomTitlesArray); // 방 목록 출력
                            break;
                        case Yacht.MODE_ENTER_ROOM:
                            if (Objects.equals(inMsg.userID, t_userID.getText())) {
                                System.out.println(inMsg.userID + "님이 " + inMsg.message + " 방에 접속");
                                openRoomWindow(roomTitle_copy);
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

//    private void requestRoomList() {
//        send(new Yacht(uid, Yacht.MODE_REQUEST_ROOM_LIST, "")); // 서버에 방 목록 요청
//    }

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
        leftPanel.add(createDisplayPanel(), BorderLayout.CENTER);// 기존의 createDisplayPanel 호출
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
                if(t_IP.getText().isEmpty()){
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

        t_input = new JTextField(30);
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
        //t_userID.setText("guest" + getLocalAddr().split("\\.")[3]);
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
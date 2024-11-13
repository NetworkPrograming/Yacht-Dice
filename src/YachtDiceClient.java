import javax.swing.*;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Vector;

public class YachtDiceClient extends JFrame {

    private String serverAddress;
    private int serverPort;
    private JTextField t_userID;

    /////////////////////////////////////////////////////////////////////////////
    //private JTextField t_hostAddr;
    //private JTextField t_portNum;
    private DefaultStyledDocument document;

    private JButton b_connect;
    private JButton b_disconnect;
    private JButton b_exit;

    private JButton b_createRoom;
    /////////////////////////////////////////////////////////////////////////////
    private Socket socket;

    private String uid;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    private Thread receiveThread = null;

    private Vector<JButton> roomButtons = new Vector<>();

    private JPanel roomPanel; // 방 패널을 멤버 변수로 선언

    String roomTitle;
    String password;

    private JTextArea t_display;
    private JTextField t_input;
    private JButton b_send;

    public YachtDiceClient(String serverAddress, int serverPort) {
        super("Yacht Game Client");
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        buildGUI();

        setMinimumSize(new Dimension(800, 700)); // 최소 크기 설정
        setSize(800, 700);
        setLocation(0, 0);
        setResizable(false); // 창 크기 조절 비활성화
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
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

    private JPanel createControlPanel() {
        JPanel p = new JPanel(new GridLayout(0, 3));

        b_connect = new JButton("접속 하기");
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                //YachtDiceClient.this.serverAddress = t_hostAddr.getText();
                //YachtDiceClient.this.serverPort = Integer.parseInt(t_portNum.getText());

                try {
                    connectToServer();
                    sendUserID();
                    // 테스트용
                    //sendMessage();
                } catch (UnknownHostException e1) {
                    printDisplay("서버 주소와 포트번호를 확인하세요 : " + e1.getMessage());
                    return;
                } catch (IOException e1) {
                    printDisplay("서버와의 연결 오류 : " + e1.getMessage());
                    return;
                }

                b_connect.setEnabled(false);
                b_disconnect.setEnabled(true);
                b_exit.setEnabled(false);

                t_userID.setEditable(false);
                //t_hostAddr.setEditable(false);
                //t_portNum.setEditable(false);
                t_input.setEnabled(true);
                b_send.setEnabled(true);

                b_createRoom.setEnabled(true);
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
                //t_hostAddr.setEditable(true);
                //t_portNum.setEditable(true);

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

    private JPanel createInfoPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));

        t_userID = new JTextField(12);
        //t_hostAddr = new JTextField(12);
        //t_portNum = new JTextField(5);

        t_userID.setText("guest" + getLocalAddr().split("\\.")[3]);
        //t_hostAddr.setText(this.serverAddress);
        //t_portNum.setText(String.valueOf(this.serverPort));
        t_userID.setHorizontalAlignment(JTextField.CENTER);

        p.add(new JLabel("아이디:"));
        p.add(t_userID);
        //p.add(new JLabel("서버주소:"));
        //p.add(t_hostAddr);
        //p.add(new JLabel("포트번호:"));
        //p.add(t_portNum);

        return p;
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

    private JPanel textPanel() {
        JPanel p = new JPanel(new BorderLayout());

        t_display = new JTextArea();
        t_display.setLineWrap(true); // 줄 바꿈 설정
        t_display.setWrapStyleWord(true); // 단어 단위로 줄 바꿈
        t_display.setEditable(false);


        JScrollPane scrollPane = new JScrollPane(t_display);
        scrollPane.setPreferredSize(new Dimension(450, 200)); // 스크롤 패널 크기 설정


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
        buttonPanel.add(Box.createVerticalStrut(50)); // 버튼 아래에 10픽셀 여백 추가

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
        roomPanel.setLayout(new BoxLayout(roomPanel, BoxLayout.Y_AXIS)); // BoxLayout으로 설정

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

                if (roomTitle.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "방 이름을 입력하세요.", "방 이름 확인", JOptionPane.WARNING_MESSAGE);
                    return; // 방 이름이 없다면 방 생성하지 않음
                }
                // 비밀번호가 필요한 비밀방일 경우 체크
                if (secretRoomCheckBox.isSelected() && (password.isEmpty())) {
                    JOptionPane.showMessageDialog(dialog, "비밀번호를 입력하세요.", "비밀번호 확인", JOptionPane.WARNING_MESSAGE);
                    return; // 비밀번호가 없다면 방 생성하지 않음
                }

                // 방을 생성하고 바로 접속한다.
                createRoom(roomTitle, password);
                enterRoom(roomTitle, password, 0);
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

    private JLabel createCenteredLabel(String text) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.CENTER); // 가운데 정렬
        return label;
    }

    private void createRoom(String roomTitle, String password) {
        ImageIcon secretRoomIcon = new ImageIcon(getClass().getResource("/resources/secret_room.png"));
        Image scaledImage = secretRoomIcon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        // 클라이언트이므로 방 이름만 출력한다.
        JButton roomButton;
        if (password == null) {
            roomButton = new JButton(roomTitle);
        } else {
            roomButton = new JButton(roomTitle, scaledIcon);
        }

        roomButton.setPreferredSize(new Dimension(231, 70)); // 버튼 가로 크기 설정
        roomButton.setMaximumSize(new Dimension(231, 70)); // 버튼 최대 크기 설정하여 가로 중앙 정렬 유지

        roomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 접속할 수 있는 환경을 추가

                int check = enterRoom(roomTitle, password, 1);

                // 방에 들어가는 로직 추가
                if (check == 0) {
                    printDisplay("아무일도 일어나지 않음");
                } else {
                    printDisplay("방에 들어갔습니다: " + roomTitle);
                }
            }
        });

        roomButtons.add(roomButton);

        // 방 목록 패널에 버튼 추가
        roomPanel.add(roomButton); // 버튼 추가
        roomPanel.add(Box.createVerticalStrut(50)); // 버튼 아래쪽 여백 추가

        roomPanel.revalidate(); // 패널 갱신
        roomPanel.repaint(); // 패널 다시 그리기
    }

    private int enterRoom(String roomTitle, String password, int flag) {
        // flag 가 1이면 다른 클라이언트가 버튼을 통해 들어가는 경우
        if (flag == 1) {
            // 방 접속 로직
            if (password != null) {
                String inputPassword = null;

                while (true) {
                    inputPassword = JOptionPane.showInputDialog(this, "비밀번호를 입력하세요:", "비밀번호 확인", JOptionPane.PLAIN_MESSAGE);

                    // 비밀번호 입력창에서 취소 버튼을 눌렀을 경우
                    if (inputPassword == null) {
                        return 0;
                    }

                    // 비밀번호가 틀린 경우
                    if (!inputPassword.equals(password)) {
                        JOptionPane.showMessageDialog(this, "비밀번호가 틀렸습니다. 다시 입력하세요.", "비밀번호 확인", JOptionPane.WARNING_MESSAGE);
                        continue; // 다시 입력 요청
                    }

                    // 비밀번호가 맞는 경우
                    //printDisplay(roomTitle + " 비밀 방에 접속했습니다. 비밀번호 : " + password);
                    // 방에 접속하면 새로운 창을 띄워서 게임을 시작할 수 있도록 해야 함
                    openRoomWindow(roomTitle);  // 방 접속 후 새로운 창 열기
                    return 1;
                }
            } else {
                // 비밀번호가 필요 없는 경우
                //printDisplay(roomTitle + " 일반 방에 접속했습니다.");
                openRoomWindow(roomTitle);
                return 1;
            }
        } else if (flag == 0) {
            //flag 가 0이면 방을 생성하자마자 방에 들어가는 경우
            if (password != null) {
                // 비밀 방인 경우
                //printDisplay(roomTitle + " 비밀 방에 접속했습니다. 비밀번호 : " + password);
                //sendRoomtitle();
                sendPassword();
                openRoomWindow(roomTitle);
                return 1;
            } else {
                // 비밀 방이 아닌 경우
                //printDisplay(roomTitle + " 일반 방에 접속했습니다.");
                sendRoomtitle();
                openRoomWindow(roomTitle);
                return 1;
            }
        }
        return 1;
    }

    private void openRoomWindow(String roomTitle) {
        // 방 접속 후 바로 GameGUI 창을 띄운다
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                GameGUI gameGUI = new GameGUI(in, out);
                gameGUI.setVisible(true);  // GameGUI 창 보이기
                //setVisible(false);
            }
        });  // GameGUI 창 바로 실행
    }

    private void printDisplay(String msg) {
        // 추후에 로그패널 만들고 거기에 표시되게 수정할것
        //System.out.println(msg);
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
                                disconnect(); // 연결 종료
                            } else {
                                printDisplay(inMsg.message);
                            }
                            break;
                        case Yacht.MODE_LOGOUT:
                            printDisplay(inMsg.message);
                            break;
                        case Yacht.MODE_TX_IMAGE:
                            printDisplay(inMsg.userID + ": " + inMsg.message);
                            //printDisplay(inMsg.image);
                            break;
                        case Yacht.MODE_TX_ROOMNAME:
                            //printDisplay(inMsg.userID + ": " + inMsg.roomTitle);
                            //printDisplay(inMsg.userID + "님이 일반 방에 접속하였습니다. (방 이름 : " + inMsg.roomTitle + ")");
                            //createRoom(inMsg.roomTitle, inMsg.passWord);
                            // repaint를 해야함 여기서

                            ///////////////////////////////////////////////////////////////////////
                            // for문을 돌면서 서버에 있던 방 목록 전체를 받아 돌면서 모두 출력해야할듯
                            roomPanel.revalidate(); // 패널 갱신
                            roomPanel.repaint(); // 패널 다시 그리기
                            ///////////////////////////////////////////////////////////////////////
                            // printDisplay(inMsg.passWord);  null임
                            break;
                        case Yacht.MODE_TX_PASSWORD:
                            //printDisplay(inMsg.userID + "님이 비밀 방에 접속하였습니다. (방 이름 : " + inMsg.roomTitle + ") (비밀번호 :" + inMsg.passWord + ")");
                            //createRoom(inMsg.roomTitle, inMsg.passWord);

                            ///////////////////////////////////////////////////////////////////////
                            // for문을 돌면서 서버에 있던 방 목록 전체를 받아 돌면서 모두 출력해야할듯
                            roomPanel.revalidate(); // 패널 갱신
                            roomPanel.repaint(); // 패널 다시 그리기
                            ///////////////////////////////////////////////////////////////////////
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

    private void disconnect() {
        b_connect.setEnabled(true);
        b_disconnect.setEnabled(false);
        b_exit.setEnabled(true);

        t_userID.setEditable(true);
        //t_hostAddr.setEditable(true);
        //t_portNum.setEditable(true);

        t_input.setEnabled(false);
        b_send.setEnabled(false);
        t_input.setText("");

        b_createRoom.setEnabled(false);
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
        String message = t_input.getText();
        send(new Yacht(uid, Yacht.MODE_TX_STRING, message));
        t_input.setText("");
    }

    private void sendRoomtitle() {
        String roomtitle1 = roomTitle;
        send(new Yacht(uid, Yacht.MODE_TX_ROOMNAME, roomtitle1));
    }

    private void sendPassword() {
        String password1 = password;
        String roomtitle1 = roomTitle;

        send(new Yacht(uid, Yacht.MODE_TX_PASSWORD, roomtitle1, password1));
    }

    private void sendUserID() {
        uid = t_userID.getText();
        //t_input.setText("");
        send(new Yacht(uid, Yacht.MODE_LOGIN, ""));
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;

        new YachtDiceClient(serverAddress, serverPort);
    }
}

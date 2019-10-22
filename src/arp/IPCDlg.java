package arp;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class IPCDlg extends JFrame implements BaseLayer {

    private final JTextArea srcIpAddress;
    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
    BaseLayer UnderLayer;

    private static LayerManager m_LayerMgr = new LayerManager();


    private JTextField ChattingWrite;

    Container contentPane;

    JTextArea ChattingArea;
    static JTextArea fileDisplay;
    JTextArea srcAddress;
    JTextArea dstAddress;

    JLabel lblsrc;
    JLabel lbldst;
    JLabel lblnic;

    JButton Setting_Button;
    JButton Chat_send_Button;
    JButton transperButton;
    JButton fileSelectButoon;

    JProgressBar jProgressBar;

    static EthernetLayer ethernetLayer = null;
    static NILayer niLayer = null;
    static ChatAppLayer chatAppLayer = null;
    static FileAppLayer fileAppLayer = null;
    // field for layer
    static TCPLayer tcpLayer;
    static IPLayer ipLayer;
    static ARPLayer arpLayer;
    static AppLayer app;

    static JComboBox<String> NICComboBox;
    int adapterNumber = 0;

    File selectedFile;
    byte[] bytesArray = null;

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        IPCDlg ipcDlg;
        app = new AppLayer("App");
        ipcDlg = new IPCDlg("GUI");

        chatAppLayer = new ChatAppLayer("Chat");
        m_LayerMgr.AddLayer(chatAppLayer);
        fileAppLayer = new FileAppLayer("File");
        m_LayerMgr.AddLayer(fileAppLayer);

        try {
            m_LayerMgr.AddLayer(ipcDlg);

            tcpLayer = new TCPLayer("Tcp");
            m_LayerMgr.AddLayer(tcpLayer);

            ipLayer = new IPLayer("Ip");
            m_LayerMgr.AddLayer(ipLayer);

            arpLayer = new ARPLayer("Arp");
            m_LayerMgr.AddLayer(arpLayer);

            ethernetLayer = new EthernetLayer("Ethernet");
            m_LayerMgr.AddLayer(ethernetLayer);

            niLayer = new NILayer("NI");
            m_LayerMgr.AddLayer(niLayer);

            m_LayerMgr.ConnectLayers(" NI ( *Ethernet ( +Ip ( *Tcp ( *Chat ( *GUI )  *File ( *GUI  ) ) ) ) ");
            m_LayerMgr.GetLayer("Ip").SetUnderLayer(m_LayerMgr.GetLayer("Arp"));
            m_LayerMgr.GetLayer("Ethernet").SetUpperUnderLayer(m_LayerMgr.GetLayer("Arp"));

            // ip레이어에 이더넷레이어 설정
            ipLayer.setEthernetLayer(ethernetLayer);

            // arp레이어에 app레이어 설정
            arpLayer.setAppLayer(app);

            app.setTcpLayer(tcpLayer);
            app.setIpLayer(ipLayer);
            app.setArpLayer(arpLayer);
            app.setEthernetLayer(ethernetLayer);
            app.setNiLayer(niLayer);

            ipLayer.setSrcIP(InetAddress.getLocalHost().getAddress());
            arpLayer.setSrcIp(InetAddress.getLocalHost().getAddress());

            InetAddress presentAddr = InetAddress.getLocalHost();
            NetworkInterface net = NetworkInterface.getByInetAddress(presentAddr);

            byte[] macAddressBytes = net.getHardwareAddress();
            arpLayer.setSrcMac(niLayer.getMacAddress());
            ethernetLayer.setSrcAddr(niLayer.getMacAddress());


            ethernetLayer.SetUpperLayer(ipLayer);

            // 어떤 어댑터를 사용할지 결정한다.
            // 디버깅을 통해 adapter list 를 이용하여 설정한다.
            // 링크가 다 연결된 후 언더레이어 접근할수 있어서 이 때 접근해준다.
            ipcDlg.addAdapterList();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public IPCDlg(String pName) {
        pLayerName = pName;

        setTitle("IPC");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(250, 250, 644, 425);
        contentPane = new JPanel();
        ((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JPanel chattingPanel = new JPanel();// chatting panel
        chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "chatting",
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        chattingPanel.setBounds(10, 5, 360, 276);
        contentPane.add(chattingPanel);
        chattingPanel.setLayout(null);

        JPanel chattingEditorPanel = new JPanel();// chatting write panel
        chattingEditorPanel.setBounds(10, 15, 340, 210);
        chattingPanel.add(chattingEditorPanel);
        chattingEditorPanel.setLayout(null);

        ChattingArea = new JTextArea();
        ChattingArea.setEditable(false);
        ChattingArea.setBounds(0, 0, 340, 210);
        chattingEditorPanel.add(ChattingArea);// chatting edit

        JPanel chattingInputPanel = new JPanel();// chatting write panel
        chattingInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        chattingInputPanel.setBounds(10, 230, 250, 20);
        chattingPanel.add(chattingInputPanel);
        chattingInputPanel.setLayout(null);


        JPanel transperPanel = new JPanel();// chatting write panel
        transperPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "파일전송",
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        transperPanel.setBounds(10, 290, 360, 80);
        contentPane.add(transperPanel);
        transperPanel.setLayout(null);


        fileSelectButoon = new JButton("파일전송");
        fileSelectButoon.setBounds(280, 20, 70, 20);
        fileSelectButoon.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser("./");
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                fileDisplay.setText(selectedFile.getAbsolutePath());
            } else {
                System.out.println("No select file");
            }

        });
        fileSelectButoon.setEnabled(false);
        transperPanel.add(fileSelectButoon);// chatting send button

        JPanel fileDisplayPanel = new JPanel();
        fileDisplayPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        fileDisplayPanel.setBounds(10, 20, 260, 20);
        transperPanel.add(fileDisplayPanel);
        fileDisplayPanel.setLayout(null);

        fileDisplay = new JTextArea();
        fileDisplay.setBounds(2, 2, 260, 20);
        fileDisplayPanel.add(fileDisplay);
        fileDisplay.setEnabled(false);

        transperButton = new JButton("전송");
        transperButton.setBounds(280, 50, 70, 20);
        transperButton.addActionListener(new FileTransferButton());
        transperPanel.add(transperButton);// chatting send button
        transperButton.setEnabled(false);

        jProgressBar = new JProgressBar();
        jProgressBar.setBounds(10, 50, 260, 20);
        transperPanel.add(jProgressBar);


        ChattingWrite = new JTextField();
        ChattingWrite.setBounds(2, 2, 250, 20);// 249
        chattingInputPanel.add(ChattingWrite);
        ChattingWrite.setColumns(10);// writing area

        JPanel settingPanel = new JPanel();
        settingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "setting",
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        settingPanel.setBounds(380, 5, 236, 371);
        contentPane.add(settingPanel);
        settingPanel.setLayout(null);

        JPanel sourceAddressPanel = new JPanel();
        sourceAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        sourceAddressPanel.setBounds(10, 96, 170, 20);
        settingPanel.add(sourceAddressPanel);
        sourceAddressPanel.setLayout(null);


        lblnic = new JLabel("NIC 선택");
        lblnic.setBounds(10, 15, 170, 20);
        settingPanel.add(lblnic);

        NICComboBox = new JComboBox<String>();
        NICComboBox.setBounds(10, 40, 170, 20);
        NICComboBox.addItemListener(new ItemListener() {
            // 마우스 클릭하면 반응하는 리스너이다.
            @Override
            public void itemStateChanged(ItemEvent e) {
                JComboBox cb = (JComboBox) e.getSource();

                Object item = e.getItem();

                // 어떤 이벤트인지 확인한다.
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // 골라진거면 어뎁터를 변경한다.
                    adapterNumber = NICComboBox.getSelectedIndex();
                    niLayer.SetAdapterNumber(adapterNumber);
                    try {
                        // 에외처리를 해주어 어뎁터를 변경한다.
                        byte sourceMac[] = niLayer.m_pAdapterList.get(adapterNumber).getHardwareAddress();
                        ethernetLayer.setSrcAddr(sourceMac);
                        srcAddress.setText(macToString(sourceMac));
                    } catch (IOException except) {
                        // 오류 발생을 알려준다.
                        System.out.println(except.getMessage());
                    }
                }
            }
        });
        settingPanel.add(NICComboBox);

        JButton btnCacheTable = new JButton("Cache Table");
        btnCacheTable.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                app.setVisible(true);
            }
        });

        btnCacheTable.setBounds(59, 301, 121, 20);
        settingPanel.add(btnCacheTable);

        lblsrc = new JLabel("Source Mac Address");
        lblsrc.setBounds(10, 75, 170, 20);
        settingPanel.add(lblsrc);

        srcAddress = new JTextArea();
        srcAddress.setBounds(2, 2, 170, 20);
        sourceAddressPanel.add(srcAddress);// src address

        JPanel destinationAddressPanel = new JPanel();
        destinationAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        destinationAddressPanel.setBounds(10, 212, 170, 20);
        settingPanel.add(destinationAddressPanel);
        destinationAddressPanel.setLayout(null);

        JLabel lbIpSrc = new JLabel("Source IP Address");
        lbIpSrc.setBounds(10, 140, 190, 20);
        settingPanel.add(lbIpSrc);

        srcIpAddress = new JTextArea();
        srcIpAddress.setBounds(10, 160, 170, 20);
        try {
            srcIpAddress.setText(InetAddress.getLocalHost().getHostAddress());
            srcIpAddress.setEditable(false);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        settingPanel.add(srcIpAddress);


        lbldst = new JLabel("Destination IP Address");
        lbldst.setBounds(10, 187, 190, 20);
        settingPanel.add(lbldst);

        dstAddress = new JTextArea();
        dstAddress.setBounds(2, 2, 170, 20);
        destinationAddressPanel.add(dstAddress);// dst address

        Setting_Button = new JButton("Setting");// setting
        Setting_Button.setBounds(80, 270, 100, 20);
        Setting_Button.addActionListener(new setAddressListener());
        settingPanel.add(Setting_Button);// setting

        Chat_send_Button = new JButton("Send");
        Chat_send_Button.setBounds(270, 230, 80, 20);
        Chat_send_Button.addActionListener(new setAddressListener());
        chattingPanel.add(Chat_send_Button);// chatting send button
        Chat_send_Button.setEnabled(false);

        setVisible(true);
    }

    public void setProgressValue(int setValue){
        jProgressBar.setStringPainted(true);
        jProgressBar.setValue(setValue);
    }

    // 바이트 배열되 되어있는 맥주소를 문자열로 바꿔둔다.
    public String macToString(byte[] mac) {
        StringBuilder buf = new StringBuilder();

        // 바이트를 한개씩 읽어와서 문자열로 변환해준다.
        for (byte b : mac) {
            if (buf.length() != 0) {
                buf.append(':');
            }
            if (b >= 0 && b < 16) {
                buf.append('0');
            }

            buf.append(Integer.toHexString((b < 0) ? b + 256 : b).toUpperCase());
        }
        return buf.toString();
    }

    private byte[] stringToMac(String addr) {

        byte[] ret = new byte[6];

        StringTokenizer tokens = new StringTokenizer(addr, "-");

        for (int i = 0; tokens.hasMoreElements(); i++) {

            String temp = tokens.nextToken();

            try {
                ret[i] = Byte.parseByte(temp, 16);
            } catch (NumberFormatException e) {
                int minus = (Integer.parseInt(temp, 16)) - 256;
                ret[i] = (byte) (minus);
            }
        }

        return ret;
    }
    // 각 변수에 레이어들을 담고 어뎁터리스트를 추가해준다.
    public void addAdapterList() {
        // 어뎁터 리스트 개수만큼 JcomboBox에 추가해준다.
        for (int i = 0; i < niLayer.m_pAdapterList.size(); i++) {
            NICComboBox.addItem(niLayer.m_pAdapterList.get(i).getDescription());
        }
    }

    public class SendFile extends Thread{
        public void run(){
            fileAppLayer.Send(selectedFile.getAbsolutePath());
        }
    }


    public class SendChat extends  Thread{
        byte input[];
        public void run(){
            chatAppLayer.Send(input, input.length);
        }

        void setByte(byte input[]){
            this.input = input;
        }
    }

    public class FileTransferButton implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            File file = selectedFile;
            new SendFile().start();
        }

    }


    class setAddressListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // 초기화 바이트 배열이다.
            byte resetMac[] = {-1, -1, -1, -1, -1, -1};

            if (e.getActionCommand() == "Setting") {
                // setting 버튼이 눌리면 수정을 금지하고 mac 주소를 설정한다.
                String dstIp = dstAddress.getText();
                try {
                    ipLayer.setDestIP(InetAddress.getByName(dstIp.trim()).getAddress());
                } catch (UnknownHostException e1) {
                    e1.printStackTrace();
                }
                dstAddress.setEnabled(false);
                Setting_Button.setText("Reset");
                fileSelectButoon.setEnabled(true);
                transperButton.setEnabled(true);
                Chat_send_Button.setEnabled(true);
            } else if (e.getActionCommand() == "Reset") {
                // reset 버튼이 눌리면 수정을 허용하고 맥주소를 초기화한다.
                dstAddress.setEnabled(true);
                ethernetLayer.setSrcAddr(resetMac);
                dstAddress.setText("");
                Setting_Button.setText("Setting");
                fileSelectButoon.setEnabled(false);
                transperButton.setEnabled(false);
                Chat_send_Button.setEnabled(false);

            } else if (e.getActionCommand() == "Send") {
                // send 버튼이 눌리면 상위 레이어에 받은 데이터를 전송한다.
                String inputString = ChattingWrite.getText();
                byte inputByte[] = inputString.getBytes();

                ChattingArea.append("[SEND] : " + inputString + "\n");

                SendChat sendChat = new SendChat();
                sendChat.setByte(inputByte);
                sendChat.start();

                ChattingWrite.setText(" ");
            }

        }
    }



    public boolean Receive(byte[] input) {

        String inputString = new String(input);

        ChattingArea.append("[RECV] : " + inputString + "\n");

        return true;
    }

    @Override
    public void SetUnderLayer(BaseLayer pUnderLayer) {
        // TODO Auto-generated method stub
        if (pUnderLayer == null)
            return;
        this.p_UnderLayer = pUnderLayer;
    }

    @Override
    public void SetUpperLayer(BaseLayer pUpperLayer) {
        // TODO Auto-generated method stub
        if (pUpperLayer == null)
            return;
        this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
        // nUpperLayerCount++;
    }

    @Override
    public String GetLayerName() {
        // TODO Auto-generated method stub
        return pLayerName;
    }

    @Override
    public BaseLayer GetUnderLayer() {
        // TODO Auto-generated method stub
        if (p_UnderLayer == null)
            return null;
        return p_UnderLayer;
    }

    @Override
    public BaseLayer GetUpperLayer(int nindex) {
        // TODO Auto-generated method stub
        if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
            return null;
        return p_aUpperLayer.get(nindex);
    }

    @Override
    public void SetUpperUnderLayer(BaseLayer pUULayer) {
        this.SetUpperLayer(pUULayer);
        pUULayer.SetUnderLayer(this);

    }

}



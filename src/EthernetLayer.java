import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class EthernetLayer implements BaseLayer {

    public class EthernetFrame {

        @Override
        public String toString(){

            StringBuilder res = new StringBuilder();

            for (int i = 0; i < srcAddr.addr.length; i++){
                if(srcAddr.addr[i] < 0){
                    res.append(Integer.toString((int) srcAddr.addr[i] + 256, 16));
                }
                else {
                    res.append(Integer.toString(srcAddr.addr[i], 16));
                }
                if(i != srcAddr.addr.length - 1) res.append(":");
            }

            return res.toString();
        }

        private class EthernetAddr {
            public byte[] addr = new byte[6];

            public EthernetAddr() {
                this.addr[0] = (byte) 0x00;
                this.addr[1] = (byte) 0x00;
                this.addr[2] = (byte) 0x00;
                this.addr[3] = (byte) 0x00;
                this.addr[4] = (byte) 0x00;
                this.addr[5] = (byte) 0x00;
            }
        }

        EthernetAddr dstAddr;
        EthernetAddr srcAddr;

        byte[] type;

        private EthernetFrame() {
            this.dstAddr = new EthernetAddr();
            this.srcAddr = new EthernetAddr();
            this.type	 = new byte[2];
        }
    }

    public int nUpperLayerCount = 0;

    public String pLayerName = null;

    public BaseLayer p_UnderLayer = null;

    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

    public EthernetFrame m_Ethernet_Header = new EthernetFrame();

    public EthernetLayer(String name) {
        pLayerName = name;
        parsingSrcMACAddress(getMacAddr());
    }

    public synchronized boolean Receive(byte[] input) {

        int opcode = byte2ToInt(input[14 + 6], input[14 + 7]);

        if ((isRightPacket(input) == false) || isRightAddress(input) == false) {
            return false;
        }

        if (opcode == 1){
            input = removeAddressHeader(input, input.length);
            // ARP Layer로 전송
            GetUpperLayer(0).Receive(input);
        }
        else if (opcode == 2){
            input = removeAddressHeader(input, input.length);
            // ARP Layer로 전송
            GetUpperLayer(0).Receive(input);
        }
        else {
            input = removeAddressHeader(input, input.length);
            // 일반적인 프레임의 경우 IP Layer로 전송
            GetUpperLayer(1).Receive(input);
        }

        return true;
    }

    public boolean Send(byte[] input, int length) {

        int opcode = byte2ToInt(input[6], input[7]);

        byte[] temp = null;

        if (opcode == 1) {
            // opcode가 1인 경우 ARP 브로드캐스팅이므로, 목적지 주소를 전부 -1로 셋팅
            temp = addressing(input, input.length,
                    m_Ethernet_Header.srcAddr.addr,
                    new byte[]{ -1, -1, -1, -1, -1, -1 });
        }
        else {
            // opcode가 2인 경우이거나 그 이외의 경우 ARP 헤더의 셋팅된 주소를 가져온다.
            temp = addressing(input, input.length,
                    new byte[]{ input[ 8], input[ 9], input[10], input[11], input[12], input[13] },
                    new byte[]{ input[18], input[19], input[20], input[21], input[22], input[23] });
        }

        if (p_UnderLayer.Send(temp, length + 14) == false) {
            return false;
        }

        return true;
    }

    private byte[] removeAddressHeader(byte[] input, int length) {

        byte[] temp = new byte[length - 14];

        for (int i = 0; i < length - 14; i++) {
            temp[i] = input[i + 14];
        }

        return temp;
    }

    private byte[] addressing(byte[] input, int length, byte[] src_address, byte[] dst_address) {

        byte[] buf = new byte[length + 14];

        buf[0]  = dst_address[0];
        buf[1]  = dst_address[1];
        buf[2]  = dst_address[2];
        buf[3]  = dst_address[3];
        buf[4]  = dst_address[4];
        buf[5]  = dst_address[5];

        buf[6]  = src_address[0];
        buf[7]  = src_address[1];
        buf[8]  = src_address[2];
        buf[9]  = src_address[3];
        buf[10] = src_address[4];
        buf[11] = src_address[5];

        buf[12] = 0x08;
        buf[13] = 0x06;

        for (int i = 0; i < length; i++)
            buf[14 + i] = input[i];

        return buf;
    }

    private void parsingSrcMACAddress(String addr) {
        StringTokenizer tokens = new StringTokenizer(addr, "-");

        for (int i = 0; tokens.hasMoreElements(); i++) {

            String temp = tokens.nextToken();

            try {
                m_Ethernet_Header.srcAddr.addr[i] = Byte.parseByte(temp, 16);
            } catch (NumberFormatException e) {
                int minus = (Integer.parseInt(temp, 16)) - 256;
                m_Ethernet_Header.srcAddr.addr[i] = (byte) (minus);
            }
        }
    }

    private String getMacAddr() {

        try {
            InetAddress presentAddr = InetAddress.getLocalHost();

            NetworkInterface net = NetworkInterface.getByInetAddress(presentAddr);

            byte[] macAddressBytes = net.getHardwareAddress();

            StringBuilder macAddressStr = null;

            if (macAddressBytes != null) {

                macAddressStr = new StringBuilder();

                for (int i = 0; i < macAddressBytes.length; i++) {

                    macAddressStr.append(String.format("%02X", macAddressBytes[i]));
                    if (i < macAddressBytes.length - 1) {
                        macAddressStr.append("-");
                    }

                }
            }
            return macAddressStr.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isRightPacket(byte[] input) {
        int protocol = byte2ToInt(input[14 + 2], input[14 + 3]);
        int HWtype   = byte2ToInt(input[14 + 0], input[14 + 1]);

        if (protocol != 0x0800 || HWtype != 0x0001) {
            return false;
        }
        return true;
    }

    private boolean isRightAddress(byte[] input) {

        int ffCount = 0;
        int fitCount = 0;

        for (int i = 0; i < 6; i++) {

            // 오른쪽 조건은 루프백을 드롭하기 위한 검사
            if (input[i] == -1 && (input[i + 6] != m_Ethernet_Header.srcAddr.addr[i])) {
                ffCount++;
            }

            if (input[i] == m_Ethernet_Header.srcAddr.addr[i]) {
                fitCount++;
            }
        }

        if (ffCount == 6 || fitCount == 6) {
            return true;
        }
        return false;
    }

    private static int byte2ToInt(byte big_byte, byte little_byte) {

        int little_int = (int) little_byte;
        int big_int = (int) big_byte;

        if (little_int < 0) {
            little_int += 256;
        }

        return (little_int + (big_int << 8));
    }

    @Override
    public String GetLayerName() {
        return pLayerName;
    }

    @Override
    public BaseLayer GetUnderLayer() {
        if (p_UnderLayer == null)
            return null;
        return p_UnderLayer;
    }

    @Override
    public BaseLayer GetUpperLayer(int nindex) {
        if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
            return null;
        return p_aUpperLayer.get(nindex);
    }

    @Override
    public void SetUnderLayer(BaseLayer pUnderLayer) {
        if (pUnderLayer == null)
            return;
        this.p_UnderLayer = pUnderLayer;
    }

    @Override
    public void SetUpperLayer(BaseLayer pUpperLayer) {
        if (pUpperLayer == null)
            return;

        this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
    }

    @Override
    public void SetUpperUnderLayer(BaseLayer pUULayer) {
        this.SetUpperLayer(pUULayer);
        pUULayer.SetUnderLayer(this);
    }
}

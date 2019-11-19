package arp;

public class RoutingTable {

    public class RoutingRow {
        byte[] destination;
        byte[] netMask;
        byte[] gateWay;
        boolean[] flags;
        String interfaceName;
        int metric;

        public RoutingRow() {
            destination = new byte[4];
            netMask = new byte[6];
            gateWay = new byte[4];
            flags = new boolean[3];
        }
    }

}

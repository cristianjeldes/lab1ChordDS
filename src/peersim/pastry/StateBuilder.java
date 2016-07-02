package peersim.pastry;

import peersim.config.*;
import peersim.core.*;
import java.util.Comparator;
import peersim.transport.Transport;

/**
 * <p>
 * Title: MSPASTRY</p>
 *
 * <p>
 * Description: MsPastry implementation for PeerSim</p>
 *
 * <p>
 * Copyright: Copyright (c) 2007</p>
 *
 * <p>
 * Company: The Pastry Group</p>
 *
 * @author Elisa Bisoffi, Manuel Cortella
 * @version 1.0
 */
public class StateBuilder implements peersim.core.Control {

    private static final String PAR_PROT = "protocol";
    private static final String PAR_TRANSPORT = "transport";

    private String prefix;
    private int mspastryid;
    private int transportid;

    public StateBuilder(String prefix) {
        this.prefix = prefix;
        mspastryid = Configuration.getPid(this.prefix + "." + PAR_PROT);
        transportid = Configuration.getPid(this.prefix + "." + PAR_TRANSPORT);
    }

    public final MSPastryProtocol get(int i) {
        return ((MSPastryProtocol) (Network.get(i)).getProtocol(mspastryid));
    }

    public final Node getNode(int i) {
        return Network.get(i);
    }

    public final Transport getTr(int i) {
        return ((Transport) (Network.get(i)).getProtocol(transportid));
    }

    public static void o(Object o) {
        System.out.println(o);
    }

    public static void x(Object o) {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean execute() {
        /* Sort the network by nodeId (Ascending) */
        Network.sort(new Comparator() {
            public int compare(Object o1, Object o2) {
                Node n1 = (Node) o1;
                Node n2 = (Node) o2;
                MSPastryProtocol p1 = (MSPastryProtocol) (n1.getProtocol(mspastryid));
                MSPastryProtocol p2 = (MSPastryProtocol) (n2.getProtocol(mspastryid));
                return Util.put0(p1.nodeId).compareTo(Util.put0(p2.nodeId));
            }
            public boolean equals(Object obj) {
                return compare(this, obj) == 0;
            }
        });

        //BUILD ROUTING TABLES
        this.buildRoutingTables();
        //BUILD LEAFSETS

        this.buildLeafsets();

//        FOR DEBUG
//        for (int n = 0; n < Network.size(); n++) {
//           System.out.println("NODO: " + RoutingTable.truncateNodeId(((MSPastryProtocol) get(n)).nodeId));
//            System.out.println(((MSPastryProtocol) get(n)).routingTable.toString());
//        }
//        FOR DEBUG
//        for (int n = 0; n < Network.size(); n++) {
//            System.out.println("NODO: " + RoutingTable.truncateNodeId(((MSPastryProtocol) get(n)).nodeId));
//            System.out.println(get(n).routingTable.toString());
//            System.out.println(((MSPastryProtocol) get(n)).leafSet.toString());
//        }
        return false;

    } //end execute()

    private void buildRoutingTables() {
        int sz = Network.size();
        int begin = 0;
        int end = 0;
	//	int rappresentanti[] = new int[MSPastryCommonConfig.BASE];
        // int rappresentanti[][] = new int[sz][MSPastryCommonConfig.BASE];

        //ROUTING TABLES
        // CADA NODE LLENA SU ROW 0 CON ENTRADAS ALEATORIAS
        for (int n = 0; n < sz; n++) {
            begin = 0;
            end = 0;
            MSPastryProtocol node = get(n);

            for (int i = 0; i < MSPastryCommonConfig.BASE; i++) {
                if (begin >= Network.size()) {
                    break;
                }

                char curChar = Util.DIGITS[i];

                if (!Util.startsWith(get(begin).nodeId, curChar)) {
                    continue;
                }

                end = begin; //aggiunta successiva

                while (((end < Network.size())) && (Util.startsWith(get(end).nodeId, curChar))) {
                    end++;
                }

                int randomIndex = begin + CommonState.r.nextInt(end - begin);

                //ROW 0 Assigned a random entry
                node.routingTable.table[0][Util.charToIndex(curChar)] = get(randomIndex).nodeId;

                //ALmacena el indice de los representantes por char
                //	rappresentanti[Util.charToIndex(curChar)] = randomIndex;
                if (Util.hasDigitAt(node.nodeId, 0, curChar)) // llamar funcion que llene (nivel a llenar, comienzo, fin, nodo)
                {
                    fillLevel(1, begin, end, node);
                }

                begin = end;

            }
            //FOR DEBUG
            //System.out.println("NODO: " + RoutingTable.truncateNodeId(node.nodeId));
            //System.out.println(node.routingTable.toString());
        }

    }

    public void fillLevel(int curLevel, int begin, int end, MSPastryProtocol node) {

        if (curLevel >= 10) //TENIA ORIGINAL 10
        {
            return;
        }

        int end2 = begin;
        int begin2 = begin;

        for (int i = 0; i < MSPastryCommonConfig.BASE; i++) {
            if (begin2 >= end) {
                break;
            }

            char curChar = Util.DIGITS[i];

            if (!Util.hasDigitAt(get(begin2).nodeId, curLevel, curChar)) {
                continue;
            }

            while (((end2 < end)) && (Util.hasDigitAt(get(end2).nodeId, curLevel, curChar))) {
                end2++;
            }

            if (end2 == begin2) {
                return;
            }

            int randomIndex = begin2 + CommonState.r.nextInt(end2 - begin2);

            //ROW 0 Assigned a random entry
            node.routingTable.table[curLevel][Util.charToIndex(curChar)] = get(randomIndex).nodeId;

            if (Util.hasDigitAt(node.nodeId, curLevel, curChar)) // llamar funcion que llene (nivel a llenar, comienzo, fin, nodo)
            {
                fillLevel(curLevel + 1, begin2, end2, node);
            }

            begin2 = end2;
        }

    }

    private void buildLeafsets() {
        int sz = Network.size();
        //CASO DE LOS EXTREMOS PARTIENDO DE CERO - CASO COUNTERWISE
        for (int k = 0; k < MSPastryCommonConfig.L / 2; k++) {
            //System.out.println("Critico Left: " + k); 
            MSPastryProtocol n = get(k);
            for (int s = k - 1; s >= 0; s--) {
                n.leafSet.pushToLeft(get(s).nodeId);
                n.leafSet.putNodeLeft(getNode(s));
            }
            for (int s = sz - 1; s >= sz - MSPastryCommonConfig.L / 2 + k; s--) {
                n.leafSet.pushToLeft(get(s).nodeId);
                //System.out.println("NODO: " +  n.leafSet.nodesL.toString());
                n.leafSet.putNodeLeft(getNode(s));
            }
        }

        //CASO DE LOS EXTREMOS PARTIENDO DE 2^128 - CASO CLOCKWISE
        for (int k = sz - 1; k >= sz - MSPastryCommonConfig.L / 2; k--) {
            MSPastryProtocol n = get(k);
            for (int s = k + 1; s < sz; s++) {
                n.leafSet.pushToRight(get(s).nodeId);
                n.leafSet.putNodeRight(getNode(s));
            }
            for (int s = 0; s <= MSPastryCommonConfig.L / 2 - (sz - k); s++) {
                n.leafSet.pushToRight(get(s).nodeId);
                n.leafSet.putNodeRight(getNode(s));
            }
        }

        // CASO GENERAL RIGHT
        for (int k = 0; k < sz - MSPastryCommonConfig.L / 2; k++) {
            MSPastryProtocol n = get(k);

            for (int s = k; s <= k + MSPastryCommonConfig.L / 2; s++) {
                n.leafSet.pushToRight(get(s).nodeId);
                n.leafSet.putNodeRight(getNode(s));
            }
        }

        // CASO GENERAL LEFT
        for (int k = MSPastryCommonConfig.L / 2; k < sz; k++) {

            MSPastryProtocol n = get(k);

            for (int s = k - 1; s > k - 1 - MSPastryCommonConfig.L / 2; s--) {

                n.leafSet.pushToLeft(get(s).nodeId);
                n.leafSet.putNodeLeft(getNode(s));
            }
        }

        //UNCOMMENT FOR DEBUG 
        //printLeafSets();  
    }

    //ONLY FOR DEBUG
    private void printLeafSets() {
        int sz = Network.size();
        System.out.println("------------- START ----------------");
        for (int l = 0; l < sz; l++) {
            MSPastryProtocol n = get(l);
            System.out.println(n.leafSet.toString());
        }
        System.out.println("------------- END ----------------");
    }

}

package peersim.pastry;

/**
 *
 *
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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import peersim.DistributedFileSystem.DistributedFileSystem;
import peersim.Traffic.Query;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;
import peersim.transport.UnreliableTransport;

//__________________________________________________________________________________________________
public class MSPastryProtocol implements Cloneable, EDProtocol {

    

    //______________________________________________________________________________________________
    /**
     * Event Handler container for managing the receiving of a message
     *
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
    public static interface Listener {

        /**
         * This method is called every time a message is received
         *
         * @param m Message
         */
        public void receive(Query q);
    }

    //______________________________________________________________________________________________
    private static final String PAR_TRANSPORT = "transport";
    private static String prefix = null;
    private UnreliableTransport transport;
    private int tid;
    private int mspastryid;
    private boolean cleaningScheduled = true;
    private static boolean _ALREADY_INSTALLED = false;
    private List<BigInteger> myKeys;
    private String size;
    //______________________________________________________________________________________________
    /**
     * nodeId of this pastry node
     */
    public BigInteger nodeId;
    public RoutingTable routingTable;
    public LeafSet leafSet;

    /**
     * Listener assingned to the receiving of a message. If null it is not
     * called
     */
    private DistributedFileSystem listener;

    //______________________________________________________________________________________________
    /**
     * allows to change/clear the listener
     *
     * @param l Listener
     */
    public void setListener(DistributedFileSystem l) {
        listener = l;
    }

    public void setMyApp(DistributedFileSystem l) {
        listener = l;
    }

    public DistributedFileSystem getApp() {
        return listener;
    }

    //______________________________________________________________________________________________
    /**
     * Replicate this object by returning an identical copy. it put the eye on
     * the fact that only the peersim initializer call this method and we
     * expects to replicate every time a non-initialized table. Thus the method
     * clone() do not fill any particular field;
     *
     * @return Object
     */
    public Object clone() {
        MSPastryProtocol dolly = new MSPastryProtocol(MSPastryProtocol.prefix);
        dolly.routingTable = (RoutingTable) this.routingTable.clone();
        dolly.leafSet = (LeafSet) this.leafSet.clone();
        return dolly;
    }

    //______________________________________________________________________________________________
    /**
     * Used only by the initializer when creating the prototype Every other
     * instance call CLONE to create the new object. clone could not use this
     * constructor, preferring a more quick constructor
     *
     * @param prefix String
     */
    public MSPastryProtocol(String prefix) {
        this.nodeId = null;              // empty nodeId
        MSPastryProtocol.prefix = prefix;

        _init();

        //this.load = 0;
        //this.query_debit = 0;
        routingTable = new RoutingTable(MSPastryCommonConfig.BITS / MSPastryCommonConfig.B, (int) Math.pow(2, MSPastryCommonConfig.B));
        myKeys = new ArrayList<>();
        //LEAFSET DE 16
        leafSet = new LeafSet(BigInteger.ZERO, MSPastryCommonConfig.L);
        tid = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
        this.mspastryid = Configuration.lookupPid(prefix.substring(prefix.lastIndexOf('.') + 1));
        this.size = Configuration.getString("SIZE");
    }

    //______________________________________________________________________________________________
    /**
     * This subrouting is called only once and allow to inizialize the internal
     * state of the MSPastreyProtocol. Every node shares the same configuration,
     * so it is sufficient caling ont this this sub in order to set up the
     * configuration
     */
    private void _init() {
        if (_ALREADY_INSTALLED) {
            return;
        }

        int b = 0, l = 0, base = 0;
        final String PAR_B = "B";
        final String PAR_L = "L";

        b = Configuration.getInt(prefix + "." + PAR_B, 4);
        l = Configuration.getInt(prefix + "." + PAR_L, MSPastryCommonConfig.BITS / b);
        base = Util.pow2(b);

        MSPastryCommonConfig.B = b;
        MSPastryCommonConfig.L = l;
        MSPastryCommonConfig.BASE = base;

        e(MSPastryCommonConfig.info() + "\n");
        _ALREADY_INSTALLED = true;
    }

    //______________________________________________________________________________________________
    /**
     * called when a Lookup message is ready to be passed through the
     * upper/application level, by calling the "message received" event handler
     * (listener). It also provide some statistical update
     *
     * @param m Message
     */
    private void deliver(Message m) {

        System.out.println("+ Path + " + RoutingTable.truncateNodeId(m.key));
        ((Query) m.body).copyPath(m.tracks.clone());
        ((Query) m.body).copyHops(m.nrHops);
        ((Query) m.body).copyTimestamp(m.timestamp);

        if (listener != null) {
            listener.receive(m.body);
        }

    }

    //______________________________________________________________________________________________
    /**
     * given one nodeId, it search through the network its node reference, by
     * performing binary serach (we concern about the ordering of the network).
     *
     * @param searchNodeId BigInteger
     * @return Node
     */
    private Node nodeIdtoNode(BigInteger searchNodeId) {
        if (searchNodeId == null) {
            return null;
        }

        int inf = 0;
        int sup = Network.size() - 1;
        int m;

        while (inf <= sup) {
            m = (inf + sup) / 2;

            BigInteger mId = ((MSPastryProtocol) Network.get(m).getProtocol(mspastryid)).nodeId;

            if (mId.equals(searchNodeId)) {
                return Network.get(m);
            }

            if (mId.compareTo(searchNodeId) < 0) {
                inf = m + 1;
            } else {
                sup = m - 1;
            }
        }
        BigInteger mId;
        for (int i = Network.size() - 1; i >= 0; i--) {
            mId = ((MSPastryProtocol) Network.get(i).getProtocol(mspastryid)).nodeId;
            if (mId.equals(searchNodeId)) {
                return Network.get(i);
            }
        }

        return null;
    }

    //______________________________________________________________________________________________
    /**
     * see MSPastry protocol "ReceiveRoute" primitive
     *
     * @param m Message
     */
    public void receiveRoute(Message m) {

        System.out.println("Recibe mensaje NODODHT: "+this.nodeId.toString());
        System.out.println(m.messageTypetoString());
        switch (m.messageType) {
            case Message.MSG_LOOKUP:
                System.out.println("Se recibe lookup");
                boolean iHaveIt = false;
                for(BigInteger b:myKeys){
                    if(b.compareTo(m.key)==0){
                        iHaveIt = true;
                        break;
                    }
                }
                if(iHaveIt){
                    System.out.println("Tengo la llave");
                    performLookUp(m);
                }
                else{
                    System.out.println("No tengo la llave");
                }
                break;
            case Message.MSG_INSERT:
                System.out.println("Se recibe insert");
                performInsertData(m);
                break;
            case Message.MSG_RESULT:
                System.out.println("Se recibe result");
                deliver(m);
                break;
        }
    }

    //RETURN THE NEXT HOP
    private BigInteger checkTables(Message m) {
        BigInteger next = null;
        int r = Util.prefixLen(m.dest, this.nodeId); //PREFIJO COMUN

        if (r == MSPastryCommonConfig.DIGITS) {
            return this.nodeId;
        }

        //SINO BUSCO EN TABLA DE RUTA
        if (r > 0) {
            char tmp = Util.put0(m.dest).charAt(r); //Next BIT

            if (this.routingTable.get(r, Util.charToIndex(tmp)) != null) {
                next = this.routingTable.get(r, Util.charToIndex(tmp));
            }
        } else {
            char tmp = Util.put0(m.dest).charAt(r); //Next BIT
            next = this.routingTable.get(r, Util.charToIndex(tmp));
        }

        return next;
    }

    private BigInteger checkLeafset(Message m) {
        BigInteger next = null;
        BigInteger[] allNodes;
        BigInteger mindist;
        BigInteger curdist;

        //SI ESTA DENTRO DEL LEAFSET
        if (leafSet.encompass(m.dest)) {
            int near = 0;
            allNodes = leafSet.listAllNodes();

            if (allNodes.length != 0) {
                mindist = m.dest.subtract(allNodes[near]).abs();
                for (int i = 1; i < allNodes.length; i++) {
                    curdist = m.dest.subtract(allNodes[i]).abs();
                    if (mindist.compareTo(curdist) > 0) {
                        mindist = curdist;
                        near = i;
                    }
                }
                // PROXIMO SALTO EN EL LEAFSET 
                next = allNodes[near];
            } //else

        }
        return next;
    }

    private BigInteger checkClosest(BigInteger nexthopRT, BigInteger nexthopLS, BigInteger nexthopLSBorder) {
        BigInteger ret = null;

        if (nexthopRT != null && nexthopLS != null) {//&& nexthopLSBorder != null){
            BigInteger distRT = this.nodeId.subtract(nexthopRT).abs();
            BigInteger distLS = this.nodeId.subtract(nexthopLS).abs();

            if (distLS.compareTo(distRT) <= 0) //&& distLS.compareTo(distLSB)<= 0)
            {
                ret = nexthopLS;
            }
            if (distRT.compareTo(distLS) <= 0) //&& distRT.compareTo(distLSB)<= 0)
            {
                ret = nexthopRT;
            }
        }

        if (nexthopRT != null && nexthopLS == null) {
            ret = nexthopRT;
        }

        if (nexthopLS != null && nexthopRT == null) {
            ret = nexthopLS;
        }

        return ret;
    }

    private BigInteger imClosest(BigInteger dest, BigInteger next) {
        BigInteger ret = null;

        if (next != null) {
            BigInteger distMe = dest.subtract(this.nodeId).abs();
            BigInteger distNext = dest.subtract(next).abs();
            if (distMe.compareTo(distNext) <= 0) {
                ret = this.nodeId;
            } else {
                ret = next;
            }
        }
        return ret;
    }

    private BigInteger closestOfLeafset(BigInteger dest) {
        BigInteger ret = null;
        BigInteger[] allNodes;

        allNodes = leafSet.listAllNodes();

        if (allNodes.length > 0) {
            BigInteger distLast = dest.subtract(allNodes[allNodes.length - 1]).abs();
            BigInteger distFirst = dest.subtract(allNodes[0]).abs();
            if (distFirst.compareTo(distLast) < 0) {
                ret = allNodes[0];
            } else {
                ret = allNodes[allNodes.length - 1];
            }
        }

        return ret;
    }

    public boolean checkRouting() {
        //System.out.println(this.listener.toString());
        double myProb = this.listener.getProbability();
        double dice = CommonState.r.nextDouble();

        if (dice <= myProb) {
            return true;
        }

        return false;
    }

    //______________________________________________________________________________________________
    /**
     * see MSPastry protocol "Route" primitive
     *
     * @param m Message
     * @param srcNode Node
     */
    private void route(Message m, Node srcNode) {

        // u("["+RoutingTable.truncateNodeId(nodeId)+"] received msg:[" + m.id + "] to route, with track: <");        o(m.traceToString(false) + ">");
        BigInteger nexthop = null;

        //leave a track of the transit of the message over this node
        m.nrHops++;
        if (m.trackSize < m.tracks.length) {
            m.trackSize++;
        }
        m.tracks[m.trackSize - 1] = this.nodeId;

        BigInteger curdist;
        BigInteger[] allNodes;
        BigInteger mindist;

        if (leafSet.encompass(m.dest)) {
            // il nodeID j in Li t.c. |k-j| � minimo
            int near = 0;
            allNodes = leafSet.listAllNodes();

            if (allNodes.length != 0) {

                mindist = m.dest.subtract(allNodes[near]).abs();
                for (int i = 1; i < allNodes.length; i++) {
                    curdist = m.dest.subtract(allNodes[i]).abs();
                    if (mindist.compareTo(curdist) > 0) {
                        mindist = curdist;
                        near = i;
                    }
                }
                nexthop = allNodes[near];
            } else {
                nexthop = this.nodeId;
            }

        } else {
            int r = Util.prefixLen(m.dest, this.nodeId);
            if (r == MSPastryCommonConfig.DIGITS) {
                deliver(m);
                o("  [route]   Delivered message src=dest=" + RoutingTable.truncateNodeId(nodeId));
                return;
            }

            nexthop = this.routingTable.get(r, Util.charToIndex(Util.put0(m.dest).charAt(r)));

            if (nexthop == null) {
                //il nodeID j in (Li U Ri) t.c. |k-j| < |k-i| && prefixLen(k,j)>=r

                BigInteger[] l = this.leafSet.listAllNodes();

                for (int jrow = 0; jrow < routingTable.rows; jrow++) {
                    for (int jcol = 0; jcol < routingTable.cols; jcol++) {
                        BigInteger nodejj = routingTable.get(jrow, jcol);
                        if (nodejj != null) {
                            if (cond1(m.dest, this.nodeId, nodejj)
                                    && cond2(m.dest, nodejj, r)) {
                                nexthop = nodejj;
                                break;
                            }
                        }
                    }
                }

                if (nexthop == null) {
                    for (int j = 0; j < l.length; j++) {
                        if (cond1(m.dest, this.nodeId, l[j])
                                && cond2(m.dest, l[j], r)) {
                            nexthop = l[j];
                            break;
                        }
                    }
                }

            } // end if (nexthop==null)
        }

        o(String.format("[%s].route([type=%s][src:%s][dest:%s][m.id=%d]): [nexthop:%s]",
                RoutingTable.truncateNodeId(nodeId),
                m.messageTypetoString(),
                "", // RoutingTable.truncateNodeId(src.nodeId),
                RoutingTable.truncateNodeId(m.dest),
                m.id,
                RoutingTable.truncateNodeId(nexthop)
        ));

        if (m.body instanceof Message.BodyJoinRequestReply && false) {
            o("m.RT " + ((Message.BodyJoinRequestReply) (m.body)).rt);
        }

        /**
         * !!! (this.nodeId.equals(m.dest)) � troppo limitativo, noi vogliamo
         * vedere se "io" sono quello pi� (numericammente) vicino possibile.
         * Poich� supponiamo di avvicinarci progressivamente, questo si traduce
         * nel controllare se sono pi� vicino dell'ultimo nodo attraversato
         * (m.traks[last])
         *
         * l'hop lo facciamo solo se facendolo... ridurremo la distanza, la
         * distanza tra destinatario e me rispetto alla distanza fra
         * destinatario e precedente
         */
        if ((m.trackSize > 0) && (nexthop != null)) {
            BigInteger src = m.tracks[m.trackSize - 1];
            if (!Util.nearer(m.dest, nexthop, src)) //if (!Util.nearer(m.dest,nexthop,src.nodeId))
            {
                nexthop = this.nodeId;
            }
        }

        if ((!this.nodeId.equals(nexthop)) && (nexthop != null)) {    //send m to nexthop
            transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
            transport.send(nodeIdtoNode(this.nodeId), nodeIdtoNode(nexthop), m, mspastryid);
        } else {
            receiveRoute(m);
        }

    }

    private static boolean cond1(BigInteger k, BigInteger i, BigInteger j) {
        return k.subtract(j).abs().compareTo(k.subtract(i).abs()) < 0;
    }

    private static boolean cond2(BigInteger k, BigInteger j, int r) {
        return Util.prefixLen(k, j) >= r;
    }

    //______________________________________________________________________________________________

    /**
     * Sort the nodes of the network by its nodeIds
     */
    private void sortNet() {
        Network.sort(new Comparator() {
            //______________________________________________________________________________________
            public int compare(Object o1, Object o2) {
                Node n1 = (Node) o1;
                Node n2 = (Node) o2;
                MSPastryProtocol p1 = (MSPastryProtocol) (n1.getProtocol(mspastryid));
                MSPastryProtocol p2 = (MSPastryProtocol) (n2.getProtocol(mspastryid));
                return Util.put0(p1.nodeId).compareTo(Util.put0(p2.nodeId));
                // return p1.nodeId.compareTo(p2.nodeId);
            }

            //______________________________________________________________________________________
            public boolean equals(Object obj) {
                return compare(this, obj) == 0;
            }
            //______________________________________________________________________________________
        });
    }

    //______________________________________________________________________________________________
    /**
     * search the node that is nerares than the specified node
     *
     * @param current Node
     * @return Node
     */
    private Node selectNeighbor(Node current) {
        //scelgo il seed come fatto nello StateBuilder per i rappresentanti
        //il seed sar� quel Node che da m� ha la minor latenza
        int candidates = 10;
        long minLatency = Long.MAX_VALUE;
        int seed = 0;

        for (int i = 0; i < candidates; i++) {
            int randomIndex;
            do {
                randomIndex = CommonState.r.nextInt(Network.size());
            } while (!Network.get(randomIndex).isUp());

            long lat = getTr(randomIndex).getLatency(current, Network.get(randomIndex));

            if (lat < minLatency) {
                minLatency = lat;
                seed = randomIndex;
            }
        }

        return Network.get(seed);
    }

    //______________________________________________________________________________________________
    /**
     * Given that this node was correctly initialized (e.g. routing table and
     * leafset created, and empty) it perform a join requesta to the mspastry
     * according to the protocol specification
     */
    public void join() {
        if (this.nodeId == null) {
            UniformRandomGenerator urg = new UniformRandomGenerator(
                    MSPastryCommonConfig.BITS, CommonState.r);
            this.setNodeId(urg.generate());
            sortNet();
        }

        Message joinrequest = Message.makeJoinRequest(null);
        joinrequest.body = new Message.BodyJoinRequestReply();
        Message.BodyJoinRequestReply body = (Message.BodyJoinRequestReply) (joinrequest.body);

        body.joiner = this.nodeId;
        body.rt = this.routingTable;
        joinrequest.dest = this.nodeId;

        Node seed = selectNeighbor(nodeIdtoNode(this.nodeId));

        peersim.edsim.EDSimulator.add(0, joinrequest, seed, mspastryid);

    }

    //______________________________________________________________________________________________
    /**
     * shortcut for getting the MSPastry level of the node with index "i" in the
     * network
     *
     * @param i int
     * @return MSPastryProtocol
     */
    public final MSPastryProtocol get(int i) {
        return ((MSPastryProtocol) (Network.get(i)).getProtocol(mspastryid));
    }

    //______________________________________________________________________________________________
    /**
     * shortcut for getting the Transport level of the node with index "i" in
     * the network
     *
     * @param i int
     * @return MSPastryProtocol
     */
    public final Transport getTr(int i) {
        return ((Transport) (Network.get(i)).getProtocol(tid));
    }

    //______________________________________________________________________________________________
    /**
     * This primitive provide the sending of the data to dest, by encapsulating
     * it into a LOOKUP Message
     *
     * @param recipient BigInteger
     * @param data Object
     */
    /* Genera un evento en la capa de pastry para un nodo destinatario
        Esta interfaz es usada por el DFS para generar eventos en la network
    */
    public void send(BigInteger recipient, Object data) {
        Message m = new Message(data);
        m.dest = recipient;
        m.src = this.nodeId;
        m.key = ((Query) data).key;
        m.value = ((Query) data).value;
        m.timestamp = CommonState.getTime();
        m.messageType =((Query) data).messageType;
        System.out.println("Tipo de mensaje: "+m.messageTypetoString());
        Node dest = nodeIdtoNode(m.dest);
        if(dest==null){
            System.out.println("Tenemos un problema");
        }
        System.out.println("Nodo " + this.nodeId.toString() + " Idnodo: " + dest.getID() + " Manda mensaje: " + m.value[0] + ";a: " + m.dest.toString());
        EDSimulator.add(0, m, dest, mspastryid);
    }
    /*
       Genera un evento en la capa de DFS para un nodo destinatario
        Esta interfgaz es usada por el DFS para generar eventos en la network
        el objetivo de esta función es intentar lograr el LookUP
        pero no se debería hacer esto!
    */
    public void send2(BigInteger recipient, Object data) {
        Message m = new Message(data);
        m.dest = recipient;
        m.src = this.nodeId;
        m.key = ((Query) data).key;
        m.value = ((Query) data).value;
        m.timestamp = CommonState.getTime();
        m.messageType =((Query) data).messageType;
        System.out.println("Tipo de mensaje: "+m.messageTypetoString());
        Node dest = nodeIdtoNode(m.dest);
        if(dest==null){
            System.out.println("Tenemos un problema");
        }
        System.out.println("Nodo " + this.nodeId.toString() + " Idnodo: " + dest.getID() + " Manda mensaje: " + m.value[0] + ";a: " + m.dest.toString());
        EDSimulator.add(0, data, dest, 4);
    }
    /*
        Se envia un mensaje directo de un nodo pastry a otro, saltandose
        el routing de pastry
    */
    public void sendDirect(BigInteger receiver, Object data) {
        Message m = Message.makeLookUp(data);
        m.dest = receiver;//((MSPastryProtocol)n.getProtocol(mspastryid)).nodeId;
        m.src = this.nodeId;
        m.key = ((Query) data).key;
        m.value = ((Query) data).value;
        m.timestamp = CommonState.getTime();
        transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
        transport.send(nodeIdtoNode(this.nodeId), nodeIdtoNode(receiver), m, mspastryid);
    }

    //______________________________________________________________________________________________
    /**
     * @param myNode Node
     * @param myPid int
     * @param m Message
     */
    void performJoinRequest(Node myNode, int myPid, Message m) {
        // aggiungi alla m.rt la riga N di myNode.R,
        // dove commonprefixlen vale n-1
        // (calcolata tra il nodo destinatatio (j) e (il nodeId di myNode)
        //System.out.println("Join REQUEST");
        MSPastryProtocol myP = ((MSPastryProtocol) myNode.getProtocol(myPid));
        Message.BodyJoinRequestReply body = (Message.BodyJoinRequestReply) m.body;

        if (nodeId.equals(body.joiner)) {
            return;
        }

        int n = Util.prefixLen(nodeId, body.joiner) + 1;

        body.rt.copyRowFrom(myP.routingTable, n);
    }

    //______________________________________________________________________________________________
    /**
     * see MSPastry protocol "performJoinReply" primitive
     */
    private void probeLS() {
        //e("probeLS\n");
        BigInteger[] leafs = this.leafSet.listAllNodes();

        for (int i = 0; i < leafs.length; i++) {
            transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);

            Message m = new Message(Message.MSG_LSPROBEREQUEST, null);
            m.dest = this.nodeId; //using m.dest to contain the source of the probe request

            transport.send(nodeIdtoNode(this.nodeId), nodeIdtoNode(leafs[i]), m, mspastryid);
        }

    }

    //______________________________________________________________________________________________
    /**
     * see MSPastry protocol "performJoinReply" primitive
     *
     * @param myNode Node
     * @param myPid int
     * @param m Message
     */
    void performJoinReply(Node myNode, int myPid, Message m) {
        // Ri.add(R u L)           (i = myself)
        // Li.add(L)
        //System.out.println("Join REPLY");

        Message.BodyJoinRequestReply reply = (Message.BodyJoinRequestReply) m.body;
        this.routingTable = (RoutingTable) reply.rt.clone();
        this.routingTable = reply.rt;

        BigInteger[] l = reply.ls.listAllNodes();

        for (int j = 0; j < l.length; j++) {
            int row, col;

            row = Util.prefixLen(this.nodeId, l[j]);

            //System.out.println("NODO: " + RoutingTable.truncateNodeId(this.nodeId) + " LS: " + RoutingTable.truncateNodeId(l[j]));
            if (!this.nodeId.equals(l[j])) {
                col = Util.charToIndex(Util.put0(l[j]).charAt(row)); /// prima era:col = Util.charToIndex(Util.put0(l[j]).charAt(row + 1));
                this.routingTable.set(row, col, l[j]);
            }
        }

        // poch� this.leafSet e' vuoto, la add() viene fatta tramite assegnazione diretta.
        this.leafSet = (LeafSet) reply.ls.clone();
        this.leafSet.nodeId = this.nodeId;

        probeLS();

    }
    /*
        Cuando recibe un evento de insert con un pedazo de una cancion
        genera el archivo y directorios para manejar los pedazos de canción
        por los que tiene que responder el nodo.
        El arraylist myKeys tiene las llaves de las que es responsable.
    */
    private void performInsertData(Message m) {
        Query q = (Query) m.body;
        File file = new File("./"+nodeId.toString());
        file.mkdir();
        File file2 = new File(file.getAbsolutePath()+"/"+q.key.toString()+".mp3");
        try {
            
            BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
            bw.write(new String((byte[])q.body));
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        myKeys.add(q.key);
    }
    
    /*función para calcular a partir de un i, el nodeId del nodo i-esimo*/
    public BigInteger getNodeId(int i){
        MSPastryProtocol node = (MSPastryProtocol) Network.get(i).getProtocol(3);
        return node.nodeId;
    }
    /*
        Ocupa el tamaño dle DHT para asignar un entero a una llave,
        esto se hace con el objetivo de calcular que nodo i-esimo
        le corresponde que llave
    */
    public int remainder(BigInteger i){
        return i.remainder(new BigInteger(this.size)).intValueExact();
    }
    
    /*
        Al llegar un lookup de una llave de la que es responsable
        recupera los datos del archivo que genero y los envía
        directamente con un mensaje de resultado al nodo que pidio los
        resultados, con el objetivo de que lleguen al DFS
        y este reconstruya el archivo
    */
    private void performLookUp(Message m) {
        File file = new File("./"+nodeId.toString());
         File file2 = new File(file.getAbsolutePath()+"/"+m.key.toString()+".mp3");
        try {
            BufferedReader br = new BufferedReader(new FileReader(file2));
            String temp = "";
            while(br.ready()){
                temp=temp+br.readLine();
            }
            br.close();
            byte[] chunk  = temp.getBytes();
            Query q = (Query) m.body;
            q.body = chunk;
            q.messageType = Query.MSG_RESULT;
            m.body = q;
            m.messageType = Message.MSG_RESULT;
            sendDirect(m.src,m);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    //______________________________________________________________________________________________
    /**
     * see MSPastry protocol "performLSProbeRequest" primitive
     *
     * @param m Message
     */
    private void performLSProbeRequest(Message m) {
        BigInteger cell = null;
        this.leafSet.push(m.dest);

        if (!this.nodeId.equals(m.dest)) {
            int row = Util.prefixLen(this.nodeId, m.dest);
            int col = Util.charToIndex(Util.put0(m.dest).charAt(row)); /// prima era:col = Util.charToIndex(Util.put0(l[j]).charAt(row + 1));
            cell = this.routingTable.get(row, col);

            if (cell != null) {
                transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
                long oldLat = transport.getLatency(nodeIdtoNode(this.nodeId), nodeIdtoNode(cell));
                long newLat = transport.getLatency(nodeIdtoNode(this.nodeId), nodeIdtoNode(m.dest));
                if (newLat > oldLat) {
                    return;
                }
            }
            this.routingTable.set(row, col, m.dest);
        }
    }

    private void rebuiltLS() {
        int sz = Network.size();
        int currentNode = CommonState.getNode().getIndex();

        if (this.leafSet.needRepairLeft()) {
            int s = currentNode;
            int k = 0;
            while (k < this.leafSet.hsize) {
                if (s > 0) {
                    s = s - 1;
                } else {
                    s = sz - 1;
                }

                if (Network.get(s).isUp()) {
                    this.leafSet.pushToLeft(get(s).nodeId);
                    k++;
                }
            }
        }
        if (this.leafSet.needRepairRight()) {
            int s = currentNode;
            int k = 0;
            while (k < this.leafSet.hsize) {
                if (s < sz - 1) {
                    s = s + 1;
                } else {
                    s = 0;
                }
                //System.out.println("S/K/CN/SZ " + s+ " / " +k + " / "+ currentNode + " / " + sz);
                if (Network.get(s).isUp()) {
                    this.leafSet.pushToRight(get(s).nodeId);
                    k++;
                }
            }
        }

    }

    //______________________________________________________________________________________________
    /**
     * the cleaning service is called occasionally in order to remove from the
     * tables of this node failed entrie.
     *
     * @param myNode Node
     * @param myPid int
     * @param m Message
     */
    private void cleaningService(Node myNode, int myPid, Message m) {
        // cleaning tables...

        BigInteger bCheck;
        Node nCheck;
        for (int irow = 0; irow < routingTable.rows; irow++) {
            for (int icol = 0; icol < routingTable.cols; icol++) {
                bCheck = routingTable.get(irow, icol);
                nCheck = nodeIdtoNode(bCheck);
                if ((nCheck == null) || (!nCheck.isUp())) {
                    routingTable.set(irow, icol, null);
                }
            }
        }

        BigInteger[] bCheck2 = leafSet.listAllNodes();
        for (int i = 0; i < bCheck2.length; i++) {
            nCheck = nodeIdtoNode(bCheck2[i]);

            //REMUEVE SI LA ENTRADA ES IGUAL AL NODO Y SI EL NODO ESTA CAIDO
            if ((nCheck == null) || (!nCheck.isUp())) {
                //System.out.println("ENTRO A REMOVER LEAFNODE");
                leafSet.removeNodeId(bCheck2[i]);
            }

        }

        //REPARANDO LEAFSET
        rebuiltLS();

        long delay = 1000 + CommonState.r.nextLong(1000);
        EDSimulator.add(delay, m, myNode, myPid);
    }

    //______________________________________________________________________________________________
    /**
     * manage the peersim receiving of the events
     *
     * @param myNode Node
     * @param myPid int
     * @param event Object
     */
    /*
        Maneja los eventos:
        Evento lookup: si llega un lookup, revisa si es responsable
        si no es responsable, envía la consulta a quien cree que es responsable
        por la llave
        Evento LookUp_DFS: Si recibe este evento se lo pasa a la capa DFS
        Evento INSERT: Si recibe este evento es pq se tiene que hacer responsable
        de una llave, y hace un INSERT entre sus datos
        Evento Result: Si recibe un evento de result, se lo pasa a la capa superior
        del DFS
    */
    @Override
    public void processEvent(Node myNode, int myPid, Object event) {
        if (!cleaningScheduled) {
            long delay = 1000 + CommonState.r.nextLong(1000);
            Message service = new Message(Message.MSG_SERVICEPOLL, "");
            service.dest = this.nodeId;
            EDSimulator.add(delay, service, myNode, myPid);
            cleaningScheduled = true;
        }

        /**
         * Parse message content Activate the correct event manager fot the
         * partiular event
         */
        this.mspastryid = myPid;
        Message m = (Message) event;
        System.out.println("Recibe mensaje NODODHT PROCESSEVENT: "+this.nodeId.toString());
        System.out.println("Se recibe desde: "+m.src);
        System.out.println(m.messageTypetoString());
        switch (m.messageType) {
            case Message.MSG_LOOKUP:
                System.out.println("Se recibe lookup");
                boolean iHaveIt = false;
                for(BigInteger b:myKeys){
                    if(b.compareTo(m.key)==0){
                        iHaveIt = true;
                        break;
                    }
                }
                if(iHaveIt){
                    System.out.println("Tengo la llave");
                    performLookUp(m);
                }
                else{
                    System.out.println("No tengo la llave");
                    m.messageType = Message.MSG_LOOKUP;
                    System.out.println("Enviamos consulta a :"+getNodeId(remainder(m.key)));
                    send(getNodeId(remainder(m.key)),m.body);
                }
                break;
            case Message.MSG_LOOKUP_DFS:
                m.messageType = Message.MSG_LOOKUP_DFS;
                this.listener.receive(m);
                break;
            case Message.MSG_INSERT:
                System.out.println("Se recibe insert");
                performInsertData(m);
                break;
            case Message.MSG_RESULT:
                System.out.println("Se recibe result");
                deliver(m);
                break;
            case Message.MSG_JOINREQUEST:
                performJoinRequest(myNode, myPid, m);
                route(m, myNode);
                break;

            case Message.MSG_JOINREPLY:
                performJoinReply(myNode, myPid, m);
                break;

            case Message.MSG_SERVICEPOLL:
                cleaningService(myNode, myPid, m);
                break;

            case Message.MSG_LSPROBEREQUEST:
                performLSProbeRequest(m);
                break;
        }

    }

    //______________________________________________________________________________________________
    /**
     * set the current NodeId
     *
     * @param tmp BigInteger
     */
    public void setNodeId(BigInteger tmp) {
        this.nodeId = tmp;
        leafSet.nodeId = tmp;

    }

    //______________________________________________________________________________________________
    /**
     * debug only
     *
     * @param o Object
     */
    private static void e(Object o) {
        if (MSPastryCommonConfig.DEBUG) {
            System.err.println(o);
        }
    }

    /**
     * debug only
     *
     * @param o Object
     */
    private static void o(Object o) {
        if (MSPastryCommonConfig.DEBUG) {
            System.out.println(o);
        }
    }

}

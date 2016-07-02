

package peersim.DistributedFileSystem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import peersim.Traffic.Query;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.pastry.MSPastryProtocol;
import peersim.utils.FileSplit;


public class DistributedFileSystem implements EDProtocol, Cloneable {

    private static final String PAR_TRANSPORT = "transport";
    private static final String PAR_STORAGE = "storage";
    private static final String PAR_REPLICA = "replicas";
    private static final String PAR_TECH = "technique";
    private static final String PAR_STEP = "step";
    private static final String PAR_MAX = "max_queries";
    private static final String PAR_WIN = "window";
    private static final String PAR_PWEAK = "wn";
    private static final String PAR_PNORMAL = "nn";
    private static final String PAR_PPOWER = "pn";
    private static final String PAR_MDEB = "max_debit";
    private static final String PAR_SIZE = "SIZE";

    protected MSPastryProtocol routeLayer;
    public int step;
    private static String prefix;
    private int pid;
    private int tid;
    public double P;
    //Se usa cuando se están esperando resultados, después de un LookUP
    private int piecesWaiting;
    //Se aguardan los resultados, cuando se esperan resultados de un LookUP
    private List<byte[]> chunks;
    //Dictionario que ocupa una llave de un archivo 
    //para acceder a la lista de llaves donde están los trozos del archivo
    private Map<BigInteger,List<BigInteger>> tableData;
    private String size;
    /*
        Se implementa un protocolo de DFS para nuestra aplicación
    */
    public DistributedFileSystem(String prefix) {
        DistributedFileSystem.prefix = prefix;
        this.pid = Configuration.lookupPid(prefix.substring(prefix.lastIndexOf('.') + 1));
        this.tid = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
        this.step = Configuration.getInt(prefix + "." + PAR_STEP);
        P = 1.0;  // Probability to accept a message equal 1
        this.routeLayer = ((MSPastryProtocol) CommonState.getNode().getProtocol(tid));
        this.routeLayer.setMyApp(this);
        tableData = new HashMap<>();
        this.size = Configuration.getString(PAR_SIZE);
    }

    
    /*
        Función que ocupa el protocolo pastry para comunicarse con el DFS
        a través de esta entrega mensajes al DFS según sea de LookUP o
        de entrega de resultados (si es que corresponde)
        En el caso del lookUP si es responsable de el LookUP y tiene
        la tabla de llaves para unir al archivo
        envía peticiones de lookup a los nodos que correspondan
        si no, devuelve la consulta al DHT, con el nodo "que el cree"
        que debería tener esa llave
        En el caso de result, disminuye los resultados esperados en 1
        y si ya los recibió todos, un el archivo en un archivo llamado
        resultado.mp3
    */
    public void receive(Object event) {		//RECIVE DESDE PASTRY
        Query q = (Query) event;
        System.out.println("DFS RECIVE MENSAJE DESDE DHT");
        switch (q.messageType) {
            case Query.MSG_LOOKUP:
                System.out.println("LookUP message:");
                System.out.println("Body: "+q.body.toString()+" Key: "+q.key.toString());
                boolean iHaveIt = false;
                for(Object key: tableData.keySet()){
                    if(((BigInteger)key).compareTo(q.key)==0){
                        iHaveIt = true;
                        break;
                    }
                }
                if(iHaveIt){
                    System.out.println("DFS Tiene la llave");
                    List<BigInteger> l = tableData.get(q.key);
                    for(BigInteger b:l){
                        q.messageType = Query.MSG_LOOKUP;
                        routeLayer.send(b, q);
                    }
                }
                else{
                    System.out.println("DFS no tienee la llave");
                    q.messageType = Query.MSG_LOOKUP_DFS;
                    routeLayer.send(getNodeId(remainder(q.key)), q);
                }
                break;
            case Query.MSG_LOOKUP_DFS:
                System.out.println("LookUP DFS message:");
                System.out.println("Body: "+q.body.toString()+" Key: "+q.key.toString());
                iHaveIt = false;
                for(Object key: tableData.keySet()){
                    if(((BigInteger)key).compareTo(q.key)==0){
                        iHaveIt = true;
                        break;
                    }
                }
                if(iHaveIt){
                    System.out.println("DFS tiene la llave");
                    List<BigInteger> l = tableData.get(q.key);
                    for(BigInteger b:l){
                        routeLayer.send(b, q);
                    }
                }
                else{
                    System.out.println("AQUI HAY UN PROBLEMA");
                    System.out.println("DFS no tiene la llave");
                    routeLayer.send(getNodeId(remainder(q.key)), q);
                }
                break;
            case Query.MSG_RESULT:
                if(piecesWaiting>0){
                    piecesWaiting--;
                    chunks.add((byte[]) q.body);
                    if(piecesWaiting==0){
                        List<BigInteger> list = tableData.get(q.key);
                        String temp = "";
                        for(int i=0;i<list.size();i++){
                            temp = temp+new String(chunks.get(i)); 
                        }
                        File f = new File("./Resultados");
                        f.mkdir();
                        try {
                            BufferedWriter bw = new BufferedWriter(new FileWriter(f.getAbsolutePath()+"/resultado.mp3"));
                            bw.write(temp);
                            bw.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
        }
    }
    /*función para calcular a partir de un i, el nodeId del nodo i-esimo*/
    public BigInteger getNodeId(int i){
        MSPastryProtocol node = (MSPastryProtocol) Network.get(i).getProtocol(3);
        return node.nodeId;
    }
    /*Ocupa el tamaño dle DHT para asignar un entero a una llave,
        esto se hace con el objetivo de calcular que nodo i-esimo
        le corresponde que llave
    */
    public int remainder(BigInteger i){
        return i.remainder(new BigInteger(this.size)).intValueExact();
    }
    /*
        Esta función es la que se implementa desde EDProtocol (Event-Driven Protocol)
        Esta función es la que procesa los eventos que se lancen a este nodo
        desde la aplicación o desde esta misma capa pero hacia otro nodo
        a través del Pastry.
        Para los mensajes de tipo LookUP o LoopUpDFS, se revisa si esa llave la posee este DFS
        si la posee, pide a cada nodo que corresponde las llaves que corresponden
        al archivo que se está buscando, si no lo tiene devuelve a pastry el lookup
        para encontrar el nodo responsable.
        Para los mensajes de Insert se hace cargo de las llaves si es que le corresponde
        la llave del nombre del archivo y de particionar el archivo y enviarselo a otros nodos.
        Si no le corresponden las llaves, envía la query al nodo que el considera que le corresponde
        la llave del archivo.
    */
    @Override
    public void processEvent(Node myNode, int pid, Object event) {  // LLEGA DEL GENERADOR DE TRAFICO
        Query q = (Query) event;
        if(q.value[0]==null){
            return;
        }
        System.out.println("RECIBIMOS MENSAJE DESDE: "+q.src.toString());
        if(q.messageType==Query.MSG_LOOKUP){
            System.out.println("LookUP message:");
            System.out.println("Body: "+q.body.toString()+" Key: "+q.key.toString());
            System.out.println("Estamos en nodo con ID: "+myNode.getID());
            boolean iHaveIt = false;
            for(Object key: tableData.keySet()){
                if(((BigInteger)key).compareTo(q.key)==0){
                    iHaveIt = true;
                    break;
                }
            }
            if(iHaveIt){
                List<BigInteger> l = tableData.get(q.key);
                for(BigInteger b:l){
                    routeLayer.send(b, q);
                }
                piecesWaiting = l.size();
                chunks = new ArrayList<>();
                System.out.println("Se inicia captura de resultados");
                System.out.println("___________________________________");
            }
            else{
                System.out.format("No tenemos esa llave: %s se la enviamos a: %s",q.key.toString(),
                        getNodeId(remainder(q.key)));
                routeLayer.send2(getNodeId(remainder(q.key)), q);
            }
        }
        else if(q.messageType==Query.MSG_INSERT){
            System.out.println("Insert message");
            System.out.println("Body: "+q.body.toString()+" Key: "+q.key.toString());
            System.out.println("Estamos en nodo con ID: "+myNode.getID());
            if(this.routeLayer.nodeId.compareTo(getNodeId(remainder(q.key)))==0){
                FileSplit fs = new FileSplit(q.value[0]);
                BigInteger initialKey = q.key;
                List<BigInteger> list = new ArrayList<>();
                for (int i = 0; i < fs.sizeListChunk(); i++) {
                    q.body = fs.getChunk(i);
                    try {
                        q.key = peersim.utils.HashSHA.applyHash(q.value[0]+Integer.toString(i));
                        list.add(getNodeId(remainder(q.key)));
                    } catch (UnsupportedEncodingException ex) {
                        ex.printStackTrace();
                    }
                    tableData.put(initialKey, list);
                    routeLayer.send(getNodeId(remainder(q.key)), q);
                }
                System.out.println("Termina distribucion para: "+q.value[0]);
            }
            else{
                routeLayer.send2(getNodeId(remainder(q.key)), q);
            }
        }
    }

    public double getProbability() {
        return this.P;
    }

    public Object clone() {
        DistributedFileSystem dolly = new DistributedFileSystem(DistributedFileSystem.prefix);
        return dolly;
    }

}

package peersim.Traffic;

import java.math.BigInteger;

public class Query {

    public long id;
    public long timestamp;
    public String[] value;
    public int messageType;
    private static long ID_GENERATOR = 0;
    public static final int MAX_PATH = 20;
    protected int pathSize = 0;
    public BigInteger[] path = null;
    public BigInteger src;
    public BigInteger dest;
    public BigInteger key;
    public Object body = null;
    public int hops;
    public static final int MSG_LOOKUP = 0;
    public static final int MSG_INSERT = 6;//COMO EL MESSAGE DE PASTRY
    public static final int MSG_REPLY = 2;
    public static final int MSG_REPLICA = 3;
    public static final int MSG_RESULT = 7;
    public static final int MSG_LOOKUP_DFS = 8;

    public Query(int messageType, Object body) {
        this.id = (ID_GENERATOR++);
        this.path = new BigInteger[MAX_PATH];
        this.messageType = messageType;
        this.body = body;
        this.value = new String[2];
    }

    public static final Query makeLookup(Object body) {
        return new Query(MSG_LOOKUP, body);
    }

    public static final Query makeInsert(Object body) {
        return new Query(MSG_INSERT, body);
    }

    public static final Query makeReply(Object body) {
        return new Query(MSG_REPLY, body);
    }

    public static final Query makeReplica(Object body) {
        return new Query(MSG_REPLICA, body);
    }
    
    public static final Query makeResult(Object body) {
        return new Query(MSG_RESULT, body);
    }

    public void copyPath(BigInteger[] track) {
        this.path = track;
    }

    public void copyHops(int h) {
        this.hops = h;
    }

    public void copyTimestamp(long t) {
        this.timestamp = t;
    }
}

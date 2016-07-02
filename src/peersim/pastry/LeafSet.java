package peersim.pastry;

import java.math.BigInteger;
import java.util.Vector;

import peersim.core.Network;
import peersim.core.Node;

//__________________________________________________________________________________________________
/**
 *
 * LeafSet class encapsulate functionalities of a Leaf Set table in a Pastry Node, allowing
 * automatic "intellingent" adding of the entries, and facilitating extraction of information
 * <p>Title: MSPASTRY</p>
 *
 * <p>Description: MsPastry implementation for PeerSim</p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: The Pastry Group</p>
 *
 * @author Elisa Bisoffi, Manuel Cortella
 * @version 1.0
 */
public class LeafSet implements Cloneable {

    //______________________________________________________________________________________________
    /**
     * indicates not filled positions
     */
    private static final BigInteger EMPTY = null;

    //______________________________________________________________________________________________
    /**
     * ordered array of the nodeIds inserted closest to and lower than the nodeId
     * left[0] is the closest (and less) of the current nodeId, and so on...
     */
    private BigInteger[] left = null;

    /**
     * ordered array of the nodeIds inserted closest to and higher than the nodeId
     * right[0] is the closest (and greater) of the current nodeId, and so on...
     */
    private BigInteger[] right = null;

    
    public Vector <Node> nodesL;
    public Vector <Node> nodesR;
    
    /**
     * total size of the leaf set
     */
    private int size = 0;

    /**
     * size of both left and right part of the leaf set.
     */
    public int hsize = 0;

    /**
     * pivot nodeId, this is needed in order to know how to organize adding/positioning/searching of
     * the entries of the leaf set
     */
    public BigInteger nodeId = null;


    //______________________________________________________________________________________________
    /**
     * Not allowed to call default (without parameters) constructor!
     */
    private LeafSet() {}

    //______________________________________________________________________________________________
    /**
     * shortcut constructor to use integers instead of BigIntegers.
     * @param myNodeId long
     * @param size int
     */
    public LeafSet(long myNodeId, int size) {
        this(new BigInteger(""+myNodeId), size);
    }

    //______________________________________________________________________________________________
    /**
     * Creates a new Leaf Set by pivoting it with the specified nodeId, and with the desired size
     * of the vector. Half of the size will be used to store nodes lessed than the pivot nodeId,
     * the other half for the greater entries. Note: is size is an odd number, (size+1) will always
     * be considered
     * @param myNodeId BigInteger the pivot nodeId of the leafset, i.e. the nodeid of the pastry
     * node owner
     * @param size int must be > 0, and possibily an even number
     */
    public LeafSet(BigInteger myNodeId, int size) {

        nodeId = myNodeId;
        hsize = size/2;
        left = new BigInteger[hsize];
        right = new BigInteger[hsize];
        nodesL = new Vector<Node>();
        nodesR = new Vector<Node>();
        
        for (int i = 0; i < hsize; i++)
         left[i]=right[i]=EMPTY;
    }


	public final MSPastryProtocol get(int i) {
		return ((MSPastryProtocol) (Network.get(i)).getProtocol(3));
	}
   

    //______________________________________________________________________________________________
    /**
     * returns EMPTY if the leaf set does not contains the specified Key.
     * returns the position in one of the two arrays of the keyToFind otherwise.
     * to establish if the keyToFind is located in left or in right array, simply check
     * if newNode > keyToFind (right) or not (left)
     * @param keyToFind long
     * @return int
     */
	private int indexOf(BigInteger keyToFind) {
		if (keyToFind.compareTo(nodeId) > 0) { //cerca a destra
			for (int index = 0; (index < hsize); index++) {
				if (right[index].equals(keyToFind)) 
					return index;
			}
			return -1;

		}else{
			//cerca a sinistra
			for (int index = 0; index < hsize; index++){
				if (left[index].equals(keyToFind)) 
					return index;
			}
			return -1;
		}
	}


    //______________________________________________________________________________________________
    /**
     * make a shift of an array v by one position, starting from the element with index "pos"
     * (pos is included), with the purpose to create a new (empty) available slot at the index "pos"
     * Note: the last element of the array is lost
     */
    private void shift(BigInteger[] v, int pos) {
        for(int i = hsize-1; i > pos; i--)
            v[i] = v[i-1];
    }



    //______________________________________________________________________________________________

    
    public int isInRight(BigInteger b){
    	
    	if(b == null)
    		return -1;

    	for (int index = 0; (index < right.length); index++) {
    		if (right[index] != null && right[index].equals(b)){ 
    			return index;
    		}
    	}
    	return -1;
    }
    
    public int isInLeft(BigInteger b){
    	
    	if(b == null)
    		return -1;
    	
    	for (int index = left.length - 1; index >=0 ; index--) {
			if(left[index] != null && left[index].equals(b)){ 
				return index;
			}
    	}
    	return -1;
    }
    
    //______________________________________________________________________________________________
    /**
     * permanently removes the specified NodeId from this Leaf Set.
     * @param b BigInteger
     * @return boolean true is some element is removed, false if the element does not exists
     */
    public boolean removeNodeId(BigInteger b) {
    	int l = -1;
    	int r = -1;
    	boolean ret = false;
    	
    	r = this.isInRight(b);
    	l = this.isInLeft(b);
    	
    	if(r != -1){
    		for (int i = r; i< right.length-1; i++) {
    			right[i] = right[i+1];
    		}
    		right[right.length-1] = EMPTY;
    		//System.out.println("REMOVIO LEAFNODE RIGHT");
    		return true;
    
    	}
    	if(l != -1){
    		for (int i = l; i< left.length-1; i++) {
    			left[i] = left[i + 1];
    		}
    		left[left.length-1] = EMPTY;
    		//System.out.println("REMOVIO LEAFNODE LEFT");
    		return true;

    	}
    	
    	return false;

    }


    //______________________________________________________________________________________________
    /**
     * if returns "Empty" indicates: DO NOT (RE)INSERT!!!
     * @param n long
     * @return int
     */
    private int correctRightPosition(BigInteger n) {
    	
    	int l=0;
    	while (l < hsize && (! (right[l] == EMPTY)) ){
    		if(right[l].equals(n))
    			return -1;
    		l++;
    	}
    	
    	// VER CASOS LIMITE
    	 if (n.compareTo(nodeId) > 0)   {
    	 
    		 for(int i = 0; i < hsize ;i++) {
    	            if (right[i] == EMPTY) 
    	            	return i;
    	            if (right[i].equals(n)) 
    	            	return -1;
    	            if (right[i].compareTo(n) > 0 && right[i].compareTo(nodeId) > 0) 
    	            	return i;
    	            if (right[i].compareTo(n) < 0 && right[i].compareTo(nodeId) < 0) 
    	            	return i;
    	        }
    	        return hsize;
    	 
    	 } else {
    	//	 System.out.println("Right critico: " + RoutingTable.truncateNodeId(n));
    		 for(int i = 0; i < hsize ;i++) {
    	            if (right[i] == EMPTY) 
    	            	return i;
    	            if (right[i].equals(n)) 
    	            	return -1;
    	            if (right[i].compareTo(n) > 0 && right[i].compareTo(nodeId) < 0){
    	            	
    	      // 		 System.out.println("Right critico: " + i);

    	            	return i;
    	            }
    	          //  if (right[i].compareTo(n) < 0 && right[i].compareTo(nodeId) < 0) 
    	          //  	return i;
    	       
    	            	
    	        }
    	        return hsize;

    		 
    	 }
        
        
    }


    /**
     * Empty indicates: DO NOT (RE)INSERT!!!
     * @param n long
     * @return int
     */
    private int correctLeftPosition(BigInteger n) {
    	// VER CASOS LIMITE
    //	 System.out.println("Left position: " + RoutingTable.truncateNodeId(n) + " in " + RoutingTable.truncateNodeId(nodeId));
    	
    	int l=0;
    	while (l < hsize && (! (left[l] == EMPTY))  ){
    		if(left[l].equals(n))
    			return -1;
    		l++;
    	}
    	
    	if (n.compareTo(nodeId) < 0)   {

    		for(int i = 0; i < hsize;i++) {
    			if (left[i] == EMPTY) 
    				return i;
    			if (left[i].equals(n)) 
    				return -1;
    			if (left[i].compareTo(n) < 0 && left[i].compareTo(nodeId) < 0) 
    				return i;
    			if (left[i].compareTo(n) > 0 && left[i].compareTo(nodeId) > 0) 
    				return i;

    		}
    		return hsize;
    	} else {
    		
    	//	 System.out.println("Left critico: " + RoutingTable.truncateNodeId(n) + " in " + RoutingTable.truncateNodeId(nodeId));
    		 
    		for(int i = 0; i < hsize;i++) {
    			if (left[i] == EMPTY) 
    				return i;
    			if (left[i].equals(n)) 
    				return -1;
    			if (left[i].compareTo(n) < 0 && left[i].compareTo(nodeId) > 0)
    					return i;
    		}
    				return hsize;
    	}
    }




    public void pushToRight(BigInteger newNode) {
       if (newNode.equals(nodeId)) 
    		return;
    	
       int index = correctRightPosition(newNode);
       if (index== -1) 
    	   return;
       if (index == hsize) 
    	   return;
       
       shift(right, index);
  //     System.out.println("Index Right: " + index);
       right[index] = newNode;
    }

    
    
    public void pushToLeft(BigInteger newNode) {
       if (newNode.equals(nodeId)) 
    		return;
       
       int index = correctLeftPosition(newNode);
       if (index==-1) 
    	   return;
       
       if (index==hsize) 
    	   return;
       
       shift(left, index);
//       System.out.println("Index Left: " + index);
       left[index] = newNode;
    }


    private int countNonEmpty(BigInteger[]a) {
        int count = 0;
        for(count = 0; (count < a.length) && (a[count]!=EMPTY);count++) /*NOOP*/ ;
        return count;
    }

    //______________________________________________________________________________________________
    /**
     * shortcut for  push(new BigInteger(""+newNode));
     * @param newNode long
     */
    
    public void push(long newNode) {
    	//System.out.println("NODO PUSH: " + RoutingTable.truncateNodeId(new BigInteger(""+newNode)));
        push(new BigInteger(""+newNode));
    }
     


    //______________________________________________________________________________________________
    private boolean checkCriticInterval(BigInteger node){
    	//CHECK CLOCKWISE
    	if(node.compareTo( get(0).nodeId) >= 0 && node.compareTo(get(hsize-1 ).nodeId) <= 0){
    		//System.out.println("TRUE");
    		return true;
    	}
    	//CHECK COUNTERWISE
    	if(node.compareTo( get(Network.size()-1).nodeId) <= 0 && node.compareTo(get(Network.size()-1 - hsize).nodeId) >= 0){
    		//System.out.println("TRUE");    		
    		return true;
    	}
    	return false;
    }
    
    
    /**
     * push into the leafset the specified node, by according the properties specified by the
     * mspastry protocol
     *
     * @param newNode long
     */
    
    public void push(BigInteger newNode) {
    	
    	// VER CASOS LIMITE
    	//System.out.println("NODO PUSH: " + RoutingTable.truncateNodeId(newNode)  );
    	if ( !newNode.equals(this.nodeId)){ 

    		//System.out.println("TO INSERT: "+ RoutingTable.truncateNodeId(newNode));
    		//System.out.println("ANTES:   "+ this.toString());
    		//SI ESTA EN INTERVALO CRITICO 
    		if(checkCriticInterval(newNode)){
    			//System.out.println("ESTA EN INTERVALO CRITICO");
    			//RIGHT PROBLEMAAAA
    			if((newNode.compareTo(this.nodeId) > 0 &&  newNode.compareTo(get(Network.size()-1).nodeId) < 0) 
    					|| (newNode.compareTo(this.nodeId) < 0 && newNode.compareTo(get(hsize-1).nodeId) < 0)){
    				//System.out.println("RIGHT");
    				pushToRight(newNode);
    		}
    			else{
    				//System.out.println("LEFT");
    				pushToLeft(newNode);
    		}

    		}else{ //SE APLICA EVALUACION TRADICIONAL
    			if (newNode.compareTo(nodeId) > 0 )
    				pushToRight(newNode);

    			if (newNode.compareTo(nodeId) < 0 )
    				pushToLeft(newNode);
    		}

    		//System.out.println("DESPUES: "+ this.toString());
    	}
    }

    //______________________________________________________________________________________________
    /**
     * returns true iff whe specified node is found in the table
     * @param node BigInteger
     * @return boolean
     */
    public boolean containsNodeId(BigInteger node) {
     return indexOf(node) != -1;
    }


    //______________________________________________________________________________________________
    /**
     * returns the lesser nodeid stored
     * @return BigInteger
     */
    private BigInteger min() {
        if (countNonEmpty(left) == 0) 
        	return nodeId;

        return left[countNonEmpty(left)-1];
    }

    /**
     * returns the greater nodeid stored
     * @return BigInteger
     */
    private BigInteger max() {
        if (countNonEmpty(right)==0) 
        	return nodeId;
        return right[countNonEmpty(right)-1];
    }

    //______________________________________________________________________________________________
    /**
     * returns true if key is between the leftmost and the rightmost.
     * it does not require that key is contained in the table, only requires that the key is
     * greater-equal than the min nodeid stored and lesser-equal than the max nodeid stored.
     * Note: this.ls.encompass(this.ls.nodeid) always returns true, in all cases.
     * @param k BigInteger
     * @return boolean
     */
    public boolean encompass(BigInteger k) {
    	boolean ret = false;
    	if(BigInteger.ZERO.compareTo(min()) < 0 && BigInteger.ZERO.compareTo(max())< 0 ){
    		if(k.compareTo(min()) < 0 && k.compareTo(max())< 0)
    			ret = true;
    		if(k.compareTo(min()) > 0 && k.compareTo(max())< 0)
    			ret =  true;
    	}
    		
        if (min().compareTo(k)  > 0 || max().compareTo(k)  < 0) 
        	ret = false;

        return ret;
    }
    
    
    public boolean needRepairLeft(){
    	
    	for(int i = 0 ; i < this.hsize ; i++) {
    		if(left[i] == EMPTY){
    			return true;
    		}
    	}
    	return false;
    }
    
    public boolean needRepairRight(){
    	
    	for(int i = 0 ; i < this.hsize ; i++) {
    		if(right[i] == EMPTY ){
    			return true;
    		}
    	}
    	return false;
    }


    //______________________________________________________________________________________________
    /**
     * Outputs an (ordered, from min to max) array of all nodes in the leaf set.
     * The actual pivot nodeid is not included.
     * @return BigInteger[]
     */
    public BigInteger[] listAllNodes() {
      int numLeft = countNonEmpty(left);
      int numRight = countNonEmpty(right);
      BigInteger[] result = new BigInteger[numLeft+numRight];
      for(int i = 0; i<numLeft;i++)
          result[i] = left[i];
      for(int i = 0; i<numRight;i++)
          result[numLeft+i] = right[i];
       return result;
    }
    

    
    
    public void putNodeLeft(Node n){
    	nodesL.add(n);	
    }

    
    public void putNodeRight(Node n){
    	nodesR.add(n);
    }
    
    
    public Object[] getAllNodesLeft(){
    	return nodesL.toArray();
    }
    
    
    public Object[] getAllNodesRight(){
    	return nodesR.toArray();
    }

    //______________________________________________________________________________________________
    /**
     * produces an exact deep clone of this Object, everything is copied
     * @return Object
     */
    public Object clone() {
        LeafSet dolly = new LeafSet();
        dolly.nodeId = this.nodeId;
        dolly.size = this.size;
        dolly.hsize = this.hsize;
        dolly.left = this.left.clone();
        dolly.right = this.right.clone();
        dolly.nodesL = new Vector<Node>();
        dolly.nodesR = new Vector<Node>();
        
        return dolly;
    }

    //______________________________________________________________________________________________
    /**
     * shortcut to base-representation specifier
     */
    public static final int HEX = 16;

    /**
     * shortcut to base-representation specifier
     */
    public static final int DEC = 10;

    /**
     * shortcut to base-representation specifier
     */
    public static final int NIB = 4;

    /**
     * shortcut to base-representation specifier
     */
    public static final int BIN = 2;

    /**
     * Outputs a representation of this leafset in the form:<BR>
     * <code>[L3;L2;L1;L0]pivot[R0;R1;R2;R3]</code><BR>
     * each entry is represented only partially, to allow a shorter
     * represantation (i.e. is cut after the 4th cipher, e.g.: "4eb0-")
     * @return String
     */
    public String toString() {

      String l = "[XX]";
      for(int i = hsize - 1 ; i>= 0 ; i--){
    	  if (left[i]!=EMPTY)
            l = l.replace("XX", RoutingTable.truncateNodeId(left[i])+";XX");
      }
      l = l.replace(";XX","");
      l = l.replace("XX","");

      String p = "{"+ RoutingTable.truncateNodeId(nodeId)  +"}";

      String r = "[XX]";
      for(int i = 0; (i<hsize)&&(right[i]!=EMPTY);i++)
      r = r.replace("XX", (RoutingTable.truncateNodeId(right[i]))+";XX");
      r = r.replace(";XX","");
      r = r.replace("XX","");

      return l+p+r;
    }
    //______________________________________________________________________________________________


} // End of class
//______________________________________________________________________________________________

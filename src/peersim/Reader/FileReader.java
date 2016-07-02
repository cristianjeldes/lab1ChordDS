package peersim.Reader;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

import peersim.config.Configuration;

public class FileReader implements peersim.core.Control {

    private static final String PAR_PROT = "protocol";
    private static final String PAR_TRANSPORT = "transport";
    private static final String PAR_DATA = "dataset";
    private String prefix;
    private int protocol;
    private int transportid;
    private String dataset;
    private static BufferedReader br;
    private static FileInputStream fstream;
    private static DataInputStream in;
    private static Vector<String> queries = new Vector<String>();

    public FileReader(String prefix) {
        this.prefix = prefix;
        protocol = Configuration.getPid(this.prefix + "." + PAR_PROT);
        transportid = Configuration.getPid(this.prefix + "." + PAR_TRANSPORT);
        dataset = Configuration.getString(this.prefix + "." + PAR_DATA);
    }

    public void readFile(String url) {
        try {
            fstream = new FileInputStream(url);
            in = new DataInputStream(fstream);
            br = new BufferedReader(new InputStreamReader(in));

        } catch (FileNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static String getQuery() {
        String strLine;
        try {
            if ((strLine = br.readLine()) != null) {
                return strLine;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static void closeFile() {
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean execute() {
        System.err.println("QUERY LOG: " + dataset);
        this.readFile(dataset);
        return false;
    }

}

package peersim.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class FileSplit {
    private List<byte[]> listBytes;
    public FileSplit(String filePath) {
        listBytes = splitFile(new File(filePath));
    }

    public byte[] getChunk(int i){
        return listBytes.get(i);
    }
    
    public int sizeListChunk(){
        return listBytes.size();
    }
    
    public List<byte[]> splitFile(File f){
        int sizeOfFiles = 128*1000;// 128*1KByte
        byte[] buffer = new byte[sizeOfFiles];
        List<byte[]> file = new ArrayList<>();
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f))) {
            int tmp = 0;
            while ((tmp = bis.read(buffer)) > 0) {
                file.add(buffer);
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return file;
    }
    
    public byte[] joinFile(List<byte[]> chunks){
        int sizeFile = 0;
        for(int i=0;i<chunks.size();i++){
            sizeFile += chunks.get(i).length;
        }
        System.out.println(sizeFile);
        
        byte[] file = new byte[sizeFile];
        int inicio = 0;
        int fin = 0;
        for(byte[] archivo : chunks){
            System.arraycopy(archivo, 0, file, inicio, archivo.length);
            inicio += archivo.length;
        }
        System.out.println(file.length);
        return file;
    }
}
package com.chedifier.ladder.base;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

public class FileUtils {
	
	public static final boolean writeString2File(String targetPath,String content, boolean append){
		PrintWriter out = null;
		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			fw = new FileWriter(targetPath, append);
			bw = new BufferedWriter(fw);
		    out = new PrintWriter(bw);
		    out.print(content);
		    out.flush();
		    return true;
		}catch (Exception e) {  
            e.printStackTrace();  
        }finally {
    		IOUtils.safeClose(out);
    		IOUtils.safeClose(bw);
    		IOUtils.safeClose(fw);
		}
		
		return false;
	}
}

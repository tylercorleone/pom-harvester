//package com.profilemycamera.pomHarvester;
//
//import java.io.IOException;
//import java.util.HashMap;
//
//import org.junit.Test;
//
//import com.profilemycamera.pomHarvester.PomHarvester;
//import com.profilemycamera.pomHarvester.graph.ArtifactsGraph;
//
///**
// * Unit test for simple App.
// */
//public class AppTest {
//
//    
//	@Test
//    public void testApp() throws IOException {
//		//String input_dir="D:/MyCode/Mypoms";
//		//String output_dir="D:\\MyCode";
//        String inDirName = "src/main/static/test_files/input";
//        String outFileName = "src/main/static/test_files/output";
//        
//        ArtifactsGraph theGraph;
//        HashMap<String, String> paramsMap = new HashMap<String, String>();
//        PomHarvester.configure(paramsMap.get("gav-filter"), paramsMap.get("ignore-versions") != null, paramsMap.get("keep-snapshots") != null, paramsMap.get("wrap-ears") != null, paramsMap.get("exclude-filter"));
//        try {
//			theGraph = PomHarvester.analyze(inDirName);
//			PomHarvester.render(theGraph, outFileName);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        //assertTrue( true );
//    }
//
//}

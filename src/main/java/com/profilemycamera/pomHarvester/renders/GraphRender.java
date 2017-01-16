package com.profilemycamera.pomHarvester.renders;

import java.io.FileWriter;

import com.profilemycamera.pomHarvester.PomHarvesterException;
import com.profilemycamera.pomHarvester.graph.ArtifactsGraph;

public interface GraphRender {
	public void render(ArtifactsGraph graph, FileWriter outFile) throws PomHarvesterException;
}

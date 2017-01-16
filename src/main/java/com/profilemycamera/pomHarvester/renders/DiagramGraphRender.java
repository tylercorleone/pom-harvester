package com.profilemycamera.pomHarvester.renders;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.profilemycamera.pomHarvester.Main;
import com.profilemycamera.pomHarvester.PomHarvester;
import com.profilemycamera.pomHarvester.PomHarvesterException;
import com.profilemycamera.pomHarvester.graph.ArtifactsGraph;
import com.profilemycamera.pomHarvester.utils.Logger;
import com.profilemycamera.pomHarvester.graph.ArtifactNode;

public class DiagramGraphRender implements GraphRender {

	@Override
	public void render(ArtifactsGraph graph, FileWriter outFile) throws PomHarvesterException {
		BufferedWriter out;
		BufferedReader in;

		try {
			out = new BufferedWriter(outFile);
			DataInputStream in_ = new DataInputStream(PomHarvester.class.getClassLoader()
					.getResourceAsStream("templates/header_diagram_template.xml.part"));
			in = new BufferedReader(new InputStreamReader(in_));
			String strLine;
			while ((strLine = in.readLine()) != null) {
				out.write(strLine + "\n");
			}
			
			graph.buildRequirers();

			int nodesCount = 0; // for the x offset

			for (ArtifactNode node : graph) {
				DiagramNodeType nodeType = DiagramNodeType.DEFAULT;
				if (node.getVersion().matches(".*-SNAPSHOT"))
					nodeType = DiagramNodeType.SNAPSHOT;
				if (!node.hasPom())
					nodeType = DiagramNodeType.NO_POM; // overrides the snapshot
												// condition
				String description = "";
				if (graph.size() > 1) {
					Double percentage = node.getRequirers().size() * 100.0 / (graph.size() - 1);
					int decimal = new Double((percentage + 0.05) * 10).intValue() % 10;
					description = node.getRequirers().size() +  "/" + (graph.size() - 1) + " nodes req. this (" + percentage.intValue() + "." + decimal + "%)";
				}

				out.write(new DiagramNode(node, nodeType, nodesCount++, description).toString());

				for (ArtifactNode dependency : node.getDependencies()) {
					out.write("<mxCell id='" + node + "_to_" + dependency
							+ "' style='edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;exitX=0.5;exitY=1;entryX=0.5;entryY=0;jettySize=auto;orthogonalLoop=1;strokeColor=#212121;'"
							+ " edge='1' parent='1' source='" + node + "' target='" + dependency + "'>" + "\n"
							+ "    <mxGeometry relative='1' as='geometry' />" + "\n" + "</mxCell>" + "\n");

				}
			}

			in_ = new DataInputStream(PomHarvester.class.getClassLoader()
					.getResourceAsStream("templates/footer_diagram_template.xml.part"));
			in = new BufferedReader(new InputStreamReader(in_));
			while ((strLine = in.readLine()) != null) {
				out.write(strLine + "\n");
			}
			in.close();
			Logger.info("------------------------------------------------------------------------");
			Logger.info("writing the graph diagram...");
			out.close();
			Logger.info("done. You can open it on https://www.draw.io");
		} catch (IOException e) {
			throw new PomHarvesterException("an IO error occurred:\n" + e.getMessage());
		}
	}

}

/*
 * utility inner classes
 */

class DiagramNode {
	private ArtifactNode node;
	DiagramNodeType type;
	int index;
	String description;

	public DiagramNode(ArtifactNode node, DiagramNodeType type, int index, String description) {
		this.node = node;
		this.type = type;
		this.index = index;
		this.description = description;
	}

	@Override
	public String toString() {
		String style = "";
		switch (type) {
		case SNAPSHOT:
			style = "fillColor=#EEEEEE;strokeColor=#555555;";
			break;
		case NO_POM:
			style = "fillColor=#dae8fc;strokeColor=#6c8ebf;";
			break;
		default:
			style = "fillColor=#FFFF66;strokeColor=#d6b656;";
		}
		return "<mxCell id='" + node
				+ "' value='&lt;font style=&quot;font-size: 15px&quot;face=&quot;Verdana&quot;&gt;&lt;b&gt;"
				+ node.getArtifactId()
				+ "&lt;/b&gt;&lt;/font&gt;&lt;br&gt;&lt;font style=&quot;font-size: 10px&quot;face=&quot;Verdana&quot;&gt;"
				+ node + ": " + description
				+ "&lt;/font&gt;' style='whiteSpace=wrap;html=1;shape=ext;double=1;rounded=1;fontSize=16;glass=0;shadow=1;"
				+ style + "' vertex='1' parent='1'>" + "\n" + "    <mxGeometry x='" + (0 + index * 250)
				+ "' y='0' width='200' height='70' as='geometry' />" + "\n" + "</mxCell>" + "\n";
	}
}

enum DiagramNodeType {
	DEFAULT, SNAPSHOT, NO_POM;
}

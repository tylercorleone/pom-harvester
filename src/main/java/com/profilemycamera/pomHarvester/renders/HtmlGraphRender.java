package com.profilemycamera.pomHarvester.renders;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;

import com.profilemycamera.pomHarvester.Main;
import com.profilemycamera.pomHarvester.PomHarvester;
import com.profilemycamera.pomHarvester.PomHarvesterException;
import com.profilemycamera.pomHarvester.graph.ArtifactsGraph;
import com.profilemycamera.pomHarvester.utils.Logger;
import com.profilemycamera.pomHarvester.graph.ArtifactNode;

public class HtmlGraphRender implements GraphRender {

	@Override
	public void render(ArtifactsGraph graph, FileWriter outFile) throws PomHarvesterException {
		BufferedWriter out;
		BufferedReader in;

		try {
			out = new BufferedWriter(outFile);
			DataInputStream in_ = new DataInputStream(
					PomHarvester.class.getClassLoader().getResourceAsStream("templates/html_header.html.part"));
			in = new BufferedReader(new InputStreamReader(in_));
			String strLine;
			while ((strLine = in.readLine()) != null) {
				out.write(strLine + "\n");
			}

			graph.buildRequirers();
			
			ArrayList<ArtifactNode> sortedGraph = graph.getSortedNodes();

			for (ArtifactNode node : sortedGraph) {
				HtmlNodeType nodeType = HtmlNodeType.NODE;
				// if (node.getVersion().matches(".*-SNAPSHOT"))
				// nodeType = HtmlNodeType.SNAPSHOT;
				// if (!node.hasPom())
				// nodeType = HtmlNodeType.NO_POM; // overrides the snapshot
				// // condition
				String description = "";
				if (graph.size() > 1) {
					Double percentage = node.getRequirers().size() * 100.0 / (graph.size() - 1);
					int decimal = new Double((percentage + 0.05) * 10).intValue() % 10;
					description = node.getRequirers().size() +  "/" + (graph.size() - 1) + " nodes requiring this artifact (" + percentage.intValue() + "." + decimal + "%)";
				}
				out.write(new HtmlNode(node, nodeType, description).toString());

			}

			in_ = new DataInputStream(
					PomHarvester.class.getClassLoader().getResourceAsStream("templates/html_footer.html.part"));
			in = new BufferedReader(new InputStreamReader(in_));
			while ((strLine = in.readLine()) != null) {
				out.write(strLine + "\n");
			}
			in.close();
			Logger.info("------------------------------------------------------------------------");
			Logger.info("writing the html graph table...");
			out.close();
			Logger.info("done.");
		} catch (IOException e) {
			throw new PomHarvesterException("an IO error occurred:\n" + e.getMessage());
		}
	}

}

/*
 * utility inner classes
 */

class HtmlNode {
	private ArtifactNode node;
	private HtmlNodeType type;
	private String description;

	public HtmlNode(ArtifactNode node, HtmlNodeType type, String description) {
		this.node = node;
		this.type = type;
		this.description = description;
	}

	@Override
	public String toString() {
		String renderedNode;
		String cssClass = "";
		switch (type) {
		case NODE:
			cssClass = "node";
			break;
		case DEPENDENCY:
			cssClass = " dependency";
			break;
		case DEPENDER:
			cssClass = " depender";
			break;
		}
		renderedNode = "<div class='" + cssClass + "'"
				+ (this.type == HtmlNodeType.NODE ? "id=" + this.node.toString() : "") + ">";
		renderedNode += "<div class='label'>";
		
		if (this.type != HtmlNodeType.NODE) {
			renderedNode += "<a href='#" + this.node.toString() + "'>";
		}
		
		// the label
		renderedNode += this.node.getArtifactId()
				+ "<br/>"
				+ "<span class='sub-label'>"
				+ this.node.toString() + ": " + this.description
				+ "</span>";
		
		if (this.type != HtmlNodeType.NODE) {
			renderedNode += "</a>";
		}
		
		renderedNode += "</div>"; // the label div
		
		// print dependers
		if (this.type == HtmlNodeType.NODE && node.getDependers().size() != 0) {
			renderedNode += "<div class='dependers-container'>";
			renderedNode += "<div class='label'>Artifacts depending from this node: "
					+ node.getDependers().size() + "</div>";
			for (ArtifactNode dependency : this.node.getDependers()) {
				renderedNode += new HtmlNode(dependency, HtmlNodeType.DEPENDER, "");
			}
			renderedNode += "</div>";
		}
		
		// print dependencies
		if (this.type == HtmlNodeType.NODE && node.getDependencies().size() != 0) {
			renderedNode += "<div class='dependencies-container'>";
			renderedNode += "<div class='label'>Dependencies: "
					+ node.getDependencies().size() + "</div>";
			for (ArtifactNode dependency : this.node.getDependencies()) {
				renderedNode += new HtmlNode(dependency, HtmlNodeType.DEPENDENCY, "");
			}
			renderedNode += "</div>";
		}
		
		renderedNode += "</div>" + "\n";

		return renderedNode;
	}
}

enum HtmlNodeType {
	NODE, DEPENDENCY, DEPENDER;
}

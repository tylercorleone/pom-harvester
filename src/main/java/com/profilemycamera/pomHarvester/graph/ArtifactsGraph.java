package com.profilemycamera.pomHarvester.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

//import org.apache.maven.project.DefaultProjectBuilder;

import com.profilemycamera.pomHarvester.Main;
import com.profilemycamera.pomHarvester.PomHarvesterException;
import com.profilemycamera.pomHarvester.utils.Logger;

public class ArtifactsGraph implements Iterable<ArtifactNode> {
	private HashSet<ArtifactNode> nodes = new HashSet<ArtifactNode>();
	
	private ArtifactNode startNode; // used to discover loops
	private HashSet<ArtifactNode> walkedNodes = new HashSet<ArtifactNode>();

	/**
	 * Add the node to the graph, merging it to the alredy present equal node in present.
	 * @return The node just added
	 */
	public ArtifactNode addNode(ArtifactNode node) {
		if(node == null) return null;
		ArtifactNode oldNode = this.getNode(node);
		if(oldNode != null) {
			if(!oldNode.hasPom() && node.hasPom()) {
				Logger.info("found a pom for " + oldNode);
				oldNode.hasPom(true);
				oldNode.setGroupId(node.getGroupId());
				oldNode.setArtifactId(node.getArtifactId());
				oldNode.setVersion(node.getVersion());
				oldNode.setPackaging(node.getPackaging());
				oldNode.setDependencies(node.getDependencies());
			}
			return oldNode;
		} else {
			Logger.info("adding node " + node + (!node.hasPom() ? " (NO POM)" : "" ));
			nodes.add(node);
			return node;
		}
	}
	
	public boolean removeNode(ArtifactNode node) {
		if(node == null) return false;
		else return nodes.remove(node);
	}
	
	public boolean containsNode(ArtifactNode node) {
		return nodes.contains(node);
	}

	@Override
	public Iterator<ArtifactNode> iterator() {
		return nodes.iterator();
	}
	
//	public Graph merge(Graph other) {
//		if(other != null) {
//			for(Node node : other.nodes) {
//				Node duplicate = this.getNode(node);
//				if(duplicate == null) nodes.add(node); // merge dependencies
//				else duplicate.merge(node); // merge dependencies
//			}
//		}
//		return this;
//	}

	// used to get the node to merge with...
	public ArtifactNode getNode(ArtifactNode node) {
		for(ArtifactNode innerNode : nodes) {
			if(innerNode.equals(node)) return innerNode;
		}
		return null;
	}

	public int size() {
		return nodes.size();
	}
	
	public ArrayList<ArtifactNode> getSortedNodes() {
		ArrayList<ArtifactNode> sortedNodes = new ArrayList<ArtifactNode>();
		for (ArtifactNode node : nodes) {
			sortedNodes.add(node);
		}
		sortedNodes.sort(new Comparator<ArtifactNode>() {
			@Override
			public int compare(ArtifactNode a, ArtifactNode b) {
				String aLabel = a.getArtifactId() + a.toString();
				String bLabel = b.getArtifactId() + b.toString();
				return aLabel.compareTo(bLabel);
			}
		});
		return sortedNodes;
	}
	
	public void buildDependers() {
		for (ArtifactNode node : nodes) {
			for (ArtifactNode dependency : node.getDependencies()) {
				dependency.addDepender(node);
			}
		}
	}
	
	public void buildRequirers() throws PomHarvesterException {
		this.buildDependers();
		for (ArtifactNode node : nodes) {
			buildRequirers(node);
		}
	}
	
	private HashSet<ArtifactNode> buildRequirers(ArtifactNode node) {
		if (startNode == node) {
			walkedNodes.add(node);
			Logger.error("graph contains loops. " + node + " is in a loop path");
			node.setRequirers(new HashSet<ArtifactNode>());
			return node.getRequirers();
		}
		
		if (walkedNodes.contains(node)) return node.getRequirers();
		startNode = node;
		
		HashSet<ArtifactNode> requirers = new HashSet<ArtifactNode>();
		requirers.addAll(node.getDependers());
		
		for (ArtifactNode depender : node.getDependers()) {
			requirers.addAll(buildRequirers(depender));
		}
		node.setRequirers(requirers);
		walkedNodes.add(node);
		return requirers;
	}

}

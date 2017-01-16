package com.profilemycamera.pomHarvester.graph;

import java.util.HashSet;

import com.profilemycamera.pomHarvester.PomHarvester;

public class ArtifactNode {
	private String groupId, artifactId, version, packaging;
	private boolean hasPom = false;
	private HashSet<ArtifactNode> dependencies; // direct dependencies of this node
	private HashSet<ArtifactNode> requireds; 	// direct and transitive dependencies of this node
	
	private HashSet<ArtifactNode> dependers; 	// nodes who have direct dependency to this node
	private HashSet<ArtifactNode> requirers; 	// nodes who have direct and transitive dependency to this node
	
	
	// private static boolean ignoreVersion = false;

	public ArtifactNode() {}
	
	public ArtifactNode(String groupId, String artifactId, String version, String packaging) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.packaging = packaging;
	}
	
	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getPackaging() {
		return packaging;
	}

	public void setPackaging(String packaging) {
		this.packaging = packaging;
	}

	public boolean hasPom() {
		return hasPom;
	}

	public void hasPom(boolean hasPom) {
		this.hasPom = hasPom;
	}
	
	/* ----- DEPENDENCIES -----*/
	public void addDependency(ArtifactNode dependency) {
		if(dependencies == null) dependencies = new HashSet<ArtifactNode>();
		dependencies.add(dependency);
	}
	
	public void setDependencies(HashSet<ArtifactNode> dependencies) {
		this.dependencies = dependencies;
	}
	
	public HashSet<ArtifactNode> getDependencies() {
		if(dependencies == null) dependencies = new HashSet<ArtifactNode>();
		return dependencies;
	}
	
	/* ----- REQUIREDS -----*/
	public void addRequired(ArtifactNode required) {
		// if(requireds == null) requireds = new HashSet<Node>();
		requireds.add(required);
	}
	
	public void setRequirers(HashSet<ArtifactNode> requirers) {
		this.requirers = requirers;
	}
	
	public HashSet<ArtifactNode> getRequireds() {
		if(requireds == null) requireds = new HashSet<ArtifactNode>();
		return requireds;
	}
	
	/* ----- DEPENDINGS to this node -----*/
	public void addDepender(ArtifactNode depender) {
		if(dependers == null) dependers = new HashSet<ArtifactNode>();
		dependers.add(depender);
	}
	
	public HashSet<ArtifactNode> getDependers() {
		if(dependers == null) dependers = new HashSet<ArtifactNode>();
		return dependers;
	}
	
	/* ----- REQUIRINGS this node -----*/
	public void addRequirer(ArtifactNode requirer) {
		if(requirers == null) requirers = new HashSet<ArtifactNode>();
		requirers.add(requirer);
	}
	
	public HashSet<ArtifactNode> getRequirers() {
		if(requirers == null) requirers = new HashSet<ArtifactNode>();
		return requirers;
	}
	
	/*
	 */
	
	@Override
	public String toString() {
		return groupId + ":" + artifactId + (PomHarvester.ignoreVersions() ? "" : "-" + version) + "." + packaging;
	}
	
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof ArtifactNode)) return false;
		ArtifactNode other_ = (ArtifactNode) other;
		boolean versionsAreEquals = true;
		boolean packagingsAreEquals = packaging.equals(other_.packaging);
		
		if(!PomHarvester.ignoreVersions() && !version.equals(other_.version)) versionsAreEquals = false;

		return groupId.equals(other_.groupId) && artifactId.equals(other_.artifactId) && versionsAreEquals && packagingsAreEquals;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + ((packaging == null) ? 0 : packaging.hashCode());
		if(!PomHarvester.ignoreVersions())
			result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}
}

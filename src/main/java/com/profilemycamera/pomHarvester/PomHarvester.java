package com.profilemycamera.pomHarvester;

import java.io.File;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.profilemycamera.pomHarvester.graph.ArtifactsGraph;
import com.profilemycamera.pomHarvester.graph.ArtifactNode;
import com.profilemycamera.pomHarvester.renders.DiagramGraphRender;
import com.profilemycamera.pomHarvester.renders.GraphRender;
import com.profilemycamera.pomHarvester.renders.HtmlGraphRender;
import com.profilemycamera.pomHarvester.utils.Logger;

public class PomHarvester {
	private static String gavFilter;
	private static boolean ignoreVersions; // ignore versions differences
										   // between artifacts?
	private static boolean keepSnapshots; // remove -SNAPSHOT to a ependency
										  // if a stable version is found?
	private static boolean wrapEars; // whether wrap ear's modules into the
									 // ear's node
	private static String excludeFilter;
	private static boolean html;		// writes an html table of dependencies
	
	private static int parsedPomCounter = 0; // used to provide statistics
	
	public static boolean ignoreVersions() {
		return ignoreVersions;
	}
	
	

	public static void configure(String gavFilter, boolean ignoreVersions, boolean keepSnapshots,
			boolean wrapEars, String excludeFilter, boolean html) {
		PomHarvester.gavFilter = gavFilter;
		PomHarvester.ignoreVersions = ignoreVersions;
		PomHarvester.keepSnapshots = keepSnapshots;
		PomHarvester.wrapEars = wrapEars;
		PomHarvester.excludeFilter = excludeFilter;
		PomHarvester.html = html;
	}

	public static ArtifactsGraph analyze(String dirName) throws PomHarvesterException {
		File directory = new File(dirName != null ? dirName : "");
		return PomHarvester.analyze(directory);
	}
	
	public static ArtifactsGraph analyze(File directory) throws PomHarvesterException {
		if(directory == null || !directory.isDirectory()) throw new PomHarvesterException("\"" + directory.getPath() + "\" is not a directory" );
		
		ArtifactsGraph graph = new ArtifactsGraph();
		Logger.info("<hr/>");
		Logger.info("Scanning for Maven pom files in \"" + directory + "\"...");
		if(PomHarvester.ignoreVersions)
			Logger.info("ignoring versions");
		if(PomHarvester.gavFilter != null)
			Logger.info("filtering \"" + PomHarvester.gavFilter + "\" artifacts");
		Logger.info();
		PomHarvester.harvestNodes(graph, directory);
		Logger.info();
		Logger.info(PomHarvester.parsedPomCounter + " poms found");
		Logger.info(graph.size() + " nodes in the graph");
		Logger.info();
		
		if(PomHarvester.wrapEars || !PomHarvester.keepSnapshots) {
			Logger.info("------------------------------------------------------------------------");
			Logger.info("Post processing the graph...");
			Logger.info();
			PomHarvester.postAnalyze(graph);
			Logger.info();
		}
		return graph;
	}
	
	private static ArtifactsGraph harvestNodes(ArtifactsGraph graph, File input) throws PomHarvesterException {
		if(input == null || !input.exists()) throw new PomHarvesterException("cannot read from \"" + input + "\"");
		
		if(input.isDirectory()) {
			for(File innerInput : input.listFiles()) {
				PomHarvester.harvestNodes(graph, innerInput);
			}
		} else {
			if (!input.getName().matches(".*\\.xml") && !input.getName().matches(".*\\.pom")) return graph;
//			Logger.info("[INFO] parsing " + input.getPath());
			++PomHarvester.parsedPomCounter;
			try {
				PomHarvester.parsePom(graph, input);
			} catch (PomHarvesterException e) {
				Logger.error("an error occurred while parsing pom \"" + input.getPath() + "\":\n\t" + e.getMessage());
			} catch (Exception e) {
				throw new PomHarvesterException(e.getMessage());
			}
		}
		return graph;
	}

	private static void parsePom(ArtifactsGraph graph, File pom) throws PomHarvesterException {
		ArtifactNode theNode = new ArtifactNode();
		theNode.hasPom(true);

		try {
			SAXBuilder builder = new SAXBuilder();
			Document document = (Document) builder.build(pom);
			Element rootNode = document.getRootElement();
	
			List list = rootNode.getChildren();
			for (int i = list.size(); --i >= 0;) {
				Element xmlNode = (Element) list.get(i);
				String xmlNodeName = xmlNode.getName();
	
				/* getting groupId from parent definition node */
				if ("parent".equals(xmlNodeName) && (theNode.getGroupId() == null || theNode.getArtifactId() == null)) {
					List xmlParentNodeList = xmlNode.getChildren();
					for (int j = xmlParentNodeList.size(); --j >= 0;) {
						Element xmlParentSubNode = (Element) xmlParentNodeList.get(j);
						if ("groupId".equals(xmlParentSubNode.getName()) && theNode.getGroupId() == null) {
							theNode.setGroupId(xmlParentSubNode.getTextTrim());
						} else if ("version".equals(xmlParentSubNode.getName()) && theNode.getVersion() == null) {
							theNode.setVersion(xmlParentSubNode.getTextTrim());
						}
					}
				} else if ("groupId".equals(xmlNodeName))
					theNode.setGroupId(xmlNode.getTextTrim());
				else if ("artifactId".equals(xmlNodeName))
					theNode.setArtifactId(xmlNode.getTextTrim());
				else if ("version".equals(xmlNodeName))
					theNode.setVersion(xmlNode.getTextTrim());
				else if ("packaging".equals(xmlNodeName))
					theNode.setPackaging(xmlNode.getTextTrim());
			} // for every xml node
	
			if (theNode.getPackaging() == null)
				theNode.setPackaging("jar");
			if(theNode.getVersion() == null && PomHarvester.ignoreVersions)
				theNode.setVersion("???");
	
			if (theNode.getGroupId() == null || theNode.getArtifactId() == null || theNode.getVersion() == null
					|| theNode.getPackaging() == null)
				throw new PomHarvesterException("skipping \"" + pom + "\": invalid pom coordinates (" + theNode + ")");
			
			// skip the artifact
			if(PomHarvester.gavFilter != null && !theNode.toString().matches(PomHarvester.gavFilter)) return;
			
			graph.addNode(theNode);
			
			for (int i = list.size(); --i >= 0;) {
				Element xmlNode = (Element) list.get(i);
				String xmlNodeName = xmlNode.getName();
	
				if (!"dependencies".equals(xmlNodeName)) continue;
	
				List xmlDepsList = xmlNode.getChildren();
				for (int j = xmlDepsList.size(); --j >= 0;) {
					xmlNode = (Element) xmlDepsList.get(j);
					
					if (!"dependency".equals(xmlNode.getName())) continue;

					ArtifactNode dependency = new ArtifactNode();
					List xmlDepList = xmlNode.getChildren();
					for (int k = xmlDepList.size(); --k >= 0;) {
						Element node3 = (Element) xmlDepList.get(k);
						String name3 = node3.getName();
						if ("groupId".equals(name3)) {
							dependency.setGroupId(node3.getTextTrim());
						} else if ("artifactId".equals(name3)) {
							dependency.setArtifactId(node3.getTextTrim());
						} else if ("version".equals(name3)) {
							dependency.setVersion(node3.getTextTrim());
						} else if ("type".equals(name3)) {
							dependency.setPackaging(node3.getTextTrim());
						}
					}
					if ((dependency.getVersion() == null && "ear".equals(theNode.getPackaging()))
							|| "${project.version}".equals(dependency.getVersion())) {
						dependency.setVersion(theNode.getVersion());
					}
					if (dependency.getPackaging() == null)
						dependency.setPackaging("jar");
					if(dependency.getVersion() == null && PomHarvester.ignoreVersions)
						dependency.setVersion("???");
	
					// skips dependencies not matching the pattern
					if (PomHarvester.gavFilter == null || dependency.toString().matches(PomHarvester.gavFilter)) {
						if (dependency.getGroupId() == null || dependency.getArtifactId() == null
								|| dependency.getVersion() == null || dependency.getPackaging() == null) {
							Logger.warning("skipping dependency, invalid coordinates: " + dependency);
							continue;
						}
						theNode.addDependency(graph.addNode(dependency));
					}
				}
			} // for every xml node
		} catch (Exception e) {
			throw new PomHarvesterException(e.getMessage());
		}
	}

	private static void postAnalyze(ArtifactsGraph graph) {
		
		if(!PomHarvester.ignoreVersions && !PomHarvester.keepSnapshots) {
			Logger.info("Trying to resolve snapshots:");
			HashMap<ArtifactNode, ArtifactNode> snapToStables = new HashMap<ArtifactNode, ArtifactNode>();
			// stabilize nodes
			for(ArtifactNode node : graph) {
				if(!node.getVersion().matches(".*-SNAPSHOT")) continue;
				// node is snapshot
				ArtifactNode stabilizedNode = new ArtifactNode(node.getGroupId(), node.getArtifactId(), "", node.getPackaging());
				stabilizedNode.setVersion(node.getVersion().substring(0, node.getVersion().length() - 9));
				ArtifactNode stableNode = graph.getNode(stabilizedNode);
				if(stableNode != null && stableNode.hasPom()) { // if it exists a stable version of the node
					Logger.info("found a stable version of " + node);
					// stableNode.merge(node);
					snapToStables.put(node, stableNode);
				}
			}
			// remove the just redirected snapshots
			for(ArtifactNode snapshot : snapToStables.keySet()) {
				graph.removeNode(snapshot);
			}
			// redirect dependencies
			for(ArtifactNode node : graph) {
				HashSet<ArtifactNode> dependencies = node.getDependencies();
				for(ArtifactNode snapshot : snapToStables.keySet()) {
					if(dependencies.remove(snapshot)) {
						dependencies.add(snapToStables.get(snapshot));
					}
				}
			}
			Logger.info(""); // to separe
		}
		
		if(PomHarvester.wrapEars) {
			Logger.info("wrapping enterprise applications:");
			HashMap<ArtifactNode, ArtifactNode> modToEar = new HashMap<ArtifactNode, ArtifactNode>();
			// wrap modules
			for(ArtifactNode ear : graph) {
				if(!ear.getPackaging().equals("ear") || !ear.hasPom()) continue;
				// node is an ear
				HashSet<ArtifactNode> modulesDependencies = new HashSet<ArtifactNode>();
				for (ArtifactNode module : ear.getDependencies()) {
					Logger.info("redirecting " + module + " to " + ear);
					modToEar.put(module, ear); // redirects the module to the containing ear
					for(ArtifactNode dependency : module.getDependencies()) {
						if(!ear.getDependencies().contains(dependency))
							modulesDependencies.add(dependency);
					}
				}
				ear.setDependencies(modulesDependencies);
			}
			// remove the just wrapped modules
			for(ArtifactNode module : modToEar.keySet()) { // remove nodes of redirected modules
				graph.removeNode(module);
			}
			// redirect dependencies
			for(ArtifactNode node : graph) {
				HashSet<ArtifactNode> dependencies = node.getDependencies();
				for(ArtifactNode module : modToEar.keySet()) {
					if(dependencies.remove(module)) {
						dependencies.add(modToEar.get(module));
					}
				}
			}
		}
	}
	
	public static void render(ArtifactsGraph theGraph, String outFileName) throws IOException, PomHarvesterException {
		if (theGraph == null)
			throw new PomHarvesterException("invalid graph");
		if (outFileName == null || outFileName.isEmpty())
			throw new PomHarvesterException("empty file name");

		String outFileNameFull = outFileName + (PomHarvester.html ? ".html" : ".xml");
		File file = new File(outFileNameFull);
		
		if (file.isDirectory()) throw new PomHarvesterException("\"" + outFileNameFull + "\" is a directory");
		if (file.isFile()) {
			java.util.Scanner scanner = new java.util.Scanner(System.in);
			String choise;
			while(true) {
				Logger.warning("<hr/>");
				Logger.warning("the file \"" + outFileNameFull + "\" alredy exists.");
				Logger.warning("do you want to overwrite it? (y/n): ", false);
				choise = scanner.nextLine();
				Logger.warning();
				if("Y".equalsIgnoreCase(choise)) break;
				if("N".equalsIgnoreCase(choise)) System.exit(0);
			}
		}
		
		FileWriter outFile;
		try {
			outFile = new FileWriter(outFileNameFull);
		} catch (IOException e) {
			throw new PomHarvesterException("IO error trying to open \"" + outFileNameFull + "\":\n" + e.getMessage());
		}
		GraphRender graphRender;
		if(PomHarvester.html)
			graphRender = new HtmlGraphRender();
		else
			graphRender = new DiagramGraphRender();
		
		try {
			Logger.info("<hr/>");
			Logger.info("rendering the graph...");
			Logger.info();
			graphRender.render(theGraph, outFile);
			Logger.info("<hr/>");
		} catch (PomHarvesterException e) {
			throw e; // for the moment
		}
	}
}

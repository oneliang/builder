package com.oneliang.tools.builder.base;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Project {

    public static final String CLASSES = "classes";
    public static final String CACHE = "cache";
    public static final String BUILD = "build";

    protected String workspace = null;
    protected String name = null;
    protected String home = null;
    protected String outputHome = null;
    protected String classesOutput = null;
    protected String cacheOutput = null;
    protected String prepareOutput = null;
    protected String[] sources = null;
    protected String[] dependProjects = null;
    protected List<String> sourceDirectoryList = new ArrayList<String>();
    protected Set<String> dependJarSet = new HashSet<String>();
    // use in building
    private List<Project> parentProjectList = null;

    public Project() {
    }

    public Project(String workspace, String name) {
        this(workspace, name, workspace);
    }

    public Project(String workspace, String name, String outputHome) {
        if (workspace == null || name == null) {
            throw new NullPointerException("workspace or name is null");
        }
        this.workspace = workspace;
        this.name = name;
        this.outputHome = outputHome;
    }

    public void initialize() {
        File file = new File(this.workspace);
        this.workspace = file.getAbsolutePath();
        this.home = this.workspace + "/" + this.name;
        file = new File(this.outputHome);
        this.outputHome = file.getAbsolutePath() + "/" + this.name + "/" + BUILD;
        this.classesOutput = this.outputHome + "/" + CLASSES;
        this.cacheOutput = this.outputHome + "/" + CACHE;
        this.prepareOutput = this.outputHome + "/prepare";
    }

    /**
     * @return the workspace
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * @param workspace
     *            the workspace to set
     */
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the home
     */
    public String getHome() {
        return home;
    }

    /**
     * @param sources
     *            the sources to set
     */
    public void setSources(String[] sources) {
        if (sources != null && sources.length > 0) {
            this.sources = sources;
        }
    }

    /**
     * @return the sourceDirectoryList
     */
    public List<String> getSourceDirectoryList() {
        return sourceDirectoryList;
    }

    /**
     * @param dependJarSet
     *            the dependJarSet to set
     */
    public void addDependJar(String dependJar) {
        this.dependJarSet.add(dependJar);
    }

    /**
     * @return the dependJarSet
     */
    public Set<String> getDependJarSet() {
        return dependJarSet;
    }

    /**
     * @param outputHome
     *            the outputHome to set
     */
    public void setOutputHome(String outputHome) {
        this.outputHome = outputHome;
    }

    /**
     * @return the outputHome
     */
    public String getOutputHome() {
        return outputHome;
    }

    /**
     * @param classesOutput
     *            the classesOutput to set
     */
    public void setClassesOutput(String classesOutput) {
        this.classesOutput = classesOutput;
    }

    /**
     * @return the classesOutput
     */
    public String getClassesOutput() {
        return classesOutput;
    }

    /**
     * @return the cacheOutput
     */
    public String getCacheOutput() {
        return cacheOutput;
    }

    /**
     * @return the dependProjects
     */
    public String[] getDependProjects() {
        return dependProjects;
    }

    /**
     * @param dependProjects
     *            the dependProjects to set
     */
    public void setDependProjects(String[] dependProjects) {
        this.dependProjects = dependProjects;
    }

    /**
     * @return the prepareOutput
     */
    public String getPrepareOutput() {
        return prepareOutput;
    }

    /**
     * @return the parentProjectList
     */
    public List<Project> getParentProjectList() {
        return parentProjectList;
    }

    /**
     * @param parentProjectList
     *            the parentProjectList to set
     */
    public void setParentProjectList(List<Project> parentProjectList) {
        this.parentProjectList = parentProjectList;
    }
}

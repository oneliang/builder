package com.oneliang.tools.builder.base;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.android.dx.merge.DexMerger;
import com.oneliang.Constant;
import com.oneliang.util.common.JavaXmlUtil;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.file.FileUtil.ZipEntryPath;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;
import com.sun.tools.javac.Main;

import proguard.ProGuard;

public final class BuilderUtil {

    private static final Logger logger = LoggerManager.getLogger(BuilderUtil.class);

    private static boolean isWindowsOS = false;
    static {
        String osName = System.getProperty("os.name");
        if (StringUtil.isNotBlank(osName) && osName.toLowerCase().indexOf("windows") > -1) {
            isWindowsOS = true;
        }
    }

    /**
     * list to command string
     * 
     * @param stringList
     * @param appendString
     * @param seperator
     * @return String
     */
    public static String listToCommandString(List<String> stringList, String appendString, String seperator) {
        StringBuilder stringBuilder = new StringBuilder();
        int index = 0;
        for (String string : stringList) {
            stringBuilder.append(string + StringUtil.nullToBlank(appendString));
            if (index < stringList.size() - 1) {
                stringBuilder.append(seperator);
            }
            index++;
        }
        return stringBuilder.toString();
    }

    /**
     * array to command string
     * 
     * @param stringArray
     * @param appendString
     *            can null
     * @param seperator
     *            can not null
     * @return String
     */
    public static String arrayToCommandString(String[] stringArray, String appendString, String seperator) {
        StringBuilder stringBuilder = new StringBuilder();
        int index = 0;
        for (String string : stringArray) {
            stringBuilder.append(string + StringUtil.nullToBlank(appendString));
            if (index < stringArray.length - 1) {
                stringBuilder.append(seperator);
            }
            index++;
        }
        return stringBuilder.toString();
    }

    /**
     * merge android manifest
     * 
     * @param packageName
     * @param allAndroidManifestList
     * @param androidManifestOutputFullFilename
     * @param isDebug
     */
    public static void mergeAndroidManifest(String packageName, int minSdkVersion, int targetSdkVersion, List<String> allAndroidManifestList, String androidManifestOutputFullFilename, boolean isDebug) {
        if (allAndroidManifestList == null || allAndroidManifestList.isEmpty()) {
            return;
        }
        try {
            List<Node> needAddPermissionNodeList = new ArrayList<Node>();
            List<Node> needAddApplicationSubNodeList = new ArrayList<Node>();
            Map<String, String> applicationAttributeMap = new HashMap<String, String>();
            Map<String, String> manifestAttributeMap = new HashMap<String, String>();
            String oneAndroidManifest = allAndroidManifestList.remove(0);
            // library
            for (String libraryAndroidManifest : allAndroidManifestList) {
                if (FileUtil.isExist(libraryAndroidManifest)) {
                    Document libraryDocument = JavaXmlUtil.parse(libraryAndroidManifest);
                    NodeList permissionNodeList = libraryDocument.getElementsByTagName("permission");
                    if (permissionNodeList != null) {
                        for (int i = 0; i < permissionNodeList.getLength(); i++) {
                            needAddPermissionNodeList.add(permissionNodeList.item(i));
                        }
                    }
                    NodeList usesPermissionNodeList = libraryDocument.getElementsByTagName("uses-permission");
                    if (usesPermissionNodeList != null) {
                        for (int i = 0; i < usesPermissionNodeList.getLength(); i++) {
                            needAddPermissionNodeList.add(usesPermissionNodeList.item(i));
                        }
                    }
                    NodeList usesFeatureNodeList = libraryDocument.getElementsByTagName("uses-feature");
                    if (usesFeatureNodeList != null) {
                        for (int i = 0; i < usesFeatureNodeList.getLength(); i++) {
                            needAddPermissionNodeList.add(usesFeatureNodeList.item(i));
                        }
                    }
                    NodeList serviceNodeList = libraryDocument.getElementsByTagName("service");
                    if (serviceNodeList != null) {
                        for (int i = 0; i < serviceNodeList.getLength(); i++) {
                            needAddApplicationSubNodeList.add(serviceNodeList.item(i));
                        }
                    }
                    NodeList activityNodeList = libraryDocument.getElementsByTagName("activity");
                    if (activityNodeList != null) {
                        for (int i = 0; i < activityNodeList.getLength(); i++) {
                            needAddApplicationSubNodeList.add(activityNodeList.item(i));
                        }
                    }
                    NodeList receiverNodeList = libraryDocument.getElementsByTagName("receiver");
                    if (receiverNodeList != null) {
                        for (int i = 0; i < receiverNodeList.getLength(); i++) {
                            needAddApplicationSubNodeList.add(receiverNodeList.item(i));
                        }
                    }
                    NodeList metaDataNodeList = libraryDocument.getElementsByTagName("meta-data");
                    if (metaDataNodeList != null) {
                        for (int i = 0; i < metaDataNodeList.getLength(); i++) {
                            needAddApplicationSubNodeList.add(metaDataNodeList.item(i));
                        }
                    }
                    NodeList providerNodeList = libraryDocument.getElementsByTagName("provider");
                    if (providerNodeList != null) {
                        for (int i = 0; i < providerNodeList.getLength(); i++) {
                            needAddApplicationSubNodeList.add(providerNodeList.item(i));
                        }
                    }
                    NodeList applicationNodeList = libraryDocument.getElementsByTagName("application");
                    if (applicationNodeList != null) {
                        for (int i = 0; i < applicationNodeList.getLength(); i++) {
                            Node applicationNode = applicationNodeList.item(i);
                            NamedNodeMap applicationNamedNodeMap = applicationNode.getAttributes();
                            for (int j = 0; j < applicationNamedNodeMap.getLength(); j++) {
                                Node attributeNode = applicationNamedNodeMap.item(j);
                                applicationAttributeMap.put(attributeNode.getNodeName(), attributeNode.getNodeValue());
                            }
                        }
                    }
                    NodeList manifestNodeList = libraryDocument.getElementsByTagName("manifest");
                    if (manifestNodeList != null) {
                        for (int i = 0; i < manifestNodeList.getLength(); i++) {
                            Node applicationNode = manifestNodeList.item(i);
                            NamedNodeMap applicationNamedNodeMap = applicationNode.getAttributes();
                            for (int j = 0; j < applicationNamedNodeMap.getLength(); j++) {
                                Node attributeNode = applicationNamedNodeMap.item(j);
                                manifestAttributeMap.put(attributeNode.getNodeName(), attributeNode.getNodeValue());
                            }
                        }
                    }
                }
            }

            Document document = JavaXmlUtil.parse(oneAndroidManifest);
            if (document != null) {
                Element root = document.getDocumentElement();
                for (Node node : needAddPermissionNodeList) {
                    root.appendChild(document.importNode(node, true));
                }
                // sdk version
                Element sdkVersionElement = document.createElement("uses-sdk");
                sdkVersionElement.setAttribute("android:minSdkVersion", String.valueOf(minSdkVersion));
                sdkVersionElement.setAttribute("android:targetSdkVersion", String.valueOf(targetSdkVersion));
                root.appendChild(sdkVersionElement);
                // manifest
                NodeList manifestNodeList = document.getElementsByTagName("manifest");
                if (manifestNodeList != null && manifestNodeList.getLength() > 0) {
                    Set<String> manifestAttributeNameSet = manifestAttributeMap.keySet();
                    for (String attributeName : manifestAttributeNameSet) {
                        ((Element) manifestNodeList.item(0)).setAttribute(attributeName, manifestAttributeMap.get(attributeName));
                    }
                    if (StringUtil.isNotBlank(packageName)) {
                        ((Element) manifestNodeList.item(0)).setAttribute("package", packageName);
                    }
                }
                // application
                NodeList applicationNodeList = document.getElementsByTagName("application");
                if (applicationNodeList != null && applicationNodeList.getLength() > 0) {
                    Set<String> applicationAttributeNameSet = applicationAttributeMap.keySet();
                    for (String attributeName : applicationAttributeNameSet) {
                        ((Element) applicationNodeList.item(0)).setAttribute(attributeName, applicationAttributeMap.get(attributeName));
                    }
                    if (isDebug) {
                        ((Element) applicationNodeList.item(0)).setAttribute("android:debuggable", String.valueOf(true));
                    }
                    for (Node node : needAddApplicationSubNodeList) {
                        applicationNodeList.item(0).appendChild(document.importNode(node, true));
                    }
                }
                JavaXmlUtil.saveDocument(document, androidManifestOutputFullFilename);
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * execute android aapt to generate R.java
     * 
     * @param aaptExecutePath
     * @param androidManifest
     * @param resourceDirectoryList
     * @param destinationDirectory
     * @param dependJarList
     * @return int
     */
    public static int executeAndroidAaptToGenerateR(String aaptExecutePath, String androidManifest, List<String> resourceDirectoryList, String destinationDirectory, List<String> dependJarList, boolean isDebug) {
        return executeAndroidAapt(aaptExecutePath, androidManifest, resourceDirectoryList, null, destinationDirectory, dependJarList, null, true, isDebug);
    }

    /**
     * execute android aapt to package resource
     * 
     * @param aaptExecutePath
     * @param androidManifest
     * @param resourceDirectoryList
     * @param assetsDirectoryList
     * @param dependJarList
     * @param resourceOutputFullFilename
     * @return int
     */
    public static int executeAndroidAaptToPackageResource(String aaptExecutePath, String androidManifest, List<String> resourceDirectoryList, List<String> assetsDirectoryList, List<String> dependJarList, String resourceOutputFullFilename, boolean isDebug) {
        return executeAndroidAapt(aaptExecutePath, androidManifest, resourceDirectoryList, assetsDirectoryList, null, dependJarList, resourceOutputFullFilename, false, isDebug);
    }

    /**
     * execute android aapt
     * 
     * @param aaptExecutePath
     * @param androidManifest
     * @param resourceDirectoryList
     * @param assetsDirectoryList
     * @param destinationDirectory
     * @param dependJarList
     * @param resourceOutputFullFilename
     * @param isGenerateR
     * @return int,exit code
     */
    private static int executeAndroidAapt(String aaptExecutePath, String androidManifest, List<String> resourceDirectoryList, List<String> assetsDirectoryList, String destinationDirectory, List<String> dependJarList, String resourceOutputFullFilename, boolean isGenerateR, boolean isDebug) {
        String command = generateAndroidAaptCommand(aaptExecutePath, androidManifest, resourceDirectoryList, assetsDirectoryList, destinationDirectory, dependJarList, resourceOutputFullFilename, isGenerateR, isDebug);
        return executeCommand(new String[] { command }, true, false, true);
    }

    /**
     * generate android aapt to generate R.java command
     * 
     * @param aaptExecutePath
     * @param androidManifest
     * @param resourceDirectoryList
     * @param destinationDirectory
     * @param dependJarList
     * @return String
     */
    public static String generateAndroidAaptToGenerateRCommand(String aaptExecutePath, String androidManifest, List<String> resourceDirectoryList, String destinationDirectory, List<String> dependJarList, boolean isDebug) {
        return generateAndroidAaptCommand(aaptExecutePath, androidManifest, resourceDirectoryList, null, destinationDirectory, dependJarList, null, true, isDebug);
    }

    /**
     * generate android aapt to package resource command
     * 
     * @param aaptExecutePath
     * @param androidManifest
     * @param resourceDirectoryList
     * @param assetsDirectoryList
     * @param dependJarList
     * @param resourceOutputFullFilename
     * @return int
     */
    public static String generateAndroidAaptToPackageResourceCommand(String aaptExecutePath, String androidManifest, List<String> resourceDirectoryList, List<String> assetsDirectoryList, List<String> dependJarList, String resourceOutputFullFilename, boolean isDebug) {
        return generateAndroidAaptCommand(aaptExecutePath, androidManifest, resourceDirectoryList, assetsDirectoryList, null, dependJarList, resourceOutputFullFilename, false, isDebug);
    }

    /**
     * execute android aapt
     * 
     * @param aaptExecutePath
     * @param androidManifest
     * @param resourceDirectoryList
     * @param assetsDirectoryList
     * @param destinationDirectory
     * @param dependJarList
     * @param resourceOutputFullFilename
     * @param isGenerateR
     * @return String
     */
    private static String generateAndroidAaptCommand(String aaptExecutePath, String androidManifest, List<String> resourceDirectoryList, List<String> assetsDirectoryList, String destinationDirectory, List<String> dependJarList, String resourceOutputFullFilename, boolean isGenerateR, boolean isDebug) {
        StringBuilder commandStringBuilder = new StringBuilder();
        if (aaptExecutePath.indexOf(StringUtil.SPACE) > 0 && BuilderUtil.isWindowsOS()) {
            aaptExecutePath = Constant.Symbol.DOUBLE_QUOTES + aaptExecutePath + Constant.Symbol.DOUBLE_QUOTES;
        }
        commandStringBuilder.append(aaptExecutePath + StringUtil.SPACE);
        commandStringBuilder.append("package" + StringUtil.SPACE);
        // commandStringBuilder.append("-v"+StringUtil.SPACE);
        commandStringBuilder.append("-f" + StringUtil.SPACE);
        if (isGenerateR) {
            commandStringBuilder.append("--generate-dependencies" + StringUtil.SPACE);
        }
        commandStringBuilder.append("-m" + StringUtil.SPACE);
        commandStringBuilder.append("--auto-add-overlay" + StringUtil.SPACE);
        commandStringBuilder.append("-M" + StringUtil.SPACE);
        commandStringBuilder.append(androidManifest + StringUtil.SPACE);
        if (resourceDirectoryList != null) {
            for (String resourceDirectory : resourceDirectoryList) {
                commandStringBuilder.append("-S" + StringUtil.SPACE);
                commandStringBuilder.append(resourceDirectory + StringUtil.SPACE);
            }
        }
        if (!isGenerateR) {
            if (assetsDirectoryList != null) {
                for (String assetDirectory : assetsDirectoryList) {
                    commandStringBuilder.append("-A" + StringUtil.SPACE);
                    commandStringBuilder.append(assetDirectory + StringUtil.SPACE);
                }
            }
        }
        if (isGenerateR) {
            if (isDebug) {
                commandStringBuilder.append("--non-constant-id" + StringUtil.SPACE);
            }
            commandStringBuilder.append("-J" + StringUtil.SPACE);
            commandStringBuilder.append(destinationDirectory + StringUtil.SPACE);
            commandStringBuilder.append("--output-text-symbols" + StringUtil.SPACE);
            commandStringBuilder.append(destinationDirectory + StringUtil.SPACE);
        } else {
            commandStringBuilder.append("-F" + StringUtil.SPACE);
            commandStringBuilder.append(resourceOutputFullFilename + StringUtil.SPACE);
        }
        if (dependJarList != null) {
            for (String dependJar : dependJarList) {
                commandStringBuilder.append("-I" + StringUtil.SPACE);
                if (dependJar.indexOf(StringUtil.SPACE) > 0 && BuilderUtil.isWindowsOS()) {
                    dependJar = Constant.Symbol.DOUBLE_QUOTES + dependJar + Constant.Symbol.DOUBLE_QUOTES;
                }
                commandStringBuilder.append(dependJar + StringUtil.SPACE);
            }
        }
        return commandStringBuilder.toString();
    }

    /**
     * execute android aidl
     * 
     * @param androidAidlExecutorPath
     * @param frameworkAidlFullFilename
     * @param sourceDirectory
     * @param destinationDirectory
     * @param aidlFullFilename
     * @return int,exit code
     */
    public static int executeAndroidAidl(String androidAidlExecutorPath, String frameworkAidlFullFilename, String sourceDirectory, String destinationDirectory, String aidlFullFilename) {
        StringBuilder commandStringBuilder = new StringBuilder();
        if (androidAidlExecutorPath.indexOf(StringUtil.SPACE) > 0 && BuilderUtil.isWindowsOS()) {
            androidAidlExecutorPath = Constant.Symbol.DOUBLE_QUOTES + androidAidlExecutorPath + Constant.Symbol.DOUBLE_QUOTES;
        }
        commandStringBuilder.append(androidAidlExecutorPath + StringUtil.SPACE);
        if (frameworkAidlFullFilename.indexOf(StringUtil.SPACE) > 0 && BuilderUtil.isWindowsOS()) {
            frameworkAidlFullFilename = Constant.Symbol.DOUBLE_QUOTES + frameworkAidlFullFilename + Constant.Symbol.DOUBLE_QUOTES;
        }
        commandStringBuilder.append("-p" + frameworkAidlFullFilename + StringUtil.SPACE);
        commandStringBuilder.append("-I" + sourceDirectory + StringUtil.SPACE);
        commandStringBuilder.append("-o" + destinationDirectory + StringUtil.SPACE);
        commandStringBuilder.append(aidlFullFilename);
        return executeCommand(new String[] { commandStringBuilder.toString() });
    }

    /**
     * execute android dx
     * 
     * @param androidDxExecutorPath
     * @param androidDxParameters
     * @param outputDexFullFilename
     * @param classesDirectoryListAndLibraryList
     */
    public static void executeAndroidDx(String androidDxExecutorPath, String androidDxParameters, String outputDexFullFilename, List<String> classesDirectoryListAndLibraryList) {
        StringBuilder commandStringBuilder = new StringBuilder();
        if (androidDxExecutorPath.indexOf(StringUtil.SPACE) > 0 && BuilderUtil.isWindowsOS()) {
            androidDxExecutorPath = Constant.Symbol.DOUBLE_QUOTES + androidDxExecutorPath + Constant.Symbol.DOUBLE_QUOTES;
        }
        commandStringBuilder.append(androidDxExecutorPath + StringUtil.SPACE);
        commandStringBuilder.append("--dex" + StringUtil.SPACE);
        commandStringBuilder.append(androidDxParameters + StringUtil.SPACE);
        commandStringBuilder.append("--output=" + outputDexFullFilename + StringUtil.SPACE);
        commandStringBuilder.append(listToCommandString(classesDirectoryListAndLibraryList, null, StringUtil.SPACE));
        executeCommand(new String[] { commandStringBuilder.toString() });
    }

    /**
     * execute android dx
     * 
     * @param outputDexFullFilename
     * @param classesDirectoryListAndLibraryList
     * @param isDebug
     */
    public static void androidDx(String outputDexFullFilename, List<String> classesDirectoryListAndLibraryList, boolean isDebug) {
        List<String> parameterList = new ArrayList<String>();
        parameterList.add("--dex");
        if (isDebug) {
            parameterList.add("--debug");
        }
        parameterList.add("--force-jumbo");
        parameterList.add("--output=" + outputDexFullFilename);
        for (String classesDirectoryListAndLibrary : classesDirectoryListAndLibraryList) {
            parameterList.add(classesDirectoryListAndLibrary);
        }
        com.android.dx.command.Main.main(parameterList.toArray(new String[] {}));
    }

    /**
     * execute java class
     * 
     * @param javaExecutorPath
     * @param classpath
     * @param className
     * @param argumentList
     * @return int
     */
    public static int executeJavaClass(String javaExecutorPath, String classpath, String className, List<String> argumentList) {
        return executeJava(javaExecutorPath, classpath, className, null, argumentList, false, true);
    }

    /**
     * execute java jar
     * 
     * @param javaExecutorPath
     * @param jarFullFilename
     * @param argumentList
     * @return int
     */
    public static int executeJavaJar(String javaExecutorPath, String jarFullFilename, List<String> argumentList) {
        return executeJava(javaExecutorPath, null, null, jarFullFilename, argumentList, true, true);
    }

    /**
     * execute java jar
     * 
     * @param javaExecutorPath
     * @param jarFullFilename
     * @param argumentList
     * @param needToLogCommand
     * @return int
     */
    public static int executeJavaJar(String javaExecutorPath, String jarFullFilename, List<String> argumentList, boolean needToLogCommand) {
        return executeJava(javaExecutorPath, null, null, jarFullFilename, argumentList, true, needToLogCommand);
    }

    /**
     * execute java
     * 
     * @param javaExecutorPath
     * @param classpath
     * @param className
     * @param jarFullFilename
     * @param argumentList
     * @param isExecuteJar
     * @param needToLogCommand
     * @return int
     */
    private static int executeJava(String javaExecutorPath, String classpath, String className, String jarFullFilename, List<String> argumentList, boolean isExecuteJar, boolean needToLogCommand) {
        List<String> parameterList = new ArrayList<String>();
        if (javaExecutorPath.indexOf(StringUtil.SPACE) > 0 && BuilderUtil.isWindowsOS()) {
            javaExecutorPath = Constant.Symbol.DOUBLE_QUOTES + javaExecutorPath + Constant.Symbol.DOUBLE_QUOTES;
        }
        parameterList.add(javaExecutorPath);
        if (isExecuteJar) {
            parameterList.add("-jar");
            parameterList.add(jarFullFilename);
        } else {
            parameterList.add("-classpath");
            parameterList.add(classpath);
            parameterList.add(className);
        }
        if (argumentList != null) {
            parameterList.addAll(argumentList);// (listToCommandString(argumentList,
                                               // null, StringUtil.SPACE));
        }
        return executeCommand(parameterList.toArray(new String[0]), needToLogCommand);
    }

    private static List<String> generateJavacParameterList(List<String> classpathList, List<String> sourceList, String destinationDirectory, boolean isDebug, List<String> processorPathList) {
        List<String> parameterList = new ArrayList<String>();
        String seperator = isWindowsOS ? Constant.Symbol.SEMICOLON : Constant.Symbol.COLON;
        if (classpathList != null && !classpathList.isEmpty()) {
            parameterList.add("-classpath");
            parameterList.add(listToCommandString(classpathList, null, seperator));
        }
        if (isDebug) {
            parameterList.add("-g");
        }
        parameterList.add("-source");
        parameterList.add("1.7");
        parameterList.add("-target");
        parameterList.add("1.7");
        parameterList.add("-nowarn");
        parameterList.add("-d");
        parameterList.add(destinationDirectory);
        parameterList.add("-encoding");
        parameterList.add(Constant.Encoding.UTF8);
        if (processorPathList != null && !processorPathList.isEmpty()) {
            parameterList.add("-processorpath");
            parameterList.add(listToCommandString(processorPathList, null, seperator));
        }

        if (sourceList != null && !sourceList.isEmpty()) {
            for (String source : sourceList) {
                parameterList.add(source);
            }
        } else {
            throw new BuildException("source list can not be null or empty");
        }
        return parameterList;
    }

    /**
     * execute javac
     * 
     * @param javacExecutorPath
     * @param classpathList
     * @param sourceList
     * @param destinationDirectory
     */
    public static int executeJavac(String javacExecutorPath, List<String> classpathList, List<String> sourceList, String destinationDirectory, boolean isDebug) {
        return executeJavac(javacExecutorPath, classpathList, sourceList, destinationDirectory, isDebug, null);
    }

    /**
     * execute javac
     * 
     * @param javacExecutorPath
     * @param classpathList
     * @param sourceList
     * @param destinationDirectory
     * @param processorPathList
     */
    public static int executeJavac(String javacExecutorPath, List<String> classpathList, List<String> sourceList, String destinationDirectory, boolean isDebug, List<String> processorPathList) {
        List<String> parameterList = new ArrayList<String>();
        if (javacExecutorPath.indexOf(StringUtil.SPACE) > 0 && BuilderUtil.isWindowsOS()) {
            javacExecutorPath = Constant.Symbol.DOUBLE_QUOTES + javacExecutorPath + Constant.Symbol.DOUBLE_QUOTES;
        }
        parameterList.add(javacExecutorPath);
        // commandStringBuilder.append("-verbose"+StringUtil.SPACE);
        parameterList.addAll(generateJavacParameterList(classpathList, sourceList, destinationDirectory, isDebug, processorPathList));
        if (isWindowsOS) {
            return executeCommand(parameterList.toArray(new String[0]));
        } else {
            String[] commands = new String[] { "sh", "-c", "\"" + listToCommandString(parameterList, null, StringUtil.SPACE) + "\"" };
            return executeCommand(commands);
        }
    }

    /**
     * tool.jar javac
     * 
     * @param classpathList
     * @param sourceList
     * @param destinationDirectory
     * @param isDebug
     * @return int,exit code
     */
    public static int javac(List<String> classpathList, List<String> sourceList, String destinationDirectory, boolean isDebug) {
        return javac(classpathList, sourceList, destinationDirectory, isDebug, null);
    }

    /**
     * tool.jar javac
     * 
     * @param classpathList
     * @param sourceList
     * @param destinationDirectory
     * @param isDebug
     * @param processorPathList
     * @return int,exit code
     */
    public static int javac(List<String> classpathList, List<String> sourceList, String destinationDirectory, boolean isDebug, List<String> processorPathList) {
        List<String> parameterList = new ArrayList<String>();
        parameterList.addAll(generateJavacParameterList(classpathList, sourceList, destinationDirectory, isDebug, processorPathList));
        return Main.compile(parameterList.toArray(new String[] {}));
    }

    /**
     * execute jar,only package jar
     * 
     * @param jarExecutorPath
     * @param outputJarFullFilename
     * @param classesDirectory
     */
    public static void executeJar(String jarExecutorPath, String outputJarFullFilename, String classesDirectory) {
        executeJar(jarExecutorPath, outputJarFullFilename, classesDirectory, false);
    }

    /**
     * execute jar,only package jar
     * 
     * @param jarExecutorPath
     * @param outputJarFullFilename
     * @param classesDirectory
     */
    public static void executeJar(String jarExecutorPath, String outputJarFullFilename, String classesDirectory, boolean isAppend) {
        StringBuilder commandStringBuilder = new StringBuilder();
        if (jarExecutorPath.indexOf(StringUtil.SPACE) > 0 && BuilderUtil.isWindowsOS()) {
            jarExecutorPath = Constant.Symbol.DOUBLE_QUOTES + jarExecutorPath + Constant.Symbol.DOUBLE_QUOTES;
        }
        commandStringBuilder.append(jarExecutorPath + StringUtil.SPACE);
        if (!isAppend) {
            commandStringBuilder.append("c");
        } else {
            commandStringBuilder.append("u");
        }
        commandStringBuilder.append("f" + StringUtil.SPACE);
        commandStringBuilder.append(outputJarFullFilename + StringUtil.SPACE);
        commandStringBuilder.append("-C" + StringUtil.SPACE);
        commandStringBuilder.append(classesDirectory + StringUtil.SPACE);
        commandStringBuilder.append(Constant.Symbol.DOT);
        executeCommand(new String[] { commandStringBuilder.toString() });
    }

    /**
     * jar just zip class file
     * 
     * @param outputJarFullFilename
     * @param classesDirectory
     */
    public static void jar(String outputJarFullFilename, String classesDirectory) {
        FileUtil.zip(outputJarFullFilename, classesDirectory, Constant.File.CLASS);
    }

    /**
     * execute jar singer
     * 
     * @param jarSingerExecutorPath
     * @param keyStore
     * @param storePassword
     * @param keyPassword
     * @param signedApkOutputFullFilename
     * @param unsingedApkFullFilename
     * @param digestalg
     * @param sigalg
     * @return int,exit code
     */
    public static int executeJarSigner(String jarSingerExecutorPath, String keyStore, String storePassword, String keyPassword, String alias, String signedApkOutputFullFilename, String unsingedApkFullFilename, String digestalg, String sigalg) {
        StringBuilder commandStringBuilder = new StringBuilder();
        if (jarSingerExecutorPath.indexOf(StringUtil.SPACE) > 0 && BuilderUtil.isWindowsOS()) {
            jarSingerExecutorPath = Constant.Symbol.DOUBLE_QUOTES + jarSingerExecutorPath + Constant.Symbol.DOUBLE_QUOTES;
        }
        commandStringBuilder.append(jarSingerExecutorPath + StringUtil.SPACE);
        commandStringBuilder.append("-keystore" + StringUtil.SPACE);
        if (keyStore.indexOf(StringUtil.SPACE) > 0 && BuilderUtil.isWindowsOS()) {
            keyStore = Constant.Symbol.DOUBLE_QUOTES + keyStore + Constant.Symbol.DOUBLE_QUOTES;
        }
        commandStringBuilder.append(keyStore + StringUtil.SPACE);
        commandStringBuilder.append("-storepass" + StringUtil.SPACE);
        commandStringBuilder.append(storePassword + StringUtil.SPACE);
        commandStringBuilder.append("-keypass" + StringUtil.SPACE);
        commandStringBuilder.append(keyPassword + StringUtil.SPACE);
        commandStringBuilder.append("-signedjar" + StringUtil.SPACE);
        commandStringBuilder.append(signedApkOutputFullFilename + StringUtil.SPACE);
        commandStringBuilder.append(unsingedApkFullFilename + StringUtil.SPACE);
        commandStringBuilder.append(alias + StringUtil.SPACE);
        commandStringBuilder.append("-digestalg" + StringUtil.SPACE);
        commandStringBuilder.append(digestalg + StringUtil.SPACE);
        commandStringBuilder.append("-sigalg" + StringUtil.SPACE);
        commandStringBuilder.append(sigalg);
        return executeCommand(new String[] { commandStringBuilder.toString() }, false);
    }

    /**
     * execute zip align
     * 
     * @param zipAlignExecutorPath
     * @param align
     * @param inputZipFullFilename
     * @param outputZipFullFilename
     * @return int
     */
    public static int executeZipAlign(String zipAlignExecutorPath, int align, String inputZipFullFilename, String outputZipFullFilename) {
        StringBuilder commandStringBuilder = new StringBuilder();
        commandStringBuilder.append(zipAlignExecutorPath + StringUtil.SPACE);
        commandStringBuilder.append(align + StringUtil.SPACE);
        commandStringBuilder.append(inputZipFullFilename + StringUtil.SPACE);
        commandStringBuilder.append(outputZipFullFilename);
        return executeCommand(new String[] { commandStringBuilder.toString() });
    }

    /**
     * execute command
     * 
     * @param command
     * @return int,exit code
     */
    public static int executeCommand(String[] commandArray) {
        return executeCommand(commandArray, true);
    }

    /**
     * execute command
     * 
     * @param command
     * @param needToLogCommand
     * @return int,exit code
     */
    public static int executeCommand(String[] commandArray, boolean needToLogCommand) {
        return executeCommand(commandArray, null, needToLogCommand, true, true);
    }

    /**
     * execute command
     * 
     * @param commandArray
     * @param needToLogCommand
     * @param needToLogNormal
     * @param needToLogError
     * @return int,exit code
     */
    public static int executeCommand(String[] commandArray, boolean needToLogCommand, boolean needToLogNormal, boolean needToLogError) {
        return executeCommand(commandArray, null, needToLogCommand, needToLogNormal, needToLogError);
    }

    /**
     * execute command
     * 
     * @param commandArray
     * @param environmentParameter
     * @return int,exit code
     */
    public static int executeCommand(String[] commandArray, String[] environmentParameter) {
        return executeCommand(commandArray, environmentParameter, true, true, true);
    }

    /**
     * execute command
     * 
     * @param commandArray
     * @param environmentParameter
     * @param needToLogCommand
     * @return int,exit code
     */
    public static int executeCommand(String[] commandArray, String[] environmentParameter, boolean needToLogCommand) {
        return executeCommand(commandArray, environmentParameter, needToLogCommand, true, true);
    }

    /**
     * execute command
     * 
     * @param command
     * @param environmentParameter
     * @param needToLogCommand
     * @param needToLogNormal
     * @param needToLogError
     * @return int,exit code
     */
    public static int executeCommand(String[] commandArray, String[] environmentParameter, boolean needToLogCommand, boolean needToLogNormal, boolean needToLogError) {
        int result = Integer.MIN_VALUE;
        if (commandArray != null) {
            try {
                Process process = null;
                if (commandArray.length == 1) {
                    String command = commandArray[0];
                    if (needToLogCommand) {
                        logger.info(command);
                    }
                    process = Runtime.getRuntime().exec(command, environmentParameter);
                } else {
                    StringBuilder commandStringBuilder = new StringBuilder();
                    for (String command : commandArray) {
                        commandStringBuilder.append(command.trim() + StringUtil.SPACE);
                    }
                    if (needToLogCommand) {
                        logger.info(commandStringBuilder);
                    }
                    process = Runtime.getRuntime().exec(commandArray, environmentParameter);
                }
                Thread errorInputThread = new Thread(new ProcessRunnable(ProcessRunnable.TYPE_ERROR, process.getErrorStream(), needToLogError));
                errorInputThread.start();
                Thread inputThread = new Thread(new ProcessRunnable(ProcessRunnable.TYPE_NORMAL, process.getInputStream(), needToLogNormal));
                inputThread.start();
                result = process.waitFor();
                errorInputThread.interrupt();
                inputThread.interrupt();
                process.destroy();
            } catch (Exception e) {
                logger.error(Constant.Base.EXCEPTION, e);
                throw new BuildException(e);
            }
        }
        return result;
    }

    /**
     * find want to generate resource list with cache
     * 
     * @param resourceDirectoryList
     * @param cacheProperties
     * @returnList<String>
     */
    public static List<String> findWantToGenerateResourceListWithCache(List<String> resourceDirectoryList, Properties cacheProperties) {
        return FileUtil.findFileListWithCache(resourceDirectoryList, cacheProperties, StringUtil.BLANK, true);
    }

    /**
     * find want to generate aidl list with cache
     * 
     * @param sourceDirectoryList
     * @param cacheProperties
     * @return List<String>
     */
    public static List<String> findWantToGenerateAidlListWithCache(List<String> sourceDirectoryList, Properties cacheProperties) {
        return FileUtil.findFileListWithCache(sourceDirectoryList, cacheProperties, Constant.Symbol.DOT + Constant.File.AIDL, true);
    }

    /**
     * find want to compile source list with cache
     * 
     * @param sourceDirectoryList
     * @param cacheProperties
     * @param isFile
     *            if true the return list is source file else is the source
     *            directory
     * @return List<String>
     */
    public static List<String> findWantToCompileSourceListWithCache(List<String> sourceDirectoryList, Properties cacheProperties, boolean isFile) {
        return FileUtil.findFileListWithCache(sourceDirectoryList, cacheProperties, Constant.Symbol.DOT + Constant.File.JAVA, isFile);
    }

    /**
     * is window os
     * 
     * @return boolean
     */
    public static boolean isWindowsOS() {
        return isWindowsOS;
    }

    /**
     * proguard
     * 
     * @param proguardConfigList
     * @param inputPathList
     * @param outputPath
     * @param classpathList
     */
    public static void proguard(List<String> proguardConfigList, List<ProguardJarPair> proguardJarPairList, List<String> classpathList) {
        List<String> parameterList = new ArrayList<String>();
        if (proguardConfigList != null) {
            for (String proguardConfig : proguardConfigList) {
                parameterList.add("-include");
                parameterList.add(proguardConfig);
            }
        }
        if (proguardJarPairList != null) {
            for (ProguardJarPair proguardJarPair : proguardJarPairList) {
                parameterList.add("-injars");
                parameterList.add(proguardJarPair.inputJar);
                parameterList.add("-outjars");
                parameterList.add(proguardJarPair.outputJar);
            }
        }
        if (classpathList != null) {
            for (String classpath : classpathList) {
                parameterList.add("-libraryjars");
                parameterList.add(classpath);
            }
        }
        ProGuard.main(parameterList.toArray(new String[] {}));
    }

    /**
     * merge dex
     * 
     * @param outputDexFullFilename
     * @param toMergeDexFullFilenameList
     */
    public static void androidMergeDex(String outputDexFullFilename, List<String> toMergeDexFullFilenameList) {
        List<String> parameterList = new ArrayList<String>();
        parameterList.add(outputDexFullFilename);
        if (toMergeDexFullFilenameList != null) {
            if (!toMergeDexFullFilenameList.isEmpty()) {
                parameterList.add(toMergeDexFullFilenameList.get(0));
            }
            parameterList.addAll(toMergeDexFullFilenameList);
        }
        try {
            DexMerger.main(parameterList.toArray(new String[] {}));
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * generating apk no compress list
     * 
     * @return List<String>
     */
    public static List<String> generatingApkNoCompressList() {
        List<String> noCompressList = new ArrayList<String>(Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".wav", ".mp2", ".mp3", ".ogg", ".aac", ".mpg", ".mpeg", ".mid", ".midi", ".smf", ".jet", ".rtttl", ".imy", ".xmf", ".mp4", ".m4a", ".m4v", ".3gp", ".3gpp", ".3g2", ".3gpp2", ".amr", ".awb", ".wma", ".wmv", "resources.arsc"));
        return noCompressList;
    }

    /**
     * generate apk
     * 
     * @param directory
     * @param apkOutputFullFilename
     */
    public static void generateApk(String directory, String apkOutputFullFilename) {
        List<String> outputFileList = FileUtil.findMatchFile(new FileUtil.MatchOption(directory));
        List<ZipEntryPath> zipEntryPathList = new ArrayList<ZipEntryPath>();
        int buildHomeFullFilenameLength = new File(directory).getAbsolutePath().length() + 1;
        List<String> noCompressList = generatingApkNoCompressList();
        for (String outputFile : outputFileList) {
            String zipEntryName = outputFile.substring(buildHomeFullFilenameLength, outputFile.length());
            zipEntryName = zipEntryName.replace(Constant.Symbol.SLASH_RIGHT, Constant.Symbol.SLASH_LEFT);
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            boolean noCompress = false;
            if (noCompressList != null) {
                for (String noCompressName : noCompressList) {
                    if (zipEntryName.equals(noCompressName) || StringUtil.isMatchPattern(zipEntryName, Constant.Symbol.WILDCARD + noCompressName)) {
                        noCompress = true;
                    }
                }
            }
            if (noCompress) {
                zipEntry.setMethod(ZipEntry.STORED);
                zipEntry.setSize(new File(outputFile).length());
                CRC32 crc32 = new CRC32();
                crc32.update(FileUtil.readFile(outputFile));
                zipEntry.setCrc(crc32.getValue());
            } else {
                zipEntry.setMethod(ZipEntry.DEFLATED);
            }
            zipEntryPathList.add(new ZipEntryPath(outputFile, zipEntry, true));
        }
        FileUtil.zip(apkOutputFullFilename, zipEntryPathList);
    }

    public static class ProguardJarPair {
        private String inputJar = null;
        private String outputJar = null;

        public ProguardJarPair(String inputJar, String outputJar) {
            this.inputJar = inputJar;
            this.outputJar = outputJar;
        }
    }

    private static class ProcessRunnable implements Runnable {
        private static final int TYPE_ERROR = 1;
        private static final int TYPE_NORMAL = 2;
        private InputStream inputStream = null;
        private int type = TYPE_NORMAL;
        private boolean needToLog = true;

        public ProcessRunnable(int type, InputStream inputStream, boolean needToLog) {
            this.type = type;
            this.inputStream = inputStream;
            this.needToLog = needToLog;
        }

        public void run() {
            BufferedInputStream bufferedInputStream = null;
            try {
                bufferedInputStream = new BufferedInputStream(this.inputStream);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));
                String string = null;
                while ((string = bufferedReader.readLine()) != null) {
                    if (this.needToLog) {
                        if (this.type == TYPE_ERROR) {
                            logger.error(string, null);
                        } else {
                            logger.info(string);
                        }
                    }
                }
            } catch (Exception e) {
                if (isWindowsOS()) {// it has stream exception in linux
                    logger.error(Constant.Base.EXCEPTION, e);
                }
            } finally {
                if (this.inputStream != null) {
                    try {
                        this.inputStream.close();
                    } catch (Exception e) {
                        if (isWindowsOS()) {
                            logger.error(Constant.Base.EXCEPTION, e);
                        }
                    }
                }
            }
        }
    }
}

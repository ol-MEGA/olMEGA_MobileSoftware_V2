package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * StageFactory creates a processing tree as defined in an XML-File.
 *
 * Example:
 *
 * <stage feature="StageAudioCapture" id="00">
 *   <stage feature="StagePreHighpass" id="10" cutoff_hz="250">
 *     <stage feature="StageProcPSD" id="11" blocksize="400" hopsize="200">
 *       <stage feature="StageFeatureWrite" id="110" prefix="PSD" nfeatures="1026"/>
 *     </stage>
 *     <stage feature="StageProcRMS" id="12" blocksize="400" hopsize="200">
 *       <stage feature="StageFeatureWrite" id="120" prefix="RMS" nfeatures="2"/>
 *     </stage>
 *   </stage>
 *   <stage feature="StageAudioWrite" id="30" filename="audio_raw"/>
 * </stage>
 *
 * For parameter see individual Stages.
 *
 * June 2018, sk
 */


class StageFactory {

    private static String TAG = "StageFactory";

    /**
     * Parses a stage (i.e. feature extraction / processing) configuration from XML
     * The input argument must specify a valid XML-File.
     * <p>
     * Returns the root stage.
     *
     * @param stageConfig XML-File defining the stage configuration
     * @return stage       Root stage
     */
    public Stage parseConfig(File stageConfig) {

        Stage stage = null;

        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(stageConfig);
            stage = buildStage(doc.getDocumentElement());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return (stage);
    }

    /**
     * Crawls along available elements and creates and attaches stages to their respective parent stage.
     * <p>
     * Returns the stage created from parentElement.
     *
     * @param parentElement Current XML element
     * @return parentStage   Stage specified in current element
     */
    public Stage buildStage(Element parentElement) {

        // get attributes from given element and write as hash map
        NamedNodeMap attributes = parentElement.getAttributes();
        HashMap parameter = new HashMap(attributes.getLength(), 1);
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);
            if (node.getNodeName() == "feature") continue;
            parameter.put(node.getNodeName(), node.getNodeValue());
        }

        String stageType = Stage.class.getPackage().getName() + "." + parentElement.getAttribute("feature");
        Class stageClass;
        Stage parentStage = null;

        try {

            System.out.println(stageType);

            // Instantiate stage
            stageClass = Class.forName(stageType);
            Constructor constructor = stageClass.getConstructor(HashMap.class);
            parentStage = (Stage) constructor.newInstance(parameter);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        NodeList nodes = parentElement.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element)) continue;
            try {
                parentStage.addConsumer(buildStage((Element) node));
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        return (parentStage);
    }

}


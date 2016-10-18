/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/

package fr.gouv.vitam.worker.core.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.utils.BinaryObjectInfo;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * CheckConformityAction Handler.<br>
 *
 */
public class CheckConformityActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckConformityActionHandler.class);

    private static final String HANDLER_ID = "CheckConformity";
    LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
    public static final String JSON_EXTENSION = ".json";
    public static final String LIFE_CYCLE_EVENT_TYPE_PROCESS = "INGEST";
    public static final String UNIT_LIFE_CYCLE_CREATION_EVENT_TYPE =
        "Check SIP – Units – Lifecycle Logbook Creation – Création du journal du cycle de vie des units";
    public static final String TXT_EXTENSION = ".txt";
    private static final int BINARY_OBJECT_INFO_RANK = 0;
    private HandlerIO handlerIO;
    private String eventDetailData;
    private String objectID;
    
    int nbOK;
    int nbKO;
    int nbWarning;

    private LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters = LogbookParametersFactory
        .newLogbookLifeCycleObjectGroupParameters();

    private LogbookLifeCyclesClient logbookClient =
        LogbookLifeCyclesClientFactory.getInstance().getClient();

    private boolean oneOrMoreMessagesDigestUpdated = false;
    private static final int ALGO_RANK = 0;
    private static final String INCOME = "MessageIdentifier du manifest";

    public CheckConformityActionHandler() {
        // Nothing
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }


    @Override
    public EngineResponse execute(WorkerParameters params, HandlerIO handler) {
        checkMandatoryParameters(params);
        handlerIO = handler;
        nbOK = 0;
        nbKO = 0;
        LOGGER.debug("CheckConformityActionHandler running ...");

        final ProcessResponse response = new ProcessResponse().setStatus(StatusCode.OK);
        response.setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_CONFORMITY_OK);
        final WorkspaceClient workspaceClient = WorkspaceClientFactory.create(params.getUrlWorkspace());

        try {
            // Get objectGroup
            final JsonNode jsonOG = getJsonFromWorkspace(workspaceClient, params.getContainerName(),
                IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + params.getObjectName());

            Map<String, BinaryObjectInfo> binaryObjects = getBinaryObjects(jsonOG);

            HandlerUtils.saveMap(params.getContainerName(), binaryObjects,
                (String) handlerIO.getOutput().get(BINARY_OBJECT_INFO_RANK), workspaceClient, true);
            
            objectID = jsonOG.findValue(SedaConstants.PREFIX_ID).toString();
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier, objectID);
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventType, OutcomeMessage.CHECK_DIGEST.value());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome, StatusCode.STARTED.toString());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail, StatusCode.STARTED.toString());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, OutcomeMessage.CHECK_DIGEST_STARTED.value());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.objectIdentifierIncome, INCOME);
            SedaUtils.updateLifeCycleForBegining(logbookLifecycleObjectGroupParameters, params);

            // checkMessageDigest
            JsonNode qualifiers = jsonOG.get(SedaConstants.PREFIX_QUALIFIERS);
            if (qualifiers != null) {
                List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
                if (versions != null && !versions.isEmpty()) {
                    for (JsonNode versionsArray : versions) {
                        for (JsonNode version : versionsArray) {
                            String objectId = version.get(SedaConstants.PREFIX_ID).asText();
                            checkMessageDigest(workspaceClient, params, binaryObjects.get(objectId), version);
                        }
                    }
                }
            }

            if (oneOrMoreMessagesDigestUpdated) {
                workspaceClient.putObject(params.getContainerName(),
                    IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + params.getObjectName(),
                    new ByteArrayInputStream(JsonHandler.writeAsString(jsonOG).getBytes()));
            }
            
            if (nbKO != 0){
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    OutcomeMessage.CHECK_DIGEST_KO.value() + 
                    " -- " + nbKO + " binary Object KO, " + nbWarning + " binary Object Warning, " + nbOK + " binary object OK");
                throw new ProcessingException("Check Conformity KO");
            } else {
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    OutcomeMessage.CHECK_DIGEST_OK.value());
            }
            
        } catch (ProcessingException e) {
            LOGGER.error(e);
            response.setStatus(StatusCode.KO);
            response.setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_CONFORMITY_KO);
        } catch (ContentAddressableStorageServerException | IOException |  InvalidParseOperationException e) {
            LOGGER.error(e);
            response.setStatus(StatusCode.FATAL);
            response.setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_CONFORMITY_KO);
        }

        try {
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier, objectID);
            SedaUtils.setLifeCycleFinalEventStatusByStep(logbookLifecycleObjectGroupParameters, response.getStatus());
        } catch (ProcessingException e) {
            LOGGER.error(e);
            if (!response.getStatus().equals(StatusCode.FATAL) && !response.getStatus().equals(StatusCode.KO)) {
                response.setStatus(StatusCode.WARNING);
            }
            response.setOutcomeMessages(HANDLER_ID, OutcomeMessage.LOGBOOK_COMMIT_KO);
        }

        LOGGER.debug("CheckConformityActionHandler response: " + response.getStatus().name());
        return response;
    }

    private void checkMessageDigest(WorkspaceClient workspaceClient, WorkerParameters params, BinaryObjectInfo binaryObject, JsonNode version)
        throws ProcessingException {
        String containerId = params.getContainerName();
        // started for binary Object
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier, binaryObject.getId());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventType, OutcomeMessage.CHECK_DIGEST.value());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome, StatusCode.STARTED.toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail, StatusCode.STARTED.toString());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, OutcomeMessage.CHECK_DIGEST_STARTED.value());
        eventDetailData = "{\"MessageDigest\":\"" + binaryObject.getMessageDigest() + "\",\"Algorithm\": \"" + binaryObject.getAlgo() + "\"} ";
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventDetailData, eventDetailData);
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.objectIdentifierIncome, INCOME);
        
        SedaUtils.updateLifeCycleForBegining(logbookLifecycleObjectGroupParameters, params);
        
        try {
            DigestType digestTypeInput = DigestType.fromValue((String) handlerIO.getInput().get(ALGO_RANK));
            InputStream inputStream =
                workspaceClient.getObject(containerId, IngestWorkflowConstants.SEDA_FOLDER + "/" + binaryObject.getUri());
            Digest vitamDigest = new Digest(digestTypeInput);
            Digest manifestDigest;
            boolean isVitamDigest = false;
            if (!binaryObject.getAlgo().equals(digestTypeInput)) {
                manifestDigest = new Digest(binaryObject.getAlgo());
                inputStream = manifestDigest.getDigestInputStream(inputStream);
            } else {
                manifestDigest = vitamDigest;
                isVitamDigest = true;
            }

            vitamDigest.update(inputStream);

            if(manifestDigest.toString().equals(binaryObject.getMessageDigest())){
                nbOK += 1;
                // update logbook case OK
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier, binaryObject.getId());
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventType, OutcomeMessage.CHECK_DIGEST.value());
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome, StatusCode.OK.name());
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail, StatusCode.OK.name());
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, OutcomeMessage.CHECK_DIGEST_OK.value());
                eventDetailData = "{\"MessageDigest\":\"" + binaryObject.getMessageDigest() + "\",\"Algorithm\": \"" + binaryObject.getAlgo() + 
                    "\", \"SystemMessageDigest\": \""+ (String) handlerIO.getInput().get(ALGO_RANK) + 
                    "\", \"SystemAlgorithm\": \""+ manifestDigest.toString() + "\"} ";
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventDetailData, eventDetailData);
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.objectIdentifierIncome, INCOME);

                if(!isVitamDigest){
                    nbOK -= 1;
                    nbWarning += 1;
                    // update objectGroup json
                    ((ObjectNode) version).put(SedaConstants.TAG_DIGEST, vitamDigest.toString());
                    ((ObjectNode) version).put(SedaConstants.ALGORITHM, (String) handlerIO.getInput().get(ALGO_RANK));                    
                    
                    // update logbook lifecyle
                    logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome, StatusCode.WARNING.name());
                    logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail, StatusCode.WARNING.name());
                    eventDetailData =  "{\"MessageDigest\":\"" + binaryObject.getMessageDigest() + "\",\"Algorithm\": \"" + binaryObject.getAlgo() + 
                        "\", \"SystemMessageDigest\": \""+ (String) handlerIO.getInput().get(ALGO_RANK) + 
                        "\", \"SystemAlgorithm\": \""+ manifestDigest.toString() + "\"} ";
                    logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventDetailData, eventDetailData);
                    logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.objectIdentifierIncome, INCOME);

                    // WARNING case
                    oneOrMoreMessagesDigestUpdated=true;
                }
                logbookClient.update(logbookLifecycleObjectGroupParameters);

            } else {
                nbKO += 1;
                // update logbook case KO
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventType, OutcomeMessage.CHECK_DIGEST.value());
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome, StatusCode.KO.name());
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail, StatusCode.KO.name());
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, OutcomeMessage.CHECK_DIGEST_KO.value());
                eventDetailData = "{\"MessageDigest\":\"" + binaryObject.getMessageDigest() + "\",\"Algorithm\": \"" + binaryObject.getAlgo() + 
                    "\", \"ComputedMessageDigest\": \""+ manifestDigest.digest().toString() + "\"} ";
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventDetailData, eventDetailData);
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.objectIdentifierIncome, INCOME);
                
                logbookClient.update(logbookLifecycleObjectGroupParameters);
            }

        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
            IOException | LogbookClientBadRequestException | LogbookClientNotFoundException | LogbookClientServerException e) {
            LOGGER.error(e);
            throw new ProcessingException(e.getMessage(), e);
        }

    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {


    }

    /**
     * Retrieve a json file as a {@link JsonNode} from the workspace.
     *
     * @param workspaceClient workspace connector
     * @param containerId container id
     * @param jsonFilePath path in workspace of the json File
     * @return JsonNode of the json file
     * @throws ProcessingException throws when error occurs
     */
    private JsonNode getJsonFromWorkspace(WorkspaceClient workspaceClient, String containerId, String jsonFilePath)
        throws ProcessingException {
        try (InputStream is =
            workspaceClient.getObject(containerId, jsonFilePath)) {
            if (is != null) {
                return JsonHandler.getFromInputStream(is, JsonNode.class);
            } else {
                LOGGER.error("Object group not found");
                throw new ProcessingException("Object group not found");
            }
        } catch (InvalidParseOperationException | IOException e) {
            LOGGER.debug("Json wrong format", e);
            throw new ProcessingException(e);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.debug("Workspace Server Error", e);
            throw new ProcessingException(e);
        }
    }

    private Map<String, BinaryObjectInfo> getBinaryObjects(JsonNode jsonOG) throws ProcessingException {
        Map<String, BinaryObjectInfo> binaryObjects = new HashMap<>();

        JsonNode work = jsonOG.get(SedaConstants.PREFIX_WORK);
        JsonNode qualifiers = work.get(SedaConstants.PREFIX_QUALIFIERS);
        if (qualifiers == null) {
            return binaryObjects;
        }

        List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
        if (versions == null || versions.isEmpty()) {
            return binaryObjects;
        }
        for (JsonNode version : versions) {
            for (JsonNode jsonBinaryObject : version) {
                binaryObjects.put(jsonBinaryObject.get(SedaConstants.PREFIX_ID).asText(),
                    new BinaryObjectInfo()
                    .setSize(jsonBinaryObject.get(SedaConstants.TAG_SIZE).asLong())
                    .setId(jsonBinaryObject.get(SedaConstants.PREFIX_ID).asText())
                    .setUri(jsonBinaryObject.get(SedaConstants.TAG_URI).asText())
                    .setMessageDigest(jsonBinaryObject.get(SedaConstants.TAG_DIGEST).asText())
                    .setAlgo(DigestType.fromValue(jsonBinaryObject.get(SedaConstants.ALGORITHM).asText())));
            }
        }
        return binaryObjects;
    }
}

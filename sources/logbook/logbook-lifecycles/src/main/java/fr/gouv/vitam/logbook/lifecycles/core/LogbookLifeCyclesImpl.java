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
package fr.gouv.vitam.logbook.lifecycles.core;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleModel;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycle;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroupInProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnitInProcess;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.lifecycles.api.LogbookLifeCycles;

/**
 * Logbook LifeCycles implementation base class
 */
public class LogbookLifeCyclesImpl implements LogbookLifeCycles {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookLifeCyclesImpl.class);


    private final LogbookDbAccess mongoDbAccess;
    private boolean disablePurge;

    /**
     * Constructor
     *
     * @param mongoDbAccess of logbook
     */
    public LogbookLifeCyclesImpl(LogbookDbAccess mongoDbAccess) {
        this.mongoDbAccess = mongoDbAccess;
    }

    /**
     * Constructor
     *
     * @param mongoDbAccess of logbook
     */
    public LogbookLifeCyclesImpl(LogbookDbAccess mongoDbAccess, boolean disablePurge) {
        this.mongoDbAccess = mongoDbAccess;
        this.disablePurge = disablePurge;
    }


    @Override
    public void createUnit(String idOperation, String idLc, LogbookLifeCycleUnitParameters parameters)
        throws LogbookAlreadyExistsException, LogbookDatabaseException, IllegalArgumentException {
        checkLifeCyclesUnitArgument(idOperation, idLc, parameters);
        mongoDbAccess.createLogbookLifeCycleUnit(idOperation, parameters);
    }

    @Override
    public void createObjectGroup(String idOperation, String idLc, LogbookLifeCycleObjectGroupParameters parameters)
        throws LogbookAlreadyExistsException, LogbookDatabaseException, IllegalArgumentException {
        checkLifeCyclesObjectGroupArgument(idOperation, idLc, parameters);
        mongoDbAccess.createLogbookLifeCycleObjectGroup(idOperation, parameters);
    }

    @Override
    public void updateUnit(String idOperation, String idLc, LogbookLifeCycleUnitParameters parameters)
        throws LogbookDatabaseException, IllegalArgumentException, LogbookNotFoundException,
        LogbookAlreadyExistsException {
        checkLifeCyclesUnitArgument(idOperation, idLc, parameters);
        updateUnit(idOperation, idLc, parameters, false);
    }

    @Override
    public void updateUnit(String idOperation, String idLc, LogbookLifeCycleUnitParameters parameters, boolean commit)
        throws LogbookNotFoundException, LogbookDatabaseException, LogbookAlreadyExistsException {
        checkLifeCyclesUnitArgument(idOperation, idLc, parameters);
        mongoDbAccess.updateLogbookLifeCycleUnit(idOperation, idLc, parameters, commit);

    }

    @Override
    public void updateObjectGroup(String idOperation, String idLc, LogbookLifeCycleObjectGroupParameters parameters)
        throws LogbookNotFoundException, LogbookDatabaseException, IllegalArgumentException,
        LogbookAlreadyExistsException {
        checkLifeCyclesObjectGroupArgument(idOperation, idLc, parameters);
        mongoDbAccess.updateLogbookLifeCycleObjectGroup(idOperation, idLc, parameters);
    }

    @Override
    public void updateObjectGroup(String idOperation, String idLc, LogbookLifeCycleObjectGroupParameters parameters, boolean commit)
            throws LogbookNotFoundException, LogbookDatabaseException, IllegalArgumentException,
            LogbookAlreadyExistsException {
        checkLifeCyclesObjectGroupArgument(idOperation, idLc, parameters);
        mongoDbAccess.updateLogbookLifeCycleObjectGroup(idOperation, idLc, parameters, commit);
    }

    @Override
    public LogbookLifeCycleUnit getUnitByOperationIdAndByUnitId(String idOperation, String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException {
        return mongoDbAccess.getLogbookLifeCycleUnit(idOperation, idLc);
    }

    @Override
    public LogbookLifeCycleObjectGroup getObjectGroupByOperationIdAndByObjectGroupId(String idOperation, String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException,
        IllegalArgumentException {
        return mongoDbAccess.getLogbookLifeCycleObjectGroup(idOperation, idLc);
    }

    @Override
    public List<LogbookLifeCycle> selectUnit(JsonNode select, boolean sliced, LogbookCollections collection)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException, VitamDBException {
        final List<LogbookLifeCycle> result = new ArrayList<>();
        try (final MongoCursor<LogbookLifeCycle> logbook =
            mongoDbAccess.getLogbookLifeCycleUnits(select, sliced, collection)) {
            if (!logbook.hasNext()) {
                throw new LogbookNotFoundException("Logbook entry not found");
            }
            while (logbook.hasNext()) {
                result.add(logbook.next());
            }
        }
        return result;
    }

    @Override
    public List<LogbookLifeCycle> selectObjectGroup(JsonNode select, LogbookCollections collection)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException, VitamDBException {
        return selectObjectGroup(select, true, collection);
    }

    @Override
    public List<LogbookLifeCycle> selectObjectGroup(JsonNode select, boolean sliced,
        LogbookCollections collection)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException, VitamDBException {
        final List<LogbookLifeCycle> result = new ArrayList<>();
        try (final MongoCursor<LogbookLifeCycle> logbookCursor =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(select, sliced, collection)) {
            if (!logbookCursor.hasNext()) {
                throw new LogbookNotFoundException("Logbook entry not found");
            }
            while (logbookCursor.hasNext()) {
                result.add(logbookCursor.next());
            }
        }
        return result;
    }

    @Override
    public void rollbackUnit(String idOperation, String idLc)
        throws LogbookNotFoundException, LogbookDatabaseException, IllegalArgumentException {
        if (!disablePurge) {
            mongoDbAccess.rollbackLogbookLifeCycleUnit(idOperation, idLc);
        }
    }

    @Override
    public void rollbackObjectGroup(String idOperation, String idLc)
        throws LogbookNotFoundException, LogbookDatabaseException, IllegalArgumentException {
        if (!disablePurge) {
            mongoDbAccess.rollbackLogbookLifeCycleObjectGroup(idOperation, idLc);
        }
    }

    @Override
    public LogbookLifeCycleUnit getUnitById(String idUnit) throws LogbookDatabaseException, LogbookNotFoundException {
        return mongoDbAccess.getLogbookLifeCycleUnit(idUnit);
    }

    @Override
    public LogbookLifeCycle getUnitById(JsonNode queryDsl, LogbookCollections collection)
        throws LogbookDatabaseException, LogbookNotFoundException {
        return mongoDbAccess.getLogbookLifeCycleUnit(queryDsl, collection);
    }

    @Override
    public LogbookLifeCycleObjectGroup getObjectGroupById(String idObjectGroup)
        throws LogbookDatabaseException, LogbookNotFoundException {
        return mongoDbAccess.getLogbookLifeCycleObjectGroup(idObjectGroup);
    }

    private void checkLifeCyclesUnitArgument(String idOperation, String idLcUnit,
        LogbookLifeCycleUnitParameters parameters) throws IllegalArgumentException {
        ParametersChecker.checkParameter("idOperation or idLifeCycle should not be null or empty", idOperation,
            idLcUnit);

        if (!parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOperation)) {
            LOGGER.error("incoherence entry for idOperation");
            throw new IllegalArgumentException("incoherence entry for idOperation");
        }

        if (!parameters.getParameterValue(LogbookParameterName.objectIdentifier).equals(idLcUnit)) {
            LOGGER.error("incoherence entry for idLifeCycles");
            throw new IllegalArgumentException("incoherence entry for idLifeCycles");
        }
    }


    private void checkLifeCyclesObjectGroupArgument(String idOperation, String idLcObjectGroup,
        LogbookLifeCycleObjectGroupParameters parameters) throws IllegalArgumentException {
        ParametersChecker.checkParameter("idOperation or idLifeCycleObjectGroup should not be null or empty",
            idOperation, idLcObjectGroup);

        if (!parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOperation)) {
            LOGGER.error("incoherence entry for idOperation");
            throw new IllegalArgumentException("incoherence entry for idOperation");
        }

        if (!(idLcObjectGroup.equals(parameters.getParameterValue(LogbookParameterName.objectIdentifier)) ||
            idLcObjectGroup.equals(parameters.getParameterValue(LogbookParameterName.lifeCycleIdentifier)))) {
            LOGGER.error("incoherence entry for idLifeCyclesObjectGroup");
            throw new IllegalArgumentException("incoherence entry for idLifeCyclesObjectGroup");
        }
    }

    @Override
    public void createBulkLogbookLifecycle(String idOp, LogbookLifeCycleParameters[] lifecycleArray)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        ParametersChecker.checkParameter("idOperation should not be null or empty", idOp);
        if (lifecycleArray == null || lifecycleArray.length == 0) {
            throw new IllegalArgumentException("No LifeCycle Logbook");
        }
        if (!lifecycleArray[0].getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOp)) {
            LOGGER.error("incoherence entry for idOperation");
            throw new IllegalArgumentException("incoherence entry for idOperation");
        }
        if (lifecycleArray instanceof LogbookLifeCycleUnitParameters[]) {
            mongoDbAccess.createBulkLogbookLifeCycleUnit((LogbookLifeCycleUnitParameters[]) lifecycleArray);
        } else {
            mongoDbAccess
                .createBulkLogbookLifeCycleObjectGroup((LogbookLifeCycleObjectGroupParameters[]) lifecycleArray);
        }
    }

    @Override
    public void updateBulkLogbookLifecycle(String idOp, LogbookLifeCycleParameters[] lifecycleArray)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        ParametersChecker.checkParameter("idOperation should not be null or empty", idOp);
        if (lifecycleArray == null || lifecycleArray.length == 0) {
            throw new IllegalArgumentException("No LifeCycle Logbook");
        }
        if (!lifecycleArray[0].getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOp)) {
            LOGGER.error("incoherence entry for idOperation");
            throw new IllegalArgumentException("incoherence entry for idOperation");
        }
        if (lifecycleArray instanceof LogbookLifeCycleUnitParameters[]) {
            mongoDbAccess.updateBulkLogbookLifeCycleUnit((LogbookLifeCycleUnitParameters[]) lifecycleArray);
        } else {
            mongoDbAccess
                .updateBulkLogbookLifeCycleObjectGroup((LogbookLifeCycleObjectGroupParameters[]) lifecycleArray);
        }
    }


    @Override
    public void commitUnit(String idOperation, String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {

        // 1- Find temporary unit lifeCycle
        LogbookLifeCycleUnitInProcess logbookLifeCycleUnitInProcess =
            mongoDbAccess.getLogbookLifeCycleUnitInProcess(idLc);
        if (logbookLifeCycleUnitInProcess == null) {
            LOGGER.error("The temporary lifeCycle wasn't found");
            throw new LogbookNotFoundException("The temporary lifeCycle wasn't found");
        }

        // 2- Check if it is a creation or an update commit
        boolean isLifeCycleExist = mongoDbAccess.existsLogbookLifeCycleUnit(idLc);

        // 3- Copy to Production Collection
        if (!isLifeCycleExist) {
            // Creation Mode from LogbookLifeCycleUnitInProcess instance
            mongoDbAccess.createLogbookLifeCycleUnit(logbookLifeCycleUnitInProcess);
        } else {
            // Update existing lifeCycle
            mongoDbAccess.updateLogbookLifeCycleUnit(logbookLifeCycleUnitInProcess);
        }
    }


    @Override
    public void commitObjectGroup(String idOperation, String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        // 1- Find temporary unit lifeCycle
        LogbookLifeCycleObjectGroupInProcess logbookLifeCycleObjectGroupInProcess =
            mongoDbAccess.getLogbookLifeCycleObjectGroupInProcess(idLc);

        if (logbookLifeCycleObjectGroupInProcess == null) {
            LOGGER.error("The temporary lifeCycle wasn't found");
            throw new LogbookNotFoundException("The temporary lifeCycle wasn't found");
        }

        // 2- Check if it is a creation or an update commit
        boolean isLifeCycleExist = mongoDbAccess.existsLogbookLifeCycleObjectGroup(idLc);

        // 3- Copy to Production Collection
        if (!isLifeCycleExist) {
            // Creation Mode from LogbookLifeCycleObjectGroupInProcess instance
            mongoDbAccess.createLogbookLifeCycleObjectGroup(logbookLifeCycleObjectGroupInProcess);
        } else {
            // Update existing lifeCycle
            mongoDbAccess.updateLogbookLifeCycleObjectGroup(logbookLifeCycleObjectGroupInProcess);
        }
    }


    @Override
    public void rollBackUnitsByOperation(String idOperation) throws LogbookNotFoundException, LogbookDatabaseException {
        if (!disablePurge) {
            mongoDbAccess.rollBackUnitLifeCyclesByOperation(idOperation);
        }
    }

    @Override
    public void rollBackObjectGroupsByOperation(String idOperation)
        throws LogbookNotFoundException, LogbookDatabaseException {
        if (!disablePurge) {
            mongoDbAccess.rollBackObjectGroupLifeCyclesByOperation(idOperation);
        }
    }


    @Override
    public LifeCycleStatusCode getUnitLifeCycleStatus(String unitId)
        throws LogbookDatabaseException, LogbookNotFoundException {
        // 1- First, check if the lifeCycle exists in the working collection
        boolean isInProcessLfc = mongoDbAccess.existsLogbookLifeCycleUnitInProcess(unitId);
        if (isInProcessLfc) {
            return LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS;
        }

        // 2- If it doesn't exist, then check in the production collection
        boolean isCommittedLfc = mongoDbAccess.existsLogbookLifeCycleUnit(unitId);
        if (isCommittedLfc) {
            return LifeCycleStatusCode.LIFE_CYCLE_COMMITTED;
        }

        // Else, lifeCycle wasn't found
        return null;
    }


    @Override
    public LifeCycleStatusCode getObjectGroupLifeCycleStatus(String objectGroupId)
        throws LogbookDatabaseException, LogbookNotFoundException {
        // 1- First, check if the lifeCycle exists in the working collection
        boolean isInProcessLfc = mongoDbAccess.existsLogbookLifeCycleObjectGroupInProcess(objectGroupId);
        if (isInProcessLfc) {
            return LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS;
        }

        // 2- If it doesn't exist, then check in the production collection
        boolean isCommittedLfc = mongoDbAccess.existsLogbookLifeCycleObjectGroup(objectGroupId);
        if (isCommittedLfc) {
            return LifeCycleStatusCode.LIFE_CYCLE_COMMITTED;
        }

        // Else, lifeCycle wasn't found
        return null;
    }

    @Override
    public void bulk(LogbookCollections collections, String idOp,
        List<? extends LogbookLifeCycleModel> logbookLifeCycleModels) throws DatabaseException {
        mongoDbAccess.bulk(collections, logbookLifeCycleModels);
    }
}



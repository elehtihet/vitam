/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.common.utils;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.processing.common.model.WorkFlow;

/**
 * Temporary process populator
 * 
 * populates workflow java object
 *
 */
public class ProcessPopulator {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessPopulator.class);

    /**
     * create workflow object :parse JSON file
     * 
     * @param workflowId
     * @return workflow's object
     * @throws WorkflowNotFoundException
     */
    public static WorkFlow populate(String workflowId) throws WorkflowNotFoundException {

        ObjectMapper objectMapper = new ObjectMapper();
        WorkFlow process = null;

        try {
            InputStream inputJSON = getFileAsInputStream(workflowId + ".json");
            process = objectMapper.readValue(inputJSON, WorkFlow.class);

        } catch (IOException e) {
            LOGGER.error("IOException thrown by populator", e);
            throw new WorkflowNotFoundException("IOException thrown by populator", e);
        }
        return process;
    }

    private static InputStream getFileAsInputStream(String workflowFile) throws IOException {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(workflowFile);
    }

}

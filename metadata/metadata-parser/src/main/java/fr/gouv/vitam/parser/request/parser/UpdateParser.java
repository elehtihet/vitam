/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
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
package fr.gouv.vitam.parser.request.parser;

import static fr.gouv.vitam.parser.request.parser.action.UpdateActionParserHelper.add;
import static fr.gouv.vitam.parser.request.parser.action.UpdateActionParserHelper.inc;
import static fr.gouv.vitam.parser.request.parser.action.UpdateActionParserHelper.max;
import static fr.gouv.vitam.parser.request.parser.action.UpdateActionParserHelper.min;
import static fr.gouv.vitam.parser.request.parser.action.UpdateActionParserHelper.pop;
import static fr.gouv.vitam.parser.request.parser.action.UpdateActionParserHelper.pull;
import static fr.gouv.vitam.parser.request.parser.action.UpdateActionParserHelper.push;
import static fr.gouv.vitam.parser.request.parser.action.UpdateActionParserHelper.rename;
import static fr.gouv.vitam.parser.request.parser.action.UpdateActionParserHelper.set;
import static fr.gouv.vitam.parser.request.parser.action.UpdateActionParserHelper.unset;

import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.builder.request.construct.Request;
import fr.gouv.vitam.builder.request.construct.Update;
import fr.gouv.vitam.builder.request.construct.action.Action;
import fr.gouv.vitam.builder.request.construct.configuration.GlobalDatas;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.GLOBAL;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.UPDATEACTION;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Update Parser: [ {root}, {query}, {filter}, {actions} ] or { $roots: root, $query : query, $filter : filter, $action
 * : action }
 *
 */
public class UpdateParser extends RequestParser {
    protected static final int ACTIONS_POS = 3;

    VarNameUpdateAdapter updateAdapter;

    /**
     * Empty constructor
     */
    public UpdateParser() {
        super();
        updateAdapter = new VarNameUpdateAdapter(adapter);
    }

    /**
     * @param adapter
     *
     */
    public UpdateParser(VarNameAdapter adapter) {
        super(adapter);
        updateAdapter = new VarNameUpdateAdapter(adapter);
    }

    @Override
    protected Request getNewRequest() {
        return new Update();
    }

    /**
     *
     * @param request containing a parsed JSON as [ {root}, {query}, {filter}, {actions} ] or { $roots: root, $query :
     *        query, $filter : filter, $action : action }
     * @throws InvalidParseOperationException
     */
    @Override
    public void parse(final JsonNode request) throws InvalidParseOperationException {
        parseJson(request);
        internalParseUpdate();
    }

    /**
     *
     * @param request containing a JSON as [ {root}, {query}, {filter}, {actions} ] or { $roots: root, $query : query,
     *        $filter : filter, $action : action }
     * @throws InvalidParseOperationException
     */
    @Override
    @Deprecated
    public void parse(final String request) throws InvalidParseOperationException {
        parseString(request);
        internalParseUpdate();
    }

    /**
     * @throws InvalidParseOperationException
     */
    private void internalParseUpdate() throws InvalidParseOperationException {
        if (rootNode.isArray()) {
            // should be 4, but each could be empty ( '{}' )
            if (rootNode.size() > ACTIONS_POS) {
                actionParse(rootNode.get(ACTIONS_POS));
            }
        } else {
            // not as array but composite as { $roots: root, $query : query,
            // $filter : filter, $action : action }
            actionParse(rootNode.get(GLOBAL.ACTION.exactToken()));
        }
    }

    /**
     * {$"action" : args, ...}
     *
     * @param rootNode
     * @throws InvalidParseOperationException
     */
    protected void actionParse(final JsonNode rootNode)
        throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }
        GlobalDatas.sanityParametersCheck(rootNode.toString(),
            GlobalDatasParser.NB_ACTIONS);
        try {
            for (final JsonNode node : (ArrayNode) rootNode) {
                Iterator<Entry<String, JsonNode>> iterator = node.fields();
                while (iterator.hasNext()) {
                    final Entry<String, JsonNode> entry = iterator.next();
                    final Action updateAction = analyseOneAction(entry.getKey(), entry.getValue());
                    ((Update) request).addActions(updateAction);
                }
                iterator = null;
            }
        } catch (final Exception e) {
            throw new InvalidParseOperationException(
                "Parse in error for Action: " + rootNode, e);
        }
    }

    /**
     * Compute the QUERY from command
     *
     * @param queryroot
     * @return the QUERY
     * @throws InvalidParseOperationException
     */
    protected static final UPDATEACTION getUpdateActionId(final String actionroot)
        throws InvalidParseOperationException {
        if (!actionroot.startsWith(ParserTokens.DEFAULT_PREFIX)) {
            throw new InvalidParseOperationException(
                "Incorrect action $command: " + actionroot);
        }
        final String command = actionroot.substring(1).toUpperCase();
        UPDATEACTION action = null;
        try {
            action = UPDATEACTION.valueOf(command);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException("Invalid action command: " + command,
                e);
        }
        return action;
    }

    protected Action analyseOneAction(final String refCommand, final JsonNode command)
        throws InvalidParseOperationException {
        GlobalDatas.sanityValueCheck(command.toString());
        final UPDATEACTION action = getUpdateActionId(refCommand);
        switch (action) {
            case ADD:
                return add(command, updateAdapter);
            case INC:
                return inc(command, updateAdapter);
            case MIN:
                return min(command, updateAdapter);
            case MAX:
                return max(command, updateAdapter);
            case POP:
                return pop(command, updateAdapter);
            case PULL:
                return pull(command, updateAdapter);
            case PUSH:
                return push(command, updateAdapter);
            case RENAME:
                return rename(command, updateAdapter);
            case SET:
                return set(command, updateAdapter);
            case UNSET:
                return unset(command, updateAdapter);
            default:
                throw new InvalidParseOperationException(
                    "Invalid command: " + refCommand);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append(request.toString()).append("\n\tLastLevel: ").append(lastDepth).toString();
    }

    @Override
    public Update getRequest() {
        return (Update) request;
    }
}

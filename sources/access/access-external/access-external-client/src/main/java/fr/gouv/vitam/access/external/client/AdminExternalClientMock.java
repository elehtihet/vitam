package fr.gouv.vitam.access.external.client;

import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.stream.StreamUtils;

/**
 * Mock client implementation for Admin External
 */
public class AdminExternalClientMock extends AbstractMockClient implements AdminExternalClient {
    private static final String COLLECTION_NOT_VALID = "Collection not valid";


    @Override
    public Status checkDocuments(AdminCollections documentType, InputStream stream, Integer tenantId)
        throws AccessExternalClientNotFoundException, AccessExternalClientException {
        StreamUtils.closeSilently(stream);
        if (AdminCollections.RULES.equals(documentType) || AdminCollections.FORMATS.equals(documentType)) {
            return Status.OK;
        }
        throw new AccessExternalClientNotFoundException(COLLECTION_NOT_VALID);
    }

    @Override
    public Status createDocuments(AdminCollections documentType, InputStream stream, Integer tenantId)
        throws AccessExternalClientNotFoundException, AccessExternalClientException {
        StreamUtils.closeSilently(stream);
        return Status.CREATED;
    }

    @Override
    public RequestResponse findDocuments(AdminCollections documentType, JsonNode select, Integer tenantId)
        throws AccessExternalClientNotFoundException, AccessExternalClientException, InvalidParseOperationException {
        if (AdminCollections.RULES.equals(documentType)) {
            return ClientMockResultHelper.getRuleList();
        }
        if (AdminCollections.FORMATS.equals(documentType)) {
            return ClientMockResultHelper.getFormatList();
        }
        throw new AccessExternalClientNotFoundException(COLLECTION_NOT_VALID);
    }

    @Override
    public RequestResponse findDocumentById(AdminCollections documentType, String documentId, Integer tenantId)
        throws AccessExternalClientException, InvalidParseOperationException {
        if (AdminCollections.RULES.equals(documentType)) {
            return ClientMockResultHelper.getRule();
        }
        if (AdminCollections.FORMATS.equals(documentType)) {
            return ClientMockResultHelper.getFormat();
        }
        throw new AccessExternalClientNotFoundException(COLLECTION_NOT_VALID);
    }

    @Override
    public RequestResponse importContracts(InputStream contracts, Integer tenantId, AdminCollections collection)
        throws InvalidParseOperationException {
        if (AdminCollections.ACCESS_CONTRACTS.equals(collection))
            return ClientMockResultHelper.createReponse(ClientMockResultHelper.getAccessContracts().toJsonNode());
        else
            return ClientMockResultHelper.createReponse(ClientMockResultHelper.getIngestContracts().toJsonNode());

    }

    @Override
    public RequestResponse updateAccessContract(JsonNode queryDsl, Integer tenantId)
        throws InvalidParseOperationException, AccessExternalClientException {
        return ClientMockResultHelper.createReponse(ClientMockResultHelper.getAccessContracts().toJsonNode());
    }

    @Override
    public RequestResponse updateIngestContract(JsonNode queryDsl, Integer tenantId)
        throws InvalidParseOperationException, AccessExternalClientException {
        return ClientMockResultHelper.createReponse(ClientMockResultHelper.getIngestContracts().toJsonNode());
    }

}

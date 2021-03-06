/**
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
 */
package fr.gouv.vitam.storage.offers.common.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

public class OfferLogDatabaseServiceTest {

    private static final String DATABASE_NAME = "Vitam-test";

    @Rule
    public MongoRule mongoRule = new MongoRule(VitamCollection.getMongoClientOptions(), DATABASE_NAME,
        OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME);

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private OfferSequenceDatabaseService offerSequenceDatabaseService;

    private OfferLogDatabaseService offerLogDatabaseService;

    @Before
    public void setUp() {
        offerLogDatabaseService =
            new OfferLogDatabaseService(offerSequenceDatabaseService, mongoRule.getMongoDatabase());
    }

    @Test
    public void should_increment_sequence_when_save_twice_the_same_document()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        when(offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID))
            .thenReturn(1L)
            .thenReturn(2L);
        // when
        offerLogDatabaseService.save("object_0", "object_name_0.json", "write");
        offerLogDatabaseService.save("object_0", "object_name_0.json", "write");
        // then
        verify(offerSequenceDatabaseService, Mockito.times(2))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);

        Document firstOfferLog = mongoRule.getMongoCollection(OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME)
            .find(Filters.and(Filters.eq("FileName", "object_name_0.json"), Filters.eq("Sequence", 1L)))
            .first();
        assertThat(firstOfferLog.get("FileName")).isEqualTo("object_name_0.json");
        assertThat(firstOfferLog.get("Sequence")).isEqualTo(1);
        assertThat(firstOfferLog.get("Container")).isEqualTo("object_0");
        Document secondOfferLog = mongoRule.getMongoCollection(OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME)
            .find(Filters.and(Filters.eq("FileName", "object_name_0.json"), Filters.eq("Sequence", 2L)))
            .first();
        assertThat(secondOfferLog.get("FileName")).isEqualTo("object_name_0.json");
        assertThat(secondOfferLog.get("Sequence")).isEqualTo(2);
        assertThat(secondOfferLog.get("Container")).isEqualTo("object_0");
    }

    @Test
    public void should_increment_sequence_when_save_two_different_container_documents()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        when(offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID))
            .thenReturn(1L)
            .thenReturn(2L);
        // when
        offerLogDatabaseService.save("object_0", "object_name_0.json", "write");
        offerLogDatabaseService.save("object_1", "object_name_1.json", "write");
        // then
        verify(offerSequenceDatabaseService, Mockito.times(2))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);

        Document firstOfferLog = mongoRule.getMongoCollection(OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME)
            .find(Filters.and(Filters.eq("FileName", "object_name_0.json"), Filters.eq("Sequence", 1L)))
            .first();
        assertThat(firstOfferLog.get("FileName")).isEqualTo("object_name_0.json");
        assertThat(firstOfferLog.get("Sequence")).isEqualTo(1);
        assertThat(firstOfferLog.get("Container")).isEqualTo("object_0");

        Document secondOfferLog = mongoRule.getMongoCollection(OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME)
            .find(Filters.and(Filters.eq("FileName", "object_name_1.json"), Filters.eq("Sequence", 2L)))
            .first();
        assertThat(secondOfferLog.get("FileName")).isEqualTo("object_name_1.json");
        assertThat(secondOfferLog.get("Sequence")).isEqualTo(2);
        assertThat(secondOfferLog.get("Container")).isEqualTo("object_1");
    }

    @Test
    public void should_sequence_valid_when_save_with_long_sequence()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        long longSequence = Integer.MAX_VALUE + 1L;
        when(offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID))
            .thenReturn(longSequence);
        // when
        offerLogDatabaseService.save("object_0", "object_name_0.json", "write");
        // then
        verify(offerSequenceDatabaseService, Mockito.times(1))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);

        Document firstOfferLog = mongoRule.getMongoCollection(OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME)
            .find(Filters.and(Filters.eq("FileName", "object_name_0.json"))).first();
        assertThat(firstOfferLog.get("FileName")).isEqualTo("object_name_0.json");
        assertThat(firstOfferLog.get("Sequence")).isEqualTo(longSequence);
        assertThat(firstOfferLog.get("Container")).isEqualTo("object_0");
    }


    @Test
    public void should_throw_ContentAddressableStorageDatabaseException_on_save_when_mongo_throws_MongoException()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        MongoDatabase mongoDatabase = Mockito.mock(MongoDatabase.class);
        MongoCollection<Document> mongoCollection = Mockito.mock(MongoCollection.class);
        Mockito.when(mongoDatabase.getCollection(Mockito.any())).thenReturn(mongoCollection);
        Mockito.doThrow(new MongoException("mongo error")).when(mongoCollection).insertOne(Mockito.any(Document.class));
        offerLogDatabaseService = new OfferLogDatabaseService(offerSequenceDatabaseService, mongoDatabase);

        // when + then
        assertThatCode(() -> {
            offerLogDatabaseService.save("object_0", "object_name_0.json", "write");
        }).isInstanceOf(ContentAddressableStorageDatabaseException.class);
    }

    @Test
    public void should_get_two_document_when_get_offer_log_from_2_limit_2_ASC()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        when(offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID))
            .thenReturn(1L)
            .thenReturn(2L).thenReturn(3L).thenReturn(4L).thenReturn(5L);
        offerLogDatabaseService.save("object_0", "object_name_0.json", "write");
        offerLogDatabaseService.save("object_0", "object_name_1.json", "write");
        offerLogDatabaseService.save("object_0", "object_name_2.json", "write");
        offerLogDatabaseService.save("object_0", "object_name_3.json", "write");
        offerLogDatabaseService.save("object_0", "object_name_4.json", "write");
        // when
        List<OfferLog> offerLogs = offerLogDatabaseService.searchOfferLog("object_0", 1L, 2, Order.ASC);
        // then
        verify(offerSequenceDatabaseService, Mockito.times(5))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        assertThat(offerLogs.size()).isEqualTo(2);
        assertThat(offerLogs.get(0)).isNotNull();
        assertThat(offerLogs.get(0).getSequence()).isEqualTo(1L);
        assertThat(offerLogs.get(1)).isNotNull();
        assertThat(offerLogs.get(1).getSequence()).isEqualTo(2L);
    }

    @Test
    public void should_get_two_document_when_get_offer_log_from_2_limit_2_DESC()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        when(offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID))
            .thenReturn(1L)
            .thenReturn(2L).thenReturn(3L).thenReturn(4L).thenReturn(5L);
        offerLogDatabaseService.save("object_0", "object_name_0.json", "write");
        offerLogDatabaseService.save("object_0", "object_name_1.json", "write");
        offerLogDatabaseService.save("object_0", "object_name_2.json", "write");
        offerLogDatabaseService.save("object_0", "object_name_3.json", "write");
        offerLogDatabaseService.save("object_0", "object_name_4.json", "write");
        // when
        List<OfferLog> offerLogs = offerLogDatabaseService.searchOfferLog("object_0", 2L, 2, Order.DESC);
        // then
        verify(offerSequenceDatabaseService, Mockito.times(5))
            .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID);
        assertThat(offerLogs.size()).isEqualTo(2);
        assertThat(offerLogs.get(0)).isNotNull();
        assertThat(offerLogs.get(0).getSequence()).isEqualTo(2L);
        assertThat(offerLogs.get(1)).isNotNull();
        assertThat(offerLogs.get(1).getSequence()).isEqualTo(1L);
    }

    @Test
    public void should_have_parse_error_when_document_invalid_time()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        Document documentInvalid = new Document();
        documentInvalid.put("Sequence", 1L);
        documentInvalid.put("FileName", "object_name_0.json");
        documentInvalid.put("Container", "object_0");
        documentInvalid.put("Time", "object_0");
        mongoRule.getMongoCollection(OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME).insertOne(documentInvalid);

        // when + then
        assertThatCode(() -> {
            offerLogDatabaseService.searchOfferLog("object_0", 0L, 1, Order.ASC);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    @Test
    public void should_throw_ContentAddressableStorageDatabaseException_on_getOfferLog_when_mongo_throws_MongoException()
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        // given
        MongoDatabase mongoDatabase = Mockito.mock(MongoDatabase.class);
        MongoCollection<Document> mongoCollection = Mockito.mock(MongoCollection.class);
        Mockito.when(mongoDatabase.getCollection(Mockito.any())).thenReturn(mongoCollection);
        Mockito.when(mongoCollection.find(Mockito.any(Bson.class))).thenThrow(new MongoException("mongo error"));
        offerLogDatabaseService = new OfferLogDatabaseService(offerSequenceDatabaseService, mongoDatabase);

        // when + then
        assertThatCode(() -> {
            offerLogDatabaseService.searchOfferLog("object_0", 0L, 1, Order.ASC);
        }).isInstanceOf(ContentAddressableStorageDatabaseException.class);
    }
}

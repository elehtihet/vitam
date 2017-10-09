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
package fr.gouv.vitam.common.mapping.dip;

import static org.assertj.core.api.Assertions.assertThat;

import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.vitam.common.mapping.dip.ArchiveUnitMapper;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import org.junit.Test;

public class ArchiveUnitMapperTest {

    @Test
    public void should_map_unit_with_empty_fields() throws Exception {
        //Given
        ArchiveUnitMapper archiveUnitMapper = new ArchiveUnitMapper();
        ArchiveUnitModel archiveUnitModel = new ArchiveUnitModel();
        archiveUnitModel.setId("1234564");
        archiveUnitModel.setDescriptiveMetadataModel(new DescriptiveMetadataModel());
        archiveUnitModel.getManagement().setStorage(new RuleCategoryModel());

        // When
        ArchiveUnitType archiveUnitType = archiveUnitMapper.map(archiveUnitModel);

        // Then
        assertThat(archiveUnitType.getId()).isEqualTo("1234564");
    }

}
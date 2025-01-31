/********************************************************************************
 * Copyright (c) 2021-2023 Robert Bosch Manufacturing Solutions GmbH
 * Copyright (c) 2021-2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.tractusx.semantics.registry.repository;

import org.eclipse.tractusx.semantics.registry.model.Shell;
import org.eclipse.tractusx.semantics.registry.model.projection.ShellMinimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ShellRepository extends JpaRepository<Shell, UUID>, JpaSpecificationExecutor<Shell> {
   Optional<Shell> findByIdExternal( @Param( "idExternal" ) String idExternal);

   boolean existsByIdShort(@Param( "idShort" ) String idShort);

   @Query( value = "select * from shell s " +
         "where s.id_external = :idExternal and (" +
         ":tenantId = :owningTenantId " +
         "or s.id in (" +
            "select si.fk_shell_id from shell_identifier si where exists (" +
            "select sider.ref_key_value from SHELL_IDENTIFIER_EXTERNAL_SUBJECT_REFERENCE_KEY sider " +
            "where (sider.ref_key_value = :tenantId " +
            "or (sider.ref_key_value = :publicWildcardPrefix and si.namespace in (:publicWildcardAllowedTypes) )) " +
            "and sider.FK_SI_EXTERNAL_SUBJECT_REFERENCE_ID="+
            "(select sies.id from SHELL_IDENTIFIER_EXTERNAL_SUBJECT_REFERENCE sies where sies.FK_SHELL_IDENTIFIER_EXTERNAL_SUBJECT_ID=si.id)"+
            "))" +
         ")",nativeQuery = true )
   Optional<Shell> findByIdExternalAndExternalSubjectId( @Param( "idExternal" ) String idExternal,
         @Param("tenantId") String tenantId,
         @Param("owningTenantId") String owningTenantId,
         @Param ("publicWildcardPrefix") String publicWildcardPrefix,
         @Param ("publicWildcardAllowedTypes") List<String> publicWildcardAllowedTypes);

   @Query( "SELECT new org.eclipse.tractusx.semantics.registry.model.projection.ShellMinimal(s.id,s.createdDate) FROM Shell s where s.idExternal = :idExternal" )
   Optional<ShellMinimal> findMinimalRepresentationByIdExternal(@Param("idExternal") String idExternal );

    List<Shell> findShellsByIdExternalIsIn(Set<String> idExternals);

    /**
     * Returns external shell ids for the given keyValueCombinations.
     * External shell ids matching the conditions below are returned:
     *   - specificAssetIds match exactly the keyValueCombinations
     *   - if externalSubjectId (tenantId) is not null it must match the tenantId
     *
     *
     * To be able to properly index the key and value conditions, the query does not use any functions.
     * Computed indexes cannot be created for mutable functions like CONCAT in Postgres.
     *
     * @param keyValueCombinations the keys values to search for as tuples
     * @param keyValueCombinationsSize the size of the key value combinations
     * @return external shell ids for the given key value combinations
     */
    @Query( value = "SELECT s.id_external FROM shell s JOIN shell_identifier si ON s.id = si.fk_shell_id  " +
          " WHERE CONCAT(si.namespace, si.identifier) IN (:keyValueCombinations) AND (:tenantId = :owningTenantId OR si.namespace = :globalAssetId   " +
          " OR EXISTS ( SELECT 1 FROM SHELL_IDENTIFIER_EXTERNAL_SUBJECT_REFERENCE_KEY sider JOIN SHELL_IDENTIFIER_EXTERNAL_SUBJECT_REFERENCE sies ON sider.FK_SI_EXTERNAL_SUBJECT_REFERENCE_ID = sies.id " +
          " WHERE ( sider.ref_key_value = :tenantId OR (sider.ref_key_value = :publicWildcardPrefix AND si.namespace IN (:publicWildcardAllowedTypes))) " +
          "AND sies.FK_SHELL_IDENTIFIER_EXTERNAL_SUBJECT_ID = si.id  ) ) "+
          "GROUP BY s.id_external HAVING COUNT(*) = :keyValueCombinationsSize"
          ,nativeQuery = true )
    List<String> findExternalShellIdsByIdentifiersByExactMatch(@Param("keyValueCombinations") List<String> keyValueCombinations,
                                                   @Param("keyValueCombinationsSize") int keyValueCombinationsSize,
                                                   @Param("tenantId") String tenantId,
                                                   @Param ("publicWildcardPrefix") String publicWildcardPrefix,
                                                   @Param ("publicWildcardAllowedTypes") List<String> publicWildcardAllowedTypes,
                                                   @Param("owningTenantId") String owningTenantId,
                                                   @Param("globalAssetId") String globalAssetId);

    /**
     * Returns external shell ids for the given keyValueCombinations.
     * External shell ids that match any keyValueCombinations are returned.
     *
     * To be able to properly index the key and value conditions, the query does not use any functions.
     * Computed indexes cannot be created for mutable functions like CONCAT in Postgres.
     *
     * @param keyValueCombinations the keys values to search for as tuples
     * @return external shell ids for the given key value combinations
     */
    @Query( value = "select s.id_external from shell s where s.id in (" +
          "select si.fk_shell_id from shell_identifier si " +
          "where concat(si.namespace,si.identifier) in (:keyValueCombinations) " +
          "and (:tenantId = :owningTenantId or si.namespace= :globalAssetId or exists (" +
               "Select sider.ref_key_value from SHELL_IDENTIFIER_EXTERNAL_SUBJECT_REFERENCE_KEY sider where (sider.ref_key_value = :tenantId or (sider.ref_key_value = :publicWildcardPrefix and si.namespace in (:publicWildcardAllowedTypes) )) and sider.FK_SI_EXTERNAL_SUBJECT_REFERENCE_ID="+
          "(select sies.id from SHELL_IDENTIFIER_EXTERNAL_SUBJECT_REFERENCE sies where sies.FK_SHELL_IDENTIFIER_EXTERNAL_SUBJECT_ID=si.id)"+
          ")) group by si.fk_shell_id " +
          ")",nativeQuery = true )
    List<String> findExternalShellIdsByIdentifiersByAnyMatch(@Param("keyValueCombinations") List<String> keyValueCombinations,
                                                          @Param("tenantId") String tenantId,
                                                          @Param ("publicWildcardPrefix") String publicWildcardPrefix,
                                                          @Param ("publicWildcardAllowedTypes") List<String> publicWildcardAllowedTypes,
                                                          @Param("owningTenantId") String owningTenantId,
                                                          @Param("globalAssetId") String globalAssetId);
}

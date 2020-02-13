/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.openjpa;

import org.springframework.orm.jpa.DefaultJpaDialect;

/**
 * {@link org.springframework.orm.jpa.JpaDialect} implementation for Apache OpenJPA.
 * Developed and tested against OpenJPA 2.2.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 2.0
 */
@Deprecated // TODO: @aleks Spring 5 removed support for OpenJPA! see https://github.com/apache/syncope/commit/17a8eb5d99a84608a64927b00e7d9352d640517e
public class OpenJpaDialect extends DefaultJpaDialect {

//    private static final long serialVersionUID = 2099118508988476959L;
//
//    @Override
//    public Object beginTransaction(final EntityManager entityManager, final TransactionDefinition definition)
//        throws PersistenceException, SQLException, TransactionException {
//
//        OpenJPAEntityManager openJpaEntityManager = getOpenJPAEntityManager(entityManager);
//
//        if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
//            // Pass custom isolation level on to OpenJPA's JDBCFetchPlan configuration
//            FetchPlan fetchPlan = openJpaEntityManager.getFetchPlan();
//            if (fetchPlan instanceof JDBCFetchPlan) {
//                IsolationLevel isolation = IsolationLevel.fromConnectionConstant(definition.getIsolationLevel());
//                ((JDBCFetchPlan) fetchPlan).setIsolation(isolation);
//            }
//        }
//
//        entityManager.getTransaction().begin();
//
//        if (!definition.isReadOnly()) {
//            // Like with EclipseLink, make sure to start the logic transaction early so that other
//            // participants using the connection (such as JdbcTemplate) run in a transaction.
//            openJpaEntityManager.beginStore();
//        }
//
//        // Custom implementation for OpenJPA savepoint handling
//        return new OpenJpaTransactionData(openJpaEntityManager);
//    }
//
//    @Override
//    public ConnectionHandle getJdbcConnection(final EntityManager entityManager, final boolean readOnly)
//        throws PersistenceException, SQLException {
//
//        return new OpenJpaConnectionHandle(getOpenJPAEntityManager(entityManager));
//    }
//
//    /**
//     * Return the OpenJPA-specific variant of {@code EntityManager}.
//     *
//     * @param em the generic {@code EntityManager} instance
//     * @return the OpenJPA-specific variant of {@code EntityManager}
//     */
//    protected OpenJPAEntityManager getOpenJPAEntityManager(final EntityManager em) {
//        return OpenJPAPersistence.cast(em);
//    }
//
//    /**
//     * Transaction data Object exposed from {@code beginTransaction},
//     * implementing the {@link SavepointManager} interface.
//     */
//    private static class OpenJpaTransactionData implements SavepointManager {
//
//        private final OpenJPAEntityManager entityManager;
//
//        private int savepointCounter = 0;
//
//        OpenJpaTransactionData(final OpenJPAEntityManager entityManager) {
//            this.entityManager = entityManager;
//        }
//
//        @Override
//        public Object createSavepoint() throws TransactionException {
//            this.savepointCounter++;
//            String savepointName = ConnectionHolder.SAVEPOINT_NAME_PREFIX + this.savepointCounter;
//            this.entityManager.setSavepoint(savepointName);
//            return savepointName;
//        }
//
//        @Override
//        public void rollbackToSavepoint(final Object savepoint) throws TransactionException {
//            this.entityManager.rollbackToSavepoint((String) savepoint);
//        }
//
//        @Override
//        public void releaseSavepoint(final Object savepoint) throws TransactionException {
//            try {
//                this.entityManager.releaseSavepoint((String) savepoint);
//            } catch (Throwable ex) {
//                LogFactory.getLog(OpenJpaTransactionData.class).debug(
//                    "Could not explicitly release OpenJPA savepoint", ex);
//            }
//        }
//    }
//
//    /**
//     * {@link ConnectionHandle} implementation that fetches a new OpenJPA-provided
//     * Connection for every {@code getConnection} call and closes the Connection on
//     * {@code releaseConnection}. This is necessary because OpenJPA requires the
//     * fetched Connection to be closed before continuing EntityManager work.
//     *
//     * @see org.apache.openjpa.persistence.OpenJPAEntityManager#getConnection()
//     */
//    private static class OpenJpaConnectionHandle implements ConnectionHandle {
//
//        private final OpenJPAEntityManager entityManager;
//
//        OpenJpaConnectionHandle(final OpenJPAEntityManager entityManager) {
//            this.entityManager = entityManager;
//        }
//
//        @Override
//        public Connection getConnection() {
//            return (Connection) this.entityManager.getConnection();
//        }
//
//        @Override
//        public void releaseConnection(final Connection con) {
//            JdbcUtils.closeConnection(con);
//        }
//    }
}

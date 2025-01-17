/*
 * Copyright (c) 2022. Cloudera, Inc. All Rights Reserved
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.cloudera.utils.hive.sre;

import com.cloudera.utils.hive.config.DBStore;
import com.cloudera.utils.hive.config.SreProcessesConfig;
import com.cloudera.utils.hive.dba.QueryStore;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionPools {
    private SreProcessesConfig config;

    private PoolingDataSource<PoolableConnection> metastoreDirectDataSource = null;
    private PoolingDataSource<PoolableConnection> hs2DataSource = null;
    private PoolingDataSource<PoolableConnection> queryAnalysisDataSource = null;

    public ConnectionPools(SreProcessesConfig config) {
        this.config = config;
    }

    public void init() {
        initMetastoreDataSource();
        initQueryAnalysisDataSource();
        initHs2DataSource();
    }

    public DataSource getMetastoreDirectDataSource() {
        if (metastoreDirectDataSource == null) {
            initMetastoreDataSource();
        }
        return metastoreDirectDataSource;
    }

    public PoolingDataSource<PoolableConnection> getQueryAnalysisDataSource() {
        if (queryAnalysisDataSource == null) {
            initQueryAnalysisDataSource();
        }
        return queryAnalysisDataSource;
    }

    public DataSource getHs2DataSource() {
        if (hs2DataSource == null) {
            initHs2DataSource();
        }
        return hs2DataSource;
    }

    public Connection getMetastoreDirectConnection() throws SQLException {
        Connection conn = null;
        if (getMetastoreDirectDataSource() != null) {
            conn = getMetastoreDirectDataSource().getConnection();
            if (this.config.getMetastoreDirect().getInitSql() != null) {
                conn.createStatement().execute(this.config.getMetastoreDirect().getInitSql());
            }
        }
        return conn;
    }

    public Connection getQueryAnalysisConnection() throws SQLException {
        Connection conn = null;
        if (getQueryAnalysisDataSource() != null) {
            conn = getQueryAnalysisDataSource().getConnection();
            if (this.config.getMetastoreDirect().getInitSql() != null) {
                conn.createStatement().execute(this.config.getMetastoreDirect().getInitSql());
            }
        }
        return conn;
    }

    public Connection getHs2Connection() throws SQLException {
        Connection conn = null;
        if (getHs2DataSource() != null) {
            conn = getHs2DataSource().getConnection();
        }
        return conn;
    }

    protected void initMetastoreDataSource() {
        // Metastore Direct
        if (config.getMetastoreDirect() != null) {
            DBStore msdb = config.getMetastoreDirect();
            ConnectionFactory msconnectionFactory =
                    new DriverManagerConnectionFactory(msdb.getUri(), msdb.getConnectionProperties());

            PoolableConnectionFactory mspoolableConnectionFactory =
                    new PoolableConnectionFactory(msconnectionFactory, null);

            ObjectPool<PoolableConnection> msconnectionPool =
                    new GenericObjectPool<>(mspoolableConnectionFactory);

            mspoolableConnectionFactory.setPool(msconnectionPool);

            this.metastoreDirectDataSource =
                    new PoolingDataSource<>(msconnectionPool);
        }
    }

    protected void initHs2DataSource() {
        // this is optional.
        if (config.getHs2() != null) {
            DBStore hs2db = config.getHs2();
            ConnectionFactory hs2connectionFactory =
                    new DriverManagerConnectionFactory(hs2db.getUri(), hs2db.getConnectionProperties());

            PoolableConnectionFactory hs2poolableConnectionFactory =
                    new PoolableConnectionFactory(hs2connectionFactory, null);

            ObjectPool<PoolableConnection> hs2connectionPool =
                    new GenericObjectPool<>(hs2poolableConnectionFactory);

            hs2poolableConnectionFactory.setPool(hs2connectionPool);

            this.hs2DataSource =
                    new PoolingDataSource<>(hs2connectionPool);
        }
    }

    protected void initQueryAnalysisDataSource() {
        // Query Analysis
        if (config.getQueryAnalysis() != null) {

            QueryStore queryAnalysis = config.getQueryAnalysis();
            ConnectionFactory qaconnectionFactory =
                    new DriverManagerConnectionFactory(queryAnalysis.getUri(), queryAnalysis.getConnectionProperties());

            PoolableConnectionFactory qapoolableConnectionFactory =
                    new PoolableConnectionFactory(qaconnectionFactory, null);

            ObjectPool<PoolableConnection> qaconnectionPool =
                    new GenericObjectPool<>(qapoolableConnectionFactory);

            qapoolableConnectionFactory.setPool(qaconnectionPool);

            this.queryAnalysisDataSource =
                    new PoolingDataSource<>(qaconnectionPool);
        }

    }

}

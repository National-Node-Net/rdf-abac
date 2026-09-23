// SPDX-License-Identifier: Apache-2.0
// Originally developed by Telicent Ltd.; subsequently adapted, enhanced, and maintained by the National Digital Twin Programme.
/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/*
 *  Modifications made by the National Digital Twin Programme (NDTP)
 *  © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
 */


package uk.gov.dbt.ndtp.jena.pbac.labels;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.sparql.core.Transactional;
import org.rocksdb.*;

/**
 * An implementation of {@link Transactional} used by the {@code RocksDB} label
 * store.
 * <p>
 * Within a transaction, a {@code put()} is appended to a {@link WriteBatch} At the
 * end of a transaction, on {@code commit()}, the current write batch is flushed to
 * the database, and then cleared for re-use.
 * <p>
 * As a consequences, the WriteBatch is not visible to to "read" operations such as
 * {@link LabelsStore#labelsForTriples}. In other words, there is no read-after-write
 * within write transaction.
 */
public class TransactionalRocksDB implements Transactional {

    private final RocksDB db;
    private final WriteBatch writeBatch;

    // Type of the transaction.
    private ThreadLocal<Optional<TxnType>> txnType = ThreadLocal.withInitial(()->Optional.empty());
    // Current mode of the transaction.
    // This is Optional.empty outside a transaction.
    private ThreadLocal<Optional<ReadWrite>> txnMode = ThreadLocal.withInitial(()  ->Optional.empty());

    // Fixed for the lifetime of a transaction
    private Optional<TxnType> getThisTxnType() { return this.txnType.get(); }
    private void setThisTxnType(Optional<TxnType> txnType) {this.txnType.set(txnType); }

    private Optional<ReadWrite> getThisTxnMode() { return this.txnMode.get(); }
    private void setThisTxnMode(Optional<ReadWrite> txnMode) {this.txnMode.set(txnMode); }

    private static final ThreadLocal<WriteOptions> writeOptions = ThreadLocal.withInitial(WriteOptions::new);

    // Development helper.
    private static final boolean TRACE = false;
    private static final PrintStream out = System.out;
    private static void trace(String fmt, Object... args) {
        out.print("RocksLabels: ");
        out.printf(fmt, args);
        if ( ! fmt.endsWith("\n") )
            out.println();
    }
    // ----

    /*package*/ TransactionalRocksDB(RocksDB db) {
        this.db = db;
        this.writeBatch = new WriteBatch();
    }

    @Override
    public void begin(TxnType txnType) {
        if ( TRACE ) trace("begin(%s)", txnType);
        Objects.requireNonNull(txnType);

        Optional<TxnType> currentTxnType = getThisTxnType();
        if (currentTxnType.isPresent()) {
            throw new IllegalStateException("Transactional RocksDB begin() called within an existing "+currentTxnType.get()+" transaction");
        }

        if ( txnType == TxnType.READ_COMMITTED_PROMOTE )
            throw new IllegalArgumentException("Transactional RocksDB begin() : not supported: READ_COMMITTED_PROMOTE");
        setThisTxnType(Optional.of(txnType));
        ReadWrite mode = TxnType.initial(txnType);
        setThisTxnMode(Optional.of(mode));
    }

    @Override
    public void begin() {
        if ( TRACE ) trace("begin()");
        begin(TxnType.WRITE);
    }

    @Override
    public boolean promote(Promote promote) {
        if ( TRACE ) trace("promote(%s)",promote);

        switch(promote) {
            case ISOLATED :
            case READ_COMMITTED :
                break;
            default : throw new RuntimeException("Transactional RocksDB promote(): bad promote type: "+promote);
        }

        Optional<ReadWrite> optReadWrite = getThisTxnMode();

        if ( optReadWrite.isEmpty() )
            throw new RuntimeException("Transactional RocksDB promote(): not in a transaction");

        if ( optReadWrite.get() != ReadWrite.WRITE ) {
            // Convert to write mode.
            setThisTxnMode(Optional.of(ReadWrite.WRITE));
        }

        return true;
    }

    @Override
    public void commit() {
        if ( TRACE ) trace("commit()");
        getThisTxnMode().ifPresent(value -> {
            if (value == ReadWrite.WRITE) {
                try {
                    db.write(writeOptions.get(), writeBatch);
                    writeBatch.clear();
                } catch (RocksDBException e) {
                    throw new RuntimeException("Could not flush write batch to RocksDB label store", e);
                }
        }});
        setThisTxnMode(Optional.empty());
        setThisTxnType(Optional.empty());
        //endInternal();
    }

    @Override
    public void abort() {
        if ( TRACE ) trace("abort()");
        getThisTxnMode().ifPresent(value -> {
            if (value == ReadWrite.WRITE) {
                writeBatch.clear();
            }
        });
        setThisTxnMode(Optional.empty());
        setThisTxnType(Optional.empty());
        //endInternal();
    }

    @Override
    public void end() {
        if ( TRACE ) trace("end()");
        getThisTxnMode().ifPresent(value -> {
            if (value == ReadWrite.WRITE) {
                if ( TRACE ) trace("forced commit");
                this.commit();
            }
        });
        endInternal();
    }

    // Finalisation steps. This is safe to call multiple times per transaction.
    private void endInternal() {
        if ( getThisTxnMode().isEmpty() )
            return;
        setThisTxnMode(Optional.empty());
        setThisTxnType(Optional.empty());
        // Assume threads are not transient.
        // Otherwise, remove thread local intances.
//        txnMode.remove();
//        txnType.remove();
//        writeOptions.remove();
    }

    @Override
    public ReadWrite transactionMode() {
        return getThisTxnMode().orElse(null);
    }

    @Override
    public TxnType transactionType() {
        return getThisTxnType().orElse(null);
    }

    @Override
    public boolean isInTransaction() {
        return getThisTxnMode().isPresent();
    }

//    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void execute(Runnable action) {
        if ( TRACE ) trace("execute");
        if (isInTransaction()) {
            action.run();
        } else {
            begin();
            action.run();
            commit();
            end();
        }
    }

    public void merge(ColumnFamilyHandle columnFamilyHandle, ByteBuffer key, ByteBuffer value) {
        rocksOperation(AbstractWriteBatch::merge, columnFamilyHandle, key, value);
    }

    public void put(ColumnFamilyHandle columnFamilyHandle, ByteBuffer key, ByteBuffer value) {
        rocksOperation(AbstractWriteBatch::put, columnFamilyHandle, key, value);
    }

    public interface WriteOperation {
        public void write(WriteBatch writeBatch, ColumnFamilyHandle columnFamilyHandle, byte[] key, byte[] value)
            throws RocksDBException;
    }

    private void rocksOperation(WriteOperation op, ColumnFamilyHandle columnFamilyHandle, ByteBuffer key, ByteBuffer value) {
        var transactionExists = getThisTxnType().isPresent();
        if (!transactionExists)
            begin(TxnType.WRITE);

        Optional<ReadWrite> transactionMode = getThisTxnMode();

        if (transactionMode.isPresent() && transactionMode.get() == ReadWrite.READ ) {
            Optional<TxnType> transactionType = getThisTxnType();

            if (transactionType.isEmpty()) {
                throw new RuntimeException("Transactional RocksDB rocksOperation() called without a transaction type");
            }

            switch (transactionType.get()) {
                case READ -> throw new RuntimeException("Cannot promote READ transaction to write");
                case READ_PROMOTE -> promote(Promote.ISOLATED);
                case READ_COMMITTED_PROMOTE -> throw new RuntimeException("Promoting READ_COMMITTED_PROMOTE transaction to write is not supported");
                case WRITE -> {}
            };
        }

        try {
            byte[] k = new byte[key.limit() - key.position()];
            key.get(k);
            byte[] v = new byte[value.limit() - value.position()];
            value.get(v);
            op.write(writeBatch, columnFamilyHandle, k, v);
        } catch (RocksDBException e) {
            throw new RuntimeException("Could not write to write batch for RocksDB label store", e);
        }

        if (!transactionExists) {
            commit();
            end();
        }
    }
}

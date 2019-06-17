package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.DefaultClientRequest;
import com.gmail.woodyc40.pbft.message.ClientReply;
import com.gmail.woodyc40.pbft.message.ClientRequest;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DefaultClient<O, R, T> implements Client<O, R> {
    private final int tolerance;
    private final long timeoutMs;
    private final ClientCodec<T> codec;
    private final ClientTransport<T> transport;

    private final Map<Long, ClientTicket<O, R>> tickets = new HashMap<>();

    public DefaultClient(int tolerance,
                         long timeoutMs,
                         ClientCodec<T> codec,
                         ClientTransport<T> transport) {
        this.tolerance = tolerance;
        this.timeoutMs = timeoutMs;
        this.codec = codec;
        this.transport = transport;
    }

    @Override
    public int tolerance() {
        return this.tolerance;
    }

    @Override
    public long timeoutMs() {
        return this.timeoutMs;
    }

    private long nextTimestamp() {
        long timestamp = System.currentTimeMillis();
        while (this.tickets.containsKey(timestamp)) {
            timestamp++;
        }

        return timestamp;
    }

    @Override
    public ClientTicket<O, R> sendRequest(O operation) {
        long timestamp = this.nextTimestamp();
        ClientRequest<O> req = new DefaultClientRequest<>(operation, timestamp, this);

        int primaryId = this.transport.primaryId();
        T encodedRequest = this.codec.encodeRequest(req);
        this.transport.sendRequest(primaryId, encodedRequest);

        ClientTicket<O, R> ticket = new DefaultClientTicket<>(this, req);
        this.tickets.put(timestamp, ticket);

        return ticket;
    }

    public boolean checkTimeout(ClientTicket<O, R> ticket) {
        long now = System.currentTimeMillis();
        long start = ticket.dispatchTime();
        long elapsed = now - start;
        if (elapsed >= this.timeoutMs) {
            ticket.updateDispatchTime();

            ClientRequest<O> request = ticket.request();
            T encodedRequest = this.codec.encodeRequest(request);
            this.transport.multicastRequest(encodedRequest);

            return true;
        }

        return false;
    }

    @Override
    public @Nullable ClientTicket<O, R> recvReply(ClientReply<R> reply) {
        long timestamp = reply.timestamp();
        ClientTicket<O, R> ticket = this.tickets.get(timestamp);
        if (ticket == null) {
            return null;
        }

        int viewNumber = reply.viewNumber();
        int primaryId = viewNumber % this.transport.countKnownReplicas();
        this.transport.setPrimaryId(primaryId);

        int replicaId = reply.replicaId();
        R result = reply.result();
        ticket.recvResult(replicaId, result, this.tolerance);

        CompletableFuture<R> future = ticket.result();
        if (future.isDone()) {
            this.tickets.remove(timestamp);
        }

        return ticket;
    }

    @Override
    public ClientCodec<T> codec() {
        return this.codec;
    }

    @Override
    public ClientTransport<T> transport() {
        return this.transport;
    }
}

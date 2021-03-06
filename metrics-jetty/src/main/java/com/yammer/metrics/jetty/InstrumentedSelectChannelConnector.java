package com.yammer.metrics.jetty;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class InstrumentedSelectChannelConnector extends SelectChannelConnector {
    private Timer duration;
    private Meter accepts, connects, disconnects;
    private Counter connections;
    private final MetricsRegistry registry;

    public InstrumentedSelectChannelConnector(int port) {
        this(Metrics.defaultRegistry(), port);
    }

    public InstrumentedSelectChannelConnector(MetricsRegistry registry,
                                              int port) {
        super();
        setPort(port);
        this.registry = registry;
    }

    public InstrumentedSelectChannelConnector() {
        registry = Metrics.defaultRegistry();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        int port = getPort();
        this.duration = registry.newTimer(SelectChannelConnector.class,
                "connection-duration",
                Integer.toString(port),
                TimeUnit.MILLISECONDS,
                TimeUnit.SECONDS);
        this.accepts = registry.newMeter(SelectChannelConnector.class,
                "accepts",
                Integer.toString(port),
                "connections",
                TimeUnit.SECONDS);
        this.connects = registry.newMeter(SelectChannelConnector.class,
                "connects",
                Integer.toString(port),
                "connections",
                TimeUnit.SECONDS);
        this.disconnects = registry.newMeter(SelectChannelConnector.class,
                "disconnects",
                Integer.toString(port),
                "connections",
                TimeUnit.SECONDS);
        this.connections = registry.newCounter(SelectChannelConnector.class,
                "active-connections",
                Integer.toString(port));
    }

    @Override
    public void accept(int acceptorID) throws IOException {
        super.accept(acceptorID);
        accepts.mark();
    }

    @Override
    protected void connectionOpened(Connection connection) {
        connections.inc();
        super.connectionOpened(connection);
        connects.mark();
    }

    @Override
    protected void connectionClosed(Connection connection) {
        super.connectionClosed(connection);
        disconnects.mark();
        final long duration = System.currentTimeMillis() - connection.getTimeStamp();
        this.duration.update(duration, TimeUnit.MILLISECONDS);
        connections.dec();
    }
}

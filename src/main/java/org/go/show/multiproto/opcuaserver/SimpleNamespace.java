package org.go.show.multiproto.opcuaserver;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.items.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimpleNamespace extends ManagedNamespaceWithLifecycle {

    public static final String NAMESPACE_URI = "urn:example:simple-opcua-server";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Random random = new Random();
    private final SubscriptionModel subscriptionModel;
    private final ScheduledExecutorService scheduledExecutorService;
    private final DynamicNodeManager dynamicNodeManager;

    private UaVariableNode temperatureNode;
    private UaVariableNode pressureNode;
    private UaVariableNode humidityNode;

    public SimpleNamespace(OpcUaServer server) {
        super(server, NAMESPACE_URI);

        subscriptionModel = new SubscriptionModel(server, this);
        scheduledExecutorService = Executors.newScheduledThreadPool(2);
        dynamicNodeManager = new DynamicNodeManager(this);

        getLifecycleManager().addLifecycle(subscriptionModel);
        getLifecycleManager().addStartupTask(this::createAndAddNodes);
        getLifecycleManager().addShutdownTask(this::shutdownExecutorService);
    }

    public DynamicNodeManager getDynamicNodeManager() {
        return dynamicNodeManager;
    }

    public org.eclipse.milo.opcua.stack.core.NamespaceTable getNamespaceTable() {
        return getServer().getNamespaceTable();
    }

    public UaVariableNode getHelloWorldVariableNode(String path) {
        if (path == null) {
            return null;
        }

        switch (path) {
            case "HelloWorld/Temperature":
                return temperatureNode;
            case "HelloWorld/Pressure":
                return pressureNode;
            case "HelloWorld/Humidity":
                return humidityNode;
            default:
                return null;
        }
    }

    private void createAndAddNodes() {
        // Create a "HelloWorld" folder and add it to the node manager
        NodeId folderNodeId = newNodeId("HelloWorld");

        UaFolderNode folderNode = new UaFolderNode(
            getNodeContext(),
            folderNodeId,
            newQualifiedName("HelloWorld"),
            LocalizedText.english("HelloWorld"));

        getNodeManager().addNode(folderNode);

        // Make sure our new folder shows up under the server's Objects folder.
        folderNode.addReference(new Reference(
            folderNode.getNodeId(),
            NodeIds.Organizes,
            NodeIds.ObjectsFolder.expanded(),
            false));

        // Add some variables to our folder
        addVariableNodes(folderNode);
    }

    private void addVariableNodes(UaFolderNode rootNode) {
        // Temperature variable
        temperatureNode = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
            .setNodeId(newNodeId("HelloWorld/Temperature"))
            .setAccessLevel(AccessLevel.READ_WRITE)
            .setBrowseName(newQualifiedName("Temperature"))
            .setDisplayName(LocalizedText.english("Temperature"))
            .setDataType(NodeIds.Double)
            .setTypeDefinition(NodeIds.BaseDataVariableType)
            .build();

        temperatureNode.setValue(new DataValue(new Variant(25.0)));
        getNodeManager().addNode(temperatureNode);
        rootNode.addOrganizes(temperatureNode);

        // Pressure variable
        pressureNode = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
            .setNodeId(newNodeId("HelloWorld/Pressure"))
            .setAccessLevel(AccessLevel.READ_WRITE)
            .setBrowseName(newQualifiedName("Pressure"))
            .setDisplayName(LocalizedText.english("Pressure"))
            .setDataType(NodeIds.Double)
            .setTypeDefinition(NodeIds.BaseDataVariableType)
            .build();

        pressureNode.setValue(new DataValue(new Variant(1013.25)));
        getNodeManager().addNode(pressureNode);
        rootNode.addOrganizes(pressureNode);

        // Humidity variable
        humidityNode = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
            .setNodeId(newNodeId("HelloWorld/Humidity"))
            .setAccessLevel(AccessLevel.READ_WRITE)
            .setBrowseName(newQualifiedName("Humidity"))
            .setDisplayName(LocalizedText.english("Humidity"))
            .setDataType(NodeIds.Double)
            .setTypeDefinition(NodeIds.BaseDataVariableType)
            .build();

        humidityNode.setValue(new DataValue(new Variant(60.0)));
        getNodeManager().addNode(humidityNode);
        rootNode.addOrganizes(humidityNode);

        // Start updating values periodically
        scheduledExecutorService.scheduleAtFixedRate(this::updateVariableValues, 1, 1, TimeUnit.SECONDS);

        // 加载持久化的节点（延迟执行，确保Spring容器已完全初始化）
        scheduledExecutorService.schedule(() -> {
            try {
                dynamicNodeManager.loadPersistedNodes();
                logger.info("持久化节点加载完成");
            } catch (Exception e) {
                logger.error("加载持久化节点失败", e);
            }
        }, 2, TimeUnit.SECONDS);
    }

    private void updateVariableValues() {
        double temperature = 20.0 + random.nextDouble() * 10.0;
        setHelloWorldValue(temperatureNode, temperature);

        double pressure = 1000.0 + random.nextDouble() * 20.0;
        setHelloWorldValue(pressureNode, pressure);

        double humidity = 40.0 + random.nextDouble() * 40.0;
        setHelloWorldValue(humidityNode, humidity);
    }

    private void setHelloWorldValue(UaVariableNode node, double baseValue) {
        if (node == null) {
            return;
        }

        NodeId dataType = node.getDataType();
        Object value;

        if (NodeIds.Boolean.equals(dataType)) {
            value = random.nextBoolean();
        } else if (NodeIds.Int32.equals(dataType)) {
            value = (int) Math.round(baseValue);
        } else if (NodeIds.Int64.equals(dataType)) {
            value = Math.round(baseValue);
        } else if (NodeIds.String.equals(dataType)) {
            value = String.valueOf(baseValue);
        } else {
            value = baseValue;
        }

        node.setValue(new DataValue(new Variant(value)));
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    private void shutdownExecutorService() {
        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutorService.shutdownNow();
        }
    }
}

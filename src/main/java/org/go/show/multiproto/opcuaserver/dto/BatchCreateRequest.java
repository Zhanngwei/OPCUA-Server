package org.go.show.multiproto.opcuaserver.dto;

import java.util.List;
import java.util.Map;

/**
 * 批量创建节点请求DTO
 */
public class BatchCreateRequest {
    
    /**
     * 要创建的节点项目列表
     */
    private List<NodeItem> items;
    
    /**
     * 创建选项
     */
    private BatchOptions options;
    
    public BatchCreateRequest() {
        this.options = new BatchOptions(); // 设置默认选项
    }
    
    public List<NodeItem> getItems() {
        return items;
    }
    
    public void setItems(List<NodeItem> items) {
        this.items = items;
    }
    
    public BatchOptions getOptions() {
        return options;
    }
    
    public void setOptions(BatchOptions options) {
        this.options = options;
    }
    
    /**
     * 节点项目
     */
    public static class NodeItem {
        private String name;
        private String type;
        
        public NodeItem() {}
        
        public NodeItem(String name, String type) {
            this.name = name;
            this.type = type;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
    }
    
    /**
     * 批量创建选项
     */
    public static class BatchOptions {
        private boolean autoCreateFolders = true;
        private String folderSeparator = "_";
        private boolean skipExisting = true;
        private String defaultAccessLevel = "READ_WRITE";
        private String namespace = "http://localhost:8080/opcua";
        private String nodeIdPrefix = "ns=2;i=";
        private boolean validateNames = true;
        private Map<String, Object> defaultValues;
        
        public BatchOptions() {
            // 设置默认值
            this.defaultValues = Map.of(
                "int", 0,
                "float", 0.0f,
                "double", 0.0,
                "boolean", false,
                "string", ""
            );
        }
        
        public boolean isAutoCreateFolders() {
            return autoCreateFolders;
        }
        
        public void setAutoCreateFolders(boolean autoCreateFolders) {
            this.autoCreateFolders = autoCreateFolders;
        }
        
        public String getFolderSeparator() {
            return folderSeparator;
        }
        
        public void setFolderSeparator(String folderSeparator) {
            this.folderSeparator = folderSeparator;
        }
        
        public boolean isSkipExisting() {
            return skipExisting;
        }
        
        public void setSkipExisting(boolean skipExisting) {
            this.skipExisting = skipExisting;
        }
        
        public String getDefaultAccessLevel() {
            return defaultAccessLevel;
        }
        
        public void setDefaultAccessLevel(String defaultAccessLevel) {
            this.defaultAccessLevel = defaultAccessLevel;
        }
        
        public String getNamespace() {
            return namespace;
        }
        
        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
        
        public String getNodeIdPrefix() {
            return nodeIdPrefix;
        }
        
        public void setNodeIdPrefix(String nodeIdPrefix) {
            this.nodeIdPrefix = nodeIdPrefix;
        }
        
        public boolean isValidateNames() {
            return validateNames;
        }
        
        public void setValidateNames(boolean validateNames) {
            this.validateNames = validateNames;
        }
        
        public Map<String, Object> getDefaultValues() {
            return defaultValues;
        }
        
        public void setDefaultValues(Map<String, Object> defaultValues) {
            this.defaultValues = defaultValues;
        }
    }
}

package org.go.show.multiproto.opcuaserver.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量创建节点响应DTO
 */
public class BatchCreateResponse {
    
    private boolean success;
    private String message;
    private BatchResults results;
    private List<NodeResult> details;
    
    public BatchCreateResponse() {
        this.results = new BatchResults();
        this.details = new ArrayList<>();
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public BatchResults getResults() {
        return results;
    }
    
    public void setResults(BatchResults results) {
        this.results = results;
    }
    
    public List<NodeResult> getDetails() {
        return details;
    }
    
    public void setDetails(List<NodeResult> details) {
        this.details = details;
    }
    
    /**
     * 批量创建结果统计
     */
    public static class BatchResults {
        private int totalItems = 0;
        private int foldersCreated = 0;
        private int variablesCreated = 0;
        private int skipped = 0;
        private List<String> errors = new ArrayList<>();
        
        public int getTotalItems() {
            return totalItems;
        }
        
        public void setTotalItems(int totalItems) {
            this.totalItems = totalItems;
        }
        
        public int getFoldersCreated() {
            return foldersCreated;
        }
        
        public void setFoldersCreated(int foldersCreated) {
            this.foldersCreated = foldersCreated;
        }
        
        public int getVariablesCreated() {
            return variablesCreated;
        }
        
        public void setVariablesCreated(int variablesCreated) {
            this.variablesCreated = variablesCreated;
        }
        
        public int getSkipped() {
            return skipped;
        }
        
        public void setSkipped(int skipped) {
            this.skipped = skipped;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
        
        public void incrementFoldersCreated() {
            this.foldersCreated++;
        }
        
        public void incrementVariablesCreated() {
            this.variablesCreated++;
        }
        
        public void incrementSkipped() {
            this.skipped++;
        }
        
        public void addError(String error) {
            this.errors.add(error);
        }
    }
    
    /**
     * 单个节点创建结果
     */
    public static class NodeResult {
        private String name;
        private String status; // success, error, skipped
        private String folderPath;
        private String variablePath;
        private String nodeId;
        private String errorMessage;
        
        public NodeResult() {}
        
        public NodeResult(String name, String status) {
            this.name = name;
            this.status = status;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getFolderPath() {
            return folderPath;
        }
        
        public void setFolderPath(String folderPath) {
            this.folderPath = folderPath;
        }
        
        public String getVariablePath() {
            return variablePath;
        }
        
        public void setVariablePath(String variablePath) {
            this.variablePath = variablePath;
        }
        
        public String getNodeId() {
            return nodeId;
        }
        
        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}

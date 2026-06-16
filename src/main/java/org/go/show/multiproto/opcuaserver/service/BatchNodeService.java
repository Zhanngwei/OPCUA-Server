package org.go.show.multiproto.opcuaserver.service;

import org.go.show.multiproto.opcuaserver.DynamicNodeManager;
import org.go.show.multiproto.opcuaserver.dto.BatchCreateRequest;
import org.go.show.multiproto.opcuaserver.dto.BatchCreateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 批量节点创建服务
 */
@Service
public class BatchNodeService {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchNodeService.class);
    
    // 类型映射：前端类型 -> OPC UA类型
    private static final Map<String, String> TYPE_MAPPING = Map.of(
        "boolean", "Boolean",
        "int", "Integer",
        "long", "Long",
        "float", "Float",
        "double", "Double",
        "string", "String"
    );
    
    // 名称验证正则表达式
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    
    private DynamicNodeManager dynamicNodeManager;
    
    public void setDynamicNodeManager(DynamicNodeManager dynamicNodeManager) {
        this.dynamicNodeManager = dynamicNodeManager;
    }
    
    /**
     * 批量创建节点
     */
    public BatchCreateResponse batchCreateNodes(BatchCreateRequest request) {
        BatchCreateResponse response = new BatchCreateResponse();
        
        if (dynamicNodeManager == null) {
            response.setSuccess(false);
            response.setMessage("节点管理器未初始化");
            return response;
        }
        
        if (request.getItems() == null || request.getItems().isEmpty()) {
            response.setSuccess(false);
            response.setMessage("没有要创建的节点项目");
            return response;
        }
        
        logger.info("开始批量创建节点，总数: {}", request.getItems().size());
        
        response.getResults().setTotalItems(request.getItems().size());
        Set<String> createdFolders = new java.util.HashSet<>();
        
        // 处理每个节点项目
        for (BatchCreateRequest.NodeItem item : request.getItems()) {
            BatchCreateResponse.NodeResult nodeResult = processNodeItem(item, request.getOptions(), createdFolders);
            response.getDetails().add(nodeResult);
            
            // 更新统计信息
            switch (nodeResult.getStatus()) {
                case "success":
                    response.getResults().incrementVariablesCreated();
                    break;
                case "skipped":
                    response.getResults().incrementSkipped();
                    break;
                case "error":
                    response.getResults().addError(nodeResult.getName() + ": " + nodeResult.getErrorMessage());
                    break;
            }
        }
        
        // 设置文件夹创建数量
        response.getResults().setFoldersCreated(createdFolders.size());
        
        // 设置整体结果
        boolean hasErrors = !response.getResults().getErrors().isEmpty();
        response.setSuccess(!hasErrors || response.getResults().getVariablesCreated() > 0);
        
        if (hasErrors) {
            response.setMessage(String.format("批量创建完成，成功: %d, 跳过: %d, 错误: %d", 
                response.getResults().getVariablesCreated(),
                response.getResults().getSkipped(),
                response.getResults().getErrors().size()));
        } else {
            response.setMessage("批量创建成功完成");
        }
        
        logger.info("批量创建完成，文件夹: {}, 变量: {}, 跳过: {}, 错误: {}", 
            response.getResults().getFoldersCreated(),
            response.getResults().getVariablesCreated(),
            response.getResults().getSkipped(),
            response.getResults().getErrors().size());
        
        return response;
    }
    
    /**
     * 处理单个节点项目
     */
    private BatchCreateResponse.NodeResult processNodeItem(
            BatchCreateRequest.NodeItem item, 
            BatchCreateRequest.BatchOptions options,
            Set<String> createdFolders) {
        
        BatchCreateResponse.NodeResult result = new BatchCreateResponse.NodeResult(item.getName(), "error");
        
        try {
            // 验证名称格式
            if (options.isValidateNames() && !isValidName(item.getName())) {
                result.setErrorMessage("名称格式无效，只允许字母、数字和下划线");
                return result;
            }
            
            // 解析名称，提取文件夹和变量名
            ParsedName parsedName = parseName(item.getName(), options.getFolderSeparator());
            if (parsedName == null) {
                result.setErrorMessage("无法解析名称格式");
                return result;
            }
            
            result.setFolderPath(parsedName.folderName);
            result.setVariablePath(parsedName.folderName + "/" + parsedName.variableName);
            
            // 验证类型
            String opcuaType = TYPE_MAPPING.get(item.getType().toLowerCase());
            if (opcuaType == null) {
                result.setErrorMessage("不支持的数据类型: " + item.getType());
                return result;
            }
            
            // 检查变量是否已存在
            if (options.isSkipExisting() && dynamicNodeManager.nodeExists(result.getVariablePath())) {
                result.setStatus("skipped");
                result.setErrorMessage("节点已存在");
                return result;
            }
            
            // 创建文件夹（如果需要）
            if (options.isAutoCreateFolders()) {
                boolean folderCreated = ensureFolderExists(parsedName.folderName, createdFolders);
                if (!folderCreated) {
                    result.setErrorMessage("创建文件夹失败");
                    return result;
                }
            }
            
            // 创建变量
            Object defaultValue = getDefaultValue(item.getType(), options.getDefaultValues());
            boolean variableCreated = dynamicNodeManager.createVariable(
                result.getVariablePath(),
                parsedName.variableName, // 使用变量名作为显示名称
                defaultValue,
                opcuaType,
                options.getDefaultAccessLevel(),
                "numeric", // 使用数字型NodeId
                null // 自动生成NodeId
            );
            
            if (variableCreated) {
                result.setStatus("success");
                result.setNodeId("auto-generated"); // 实际NodeId需要从DynamicNodeManager获取
            } else {
                result.setErrorMessage("创建变量失败");
            }
            
        } catch (Exception e) {
            logger.error("处理节点项目失败: {}", item.getName(), e);
            result.setErrorMessage("处理失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 解析名称，提取文件夹名和变量名
     */
    private ParsedName parseName(String name, String separator) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        int separatorIndex = name.lastIndexOf(separator);
        if (separatorIndex <= 0 || separatorIndex >= name.length() - 1) {
            return null; // 没有找到分隔符或分隔符位置不正确
        }
        
        String folderName = name.substring(0, separatorIndex);
        String variableName = name.substring(separatorIndex + separator.length());
        
        return new ParsedName(folderName, variableName);
    }
    
    /**
     * 确保文件夹存在
     */
    private boolean ensureFolderExists(String folderPath, Set<String> createdFolders) {
        if (dynamicNodeManager.nodeExists(folderPath)) {
            return true; // 文件夹已存在
        }
        
        if (createdFolders.contains(folderPath)) {
            return true; // 本次批量操作中已创建
        }
        
        try {
            boolean created = dynamicNodeManager.createFolder(
                folderPath,
                folderPath, // 使用路径作为显示名称
                "numeric", // 使用数字型NodeId
                null // 自动生成NodeId
            );
            
            if (created) {
                createdFolders.add(folderPath);
                logger.debug("创建文件夹: {}", folderPath);
            }
            
            return created;
        } catch (Exception e) {
            logger.error("创建文件夹失败: {}", folderPath, e);
            return false;
        }
    }
    
    /**
     * 获取默认值
     */
    private Object getDefaultValue(String type, Map<String, Object> defaultValues) {
        Object value = defaultValues.get(type.toLowerCase());
        if (value != null) {
            return value;
        }
        
        // 如果没有配置默认值，使用类型默认值
        switch (type.toLowerCase()) {
            case "boolean":
                return false;
            case "int":
                return 0;
            case "long":
                return 0L;
            case "float":
                return 0.0f;
            case "double":
                return 0.0;
            case "string":
            default:
                return "";
        }
    }
    
    /**
     * 验证名称格式
     */
    private boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }
    
    /**
     * 解析后的名称
     */
    private static class ParsedName {
        final String folderName;
        final String variableName;
        
        ParsedName(String folderName, String variableName) {
            this.folderName = folderName;
            this.variableName = variableName;
        }
    }
}

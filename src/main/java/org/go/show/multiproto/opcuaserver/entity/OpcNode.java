package org.go.show.multiproto.opcuaserver.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * OPC UA节点实体类
 * 用于持久化存储OPC UA节点信息
 */
@Entity
@Table(name = "opc_nodes", indexes = {
    @Index(name = "idx_path", columnList = "path", unique = true),
    @Index(name = "idx_parent_path", columnList = "parent_path"),
    @Index(name = "idx_node_type", columnList = "node_type")
})
public class OpcNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 节点路径，唯一标识符
     */
    @Column(name = "path", length = 500, nullable = false, unique = true)
    @NotBlank(message = "节点路径不能为空")
    private String path;

    /**
     * 节点类型：folder 或 variable
     */
    @Column(name = "node_type", length = 20, nullable = false)
    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

    /**
     * 显示名称
     */
    @Column(name = "display_name", length = 200)
    private String displayName;

    /**
     * 数据类型（仅对变量节点有效）
     */
    @Column(name = "data_type", length = 50)
    private String dataType;

    /**
     * 节点ID
     */
    @Column(name = "node_id", length = 100)
    private String nodeId;

    /**
     * 父节点路径
     */
    @Column(name = "parent_path", length = 500)
    private String parentPath;

    /**
     * 访问级别（仅对变量节点有效）
     */
    @Column(name = "access_level", length = 20)
    private String accessLevel;

    /**
     * 当前值（仅对变量节点有效）
     */
    @Column(name = "current_value", columnDefinition = "TEXT")
    private String currentValue;

    /**
     * 值的数据类型（用于反序列化）
     */
    @Column(name = "value_type", length = 50)
    private String valueType;

    /**
     * 是否可写（仅对变量节点有效）
     */
    @Column(name = "writable")
    private Boolean writable;

    /**
     * 描述信息
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    @NotNull
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    @NotNull
    private LocalDateTime updatedAt;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    // 构造函数
    public OpcNode() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public OpcNode(String path, String nodeType, String displayName) {
        this();
        this.path = path;
        this.nodeType = nodeType;
        this.displayName = displayName;
    }

    // JPA生命周期回调
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public Boolean getWritable() {
        return writable;
    }

    public void setWritable(Boolean writable) {
        this.writable = writable;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "OpcNode{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", nodeType='" + nodeType + '\'' +
                ", displayName='" + displayName + '\'' +
                ", nodeId='" + nodeId + '\'' +
                '}';
    }
}

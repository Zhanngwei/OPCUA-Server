package org.go.show.multiproto.opcuaserver.repository;

import org.go.show.multiproto.opcuaserver.entity.OpcNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OPC UA节点数据访问接口
 */
@Repository
public interface OpcNodeRepository extends JpaRepository<OpcNode, Long> {

    /**
     * 根据路径查找节点
     */
    Optional<OpcNode> findByPath(String path);

    /**
     * 根据路径删除节点
     */
    void deleteByPath(String path);

    /**
     * 检查路径是否存在
     */
    boolean existsByPath(String path);

    /**
     * 根据父路径查找所有子节点
     */
    List<OpcNode> findByParentPathOrderByNodeTypeDescPathAsc(String parentPath);

    /**
     * 根据节点类型查找节点
     */
    List<OpcNode> findByNodeTypeOrderByPathAsc(String nodeType);

    /**
     * 查找所有启用的节点
     */
    List<OpcNode> findByEnabledTrueOrderByPathAsc();

    /**
     * 根据父路径和节点类型查找节点
     */
    List<OpcNode> findByParentPathAndNodeTypeOrderByPathAsc(String parentPath, String nodeType);

    /**
     * 查找所有文件夹节点
     */
    @Query("SELECT n FROM OpcNode n WHERE n.nodeType = 'folder' AND n.enabled = true ORDER BY n.path")
    List<OpcNode> findAllFolders();

    /**
     * 查找所有变量节点
     */
    @Query("SELECT n FROM OpcNode n WHERE n.nodeType = 'variable' AND n.enabled = true ORDER BY n.path")
    List<OpcNode> findAllVariables();

    /**
     * 根据路径前缀查找节点（用于查找某个路径下的所有子节点）
     */
    @Query("SELECT n FROM OpcNode n WHERE n.path LIKE :pathPrefix AND n.enabled = true ORDER BY n.path")
    List<OpcNode> findByPathStartingWith(@Param("pathPrefix") String pathPrefix);

    /**
     * 查找指定路径的直接子节点
     */
    @Query("SELECT n FROM OpcNode n WHERE n.parentPath = :parentPath AND n.enabled = true ORDER BY n.nodeType DESC, n.path ASC")
    List<OpcNode> findDirectChildren(@Param("parentPath") String parentPath);

    /**
     * 查找所有根节点（没有父路径的节点）
     */
    @Query("SELECT n FROM OpcNode n WHERE (n.parentPath IS NULL OR n.parentPath = '') AND n.enabled = true ORDER BY n.nodeType DESC, n.path ASC")
    List<OpcNode> findRootNodes();

    /**
     * 根据NodeId查找节点
     */
    Optional<OpcNode> findByNodeId(String nodeId);

    /**
     * 检查NodeId是否存在
     */
    boolean existsByNodeId(String nodeId);

    /**
     * 查找最大的数字型NodeId（用于生成新的NodeId）
     */
    @Query("SELECT MAX(CAST(SUBSTRING(n.nodeId, LOCATE('i=', n.nodeId) + 2) AS int)) FROM OpcNode n WHERE n.nodeId LIKE 'ns=2;i=%'")
    Integer findMaxNumericNodeId();

    /**
     * 统计节点总数
     */
    @Query("SELECT COUNT(n) FROM OpcNode n WHERE n.enabled = true")
    long countEnabledNodes();

    /**
     * 根据节点类型统计数量
     */
    @Query("SELECT COUNT(n) FROM OpcNode n WHERE n.nodeType = :nodeType AND n.enabled = true")
    long countByNodeType(@Param("nodeType") String nodeType);
}
